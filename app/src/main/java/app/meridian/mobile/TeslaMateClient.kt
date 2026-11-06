package app.meridian.mobile

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.time.Instant
import java.time.LocalDate
import java.util.Base64
import kotlin.coroutines.coroutineContext

internal class TeslaMateClient(
    private val base: String,
    private val username: String = "",
    private val password: String = "",
    private val gps: Boolean = true,
    private val read: ((String) -> JSONObject)? = null
) {
    private val snapshots = object:LinkedHashMap<String,Pair<Instant,List<JSONObject>>>(8,.75f,true) {
        override fun removeEldestEntry(eldest:MutableMap.MutableEntry<String,Pair<Instant,List<JSONObject>>>?)=size>6
    }
    private val mutex = Mutex()

    private suspend fun get(path: String): JSONObject = withContext(Dispatchers.IO) {
        coroutineContext.ensureActive()
        if (read != null) return@withContext read.invoke(path)
        val connection = URI(base + "/api/v1/" + path).toURL().openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = false
        connection.connectTimeout = 15000; connection.readTimeout = 30000
        connection.setRequestProperty("Accept", "application/json")
        if (username.isNotBlank() && password.isNotEmpty()) {
            val encoded = Base64.getEncoder().encodeToString("$username:$password".toByteArray(Charsets.UTF_8))
            connection.setRequestProperty("Authorization", "Basic $encoded")
        }
        try {
            val status = connection.responseCode
            if (status == 401 || status == 403) throw IllegalStateException("连接认证失败，请检查服务的访问凭据")
            if (status !in 200..299) throw HttpReadFailure(status)
            val bytes = connection.inputStream.use { it.readCapped(24 * 1024 * 1024) }
            coroutineContext.ensureActive()
            val response = try { JSONObject(bytes.toString(Charsets.UTF_8)) } catch (_: Exception) {
                throw IOException("TeslaMate 未返回有效数据")
            }
            if (response.optJSONObject("data") == null) throw IOException("TeslaMate 返回的数据结构无效")
            response
        } finally { connection.disconnect() }
    }

    private suspend fun settings(): TeslaMateUnits {
        val setting = get("globalsettings").getJSONObject("data").getJSONObject("settings")
        val raw = setting.getJSONObject("teslamate_units")
        return TeslaMateUnits(raw.getString("unit_of_length"), raw.getString("unit_of_temperature"),
            setting.optJSONObject("teslamate_webgui")?.optString("preferred_range", "rated") ?: "rated")
    }

    private fun carData(response: JSONObject, car: Long): JSONObject {
        val data = response.getJSONObject("data")
        require(data.getJSONObject("car").getLong("car_id") == car) { "服务返回了不匹配的车辆" }
        return data
    }

    private suspend fun records(car: Long, kind: String, configuredUnits:TeslaMateUnits, startDate: LocalDate? = null): List<JSONObject> = mutex.withLock {
        val key = "$car/$kind/${startDate ?: "all"}/${configuredUnits.preferredRange}"; val now = Instant.now()
        snapshots.entries.removeAll { !now.isBefore(it.value.first.plusSeconds(60)) }
        snapshots[key]?.takeIf { now.isBefore(it.first.plusSeconds(60)) }?.let { return@withLock it.second }
        val result = mutableListOf<JSONObject>(); val seen = hashSetOf<Long>()
        val idKey = if (kind == "drives") "drive_id" else "charge_id"
        val cutoff = URLEncoder.encode(now.toString(), "UTF-8")
        val lowerBound = startDate?.let { "&startDate=" + URLEncoder.encode(it.atStartOfDay(teslaMateZone).toInstant().toString(), "UTF-8") }.orEmpty()
        var previous: Instant? = null
        for (page in 1..200) {
            coroutineContext.ensureActive()
            val data = carData(get("cars/$car/$kind?page=$page&show=100&endDate=$cutoff$lowerBound"), car)
            require(data.has(kind)) { "服务未返回记录列表" }
            val array = if (data.isNull(kind)) JSONArray() else data.getJSONArray(kind)
            require(array.length() <= 100) { "服务返回的分页数量异常" }
            if (array.length() == 0) {
                snapshots[key] = now to result.toList()
                return@withLock result.toList()
            }
            val pageUnits=tmResponseUnits(data,configuredUnits)
            for (i in 0 until array.length()) {
                val row = array.getJSONObject(i); val id = row.getLong(idKey)
                require(id > 0 && seen.add(id)) { "记录分页发生变化，请重新读取" }
                val start = tmInstant(row.getString("start_date")); val end = tmInstant(row.getString("end_date"))
                require(!end.isBefore(start) && !end.isAfter(now) && previous?.let { !start.isAfter(it) } != false) { "记录时间或分页顺序异常" }
                previous = start; result.add(if(kind=="drives")tmDrive(row,pageUnits,gps)else tmCharge(row,pageUnits))
            }
        }
        throw IOException("记录超过单次读取上限，请选择日期范围后重试")
    }

    suspend fun call(path: String, body: JSONObject?, selectedCar: String): JSONObject {
        val u = settings()
        if (path == "/car/get") {
            val raw = get("cars").getJSONObject("data").getJSONArray("cars")
            return envelope(JSONArray((0 until raw.length()).map { tmCar(raw.getJSONObject(it)) }))
        }
        val car = (body?.opt("carId")?.toString() ?: selectedCar).toLongOrNull()?.takeIf { it > 0 }
            ?: throw IllegalArgumentException("请先选择车辆")
        return when (path) {
            "/dashboard/get" -> envelope(dashboard(car, u, body?.optString("statDate").orEmpty()))
            "/car/trip/get", "/car/charge/history" -> {
                val charging = path == "/car/charge/history"
                val startDate = body?.optString("startDate")?.takeIf { it.isNotBlank() }?.take(10)?.let { LocalDate.parse(it) }
                val mapped = enrichedRecords(car, if (charging) "charges" else "drives", u, startDate)
                envelope(tmRecordPage(mapped, charging, body ?: JSONObject()))
            }
            "/car/trip/getDetail" -> {
                val id = body?.getLong("tripId")?.takeIf { it > 0 } ?: throw IllegalArgumentException("缺少行程编号")
                val data=carData(get("cars/$car/drives/$id"),car)
                envelope(tmEnrich(tmDrive(data.getJSONObject("drive"),tmResponseUnits(data,u),gps),metadata(car,"drives")[id],false,gps))
            }
            "/car/charge/detail" -> {
                val id = body?.getLong("recordId")?.takeIf { it > 0 } ?: throw IllegalArgumentException("缺少充电编号")
                val data=carData(get("cars/$car/charges/$id"),car)
                envelope(tmEnrich(tmCharge(data.getJSONObject("charge"),tmResponseUnits(data,u)),metadata(car,"charges")[id],true,gps))
            }
            "/car/trip/routeComparison" -> {val rows=enrichedRecords(car,"drives",u);envelope(JSONObject().put("records",tmArray(rows)).put("groups",tmArray(tmRoutes(rows))))}
            "/statistics/energy-overview" -> envelope(statistics(car,u,body?:JSONObject()))
            "/trip-month-stats/get" -> envelope(tmMonthly(enrichedRecords(car,"drives",u),enrichedRecords(car,"charges",u),tmPeriod(body?:JSONObject())))
            "/dashboard/getTrendData" -> {val period=tmPeriod(body?:JSONObject());envelope(tmTrend(records(car,"drives",u,period.start),records(car,"charges",u,period.start),period))}
            "/car/trip/cityFootprint/get" -> envelope(regions(car,u,body?:JSONObject(),false))
            "/car/trip/cityFootprint/detail" -> envelope(regions(car,u,body?:JSONObject(),true))
            "/car/trip/trackMap" -> envelope(tracks(car,u,body?:JSONObject()))
            "/car/status/health" -> envelope(history(car,body?:JSONObject()))
            "/car/status/history" -> {val data=history(car,body?:JSONObject());val all=data.getJSONArray("allStates").objects();val page=body?.optInt("pageNum",1)?.coerceAtLeast(1)?:1;envelope(JSONObject().put("list",tmArray(all.drop(((page-1L)*20).coerceAtMost(all.size.toLong()).toInt()).take(20))).put("total",all.size))}
            "/car/charge/current" -> envelope(currentCharge(car,u))
            "/car/status/current" -> {val data=carData(get("cars/$car/status"),car);envelope(JSONObject().put("status",data.getJSONObject("status")).put("units",data.getJSONObject("units")).tmPut("batteryHealth",batteryHealth(car)))}
            else -> throw IllegalStateException("当前车辆服务暂不支持这项功能")
        }
    }

    private suspend fun latest(car:Long,kind:String,configuredUnits:TeslaMateUnits):JSONObject? {
        val data=carData(get("cars/$car/$kind?page=1&show=1"),car)
        require(data.has(kind)){"服务未返回记录列表"}
        val row=if(data.isNull(kind))null else data.getJSONArray(kind).optJSONObject(0)
        val actual=tmResponseUnits(data,configuredUnits)
        return row?.let{if(kind=="drives")tmDrive(it,actual,gps)else tmCharge(it,actual)}
    }

    private suspend fun dashboard(car: Long, u: TeslaMateUnits, date: String): JSONObject = coroutineScope {
        val today = LocalDate.now(teslaMateZone)
        val selected = date.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) } ?: today
        val statusJob = async {
            try { val data=carData(get("cars/$car/status"),car);data.getJSONObject("status") to tmResponseUnits(data,u) }
            catch (_: IOException) { null }
        }
        val driveJob = async { records(car, "drives", u, selected.minusDays(59)) }
        val chargeJob = async { latest(car,"charges",u) }
        val statusResult = statusJob.await();val status=statusResult?.first;val statusUnits=statusResult?.second?:u
        val drives = driveJob.await(); val charge = chargeJob.await()
        val lastTrip = drives.firstOrNull() ?: latest(car,"drives",u)
        val overview = JSONObject().put("carMode", status?.let { tmState(it) } ?: "状态暂不可用")
        if (status != null) {
            val battery = status.optJSONObject("battery_details"); val climate = status.optJSONObject("climate_details")
            val charging = status.optJSONObject("charging_details"); val state = status.optString("state")
            val current = state in listOf("online", "driving", "charging")
            overview.tmPut("batteryLevel", battery?.let { tmNumber(it, "battery_level") })
                .tmPut("remainingRange", battery?.let { statusUnits.distance(tmNumber(it, if (u.preferredRange == "ideal") "ideal_battery_range" else "rated_battery_range")) })
                .tmPut("insideTemp", climate?.let { statusUnits.celsius(tmNumber(it, "inside_temp")) })
                .tmPut("outsideTemp", climate?.let { statusUnits.celsius(tmNumber(it, "outside_temp")) })
                .tmPut("odometer", statusUnits.distance(tmNumber(status, "odometer")))
                .put("carLocation", status.optJSONObject("car_geodata")?.optString("geofence").orEmpty().ifBlank { "位置暂无名称" })
                .put("sentrySwitch", status.optJSONObject("car_status")?.let { if (it.has("sentry_mode")) if (it.getBoolean("sentry_mode")) "开启" else "关闭" else null } ?: "暂无数据")
            if (current) {
                overview.tmPut("speed", status.optJSONObject("driving_details")?.let { statusUnits.distance(tmNumber(it, "speed")) })
                if (state == "charging" || charging?.optString("charging_state") == "Charging") {
                    overview.put("chargeState", 2).tmPut("currentPowerKw", charging?.let { tmNumber(it, "charger_power") })
                        .tmPut("electricVoltageV", charging?.let { tmNumber(it, "charger_voltage") })
                        .tmPut("electricCurrentA", charging?.let { tmNumber(it, "charger_actual_current") })
                }
            }
        }
        val recent = drives.filter { !LocalDate.parse(it.getString("tripStartTime").take(10)).isBefore(selected.minusDays(29)) && !LocalDate.parse(it.getString("tripStartTime").take(10)).isAfter(selected) }
        val day = recent.filter { it.getString("tripStartTime").startsWith(selected.toString()) }
        overview.tmPut("recent30Mileage", tmSum(recent,"mileageKm"))
            .tmPut("recent30DrivingDurationMs", tmSum(recent,"durationMinutes")?.times(60000))
        val brief = JSONArray().put(JSONObject().put("title", "今日行程").put("value", day.size).put("unit", "次"))
            .put(JSONObject().put("title", "今日里程").tmPut("value", tmSum(day,"mileageKm")).put("unit", "km"))
            .put(JSONObject().put("title", "驾驶时长").tmPut("value", tmSum(day,"durationMinutes")).put("unit", "分钟"))
        val mileage = JSONArray()
        for (offset in 29 downTo 0) {
            val d = selected.minusDays(offset.toLong()); val rows = recent.filter { it.getString("tripStartTime").startsWith(d.toString()) }
            mileage.put(JSONObject().put("date", d.toString()).tmPut("mileage", tmSum(rows,"mileageKm")?.times(1000)))
        }
        tmAverageEnergy(day)?.let{brief.put(JSONObject().put("title","平均能耗").put("value",it).put("unit","Wh/km"))}
        val previous=drives.filter{val date=LocalDate.parse(it.getString("tripStartTime").take(10));!date.isBefore(selected.minusDays(59))&&date.isBefore(selected.minusDays(29))}
        val comparisons=JSONArray().put(JSONObject().put("label","行驶里程").tmPut("currentValue",tmSum(recent,"mileageKm")).tmPut("previousValue",tmSum(previous,"mileageKm")).put("unit","km"))
            .put(JSONObject().put("label","平均能耗").tmPut("currentValue",tmAverageEnergy(recent)).tmPut("previousValue",tmAverageEnergy(previous)).put("unit","Wh/km"))
        val trend=tmTrend(recent,emptyList(),TmPeriod(selected.minusDays(29),selected)).getJSONObject("drivingTrend")
        val comparison=JSONObject().tmPut("currentPeriodEnergy",tmAverageEnergy(recent)).tmPut("previousPeriodEnergy",tmAverageEnergy(previous))
        val currentEnergy=tmAverageEnergy(recent);val previousEnergy=tmAverageEnergy(previous)
        if(currentEnergy!=null&&previousEnergy!=null&&previousEnergy>0)comparison.put("changePercent",(currentEnergy-previousEnergy)/previousEnergy*100)
        JSONObject().put("overview", overview).put("dailyBrief", brief).put("_teslamate", true)
            .put("comparisons",comparisons).put("energyComparison",comparison).put("analysisStats",JSONObject().put("totalTrips",recent.size).tmPut("totalPowerConsumption",tmKnownSum(recent,"totalPowerConsumption")))
            .put("_teslamateStatusNote", (status?.let { tmState(it) } ?: "状态暂不可用") + " · 显示 TeslaMate 最近记录，读取时间不代表车辆采样时间")
            .put("drivingTrend",trend)
            .tmPut("latestTrip", lastTrip).tmPut("latestCharge", charge)
    }

    private val metaMutex=Mutex()
    private val metadataCache=object:LinkedHashMap<String,Pair<Instant,Map<Long,JSONObject>>>(8,.75f,true){override fun removeEldestEntry(eldest:MutableMap.MutableEntry<String,Pair<Instant,Map<Long,JSONObject>>>?)=size>8}
    private val unavailableMetadata=mutableSetOf<String>()
    private suspend fun metadata(car:Long,kind:String):Map<Long,JSONObject> = metaMutex.withLock {
        val key="$car/$kind";val now=Instant.now()
        metadataCache.entries.removeAll{!now.isBefore(it.value.first.plusSeconds(60))}
        metadataCache[key]?.let{return@withLock it.second}
        val rows=linkedMapOf<Long,JSONObject>();val idKey=if(kind=="drives")"drive_id"else"charge_id"
        for(page in 1..100){
            val data=try{get("cars/$car/meridian/$kind?page=$page&show=500").getJSONObject("data")}catch(e:HttpReadFailure){if(e.status==404){unavailableMetadata.add(key);metadataCache[key]=now to emptyMap();return@withLock emptyMap()};throw e}
            require(data.getLong("car_id")==car){"扩展接口车辆编号不匹配"}
            val batch=data.getJSONArray("records")
            if(batch.length()==0){unavailableMetadata.remove(key);metadataCache[key]=now to rows;return@withLock rows}
            for(i in 0 until batch.length()){val row=batch.getJSONObject(i);val id=row.getLong(idKey);require(id>0&&!rows.containsKey(id)){"扩展数据分页发生变化"};rows[id]=row}
        }
        throw IOException("扩展数据超过本次读取上限")
    }
    private suspend fun enrichedRecords(car:Long,kind:String,u:TeslaMateUnits,start:LocalDate?=null):List<JSONObject> {
        val raw=records(car,kind,u,start);val extra=metadata(car,kind)
        return raw.map{tmEnrich(it,extra[it.getLong("id")],kind=="charges",gps)}
    }
    private suspend fun batteryHealth(car:Long):JSONObject? {
        val raw=try{carData(get("cars/$car/battery-health"),car).getJSONObject("battery_health")}catch(_:IOException){return null}
        return JSONObject().tmPut("latestHealthPercent",tmNumber(raw,"battery_health_percentage")?.takeIf{it>0}).tmPut("latestCapacityKwh",tmNumber(raw,"current_capacity")?.takeIf{it>0})
            .put("disclaimer","基于 TeslaMate 充电与续航记录的估算，不是厂商电池检测结果。")
    }
    private suspend fun statistics(car:Long,u:TeslaMateUnits,body:JSONObject):JSONObject = coroutineScope {
        val trips=async{enrichedRecords(car,"drives",u)};val additions=async{enrichedRecords(car,"charges",u)};val health=async{batteryHealth(car)}
        val rows=trips.await();val charges=additions.await();val period=tmPeriod(body)
        tmStatistics(rows,charges,period).tmPut("batteryHealth",health.await()).put("cityFootprint",tmFootprint(rows.filter{period.contains(it)},JSONObject()))
    }
    private suspend fun regions(car:Long,u:TeslaMateUnits,body:JSONObject,detail:Boolean):JSONObject {
        val all=enrichedRecords(car,"drives",u,body.optString("startDate").takeIf{it.isNotBlank()}?.take(10)?.let{LocalDate.parse(it)})
        require("$car/drives" !in unavailableMetadata){"车辆服务缺少地区数据接口，请连接 Meridian 增强 API"}
        val period=if(body.has("startDate")||body.has("endDate"))tmPeriod(body)else null
        val rows=all.filter{period?.contains(it)!=false}
        val query=JSONObject(body.toString())
        if(detail)query.put("level",when(body.optString("level")){"PROVINCE"->"NATION";"CITY"->"PROVINCE";else->"CITY"})
        val result=tmFootprint(rows,query)
        result.put("coverageNote","按已记录行程起终点归属统计；没有结构化地区的记录不推测归属。")
        if(!detail)return result
        val list=result.getJSONArray("regions").objects().filter{body.optString("keyword").let{k->k.isBlank()||it.getString("name").contains(k)}}
        val size=body.optInt("pageSize",20).coerceIn(1,100);val page=body.optInt("pageNum",1).coerceAtLeast(1)
        return JSONObject().put("list",tmArray(list.drop(((page-1L)*size).coerceAtMost(list.size.toLong()).toInt()).take(size))).put("size",list.size).put("total",list.size)
    }
    private suspend fun tracks(car:Long,u:TeslaMateUnits,body:JSONObject):JSONObject {
        val rows=enrichedRecords(car,"drives",u).filter{row->(!body.has("startDate")&&!body.has("endDate")||tmPeriod(body).contains(row))&&tmFilterRegion(row,body)}
        if(rows.isEmpty())return JSONObject().put("tracks",JSONArray()).put("frequentRoutes",JSONArray()).put("summary",JSONObject().put("trackTrips",0).put("totalMileageKm",0))
        val first=rows.minOf{LocalDate.parse(it.getString("tripStartTime").take(10))};val last=if(body.has("endDate"))LocalDate.parse(body.getString("endDate").take(10)).plusDays(1).atStartOfDay(teslaMateZone).toInstant()else Instant.now()
        val from=URLEncoder.encode(first.atStartOfDay(teslaMateZone).toInstant().toString(),"UTF-8");val to=URLEncoder.encode(last.toString(),"UTF-8")
        val payload=get("cars/$car/meridian/tracks?page=1&show=100&startDate=$from&endDate=$to").getJSONObject("data")
        require(payload.getLong("car_id")==car){"轨迹车辆编号不匹配"}
        val points=payload.getJSONArray("points").objects().groupBy{it.getLong("drive_id")}
        val selected=rows.filter{points.containsKey(it.getLong("id"))}.take(100).map{row->
            val samples=points.getValue(row.getLong("id")).mapNotNull{point->val lat=tmDbNumber(point,"latitude");val lon=tmDbNumber(point,"longitude");if(lat==null||lon==null)null else tmMapCoordinate(lat,lon,gps).let{JSONObject().put("latitude",it.first).put("longitude",it.second)}}
            JSONObject(row.toString()).put("track",JSONObject().put("locationTrack",tmArray(samples)))
        }
        return JSONObject().put("tracks",tmArray(selected)).put("frequentRoutes",tmArray(tmRoutes(rows).take(10)))
            .put("summary",JSONObject().put("trackTrips",selected.size).tmPut("totalMileageKm",tmSum(selected,"mileageKm")).tmPut("totalDurationMinutes",tmSum(selected,"durationMinutes")))
            .put("coverageNote","地图展示当前范围最近 ${selected.size}/${rows.size} 条有采样行程；点位已抽样。高频路线按当前范围全部记录统计，可缩小日期范围查看更早轨迹。")
    }
    private val historyMutex=Mutex()
    private val historyCache=object:LinkedHashMap<String,Pair<Instant,JSONObject>>(6,.75f,true){override fun removeEldestEntry(eldest:MutableMap.MutableEntry<String,Pair<Instant,JSONObject>>?)=size>6}
    private suspend fun history(car:Long,body:JSONObject):JSONObject = historyMutex.withLock {
        val period=tmPeriod(body);val from=body.optString("detailStartTime").takeIf{it.isNotBlank()}?.let{it.replace(' ','T').let{v->if(v.endsWith("Z")||Regex("[+-]\\d\\d:\\d\\d$").containsMatchIn(v))tmInstant(v)else java.time.LocalDateTime.parse(v).atZone(teslaMateZone).toInstant()}}?:period.start.atStartOfDay(teslaMateZone).toInstant()
        val until=body.optString("detailEndTime").takeIf{it.isNotBlank()}?.let{it.replace(' ','T').let{v->if(v.endsWith("Z")||Regex("[+-]\\d\\d:\\d\\d$").containsMatchIn(v))tmInstant(v)else java.time.LocalDateTime.parse(v).atZone(teslaMateZone).toInstant()}}?:period.end.plusDays(1).atStartOfDay(teslaMateZone).toInstant()
        val key="$car/$from/$until/${body.optBoolean("excludeParking")}";val now=Instant.now()
        historyCache.entries.removeAll{!now.isBefore(it.value.first.plusSeconds(60))}
        historyCache[key]?.let{return@withLock JSONObject(it.second.toString())}
        val start=URLEncoder.encode(from.toString(),"UTF-8");val end=URLEncoder.encode(minOf(until,now).toString(),"UTF-8")
        val raw=get("cars/$car/meridian/history?startDate=$start&endDate=$end").getJSONObject("data")
        require(raw.getLong("car_id")==car){"历史车况车辆编号不匹配"}
        val samples=raw.getJSONArray("samples").objects().map{r->JSONObject().put("time",tmTime(r.getString("time"))).tmPut("batteryLevel",tmDbNumber(r,"battery_level"))
            .tmPut("remainingRange",tmDbNumber(r,"remaining_range")).tmPut("insideTemp",tmDbNumber(r,"inside_temp")).tmPut("outsideTemp",tmDbNumber(r,"outside_temp"))
            .tmPut("currentSpeed",tmDbNumber(r,"speed")).tmPut("odometer",tmDbNumber(r,"odometer"))}.filter{!body.optBoolean("excludeParking")||(tmNumber(it,"currentSpeed")?:0.0)>0}
        val states=raw.getJSONArray("states").objects().map{r->JSONObject().put("id",r.getLong("id")).put("status",tmState(JSONObject().put("state",r.optString("state"))))
            .put("startTime",tmTime(r.getString("start_time"))).apply{if(!r.isNull("end_time"))put("endTime",tmTime(r.getString("end_time")))}}
        val result=JSONObject().put("metricTrail",tmArray(samples)).put("statusTimeline",tmArray(states.take(50))).put("allStates",tmArray(states))
            .put("coverageNote","历史指标每小时取最后一个实际采样点；状态时间线来自 TeslaMate 记录器，最多展示1000条；不推算未采集时段。")
        historyCache[key]=now to result
        JSONObject(result.toString())
    }
    private suspend fun currentCharge(car:Long,u:TeslaMateUnits):JSONObject {
        val status=carData(get("cars/$car/status"),car).getJSONObject("status")
        if(status.optString("state") !in listOf("online","charging")||status.optJSONObject("charging_details")?.optString("charging_state")!="Charging")return JSONObject().put("active",false)
        val data=carData(get("cars/$car/charges/current"),car);val raw=JSONObject(data.getJSONObject("charge").toString())
        if(!raw.optBoolean("is_charging"))return JSONObject().put("active",false)
        raw.put("end_date",Instant.now().toString())
        raw.optJSONObject("battery_details")?.let{it.put("end_battery_level",it.opt("current_battery_level"))}
        return tmCharge(raw,tmResponseUnits(data,u)).put("active",true).put("_ongoing",true)
    }
    internal suspend fun invalidate(){mutex.withLock{snapshots.clear()};metaMutex.withLock{metadataCache.clear()};historyMutex.withLock{historyCache.clear()}}

    private fun envelope(data: Any) = JSONObject().put("code", 200).put("data", data)
}

internal fun tmRecordPage(rows: List<JSONObject>, charge: Boolean, body: JSONObject): JSONObject {
    val time = if (charge) "startTime" else "tripStartTime"
    val start = body.optString("startDate").takeIf { it.isNotBlank() }?.take(10)?.let { LocalDate.parse(it) }
    val end = body.optString("endDate").takeIf { it.isNotBlank() }?.take(10)?.let { LocalDate.parse(it) }
    val keyword = body.optString("keyword").trim()
    val filtered = rows.filter { row ->
        val day = LocalDate.parse(row.getString(time).take(10))
        (start == null || !day.isBefore(start)) && (end == null || !day.isAfter(end)) &&
            tmFilterRegion(row,body) && (keyword.isBlank() || listOf("startLocation", "endLocation", "locationName").any { row.optString(it).contains(keyword, true) })
    }.let { if (body.optString("sortOrder") == "asc") it.sortedWith(compareBy<JSONObject> { row -> tmInstant(row.getString(time)) }.thenBy { row -> row.getLong("id") }) else it.sortedWith(compareByDescending<JSONObject> { row -> tmInstant(row.getString(time)) }.thenByDescending { row -> row.getLong("id") }) }
    val size = body.optInt("pageSize", 20).coerceIn(1, 100)
    val page = body.optInt("pageNum", 1).coerceAtLeast(1)
    val offset = ((page.toLong() - 1) * size).coerceAtMost(filtered.size.toLong()).toInt()
    val summary = if (charge) JSONObject().put("totalSessions", filtered.size)
        .tmPut("totalEnergy", tmSum(filtered,"chargeEnergy"))
        .tmPut("totalDurationMinutes", tmSum(filtered,"durationMinutes"))
    else JSONObject().put("totalTrips", filtered.size).tmPut("totalMileage", tmSum(filtered,"mileageKm")?.times(1000))
        .tmPut("totalDuration", tmSum(filtered,"durationMinutes")?.times(60000))
    return JSONObject().put("list", JSONArray(filtered.drop(offset).take(size))).put("total", filtered.size).put("size",filtered.size).put("summary", summary).put("_teslamate", true)
}
