package app.meridian.mobile

import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.*

internal enum class VehicleBackend { XIAOMI, TESLAMATE }
internal val teslaMateZone: ZoneId = ZoneId.of("Asia/Shanghai")
internal fun tmNumber(j: JSONObject, key: String): Double? = (j.opt(key) as? Number)?.toDouble()?.takeIf { it.isFinite() }
internal fun JSONObject.tmPut(key: String, value: Any?): JSONObject = apply { if (value != null) put(key, value) }
internal fun tmTime(value: String): String = OffsetDateTime.parse(value).atZoneSameInstant(teslaMateZone).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
internal fun tmInstant(value: String): Instant = OffsetDateTime.parse(value).toInstant()

internal fun connectionAddress(input: String, backend: VehicleBackend): String {
    val uri = URI(input.trim().trimEnd('/'))
    require(!uri.host.isNullOrBlank() && uri.userInfo == null && uri.query == null && uri.fragment == null) { "请输入有效的服务根地址" }
    val host = uri.host.lowercase()
    val octets = host.split('.').mapNotNull { it.toIntOrNull()?.takeIf { n -> n in 0..255 } }
    val local = host == "localhost" || host == "[::1]" || host.endsWith(".local") ||
        (host.split('.').size == 4 && octets.size == 4 && (octets[0] == 10 || octets[0] == 127 || (octets[0] == 192 && octets[1] == 168) || (octets[0] == 172 && octets[1] in 16..31)))
    require(uri.scheme == "https" || (backend == VehicleBackend.TESLAMATE && uri.scheme == "http" && local)) {
        "请使用 HTTPS；TeslaMate 可使用局域网 HTTP 地址"
    }
    return uri.toString().let { if (backend == VehicleBackend.TESLAMATE) it.removeSuffix("/api/v1").removeSuffix("/api") else it }
}

internal data class TeslaMateUnits(val length: String, val temperature: String, val preferredRange: String = "rated") {
    init { require(length in setOf("km", "mi") && temperature in setOf("C", "F")) { "TeslaMate 单位配置无效" } }
    val distanceFactor get() = if (length == "mi") 1.609344 else 1.0
    fun distance(value: Double?) = value?.times(distanceFactor)
    fun celsius(value: Double?) = value?.let { if (temperature == "F") (it - 32) / 1.8 else it }
    fun range(row: JSONObject) = row.optJSONObject(if (preferredRange == "ideal") "range_ideal" else "range_rated")
}

internal fun tmResponseUnits(data:JSONObject,fallback:TeslaMateUnits):TeslaMateUnits {
    val declared=data.optJSONObject("units")?:return fallback
    return TeslaMateUnits(declared.optString("unit_of_length").ifBlank{fallback.length},
        declared.optString("unit_of_temperature").ifBlank{fallback.temperature},fallback.preferredRange)
}

internal fun tmCar(row: JSONObject): JSONObject {
    val id = row.getLong("car_id")
    require(id > 0) { "车辆编号无效" }
    val model = row.optJSONObject("car_details")?.optString("model").orEmpty()
    return JSONObject().put("id", id).put("carName", row.optString("name").ifBlank { "我的 Tesla" })
        .put("carModel", if (model.isBlank()) "Tesla" else "Tesla Model $model").put("_teslamate", true)
}

internal fun tmDrive(row: JSONObject, units: TeslaMateUnits, gps: Boolean = true): JSONObject {
    val id = row.getLong("drive_id")
    val start = tmTime(row.getString("start_date")); val end = tmTime(row.getString("end_date"))
    require(id > 0 && !tmInstant(end).isBefore(tmInstant(start))) { "行程编号或时间无效" }
    val range = units.range(row)
    val distance = units.distance(row.optJSONObject("odometer_details")?.let { tmNumber(it,"odometer_distance") })
    val energy = tmNumber(row,"energy_consumed_net")
    // 1.25.0 converts consumption like a distance in mile mode; derive from the paired energy and normalized distance.
    val consumption = if(tmNumber(row,"consumption_net")!=null && energy!=null && distance!=null && distance>0)energy/distance*100 else null
    val result = JSONObject().put("id", id).put("tripId", id).put("_teslamate", true)
        .put("tripStartTime", start).put("tripEndTime", end)
        .put("startLocation", row.optString("start_address")).put("endLocation", row.optString("end_address"))
        .tmPut("mileageKm", units.distance(row.optJSONObject("odometer_details")?.let { tmNumber(it, "odometer_distance") }))
        .tmPut("durationMinutes", tmNumber(row, "duration_min"))
        .tmPut("drivingMileage",distance?.times(1000)).tmPut("drivingDuration",tmNumber(row,"duration_min")?.times(60000))
        .tmPut("avgSpeed", units.distance(tmNumber(row, "speed_avg"))).tmPut("maxSpeed", units.distance(tmNumber(row, "speed_max")))
        .tmPut("totalPowerConsumption", tmNumber(row, "energy_consumed_net"))
        .tmPut("tripEnergyConsumption", consumption)
        .tmPut("startRemainingRange", range?.let { units.distance(tmNumber(it, "start_range")) })
        .tmPut("endRemainingRange", range?.let { units.distance(tmNumber(it, "end_range")) })
    row.optJSONArray("drive_details")?.let { raw ->
        val sorted = (0 until raw.length()).map { raw.getJSONObject(it) }.filter {
            tmNumber(it, "latitude")?.let { n -> n in -85.0..85.0 } == true &&
                tmNumber(it, "longitude")?.let { n -> n in -180.0..180.0 } == true
        }.sortedWith(compareBy<JSONObject> { tmInstant(it.getString("date")) }.thenBy { it.getLong("detail_id") })
        val samples = JSONArray()
        val stride = ceil(sorted.size / 1200.0).toInt().coerceAtLeast(1)
        sorted.filterIndexed { index, _ -> index % stride == 0 || index == sorted.lastIndex }.forEach { point ->
            val (lat, lon) = tmMapCoordinate(point.getDouble("latitude"), point.getDouble("longitude"), gps)
            samples.put(JSONObject().put("latitude", lat).put("longitude", lon).put("time", tmTime(point.getString("date")))
                .tmPut("speed", units.distance(tmNumber(point, "speed"))).tmPut("power",tmNumber(point,"power")).tmPut("batteryLevel",tmNumber(point,"battery_level")))
        }
        result.put("track", JSONObject().put("locationTrack", samples).put("speedSamples", samples))
    }
    return result
}

internal fun tmCharge(row: JSONObject, units: TeslaMateUnits): JSONObject {
    val id = row.getLong("charge_id")
    val start = tmTime(row.getString("start_date")); val end = tmTime(row.getString("end_date"))
    require(id > 0 && !tmInstant(end).isBefore(tmInstant(start))) { "充电编号或时间无效" }
    val battery = row.optJSONObject("battery_details")
    val result = JSONObject().put("id", id).put("recordId", id).put("_teslamate", true)
        .put("startTime", start).put("endTime", end).put("locationName", row.optString("address"))
        .tmPut("chargeEnergy", tmNumber(row, "charge_energy_added")).tmPut("durationMinutes", tmNumber(row, "duration_min"))
        .tmPut("startSoc", battery?.let { tmNumber(it, "start_battery_level") }).tmPut("endSoc", battery?.let { tmNumber(it, "end_battery_level") })
    battery?.let{result.tmPut("startBattery",tmNumber(it,"start_battery_level")).tmPut("endBattery",tmNumber(it,"end_battery_level"))}
    // API 1.25.0 collapses unknown costs to zero; omit cost until provenance is available.
    row.optJSONArray("charge_details")?.let { raw ->
        val samples = JSONArray()
        (0 until raw.length()).map { raw.getJSONObject(it) }
            .sortedWith(compareBy<JSONObject> { tmInstant(it.getString("date")) }.thenBy { it.getLong("detail_id") }).forEach { point ->
                val charger = point.optJSONObject("charger_details")
                samples.put(JSONObject().put("sampleTime", tmTime(point.getString("date")))
                    .tmPut("batteryLevel", tmNumber(point, "battery_level"))
                    .tmPut("currentPowerKw", charger?.let { tmNumber(it, "charger_power") })
                    .tmPut("electricVoltageV", charger?.let { tmNumber(it, "charger_voltage") })
                    .tmPut("electricCurrentA", charger?.let { tmNumber(it, "charger_actual_current") }))
            }
        result.put("samples", samples)
        val powers = (0 until samples.length()).mapNotNull { tmNumber(samples.getJSONObject(it), "currentPowerKw") }
        result.tmPut("peakPowerKw", powers.maxOrNull())
        var integral=0.0;var seconds=0.0
        samples.objects().zipWithNext().forEach{(a,b)->val dt=java.time.Duration.between(tmInstant(a.getString("sampleTime")),tmInstant(b.getString("sampleTime"))).toMillis()/1000.0
            val p=tmNumber(a,"currentPowerKw");val q=tmNumber(b,"currentPowerKw")
            if(dt>0&&dt<=600&&p!=null&&q!=null){integral+=(p+q)/2*dt;seconds+=dt}}
        if(seconds>0)result.put("avgPowerKw",integral/seconds)
    }
    return result
}

internal val teslaMateHomeCards=setOf("vehicle","range","summary","condition","sentry","temperature","brief","recentTrip","recentCharge","trends","charging","ledger","analysis","energyTrend","comparison","history","report")
internal fun tmSum(rows:List<JSONObject>,key:String):Double? = if(rows.any{tmNumber(it,key)==null})null else rows.sumOf{tmNumber(it,key)!!}

internal fun tmState(status: JSONObject): String = when (status.optString("state")) {
    "asleep" -> "车辆休眠"; "offline" -> "车辆离线"; "driving" -> "行驶中"; "charging" -> "充电中"; "online" -> "车辆在线"; else -> "状态待更新"
}

/** Local display conversion; raw TeslaMate coordinates are never modified. */
internal fun tmMapCoordinate(lat: Double, lon: Double, gps: Boolean): Pair<Double, Double> {
    if (!gps || lon !in 72.004..137.8347 || lat !in 0.8293..55.8271) return lat to lon
    val x = lon - 105; val y = lat - 35
    var dLat = -100 + 2*x + 3*y + .2*y*y + .1*x*y + .2*sqrt(abs(x))
    dLat += (20*sin(6*x*PI) + 20*sin(2*x*PI))*2/3
    dLat += (20*sin(y*PI) + 40*sin(y/3*PI))*2/3
    dLat += (160*sin(y/12*PI) + 320*sin(y*PI/30))*2/3
    var dLon = 300 + x + 2*y + .1*x*x + .1*x*y + .1*sqrt(abs(x))
    dLon += (20*sin(6*x*PI) + 20*sin(2*x*PI))*2/3
    dLon += (20*sin(x*PI) + 40*sin(x/3*PI))*2/3
    dLon += (150*sin(x/12*PI) + 300*sin(x/30*PI))*2/3
    val rad = lat/180*PI; val magic = 1 - .00669342162296594323*sin(rad).pow(2); val root = sqrt(magic)
    return (lat + dLat*180 / ((6378245.0*(1-.00669342162296594323))/(magic*root)*PI)) to
        (lon + dLon*180 / (6378245.0/root*cos(rad)*PI))
}
