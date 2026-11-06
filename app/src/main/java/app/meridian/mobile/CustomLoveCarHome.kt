@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package app.meridian.mobile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import kotlinx.coroutines.*
import org.json.*

@Composable internal fun CustomLoveCarHome(api:Api,car:JSONObject,d:JSONObject,scroll:ScrollState,editing:Boolean,onEditing:(Boolean)->Unit,theme:String,onTheme:(String)->Unit,onDragging:(Boolean)->Unit,onOpen:(String,String)->Unit,onHealth:()->Unit,resetRequest:Int=0,layoutRequest:Int=0){
 val context=LocalContext.current;val store=remember{HomeLayoutStore(context)};val carId=car.safeText("id");val orderState=remember(api.base,carId){mutableStateOf(store.order(api.base,carId))};var order by orderState
 var hidden by remember(api.base,carId){mutableStateOf(store.hidden(api.base,carId))}
 LaunchedEffect(layoutRequest){order=store.order(api.base,carId);hidden=store.hidden(api.base,carId)}
 val save={store.saveOrder(api.base,carId,order)}
 BackHandler(editing){save();onEditing(false)}
 DisposableEffect(api.base,carId){val service=api.base;onDispose{store.saveOrder(service,carId,orderState.value);onDragging(false)}}
 val scope=rememberCoroutineScope();var busy by remember{mutableStateOf(false)};var issue by remember{mutableStateOf("")};var record by remember{mutableStateOf<Pair<Boolean,JSONObject>?>(null)};var analysis by remember{mutableStateOf(false)}
 val o=d.optJSONObject("overview")?:JSONObject();val comparisons=d.optJSONArray("comparisons")?:JSONArray();val stats=d.optJSONObject("analysisStats");val trends=d.optJSONObject("drivingTrend");val ad=stats?.number("adMileage");val human=stats?.number("humanMileage")
 if(analysis)ComprehensiveAnalysis(JSONObject().put("metrics",comparisons).put("analysis",stats)){analysis=false}
 record?.let{if(it.first)ChargeDetailPage(it.second,api){record=null}else TripDetailPage(api,it.second){record=null}}
 fun openRecord(row:JSONObject?,charge:Boolean){if(editing)return;if(row==null){onOpen(if(charge)"充电记录"else"行程记录",if(charge)"/app/charge"else"/app/trips");return};scope.launch{busy=true;issue="";try{val key=if(charge)"recordId"else"tripId";val id=row.opt(key)?:row.opt("id")?:throw Exception("记录缺少编号");val detail=api.call(if(charge)"/car/charge/detail"else"/car/trip/getDetail",JSONObject().put(key,id)).optJSONObject("data")?:throw Exception("暂无详情数据");record=charge to detail}catch(e:Exception){if(e is CancellationException)throw e;issue=e.message?:"详情读取失败"}finally{busy=false}}}
 val state=if(api.isTeslaMate)o.safeText("carMode","状态待更新")else when{o.number("speed")?.let{it>0}==true->numberText(o.number("speed"))+" km/h";o.optInt("chargeState") in listOf(1,2)->"充电中";o.optInt("chargeState")==3->"充电完成";o.safeText("carMode").isNotBlank()&&o.safeText("carMode")!in listOf("不可用","正常模式")->o.safeText("carMode");o.has("speed")||o.has("chargeState")->"已停放";else->"状态待更新"}
 val recent=o.optJSONObject("latestTrip")?:d.optJSONObject("latestTrip");val charge=o.optJSONObject("latestCharge")?:d.optJSONObject("latestCharge")
 val active=homeCardIds.filter{it !in hidden}.filter{!api.isTeslaMate||it in teslaMateHomeCards}.filter{when(it){"charging"->o.optInt("chargeState") in listOf(1,2);"efficiency"->d.optJSONObject("energyDistribution")!=null;"time","dailyAverage"->d.optJSONObject("timeDistribution")!=null;"comparison"->d.optJSONObject("energyComparison")!=null;else->true}}
 CompositionLocalProvider(LocalReferenceHome provides true){
  var appliedReset by remember{mutableIntStateOf(resetRequest)}
  LaunchedEffect(resetRequest){if(resetRequest!=appliedReset){order=homeCardIds;hidden=emptySet();store.saveHidden(api.base,carId,hidden);store.saveOrder(api.base,carId,order);appliedReset=resetRequest}}
  if(editing)Text("长按卡片拖动 · 点右上角减号移除",fontSize=12.sp,color=muted)
  if(api.isTeslaMate)d.safeText("_teslamateStatusNote").takeIf{it.isNotBlank()}?.let{Text(it,fontSize=11.sp,lineHeight=17.sp,color=muted)}
  if(busy)FlowLoading(label="正在读取记录…")
  if(issue.isNotBlank())Text(issue,fontSize=13.sp,color=MaterialTheme.colorScheme.error)
  EditableHomeGrid(active,order,editing,scroll,{from,to->order=moveHomeCard(order,from,to)},onDragging,save,onRemove={id->hidden=hidden+id;store.saveHidden(api.base,carId,hidden)}){id->
   CompositionLocalProvider(LocalHomeCardTint provides homeCardTint(theme,id),LocalPalette provides homeCardPalette(theme,id,LocalPalette.current)){when(id){
    "vehicle"->VehicleStudio(car)
    "range"->ReferenceCard{
     Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
      Row(Modifier.weight(1f),verticalAlignment=Alignment.Bottom){Text(homeValue(o.opt("remainingRange")),fontSize=40.sp,lineHeight=46.sp,fontWeight=FontWeight.Medium,letterSpacing=(-1).sp);Text("km",fontSize=16.sp,modifier=Modifier.padding(start=4.dp,bottom=5.dp))}
      Column(horizontalAlignment=Alignment.End){Text(homeValue(o.opt("batteryLevel"))+"%",fontSize=21.sp,fontWeight=FontWeight.Medium);Text("剩余电量",fontSize=12.sp,color=muted)}
     }
     val battery=o.number("batteryLevel");val tint=accent;val track=glassEdge
     Canvas(Modifier.fillMaxWidth().height(8.dp)){drawRoundRect(track.copy(alpha=.6f),cornerRadius=androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()));if(battery!=null)drawRoundRect(Brush.horizontalGradient(listOf(tint.copy(alpha=.6f),tint)),size=androidx.compose.ui.geometry.Size(size.width*(battery/100).coerceIn(0.0,1.0).toFloat(),size.height),cornerRadius=androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()))}
     Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){Icon(Icons.Outlined.LocationOn,null,Modifier.size(14.dp),tint=muted);Text(o.safeText("carLocation","位置暂无数据"),fontSize=12.sp,color=muted,modifier=Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis);Text(state,fontSize=12.sp,color=muted)}
    }
    "summary"->ReferenceCard{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
     Row(Modifier.weight(1f).cardClick(enabled=!editing){onOpen("近30天用车","/app/energy?view=overview")},verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){Text("近30天",fontSize=11.sp,color=muted);Text(homeValue(o.opt("recent30Mileage"))+" km",fontSize=14.sp,fontWeight=FontWeight.Medium)}
     Box(Modifier.width(.6.dp).height(18.dp).background(glassEdge));Icon(Icons.Outlined.Schedule,null,Modifier.size(15.dp),tint=muted);Text(o.number("recent30DrivingDurationMs")?.let{durationText(it.toLong())}?:"—",fontSize=13.sp,fontWeight=FontWeight.Medium)
    }}
    "condition"->Box(Modifier.fillMaxWidth().cardClick(enabled=!editing){onHealth()}){ReferenceCard{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){Icon(Icons.Outlined.HealthAndSafety,null,Modifier.size(17.dp),tint=accent);Text("车况",Modifier.weight(1f),fontSize=13.sp,fontWeight=FontWeight.Medium);Icon(Icons.Outlined.ChevronRight,null,Modifier.size(14.dp),tint=muted)};Text(state,fontSize=23.sp,fontWeight=FontWeight.SemiBold);Text(if(api.isTeslaMate)"TeslaMate 最近状态"else"车辆当前状态",fontSize=11.sp,color=muted);Text("查看完整车况",fontSize=12.sp,color=accent)}}
    "ledger"->HomeLedgerCard(api,carId,!editing){onOpen("用车账单","/app/ledger")}
    "sentry"->CompactStatus("哨兵",o.safeText("sentrySwitch","暂无数据"),Icons.Outlined.Security,Modifier.fillMaxWidth()){if(!editing&&!api.isTeslaMate)onOpen("哨兵 · 远程查看","/app/sentry")}
    "temperature"->CompactStatus("车内",homeValue(o.opt("insideTemp"))+if(o.number("insideTemp")!=null)"°C"else"",Icons.Outlined.Thermostat,Modifier.fillMaxWidth(),null)
    "brief"->DailyBriefPanel(api,car.get("id"),d.optJSONArray("dailyBrief")?:JSONArray(),compact=true,trend=trends?.optJSONArray("mileageStats"))
    "recentTrip"->ReferenceRecordCard(recent,false){openRecord(recent,false)}
    "recentCharge"->ReferenceRecordCard(charge,true){openRecord(charge,true)}
    "analysis"->ReferenceAnalysis(comparisons){if(!editing)analysis=true}
    "trends"->ReferenceCard{Text("近30天里程趋势",fontSize=14.sp,fontWeight=FontWeight.Medium);HomeMiniTrend(trends?.optJSONArray("mileageStats")?:JSONArray(),"每日里程 · km")}
    "driving"->CompactDrivingCard(ad,human)
    "energyTrend"->HomeDrivingTrends(api,car.get("id"),trends)
    "efficiency","time"->{val v=d.optJSONObject(if(id=="efficiency")"energyDistribution"else"timeDistribution")?:JSONObject();val values=if(id=="efficiency")listOf("低能耗" to v.optDouble("lowEnergyPercent",0.0),"中能耗" to v.optDouble("midEnergyPercent",0.0),"高能耗" to v.optDouble("highEnergyPercent",0.0))else listOf("白天" to v.optDouble("daytimePercent",0.0),"夜间" to v.optDouble("nightPercent",0.0));ReferenceDistribution(if(id=="efficiency")"能效分布 · 近30天"else"驾驶时段 · 近30天",values)}
    "comparison"->{val v=d.optJSONObject("energyComparison")?:JSONObject();ReferenceCard{Text("能耗周期对比",fontSize=15.sp,fontWeight=FontWeight.Medium);ComparisonBars("近30天 / 前30天",v.number("currentPeriodEnergy"),v.number("previousPeriodEnergy"),"Wh/km");v.number("changePercent")?.let{Text(String.format(java.util.Locale.getDefault(),"较上周期 %+.1f%%",it),fontSize=12.sp,color=muted)}}}
    "dailyAverage"->ReferenceCard{Text("日均驾驶",fontSize=13.sp,color=muted);Text(numberText(d.optJSONObject("timeDistribution")?.number("avgDailyDrivingHours"))+" 小时",fontSize=24.sp,fontWeight=FontWeight.Medium)}
    "charging"->ReferenceCard{SectionTitle("实时充电",Icons.Outlined.EvStation);HomeMetricTiles(listOf(HomeMetricValue("当前功率",homeValue(o.opt("currentPowerKw")),"kW"),HomeMetricValue("补能速度",homeValue(o.opt("chargeSpeedKmh")),"km/h"),HomeMetricValue("电压",homeValue(o.opt("electricVoltageV")),"V"),HomeMetricValue("电流",homeValue(o.opt("electricCurrentA")),"A")))}
    "history","report"->Box(Modifier.fillMaxWidth().cardClick(enabled=!editing){if(id=="history")onHealth()else onOpen("月度报告","/app/energy?view=report")}){ReferenceCard{Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(7.dp)){Icon(if(id=="history")Icons.Outlined.Article else Icons.Outlined.BarChart,null,Modifier.size(17.dp),tint=muted);Text(if(id=="history")"车况历史"else"月度报告",fontSize=13.sp,modifier=Modifier.weight(1f));Icon(Icons.Outlined.ChevronRight,null,Modifier.size(14.dp),tint=muted)};if(id=="history")d.optJSONObject("latestHealthAnomaly")?.safeText("title")?.takeIf{it.isNotBlank()}?.let{Text(it,fontSize=11.sp,color=muted)}}}
   }}
  }
 }
}
@Composable private fun CompactStatus(label:String,value:String,icon:androidx.compose.ui.graphics.vector.ImageVector,modifier:Modifier,onClick:(()->Unit)?){Column(modifier.fillMaxHeight().background(if(LocalHomeCardTint.current==Color.Transparent)glassWhite else lerp(glassWhite,LocalHomeCardTint.current,.12f),RoundedCornerShape(15.dp)).border(.6.dp,glassEdge,RoundedCornerShape(15.dp)).then(if(onClick!=null)Modifier.cardClick(onClick=onClick)else Modifier).padding(horizontal=5.dp,vertical=12.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(5.dp)){Icon(icon,null,Modifier.size(21.dp),tint=accent);Text("$label $value",fontSize=11.sp,lineHeight=17.sp,color=ink)}}
@Composable private fun ReferenceRecordCard(data:JSONObject?,charge:Boolean,onOpen:()->Unit){Box(Modifier.fillMaxWidth().cardClick(onClick=onOpen)){ReferenceCard{
 Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(if(charge)"最近充电"else"最近行程",fontSize=14.sp,fontWeight=FontWeight.Medium,modifier=Modifier.weight(1f));Icon(Icons.Outlined.ChevronRight,null,Modifier.size(14.dp),tint=muted)}
 if(data==null){Icon(if(charge)Icons.Outlined.Power else Icons.Outlined.Route,null,Modifier.size(26.dp).align(Alignment.CenterHorizontally),tint=muted);Text("暂无记录",fontSize=12.sp,color=muted,modifier=Modifier.align(Alignment.CenterHorizontally))}
 else if(charge){Text(data.safeText("locationName","充电记录"),fontSize=13.sp,lineHeight=20.sp);Text(data.safeText("chargeEnergy","—")+" kWh",fontSize=12.sp,color=muted)}
 else{val places=recentTripAddresses(data);RoutePoint(privateAddress(places.first),accent);if(places.second.isNotBlank())RoutePoint(privateAddress(places.second),muted);Text(data.safeText("mileageKm","—")+" km · "+data.safeText("durationMinutes","—")+" 分钟",fontSize=11.sp,color=muted)}
}}}
@Composable private fun ReferenceDistribution(title:String,values:List<Pair<String,Double>>){ReferenceCard{Text(title,fontSize=15.sp,fontWeight=FontWeight.Medium);val valid=values.filter{it.second.isFinite()&&it.second>=0};val total=valid.sumOf{it.second};val tint=accent;val track=glassEdge;val colors=if(LocalGlobalTheme.current.id=="vivid"){if(title.startsWith("能效"))listOf(Color(0xFF2CC653),Color(0xFFFF9500),Color(0xFFFF3B30))else listOf(Color(0xFFFFC900),Color(0xFF6254DE))}else listOf(tint,tint.copy(alpha=.5f),track)
 if(total>0){Canvas(Modifier.fillMaxWidth().height(10.dp)){var x=0f;valid.forEachIndexed{i,item->val w=(item.second/total*size.width).toFloat();drawRect(colors[i%colors.size],Offset(x,0f),androidx.compose.ui.geometry.Size(w,size.height));x+=w}};FlowRow(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){valid.forEachIndexed{i,item->Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)){Box(Modifier.size(6.dp).background(colors[i%colors.size],CircleShape));Text(item.first+" "+numberText(item.second)+"%",fontSize=11.sp,color=muted)}}}}else Text("暂无分布数据",fontSize=12.sp,color=muted)
}}
