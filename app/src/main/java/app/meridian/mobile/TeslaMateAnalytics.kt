package app.meridian.mobile

import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

internal data class TmPeriod(val start:LocalDate,val end:LocalDate) {
    init { require(!end.isBefore(start)) { "日期范围无效" };require(ChronoUnit.DAYS.between(start,end)<=36525){"日期范围过大"} }
    fun contains(row:JSONObject,charge:Boolean=false)=LocalDate.parse(row.getString(if(charge)"startTime"else"tripStartTime").take(10)).let{!it.isBefore(start)&&!it.isAfter(end)}
}
internal fun tmPeriod(body:JSONObject,today:LocalDate=LocalDate.now(teslaMateZone)):TmPeriod {
    body.optString("month").takeIf{it.isNotBlank()}?.let{val m=YearMonth.parse(it,DateTimeFormatter.ofPattern("yyyyMM"));return TmPeriod(m.atDay(1),m.atEndOfMonth())}
    val end=body.optString("endDate").takeIf{it.isNotBlank()}?.take(10)?.let{LocalDate.parse(it)}?:body.optString("statDate").takeIf{it.isNotBlank()}?.let{LocalDate.parse(it)}?:today
    val start=body.optString("startDate").takeIf{it.isNotBlank()}?.take(10)?.let{LocalDate.parse(it)}?:end.minusDays(body.optInt("days",30).coerceIn(1,3660)-1L)
    return TmPeriod(start,end)
}
internal fun tmDecimal(j:JSONObject,key:String):BigDecimal? = when(val v=j.opt(key)){is Number,is String->v.toString().toBigDecimalOrNull();else->null}
internal fun tmDbNumber(j:JSONObject,key:String)=tmDecimal(j,key)?.toDouble()?.takeIf{it.isFinite()}
internal fun tmKnownSum(rows:List<JSONObject>,key:String):Double? {
    val known=rows.mapNotNull{tmNumber(it,key)}
    return if(rows.isNotEmpty()&&known.isEmpty())null else known.sum()
}
internal fun tmAverageEnergy(rows:List<JSONObject>):Double? {
    val paired=rows.filter{tmNumber(it,"tripEnergyConsumption")!=null&&tmNumber(it,"totalPowerConsumption")!=null&&(tmNumber(it,"mileageKm")?:0.0)>0}
    val distance=paired.sumOf{tmNumber(it,"mileageKm")!!}
    return if(distance>0)paired.sumOf{tmNumber(it,"totalPowerConsumption")!!}/distance*1000 else null
}
internal fun tmCost(row:JSONObject):Pair<BigDecimal,String>? {
    if(row.optBoolean("_serverEffective")){
        val amount=tmDecimal(row,"costEffective")?:return null
        val source=row.optString("effectiveCostSource","服务端计算")
        if(source=="默认估算"&&maxOf(tmNumber(row,"supplyEnergy")?:0.0,tmNumber(row,"chargeEnergy")?:0.0)<=0)return null
        return amount to source
    }
    for((key,label) in listOf("costManual" to "人工估算","costRecorded" to "原始记录","costTou" to "分时计算"))tmDecimal(row,key)?.let{return it to label}
    return null
}
internal fun tmMetric(label:String,value:Any?,unit:String="",note:String="")=JSONObject().put("label",label).tmPut("value",value).put("unit",unit).put("description",note)
internal fun tmArray(rows:List<JSONObject>)=JSONArray(rows)
internal fun tmTrend(drives:List<JSONObject>,charges:List<JSONObject>,period:TmPeriod):JSONObject {
    val mileage=JSONArray();val energy=JSONArray();val charge=JSONArray()
    var date=period.start
    while(!date.isAfter(period.end)) {
        val day=drives.filter{it.getString("tripStartTime").startsWith(date.toString())}
        val additions=charges.filter{it.getString("startTime").startsWith(date.toString())}
        mileage.put(JSONObject().put("date",date.toString()).tmPut("mileage",tmSum(day,"mileageKm")?.times(1000)))
        energy.put(JSONObject().put("date",date.toString()).tmPut("energyConsumption",tmAverageEnergy(day)))
        charge.put(JSONObject().put("date",date.toString()).tmPut("energy",tmSum(additions,"chargeEnergy")))
        date=date.plusDays(1)
    }
    return JSONObject().put("drivingTrend",JSONObject().put("mileageStats",mileage).put("energyTrend",energy).put("chargeTrend",charge)).put("_teslamate",true)
}
internal fun tmRoutes(drives:List<JSONObject>):List<JSONObject> = drives.groupBy{it.optString("startLocation") to it.optString("endLocation")}
    .filterKeys{it.first.isNotBlank()&&it.second.isNotBlank()}.entries.sortedByDescending{it.value.size}.map{(route,rows)->
        JSONObject().put("routeName",route.first+" → "+route.second).put("title",route.first+" → "+route.second).put("tripCount",rows.size).put("count",rows.size)
            .put("value","${rows.size} 次").put("meta","按已记录起终点分组")
            .tmPut("averageDurationMinutes",tmSum(rows,"durationMinutes")?.div(rows.size)).tmPut("averageEnergyWhKm",tmAverageEnergy(rows))
    }
internal fun tmStatistics(allDrives:List<JSONObject>,allCharges:List<JSONObject>,period:TmPeriod):JSONObject {
    val drives=allDrives.filter{period.contains(it)};val charges=allCharges.filter{period.contains(it,true)}
    val sampleCount=drives.count{tmNumber(it,"tripEnergyConsumption")!=null}
    val coverage="TeslaMate 按额定续航变化估算；耗电已知 ${drives.count{tmNumber(it,"totalPowerConsumption")!=null}}/${drives.size} 条，平均能耗有效 $sampleCount/${drives.size} 条，平均值按对应里程加权。"
    val groups=JSONArray()
    fun group(key:String,title:String,items:List<JSONObject>){groups.put(JSONObject().put("key",key).put("title",title).put("items",tmArray(items)))}
    group("overview","用车概况",listOf(tmMetric("行程次数",drives.size,"次"),tmMetric("活跃天数",drives.map{it.getString("tripStartTime").take(10)}.distinct().size,"天"),
        tmMetric("行驶里程",tmSum(drives,"mileageKm"),"km","仅统计已记录行程"),tmMetric("驾驶时长",tmSum(drives,"durationMinutes")?.div(60),"小时")))
    group("trip","行程表现",listOf(tmMetric("单次最远",drives.mapNotNull{tmNumber(it,"mileageKm")}.maxOrNull(),"km"),tmMetric("单次最久",drives.mapNotNull{tmNumber(it,"durationMinutes")}.maxOrNull(),"分钟"),
        tmMetric("平均车速",tmSum(drives,"mileageKm")?.let{distance->tmSum(drives,"durationMinutes")?.takeIf{it>0}?.let{distance/it*60}},"km/h"),tmMetric("最高车速",drives.mapNotNull{tmNumber(it,"maxSpeed")}.maxOrNull(),"km/h")))
    group("energy","能耗分析",listOf(tmMetric("平均能耗",tmAverageEnergy(drives),"Wh/km",coverage),tmMetric("已知估算耗电",tmKnownSum(drives,"totalPowerConsumption"),"kWh",coverage),tmMetric("有效能耗记录",sampleCount,"条",coverage)))
    val costs=charges.mapNotNull{tmCost(it)}
    val knownCost=if(charges.isNotEmpty()&&costs.isEmpty())null else costs.fold(BigDecimal.ZERO){a,b->a+b.first}.toPlainString()
    group("charge","充电概况",listOf(tmMetric("充电次数",charges.size,"次"),tmMetric("充入电量",tmSum(charges,"chargeEnergy"),"kWh"),tmMetric("供电侧已记录电量",tmKnownSum(charges,"supplyEnergy"),"kWh","保留原始供电侧字段；缺失不补零"),
        tmMetric("充电时长",tmSum(charges,"durationMinutes")?.div(60),"小时"),tmMetric("入账金额（含估算）",knownCost,"记录金额","${costs.size}/${charges.size} 条有金额；包含原始记录及人工、分时或默认估算，缺失不代表免费"),tmMetric("无可用金额记录",charges.size-costs.size,"条")))
    val charts=JSONArray()
    fun chart(key:String,title:String,points:List<JSONObject>,unit:String="次"){charts.put(JSONObject().put("key",key).put("title",title).put("unit",unit).put("points",tmArray(points)))}
    chart("tripHour","出发行程时段",(0..23).map{hour->JSONObject().put("label","${hour}时").put("value",drives.count{tmInstant(it.getString("tripStartTime")).atZone(teslaMateZone).hour==hour})})
    chart("tripWeekday","每周出行分布",(1..7).map{day->JSONObject().put("label",listOf("周一","周二","周三","周四","周五","周六","周日")[day-1]).put("value",drives.count{LocalDate.parse(it.getString("tripStartTime").take(10)).dayOfWeek.value==day})})
    chart("chargeHour","开始充电时段",(0..23).map{hour->JSONObject().put("label","${hour}时").put("value",charges.count{tmInstant(it.getString("startTime")).atZone(teslaMateZone).hour==hour})})
    val bins=listOf(0.0 to 100.0,100.0 to 150.0,150.0 to 200.0,200.0 to Double.POSITIVE_INFINITY)
    chart("tripDistanceEnergy","行程能耗区间",bins.map{(a,b)->JSONObject().put("label",if(b.isFinite())"${a.toInt()}–${b.toInt()}"else"≥${a.toInt()}").put("value",drives.count{r->tmNumber(r,"tripEnergyConsumption")?.times(10)?.let{it>=a&&it<b}==true})})
    fun ranks(rows:List<JSONObject>):JSONObject {
        fun rank(key:String,unit:String)=JSONObject().put("items",tmArray(rows.filter{tmNumber(it,key)!=null}.sortedByDescending{tmNumber(it,key)}.take(5).map{r->JSONObject().put("id",r.getLong("id")).put("title",r.optString("startLocation")+" → "+r.optString("endLocation")).put("meta",r.getString("tripStartTime").take(10)).put("value",numberText(tmNumber(r,key))+" "+unit)}))
        return JSONObject().put("topEnergyTrips",rank("totalPowerConsumption","kWh")).put("topAvgEnergyTrips",rank("tripEnergyConsumption","kWh/100km"))
    }
    val recentStart=LocalDate.now(teslaMateZone).minusDays(29)
    return JSONObject().put("periodDashboard",JSONObject().put("metricGroups",groups).put("charts",charts).put("lists",JSONArray().put(JSONObject().put("key","frequentRoutes").put("items",tmArray(tmRoutes(drives).take(10))))))
        .put("rankingWindows",JSONObject().put("thirtyDays",ranks(allDrives.filter{!LocalDate.parse(it.getString("tripStartTime").take(10)).isBefore(recentStart)})).put("all",ranks(allDrives)))
        .put("_teslamate",true).put("dataCoverage",coverage)
}
internal fun tmMonthly(drives:List<JSONObject>,charges:List<JSONObject>,period:TmPeriod):JSONObject {
    val rows=drives.filter{period.contains(it)};val additions=charges.filter{period.contains(it,true)}
    val m=YearMonth.from(period.start);val previous=TmPeriod(m.minusMonths(1).atDay(1),m.minusMonths(1).atEndOfMonth());val earlier=drives.filter{previous.contains(it)}
    val trend=tmTrend(rows,additions,period).getJSONObject("drivingTrend").getJSONArray("mileageStats").objects().map{JSONObject(it.toString()).apply{tmNumber(this,"mileage")?.let{put("mileage",it/1000)}}}
    val note="按行程开始日统计已记录数据；耗电为 TeslaMate 估算，费用缺失不补零。"
    return JSONObject().put("_teslamate",true).put("availDays",rows.map{it.getString("tripStartTime").take(10)}.distinct().size)
        .tmPut("monthMileage",tmSum(rows,"mileageKm")).tmPut("totalEnergyConsumption",tmSum(rows,"totalPowerConsumption"))
        .tmPut("knownEnergyConsumption",tmKnownSum(rows,"totalPowerConsumption")).put("energyCoverage","估算耗电已知 ${rows.count{tmNumber(it,"totalPowerConsumption")!=null}}/${rows.size} 条；平均能耗按有效样本里程加权。")
        .tmPut("avgEnergyConsumption",tmAverageEnergy(rows)?.div(10)).tmPut("maxTripMileage",rows.mapNotNull{tmNumber(it,"mileageKm")}.maxOrNull())
        .tmPut("maxTripDuration",rows.mapNotNull{tmNumber(it,"durationMinutes")}.maxOrNull()?.times(60000)).put("dayMileages",tmArray(trend))
        .put("monthlyTag","$m · TeslaMate 用车记录").put("storySummary","本月记录 ${rows.size} 次行程、${additions.size} 次充电。$note")
        .put("periodComparison",JSONObject().tmPut("currentMileage",tmSum(rows,"mileageKm")).tmPut("previousMileage",tmSum(earlier,"mileageKm"))
            .tmPut("currentAvgEnergy",tmAverageEnergy(rows)?.div(10)).tmPut("previousAvgEnergy",tmAverageEnergy(earlier)?.div(10))
            .tmPut("currentTotalEnergy",tmSum(rows,"totalPowerConsumption")).tmPut("previousTotalEnergy",tmSum(earlier,"totalPowerConsumption")))
}

internal fun tmEnrich(row:JSONObject,extra:JSONObject?,charge:Boolean,gps:Boolean):JSONObject {
    val result=JSONObject(row.toString())
    if(extra==null)return result
    if(charge) {
        for((raw,key)in listOf("cost_recorded" to "costRecorded","cost_tou" to "costTou","cost_manual" to "costManual","manual_rate" to "manualRate","cost_effective" to "costEffective","override_original_cost" to "overrideOriginalCost"))tmDecimal(extra,raw)?.let{result.put(key,it.toPlainString())}
        result.put("overrideSource",extra.optString("override_source")).put("effectiveCostSource",extra.optString("cost_source","服务端计算"))
        if(extra.has("cost_effective"))result.put("_serverEffective",true)
        result.tmPut("supplyEnergy",tmDbNumber(extra,"energy_used_raw"))
        result.put("_costProvenance",true)
        tmCost(result)?.let{result.put("costAmount",it.first.toPlainString()).put("costSource",it.second)}
    } else {
        for(prefix in listOf("start","end")) {
            for(field in listOf("province","city","district","country"))result.put(prefix+field.replaceFirstChar{it.uppercase()},extra.optString("${prefix}_$field"))
            val province=result.optString(prefix+"Province")
            if(regionName(province) in setOf("北京","天津","上海","重庆","香港","澳门")){
                val oldCity=result.optString(prefix+"City")
                if(result.optString(prefix+"District").isBlank()&&oldCity!=province&&(oldCity.endsWith("区")||oldCity.endsWith("县")))result.put(prefix+"District",oldCity)
                result.put(prefix+"City",province)
            }
            val lat=tmDbNumber(extra,"${prefix}_latitude");val lon=tmDbNumber(extra,"${prefix}_longitude")
            if(lat!=null&&lon!=null){val point=tmMapCoordinate(lat,lon,gps);result.put(if(prefix=="start")"tripStartLatitude"else"tripEndLatitude",point.first).put(if(prefix=="start")"tripStartLongitude"else"tripEndLongitude",point.second)}
            tmDbNumber(extra,"${prefix}_battery")?.let{result.put(if(prefix=="start")"startBattery"else"endBattery",it)}
        }
    }
    return result
}
internal fun tmEndpointMatches(row:JSONObject,body:JSONObject,prefix:String):Boolean =
    listOf("provinceName" to "Province","cityName" to "City","districtName" to "District").all{(key,field)->body.optString(key).trim().let{name->name.isBlank()||regionName(row.optString(prefix+field))==regionName(name)}}
internal fun tmFilterRegion(row:JSONObject,body:JSONObject)=listOf("start","end").any{tmEndpointMatches(row,body,it)}
internal fun tmFootprint(rows:List<JSONObject>,body:JSONObject):JSONObject {
    val matching=rows.filter{tmFilterRegion(it,body)}
    fun regions(field:String):List<JSONObject>{
        val visits=linkedMapOf<String,JSONObject>();val tripIds=mutableMapOf<String,MutableSet<Long>>()
        matching.forEach{row->listOf("start","end").filter{tmEndpointMatches(row,body,it)}.forEach{prefix->
            val name=row.optString(prefix+field).trim()
            if(name.isNotBlank()){
                val parents=when(field){"City"->listOf("Country","Province");"District"->listOf("Country","Province","City");else->listOf("Country")}
                val key=(parents.map{regionName(row.optString(prefix+it))}+regionName(name)).joinToString("|")
                val time=row.getString(if(prefix=="start")"tripStartTime"else"tripEndTime")
                val group=visits.getOrPut(key){JSONObject().put("name",name).put("parentName",row.optString(prefix+if(field=="District")"City"else"Province")).put("grandParentName",row.optString(prefix+"Province")).put("firstVisitTime",time).put("lastVisitTime",time)}
                if(time<group.getString("firstVisitTime"))group.put("firstVisitTime",time)
                if(time>group.getString("lastVisitTime"))group.put("lastVisitTime",time)
                val ids=tripIds.getOrPut(key){mutableSetOf()};ids.add(row.getLong("id"));group.put("visitTripCount",ids.size)
            }
        }}
        return visits.values.sortedByDescending{it.getInt("visitTripCount")}
    }
    val provinces=regions("Province");val cities=regions("City");val districts=regions("District")
    val items=when(body.optString("level","NATION")){"CITY"->districts;"PROVINCE"->cities;else->provinces}
    return JSONObject().put("summary",JSONObject().put("provinceCount",provinces.size).put("cityCount",cities.size).put("districtCount",districts.size).put("regionCount",items.size).put("visitTripCount",matching.size))
        .put("regions",tmArray(items)).put("topCities",tmArray(cities.take(10))).put("_teslamate",true)
}

internal fun tmMergeLedger(existing:List<JSONObject>,records:List<JSONObject>,ignored:Set<String>):List<JSONObject> {
    val result=existing.associateBy{it.getString("id")}.toMutableMap()
    records.forEach{record->val id="auto-charge:"+record.getLong("id");if(id in ignored)return@forEach
        val old=result[id];if(old!=null&&old.optString("source")!in setOf("charge","teslamate"))return@forEach
        val cost=tmCost(record)
        if(cost==null){if(old?.optString("source")=="teslamate"&&record.optBoolean("_costProvenance"))result.remove(id);return@forEach}
        val cents=cost.first.setScale(2,RoundingMode.HALF_UP).movePointRight(2).longValueExact()
        result[id]=JSONObject().put("id",id).put("chargeId",record.getLong("id").toString()).put("date",record.getString("startTime").take(10)).put("category","charge")
            .put("cents",cents).put("source","teslamate").put("energyKwh",record.opt("chargeEnergy")?:JSONObject.NULL).put("note",cost.second+if(cost.first.signum()==0)" · 记录金额为0"else"")
            .put("costSource",cost.second).put("costRecorded",record.opt("costRecorded")?:JSONObject.NULL).put("costTou",record.opt("costTou")?:JSONObject.NULL).put("costManual",record.opt("costManual")?:JSONObject.NULL)
    }
    return result.values.toList()
}
