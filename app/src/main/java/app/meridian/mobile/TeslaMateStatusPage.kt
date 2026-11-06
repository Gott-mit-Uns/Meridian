package app.meridian.mobile

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable internal fun TeslaMateStatusPage(api:Api,carId:Any,dashboard:JSONObject?,onClose:()->Unit) {
    var details by remember(carId){mutableStateOf<JSONObject?>(null)}
    var issue by remember{mutableStateOf("")};var loading by remember{mutableStateOf(true)};var retry by remember{mutableIntStateOf(0)}
    var historical by remember{mutableStateOf(false)};var charging by remember{mutableStateOf<JSONObject?>(null)}
    val scope=rememberCoroutineScope()
    LaunchedEffect(carId,retry){loading=true;issue="";try{details=api.call("/car/status/current",JSONObject().put("carId",carId)).getJSONObject("data")}catch(e:Exception){if(e is CancellationException)throw e;issue=e.message?:"车况读取失败"}finally{loading=false}}
    if(historical)Health(api,carId){historical=false}
    charging?.let{ChargeDetailPage(it,api,onClose={charging=null})}
    val overview=dashboard?.optJSONObject("overview")?:JSONObject()
    val status=details?.optJSONObject("status")
    FullscreenDataPage("车辆状态",onClose,refreshing=loading,onRefresh={retry++}) {
        CachedReadStatus(details)
        Panel{Text(status?.let{tmState(it)}?:overview.safeText("carMode","状态待更新"),fontSize=23.sp)
            Text("来自 TeslaMate 最近记录；离线、休眠时保留最后数值，读取时间不是车辆采样时间。",fontSize=12.sp,color=muted)
            status?.safeText("state_since")?.takeIf{it.isNotBlank()}?.let{Text("状态开始于 "+prettyTime(it),fontSize=12.sp,color=muted)}}
        if(issue.isNotBlank())Panel{Text(issue,color=muted);CinemaOutlineButton({retry++}){Text("重试")}}
        val battery=status?.optJSONObject("battery_details");val climate=status?.optJSONObject("climate_details")
        val rawUnits=details?.optJSONObject("units");val units=rawUnits?.let{TeslaMateUnits(it.getString("unit_of_length"),it.getString("unit_of_temperature"))}
        Panel{SectionTitle("车辆指标",Icons.Outlined.DirectionsCar)
            HomeMetricTiles(listOf(HomeMetricValue("电量",numberText(battery?.number("battery_level")),"%"),HomeMetricValue("额定续航",numberText(units?.distance(battery?.number("rated_battery_range"))),"km"),
                HomeMetricValue("车内温度",numberText(units?.celsius(climate?.number("inside_temp"))),"°C"),HomeMetricValue("车外温度",numberText(units?.celsius(climate?.number("outside_temp"))),"°C")))
            Text("累计里程  "+numberText(units?.distance(status?.number("odometer")))+" km",fontSize=14.sp)}
        status?.optJSONObject("car_status")?.let{flags->Panel{SectionTitle("门锁与哨兵",Icons.Outlined.Security)
            for((key,label)in listOf("locked" to "门锁","doors_open" to "车门","windows_open" to "车窗","frunk_open" to "前备厢","trunk_open" to "后备厢","sentry_mode" to "哨兵模式")){
                val value=if(!flags.has(key)||flags.isNull(key))"未知"else if(key=="locked")if(flags.getBoolean(key))"已锁定"else"未锁定"else if(flags.getBoolean(key))"开启"else"关闭"
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(label,fontSize=14.sp,color=muted);Text(value,fontSize=14.sp)}
            }}}
        status?.optJSONObject("tpms_details")?.let{tire->Panel{SectionTitle("胎压",Icons.Outlined.Speed)
            HomeMetricTiles(listOf("fl" to "左前","fr" to "右前","rl" to "左后","rr" to "右后").map{(key,label)->HomeMetricValue(label,numberText(tire.number("tpms_pressure_$key")),rawUnits?.optString("unit_of_pressure").orEmpty())})}}
        details?.optJSONObject("batteryHealth")?.let{health->Panel{SectionTitle("电池健康估算",Icons.Outlined.BatteryChargingFull)
            HomeMetricTiles(listOf(HomeMetricValue("估算健康度",numberText(health.number("latestHealthPercent")),"%"),HomeMetricValue("估算容量",numberText(health.number("latestCapacityKwh")),"kWh")))
            Text(health.safeText("disclaimer"),fontSize=12.sp,color=muted)}}
        status?.optJSONObject("charging_details")?.let{charge->Panel{SectionTitle("补能状态",Icons.Outlined.EvStation)
            Text("充电上限  "+numberText(charge.number("charge_limit_soc"))+"%",fontSize=14.sp)
            val active=status.optString("state") in listOf("online","charging")&&charge.optString("charging_state")=="Charging"
            if(active){HomeMetricTiles(listOf(HomeMetricValue("功率",numberText(charge.number("charger_power")),"kW"),HomeMetricValue("预计剩余",charge.number("time_to_full_charge")?.let{durationText((it*3600000).toLong())}?:"—","")))
                CinemaButton({scope.launch{try{val data=api.call("/car/charge/current",JSONObject().put("carId",carId)).getJSONObject("data");if(data.optBoolean("active"))charging=data else issue="当前没有正在充电的记录"}catch(e:Exception){if(e is CancellationException)throw e;issue=e.message?:"充电过程读取失败"}}}){Text("查看正在充电过程")}}
            else Text("当前没有确认正在进行的充电",fontSize=12.sp,color=muted)}}
        CinemaOutlineButton({historical=true},Modifier.fillMaxWidth()){Text("查看车况历史与时间线")}
    }
}

@Composable internal fun TeslaMateVehiclesPage(api:Api,cars:List<JSONObject>,onClose:()->Unit) {
    var message by remember{mutableStateOf("")};var busy by remember{mutableStateOf(false)};val scope=rememberCoroutineScope()
    FullscreenDataPage("车辆与数据",onClose){
        cars.forEach{car->Panel{SectionTitle(car.safeText("carName"),Icons.Outlined.DirectionsCar);Text(car.safeText("carModel"),fontSize=16.sp)}}
        Panel{Text("车辆由 TeslaMate 管理。本机刷新只重新读取数据，不唤醒车辆或修改采集设置。",fontSize=13.sp,color=muted)
            CinemaButton({scope.launch{busy=true;try{api.refreshTeslaMate();message="读取缓存已更新，返回页面后下拉刷新"}finally{busy=false}}},enabled=!busy){Text(if(busy)"正在更新…"else"刷新读取缓存")}
            if(message.isNotBlank())Text(message,fontSize=12.sp,color=muted)}
    }
}
