package app.meridian.mobile
import androidx.compose.runtime.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import kotlinx.coroutines.*
import org.json.*
import java.security.MessageDigest

@Composable fun ActionPanel(title:String,icon:ImageVector,onClick:()->Unit,content:@Composable ColumnScope.()->Unit){Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).clickable(onClick=onClick)){Panel{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){Icon(icon,null,tint=accent,modifier=Modifier.size(18.dp));Text(title,fontSize=15.sp,fontWeight=FontWeight.SemiBold,color=ink,modifier=Modifier.weight(1f));Icon(Icons.Outlined.ChevronRight,null,tint=muted,modifier=Modifier.size(17.dp))};content()}}}
@Composable fun LoveCarHome(api:Api,car:JSONObject,d:JSONObject,onOpen:(String,String)->Unit,onHealth:()->Unit){
 val scope=rememberCoroutineScope();var loadingDetail by remember{mutableStateOf(false)};var detailIssue by remember{mutableStateOf("")};var recordDetail by remember{mutableStateOf<Pair<Boolean,JSONObject>?>(null)}
 val context=LocalContext.current
 val o=d.optJSONObject("overview")?:JSONObject();val brief=d.optJSONArray("dailyBrief")?:JSONArray();val comparisons=d.optJSONArray("comparisons")?:JSONArray();var sheet by remember{mutableStateOf<Pair<String,JSONObject>?>(null)}
 if(sheet!=null)ComprehensiveAnalysis(sheet!!.second){sheet=null}
 if(loadingDetail)FlowLoading(Modifier.fillMaxWidth())
 if(detailIssue.isNotBlank())Text(detailIssue,color=Color(0xFFBF6D54),fontSize=14.sp)
 if(recordDetail!=null&&!recordDetail!!.first)TripDetailPage(api,recordDetail!!.second){recordDetail=null}
 if(recordDetail!=null&&recordDetail!!.first)ChargeDetailPage(recordDetail!!.second,api){recordDetail=null}
 fun openRecord(record:JSONObject?,charge:Boolean){
   if(record==null)return
   scope.launch{loadingDetail=true;detailIssue="";try{val id=record.opt(if(charge)"recordId"else"tripId")?:record.opt("id")?:throw Exception("记录缺少编号，请到行程页查看");val result=api.call(if(charge)"/car/charge/detail"else"/car/trip/getDetail",JSONObject().put(if(charge)"recordId"else"tripId",id)).optJSONObject("data")?:throw Exception("暂无详情数据");recordDetail=charge to result}catch(e:Exception){if(e is CancellationException)throw e;detailIssue=e.message?:"详情读取失败"}finally{loadingDetail=false}}
 }
 VehicleStudio(car)
 val recent=o.optJSONObject("latestTrip")?:d.optJSONObject("latestTrip");val charge=o.optJSONObject("latestCharge")?:d.optJSONObject("latestCharge");val anomaly=d.optJSONObject("latestHealthAnomaly")
 val state=when{ o.safeText("carMode").isNotBlank()&&o.safeText("carMode")!in listOf("正常模式","不可用")->o.safeText("carMode");o.optDouble("speed",0.0)>0->o.safeText("speed")+" km/h";o.optInt("chargeState",0)in listOf(1,2)->"充电中";o.optInt("chargeState",0)==3->"充电完成";!o.has("speed")&&!o.has("chargeState")&&o.safeText("carMode").isBlank()->"状态待更新";else->"静止中"}
 HomeGlass {
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly,verticalAlignment=Alignment.CenterVertically){
     Box(Modifier.weight(1f)){Box(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center){HeroNumber(o.opt("batteryLevel"),"%","剩余电量")}}
     Box(Modifier.width(1.dp).height(36.dp).background(glassEdge))
     Box(Modifier.weight(1f)){Box(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center){HeroNumber(o.opt("remainingRange"),"km","剩余续航")}}
   }
   val battery=o.optDouble("batteryLevel",Double.NaN);val batteryTrack=softWash;val batteryAccent=Color(0xFF668A93)
   if(battery.isFinite())Canvas(Modifier.fillMaxWidth().padding(horizontal=12.dp).height(3.dp)){
      drawRoundRect(batteryTrack,cornerRadius=androidx.compose.ui.geometry.CornerRadius(5f))
      drawRoundRect(Brush.horizontalGradient(listOf(Color(0xFFA4BAC3),batteryAccent)),size=androidx.compose.ui.geometry.Size(size.width*(battery/100).toFloat().coerceIn(0f,1f),size.height),cornerRadius=androidx.compose.ui.geometry.CornerRadius(5f))
   }
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){
     Icon(Icons.Outlined.LocationOn,null,tint=muted,modifier=Modifier.size(13.dp))
     Text(o.safeText("carLocation","位置暂无数据"),fontSize=13.sp,color=muted,modifier=Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis)
     Text(state,fontSize=13.sp,color=if(state.contains("充电"))Color(0xFF668A93)else muted,modifier=Modifier.clip(RoundedCornerShape(8.dp)).background(softWash).padding(horizontal=7.dp,vertical=4.dp))
   }
 }
 Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max),horizontalArrangement=Arrangement.spacedBy(9.dp)){
   HomeSmallMetric("近30天里程",homeValue(o.opt("recent30Mileage")),"km",Icons.Outlined.Route,Color(0xFF587DAA),Modifier.weight(1f)){onOpen("近30天用车","/app/energy?view=overview")}
   HomeSmallMetric("近30天驾驶",if(o.has("recent30DrivingDurationMs")&&!o.isNull("recent30DrivingDurationMs"))durationText(o.optLong("recent30DrivingDurationMs"))else"—","",Icons.Outlined.Schedule,Color(0xFF7C879C),Modifier.weight(1f),null)
 }
 Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max),horizontalArrangement=Arrangement.spacedBy(8.dp)){
   HomeStatus("车况详情",state,Icons.Outlined.HealthAndSafety,Color(0xFF53A69B),Modifier.weight(1f),onHealth)
   HomeStatus("哨兵模式",o.safeText("sentrySwitch","暂无数据"),Icons.Outlined.Security,Color(0xFF8986A9),Modifier.weight(1f)){onOpen("哨兵 · 远程查看","/app/sentry")}
   HomeStatus("车内温度",homeValue(o.opt("insideTemp"))+if(o.has("insideTemp")&&!o.isNull("insideTemp"))"°C"else"",Icons.Outlined.Thermostat,Color(0xFFAD8D6B),Modifier.weight(1f),null)
 }
 Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max),horizontalArrangement=Arrangement.spacedBy(10.dp)){
  HomeQuickCard("最近行程",recent?.safeText("route",recent.safeText("startLocation","出发地")+" → "+recent.safeText("endLocation","目的地"))?:"暂无行程记录",recent?.let{it.safeText("mileageKm","—")+" km · "+it.safeText("durationMinutes","—")+" 分钟"}?:"查看用车记录",Icons.Outlined.Route,Modifier.weight(1f)){if(recent==null)onOpen("行程记录","/app/trips")else openRecord(recent,false)}
  HomeQuickCard("最近充电",charge?.safeText("locationName","充电记录")?:"暂无充电记录",charge?.let{it.safeText("chargeEnergy","—")+" kWh"}?:"查看补能记录",Icons.Outlined.EvStation,Modifier.weight(1f)){if(charge==null)onOpen("充电记录","/app/charge")else openRecord(charge,true)}
 }
 Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max),horizontalArrangement=Arrangement.spacedBy(10.dp)){
  HomeQuickCard("车况提醒",anomaly?.safeText("title","车况与历史")?:"车况与历史",anomaly?.safeText("happenTime","查看状态变化")?:"查看状态变化",Icons.Outlined.VerifiedUser,Modifier.weight(1f),onHealth)
  HomeQuickCard("本月报告","月度用车回顾","里程、能耗与驾驶表现",Icons.Outlined.Article,Modifier.weight(1f)){onOpen("月度报告","/app/energy?view=report")}
 }
 if(o.optInt("chargeState",0)in listOf(1,2))Panel{SectionTitle("实时充电",Icons.Outlined.EvStation);Row{InlineMetric("当前功率",o.safeText("currentPowerKw","—")+" kW",Icons.Outlined.Bolt,Modifier.weight(1f));InlineMetric("补能速度",o.safeText("chargeSpeedKmh","—")+" km/h",Icons.Outlined.Speed,Modifier.weight(1f))};Row{InlineMetric("电压",o.safeText("electricVoltageV","—")+" V",Icons.Outlined.ElectricalServices,Modifier.weight(1f));InlineMetric("电流",o.safeText("electricCurrentA","—")+" A",Icons.Outlined.BatteryChargingFull,Modifier.weight(1f))}}
 DailyBriefPanel(api,car.get("id"),brief)
 ActionPanel("综合分析 · 近30天",Icons.Outlined.Insights,{sheet="综合分析" to JSONObject().put("metrics",comparisons).put("analysis",d.optJSONObject("analysisStats"))}){
  if(comparisons.length()==0)Text("暂无综合分析数据",color=muted,fontSize=14.sp)
  HomeMetricTiles(comparisons.objects().map{m->HomeMetricValue(m.safeText("label"),homeValue(m.opt("currentValue")),analysisUnit(m),m.number("changePercent")?.let{"较上周期 "+String.format(java.util.Locale.getDefault(),"%+.1f%%",it)}?:"暂无上周期对比")})
 }

 val analysis=d.optJSONObject("analysisStats");if(analysis!=null){val ad=analysis.optDouble("adMileage",Double.NaN);val human=analysis.optDouble("humanMileage",Double.NaN);if(ad.isFinite()&&human.isFinite()&&ad+human>0)Box{DonutCard("驾驶方式",listOf("辅助驾驶" to ad,"人工驾驶" to human),"km")}}
 HomeDrivingTrends(api,car.get("id"),d.optJSONObject("drivingTrend"))
 for((key,title)in listOf("energyDistribution" to "能效分布","timeDistribution" to "驾驶时段分布")){val v=d.optJSONObject(key)?:continue;val values=if(key=="energyDistribution")listOf("低能耗" to v.optDouble("lowEnergyPercent",0.0),"中能耗" to v.optDouble("midEnergyPercent",0.0),"高能耗" to v.optDouble("highEnergyPercent",0.0))else listOf("白天" to v.optDouble("daytimePercent",0.0),"夜间" to v.optDouble("nightPercent",0.0));if(values.sumOf{it.second}>0)Box{DonutCard(title,values,"%",if(key=="energyDistribution")v.number("avgEnergyConsumption")?.let{numberText(it)}else null,if(key=="energyDistribution")"Wh/km"else null)}}
 d.optJSONObject("energyComparison")?.let{v->Panel{
  SectionTitle("能耗周期对比",Icons.Outlined.CompareArrows)
  ComparisonBars("近30天 · 对比前30天",v.number("currentPeriodEnergy"),v.number("previousPeriodEnergy"),"Wh/km")
  v.number("changePercent")?.let{Text(if(it==0.0)"两周期能效持平"else"较上周期 "+String.format(java.util.Locale.getDefault(),"%+.1f%%",it),fontSize=14.sp,color=muted)}
 }}
 d.optJSONObject("timeDistribution")?.number("avgDailyDrivingHours")?.let{hours->Panel{
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.Schedule,null,tint=Color(0xFF587DAA),modifier=Modifier.size(20.dp));Text("日均驾驶",fontSize=15.sp,color=muted,modifier=Modifier.weight(1f).padding(start=8.dp));Text(numberText(hours)+" 小时",fontSize=21.sp,fontWeight=FontWeight.Medium,color=ink)}
  Text("近30天 · 白天 06:00—18:00，夜间 18:00—06:00",fontSize=13.sp,lineHeight=20.sp,color=muted)
 }}

}
@Composable fun HomeDetail(j:JSONObject){VisualHomeDetails(j)}

private val homeTones=listOf(Color(0xFF587DAA),Color(0xFF7C879C),Color(0xFF668A93),Color(0xFFAD8D6B))
fun homeValue(value:Any?):String=if(value==null||value==JSONObject.NULL||value.toString().trim().lowercase() in listOf("null","undefined","nan","infinity","-infinity"))"—"else displayValue("",value)
@Composable fun HomeGlass(content:@Composable ColumnScope.()->Unit){
 val dark=LocalPalette.current.dark
 Column(Modifier.fillMaxWidth().background(Brush.linearGradient(if(dark)listOf(Color(0xFF232830),Color(0xFF14171D))else listOf(Color(0xFFFDFBF7),Color(0xFFF0F3F8))),RoundedCornerShape(24.dp)).border(.7.dp,if(dark)Color(0xFF363E49)else Color(0xFFE5E2DD),RoundedCornerShape(24.dp)).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp),content=content)
}
@Composable fun HomeSmallMetric(label:String,value:String,unit:String,icon:ImageVector,tint:Color,modifier:Modifier,onClick:(()->Unit)?){
 Column(modifier.fillMaxHeight().clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(listOf(glassWhite,glassWhite))).border(.8.dp,glassEdge,RoundedCornerShape(20.dp)).then(if(onClick!=null)Modifier.clickable(onClick=onClick)else Modifier).padding(13.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
  Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){Icon(icon,null,tint=tint,modifier=Modifier.size(16.dp));Text(label,fontSize=14.sp,color=muted,modifier=Modifier.weight(1f));if(onClick!=null)Icon(Icons.Outlined.ChevronRight,"查看详情",tint=muted,modifier=Modifier.size(13.dp))}
  Row(verticalAlignment=Alignment.Bottom){Text(value,fontSize=22.sp,lineHeight=28.sp,fontWeight=FontWeight.Medium,color=ink);if(unit.isNotBlank())Text(unit,fontSize=12.sp,color=muted,modifier=Modifier.padding(start=4.dp,bottom=3.dp))}
 }
}
@Composable fun HomeStatus(label:String,value:String,icon:ImageVector,tint:Color,modifier:Modifier,onClick:(()->Unit)?){
 Column(modifier.fillMaxHeight().clip(RoundedCornerShape(19.dp)).background(glassWhite).border(.8.dp,glassEdge,RoundedCornerShape(19.dp)).then(if(onClick!=null)Modifier.clickable(onClick=onClick)else Modifier).padding(vertical=11.dp,horizontal=5.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(5.dp)){
  Box(Modifier.size(32.dp).background(Brush.linearGradient(listOf(tint.copy(alpha=.16f),tint.copy(alpha=.04f))),RoundedCornerShape(11.dp)),contentAlignment=Alignment.Center){Icon(icon,null,tint=tint,modifier=Modifier.size(19.dp))}
  Row(verticalAlignment=Alignment.CenterVertically){Text(label,fontSize=14.sp,fontWeight=FontWeight.Medium,color=ink);if(onClick!=null)Icon(Icons.Outlined.ChevronRight,"查看详情",tint=muted,modifier=Modifier.size(12.dp))}
  Text(value,fontSize=14.sp,color=muted,maxLines=1,overflow=TextOverflow.Ellipsis)
 }
}
@Composable fun HomeMetricCell(label:String,value:String,unit:String,trend:String,index:Int,modifier:Modifier){
 MetricDataCard(label,value,unit,metricIcon(label),modifier.fillMaxHeight(),homeTones[index%homeTones.size],trend)
}

@Composable fun HomeQuickCard(title:String,value:String,meta:String,icon:ImageVector,modifier:Modifier,onClick:()->Unit){Column(modifier.fillMaxHeight().clip(RoundedCornerShape(18.dp)).background(glassWhite).border(.7.dp,glassEdge,RoundedCornerShape(18.dp)).clickable(onClick=onClick).padding(13.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
 Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){Icon(icon,null,tint=muted,modifier=Modifier.size(14.dp));Text(title,fontSize=14.sp,color=muted,modifier=Modifier.weight(1f));Icon(Icons.Outlined.ChevronRight,null,tint=muted,modifier=Modifier.size(14.dp))}
 Text(value,fontSize=16.sp,lineHeight=24.sp,fontWeight=FontWeight.SemiBold,color=ink,maxLines=3,overflow=TextOverflow.Ellipsis)
 Text(meta,fontSize=13.sp,lineHeight=19.sp,color=muted,maxLines=2,overflow=TextOverflow.Ellipsis)
}}
