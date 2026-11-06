package app.meridian.mobile

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import kotlinx.coroutines.*
import org.json.*
import kotlin.math.*

@Composable private fun EnergyBars(chart:JSONObject,unit:String){
 val rows=(chart.optJSONArray("points")?:JSONArray()).objects().filter{it.number("value")!=null};var selected by remember(chart){mutableIntStateOf(0)}
 val tint=accent;val track=glassEdge
 Panel{Text(chart.safeText("title","分布趋势"),fontSize=15.sp,fontWeight=FontWeight.SemiBold,color=ink)
  chart.safeText("subtitle").takeIf{it.isNotBlank()}?.let{Text(it,fontSize=11.sp,color=muted)}
  if(rows.isEmpty())Text("当前范围暂无数据",fontSize=12.sp,color=muted)else{
   val row=rows[selected.coerceIn(rows.indices)];Text(row.safeText("label")+" · "+numberText(row.number("value"))+" "+unit,fontSize=20.sp,color=ink)
   row.number("extraValue")?.let{Text("关联行程 "+numberText(it)+" 次",fontSize=11.sp,color=muted)}
   val low=min(0.0,rows.minOf{it.number("value")?:0.0});val high=max(0.0,rows.maxOf{it.number("value")?:0.0}).coerceAtLeast(low+.001)
   Canvas(Modifier.fillMaxWidth().height(160.dp).pointerInput(rows){detectTapGestures{p->selected=(p.x/size.width*rows.size).toInt().coerceIn(rows.indices)}}){
    val slot=size.width/rows.size;val zero=(size.height*(1-(0-low)/(high-low))).toFloat();rows.forEachIndexed{i,item->val y=(size.height*(1-((item.number("value")?:0.0)-low)/(high-low))).toFloat();drawRoundRect(if(i==selected)tint else tint.copy(alpha=.25f),topLeft=androidx.compose.ui.geometry.Offset(slot*i+slot*.2f,min(zero,y)),size=androidx.compose.ui.geometry.Size(slot*.6f,abs(zero-y)),cornerRadius=androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))};drawLine(track,androidx.compose.ui.geometry.Offset(0f,zero),androidx.compose.ui.geometry.Offset(size.width,zero),1.dp.toPx())
   }
   if(rows.size>1)CinemaSlider(selected.toFloat(),{selected=it.roundToInt().coerceIn(rows.indices)},valueRange=0f..rows.lastIndex.toFloat())
  }
 }
}

@Composable fun EnergyAnalysis(root:JSONObject,trend:JSONObject?,api:Api,carId:Any,days:Int){
 var explorer by remember{mutableStateOf(false)}
 CinemaTonalButton({explorer=true}){Icon(Icons.Outlined.FilterAlt,null,Modifier.size(17.dp));Spacer(Modifier.width(6.dp));Text("按能耗区间查看行程")}
 if(explorer)EnergyRecordExplorer(api,carId,days){explorer=false}
 val scope=rememberCoroutineScope();var window by remember{mutableStateOf("thirtyDays")};var detail by remember{mutableStateOf<JSONObject?>(null)};var busy by remember{mutableStateOf(false)};var issue by remember{mutableStateOf("")}
 fun openTrip(row:JSONObject){val id=row.opt("id")?:row.opt("tripId")?:return;scope.launch{busy=true;issue="";try{detail=api.call("/car/trip/getDetail",JSONObject().put("tripId",id)).optJSONObject("data")}catch(e:Exception){if(e is CancellationException)throw e;issue=e.message?:"行程读取失败"}finally{busy=false}}}
 if(detail!=null)TripDetailPage(api,detail!!){detail=null}
 if(busy)FlowLoading(label="正在读取关联行程…")
 if(issue.isNotBlank())Text(issue,color=MaterialTheme.colorScheme.error,fontSize=12.sp)
 statisticsMetricGroups(root).filter{it.key in listOf("energy","efficiency")}.forEach{group->Panel{SectionTitle(group.title,if(group.key=="efficiency")Icons.Outlined.DonutLarge else Icons.Outlined.Bolt);Text("最近 $days 天",fontSize=11.sp,color=muted);StatisticsCards(group.items,trend,days,group.scope)}}
 val dashboard=root.optJSONObject("periodDashboard")
 (dashboard?.optJSONArray("charts")?:JSONArray()).objects().forEach{chart->val key=chart.safeText("key");val points=chart.optJSONArray("points")?:JSONArray()
  when(key){
   "energyDistribution","drivingTime"->{val values=points.objects().mapNotNull{row->row.number("value")?.let{row.safeText("label") to it}};if(values.sumOf{it.second}>0)DonutCard(chart.safeText("title","能耗分布"),values,"%")}
   "tripDistanceEnergy"->EnergyBars(chart,chart.safeText("unit"))
   "tripHour","tripWeekday","chargeHour"->EnergyBars(chart,"次")
  }
 }
 val t=trend?.optJSONObject("drivingTrend")?:trend;t?.optJSONArray("energyTrend")?.let{InteractiveTrend("每日能耗","Wh/km",chartSamples(it,"energyConsumption"))}
 root.optJSONObject("batteryHealth")?.let{health->Panel{SectionTitle("电池健康估算",Icons.Outlined.BatteryChargingFull);Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max),horizontalArrangement=Arrangement.spacedBy(8.dp)){MetricDataCard("当前健康度",numberText(health.number("latestHealthPercent")),"%",Icons.Outlined.VerifiedUser,Modifier.weight(1f).fillMaxHeight());MetricDataCard("估算容量",numberText(health.number("latestCapacityKwh")),"kWh",Icons.Outlined.BatteryFull,Modifier.weight(1f).fillMaxHeight())};val rows=health.optJSONArray("points")?:JSONArray();if(rows.length()>0)InteractiveTrend("健康度趋势","%",chartSamples(rows,"healthPercent",dateKey="label"),framed=false);health.safeText("disclaimer").takeIf{it.isNotBlank()}?.let{Text(it,fontSize=11.sp,color=muted)}}}
 Text("行程能耗排行",fontSize=17.sp,fontWeight=FontWeight.SemiBold,color=ink)
 CompactSegments(listOf("thirtyDays" to "近30天","all" to "全部"),window,{window=it})
 val ranks=root.optJSONObject("rankingWindows")?.optJSONObject(window)
 for((key,title)in listOf("topEnergyTrips" to "总耗电排行","topAvgEnergyTrips" to "平均能耗排行")){val entries=ranks?.optJSONObject(key)?.optJSONArray("items")?:JSONArray();Panel{SectionTitle(title,Icons.Outlined.BarChart);if(entries.length()==0)Text("暂无排行记录",fontSize=12.sp,color=muted);entries.objects().take(5).forEachIndexed{i,row->Row(Modifier.fillMaxWidth().clickable(enabled=row.has("id")||row.has("tripId")){openTrip(row)}.padding(vertical=10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){Text("${i+1}",fontSize=17.sp,color=accent);Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)){Text(row.safeText("title","行程"),fontSize=13.sp,color=ink);Text(row.safeText("meta"),fontSize=10.sp,color=muted)};Text(row.safeText("value"),fontSize=13.sp,color=ink);if(row.has("id")||row.has("tripId"))Icon(Icons.Outlined.ChevronRight,null,tint=muted,modifier=Modifier.size(15.dp))}}}}
 (dashboard?.optJSONArray("lists")?:JSONArray()).objects().filter{it.safeText("key")=="frequentRoutes"}.forEach{list->Panel{SectionTitle("高频路线",Icons.Outlined.Route);val items=list.optJSONArray("items")?:JSONArray();if(items.length()==0)Text("暂无高频路线",fontSize=12.sp,color=muted);items.objects().forEach{row->Text(row.safeText("title",row.safeText("label",row.safeText("routeName"))),fontSize=13.sp,color=ink);Text(row.safeText("value")+" "+row.safeText("meta"),fontSize=11.sp,color=muted)}}}
 root.optJSONObject("cityFootprint")?.let{footprint->val cities=footprint.optJSONArray("topCities")?:JSONArray();if(cities.length()>0)Panel{SectionTitle("热门到访城市",Icons.Outlined.LocationCity);cities.objects().take(5).forEach{row->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(row.safeText("name"),fontSize=13.sp,color=ink);Text(row.safeText("visitTripCount")+" 次",fontSize=12.sp,color=muted)}}}}
}
