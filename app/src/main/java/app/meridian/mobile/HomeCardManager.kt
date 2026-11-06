package app.meridian.mobile
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import org.json.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.*

internal fun homePreset(name:String):List<String> = when(name){"日常"->listOf("vehicle","range","summary","condition","ledger","sentry","temperature","brief","recentTrip","analysis","report");"长途"->listOf("vehicle","range","brief","recentTrip","energyTrend","driving","efficiency","time","analysis");"补能"->listOf("vehicle","range","charging","recentCharge","summary","comparison","report");else->homeCardIds}
@Composable internal fun HomeCardManager(service:String,carId:String,onChanged:()->Unit,onClose:()->Unit,dashboard:JSONObject?=null,car:JSONObject?=null){
 val available=if(car?.optBoolean("_teslamate")==true)homeCardIds.filter{it in teslaMateHomeCards}else homeCardIds
 val context=LocalContext.current;val store=remember{HomeLayoutStore(context)};var hidden by remember{mutableStateOf(store.hidden(service,carId))}
 fun update(ids:List<String>){store.saveOrder(service,carId,normalizedHomeOrder(ids));hidden=homeCardIds.toSet()-ids.toSet();store.saveHidden(service,carId,hidden);onChanged()}
 FlowDetailPanel(onClose,title={Text("首页卡片")},body={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
 Text("选择布局预设，或逐张显示、隐藏。调整立即保存。",fontSize=12.sp,color=muted)
 CompactSegments(listOf("日常","长途","补能").map{it to it},"",{update(homePreset(it))})
 CinemaTextButton({update(homeCardIds)}){Icon(Icons.Outlined.RestartAlt,null,Modifier.size(16.dp));Text("恢复全部卡片和顺序")}
 Text("下方展示当前主题的卡片预览；数值来自最近同步，未返回时保留为空。",fontSize=12.sp,color=muted)
 available.forEach{id->Column(Modifier.fillMaxWidth().background(softWash.copy(alpha=.5f),RoundedCornerShape(20.dp)).border(if(id !in hidden)1.dp else .5.dp,if(id !in hidden)accent.copy(alpha=.35f)else glassEdge,RoundedCornerShape(20.dp)).padding(10.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(homeCardTitle(id),Modifier.weight(1f),fontSize=14.sp);Switch(id !in hidden,{visible->hidden=if(visible)hidden-id else hidden+id;store.saveHidden(service,carId,hidden);onChanged()})}
  HomeCardStylePreview(id,dashboard?:JSONObject(),car)
 }}
 }},confirm={CinemaButton(onClose){Text("完成")}})
}

@Composable private fun HomeCardStylePreview(id:String,data:JSONObject,car:JSONObject?){
 val overview=data.optJSONObject("overview")?:JSONObject();val theme=LocalGlobalTheme.current.id
 CompositionLocalProvider(LocalReferenceHome provides true,LocalHomeCardTint provides homeCardTint(theme,id),LocalPalette provides homeCardPalette(theme,id,LocalPalette.current)){
 ReferenceCard{
  when(id){
   "vehicle"->{Image(painterResource(R.drawable.car_su7_yahui_reference),"爱车卡片预览",Modifier.fillMaxWidth().height(90.dp));Text(car?.safeText("carName","我的爱车")?:"我的爱车",fontSize=13.sp,color=muted)}
   "range"->{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.Bottom){Text(homeValue(overview.opt("remainingRange"))+" km",fontSize=30.sp,modifier=Modifier.weight(1f));Text(homeValue(overview.opt("batteryLevel"))+"%",fontSize=18.sp,color=accent)};Box(Modifier.fillMaxWidth().height(5.dp).background(softWash,RoundedCornerShape(3.dp)))}
   "summary"->CompactDailyMetrics(listOf(HomeMetricValue("近30天里程",homeValue(overview.opt("recent30Mileage")),"km"),HomeMetricValue("近30天驾驶",overview.number("recent30DrivingDurationMs")?.let{durationText(it.toLong())}?:"—","")))
   "brief"->{Text("今日概览",fontSize=15.sp);val values=data.optJSONArray("dailyBrief")?.objects()?.map{dailyBriefMetric(it,true)}.orEmpty();if(values.isNotEmpty())CompactDailyMetrics(values.take(4))else Text("暂无今日数据",fontSize=13.sp,color=muted)}
   "recentTrip"->{val trip=overview.optJSONObject("latestTrip")?:data.optJSONObject("latestTrip");Text("最近行程",fontSize=15.sp);if(trip!=null){val places=recentTripAddresses(trip);RoutePoint(privateAddress(places.first),accent);if(places.second.isNotBlank())RoutePoint(privateAddress(places.second),muted);Text(trip.safeText("mileageKm","—")+" km · "+trip.safeText("durationMinutes","—")+" 分钟",fontSize=12.sp,color=muted)}else Text("暂无行程记录",fontSize=13.sp,color=muted)}
   "recentCharge"->{Text("最近充电",fontSize=15.sp);val charge=overview.optJSONObject("latestCharge")?:data.optJSONObject("latestCharge");Text(charge?.safeText("locationName","暂无充电记录")?:"暂无充电记录",fontSize=13.sp,color=muted);charge?.number("chargeEnergy")?.let{Text(numberText(it)+" kWh",fontSize=23.sp)}}
   "analysis"->{Text("综合分析",fontSize=15.sp);val items=data.optJSONArray("comparisons")?.objects().orEmpty();items.take(2).forEach{Text(it.safeText("label")+"  "+homeValue(it.opt("currentValue")),fontSize=13.sp,color=muted)};SolidActionButton({},Modifier.fillMaxWidth()){Text("查看完整分析")}}
   "trends","energyTrend"->{Text("行车趋势",fontSize=15.sp);HomeMiniTrend(data.optJSONObject("drivingTrend")?.optJSONArray("mileageStats")?:JSONArray())}
   "ledger"->{Icon(Icons.Outlined.AccountBalanceWallet,null,Modifier.size(24.dp),tint=accent);Text("用车账单",fontSize=14.sp);Text("本月已记费用 · 元/km",fontSize=13.sp,color=muted)}
   "condition","sentry","temperature"->{val icon=when(id){"condition"->Icons.Outlined.HealthAndSafety;"sentry"->Icons.Outlined.Security;else->Icons.Outlined.Thermostat};Icon(icon,null,Modifier.size(24.dp),tint=accent);Text(homeCardTitle(id),fontSize=14.sp);Text(when(id){"condition"->overview.safeText("carMode","状态待更新");"sentry"->overview.safeText("sentrySwitch","暂无数据");else->homeValue(overview.opt("insideTemp"))+if(overview.number("insideTemp")!=null)"°C"else""},fontSize=13.sp,color=muted)}
   "comparison"->{Text("能耗周期对比",fontSize=15.sp);val v=data.optJSONObject("energyComparison");ComparisonBars("近30天 / 前30天",v?.number("currentPeriodEnergy"),v?.number("previousPeriodEnergy"),"Wh/km")}
   "dailyAverage"->{Text("日均驾驶",fontSize=13.sp,color=muted);Text(numberText(data.optJSONObject("timeDistribution")?.number("avgDailyDrivingHours"))+" 小时",fontSize=25.sp)}
   "charging"->CompactDailyMetrics(listOf(HomeMetricValue("当前功率",homeValue(overview.opt("currentPowerKw")),"kW"),HomeMetricValue("补能速度",homeValue(overview.opt("chargeSpeedKmh")),"km/h")))
   else->{Icon(when(id){"history"->Icons.Outlined.History;"report"->Icons.Outlined.Article;"driving"->Icons.Outlined.AutoMode;else->Icons.Outlined.DonutLarge},null,Modifier.size(24.dp),tint=accent);Text(homeCardTitle(id),fontSize=15.sp);Text(when(id){"driving"->"辅助驾驶与人工驾驶占比";"efficiency"->"低、中、高能耗分布";"time"->"白天与夜间驾驶时段";"report"->"月度用车报告";else->"查看车辆历史状态"},fontSize=12.sp,color=muted)}
  }
 }
 }
}
