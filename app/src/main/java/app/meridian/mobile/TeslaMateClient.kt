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
                val mapped = records(car, if (charging) "charges" else "drives", u, startDate)
                envelope(tmRecordPage(mapped, charging, body ?: JSONObject()))
            }
            "/car/trip/getDetail" -> {
                val id = body?.getLong("tripId")?.takeIf { it > 0 } ?: throw IllegalArgumentException("缺少行程编号")
                val data=carData(get("cars/$car/drives/$id"),car)
                envelope(tmDrive(data.getJSONObject("drive"),tmResponseUnits(data,u),gps))
            }
            "/car/charge/detail" -> {
                val id = body?.getLong("recordId")?.takeIf { it > 0 } ?: throw IllegalArgumentException("缺少充电编号")
                val data=carData(get("cars/$car/charges/$id"),car)
                envelope(tmCharge(data.getJSONObject("charge"),tmResponseUnits(data,u)))
            }
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
        val driveJob = async { records(car, "drives", u, selected.minusDays(29)) }
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
        JSONObject().put("overview", overview).put("dailyBrief", brief).put("_teslamate", true)
            .put("_teslamateStatusNote", (status?.let { tmState(it) } ?: "状态暂不可用") + " · 显示 TeslaMate 最近记录，读取时间不代表车辆采样时间")
            .put("drivingTrend", JSONObject().put("mileageStats", mileage))
            .tmPut("latestTrip", lastTrip).tmPut("latestCharge", charge)
    }

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
            (keyword.isBlank() || listOf("startLocation", "endLocation", "locationName").any { row.optString(it).contains(keyword, true) })
    }.let { if (body.optString("sortOrder") == "asc") it.sortedWith(compareBy<JSONObject> { row -> tmInstant(row.getString(time)) }.thenBy { row -> row.getLong("id") }) else it.sortedWith(compareByDescending<JSONObject> { row -> tmInstant(row.getString(time)) }.thenByDescending { row -> row.getLong("id") }) }
    val size = body.optInt("pageSize", 20).coerceIn(1, 100)
    val page = body.optInt("pageNum", 1).coerceAtLeast(1)
    val offset = ((page.toLong() - 1) * size).coerceAtMost(filtered.size.toLong()).toInt()
    val summary = if (charge) JSONObject().put("totalSessions", filtered.size)
        .tmPut("totalEnergy", tmSum(filtered,"chargeEnergy"))
        .tmPut("totalDurationMinutes", tmSum(filtered,"durationMinutes"))
    else JSONObject().put("totalTrips", filtered.size).tmPut("totalMileage", tmSum(filtered,"mileageKm")?.times(1000))
        .tmPut("totalDuration", tmSum(filtered,"durationMinutes")?.times(60000))
    return JSONObject().put("list", JSONArray(filtered.drop(offset).take(size))).put("total", filtered.size).put("summary", summary).put("_teslamate", true)
}
