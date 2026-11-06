package app.meridian.mobile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import kotlinx.coroutines.CancellationException
import org.json.*
import java.time.LocalDate

@Composable internal fun ComprehensiveAnalysis(data:JSONObject,onClose:()->Unit){
 val metrics=data.optJSONArray("metrics")?.objects().orEmpty();var selected by remember{mutableIntStateOf(0)}
 FullscreenDataPage("综合分析",onClose){
  Text("最近30天",fontSize=13.sp,color=muted)
  Text("每一程，都有迹可循",fontSize=25.sp,lineHeight=34.sp,fontWeight=FontWeight.Medium,color=ink)
  Text("对比前30天的用车表现，点选指标查看变化。",fontSize=12.sp,lineHeight=20.sp,color=muted)
  if(metrics.isNotEmpty()){
   Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(7.dp)){
    metrics.forEachIndexed{i,m->CinemaChoice(selected==i,{selected=i},{Text(m.safeText("label"))})}
   }
   val metric=metrics[selected.coerceIn(metrics.indices)];val label=metric.safeText("label")
   Panel{
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Icon(metricIcon(label),null,tint=Color(0xFF689D91),modifier=Modifier.size(22.dp));Text(label,fontSize=15.sp,color=muted,modifier=Modifier.padding(start=8.dp))}
    Text(homeValue(metric.opt("currentValue")),fontSize=42.sp,lineHeight=52.sp,fontWeight=FontWeight.Medium,color=ink)
    Text(analysisUnit(metric),fontSize=13.sp,color=muted)
    ComparisonBars("周期变化",metric.number("currentValue"),metric.number("previousValue"),analysisUnit(metric))
    val change=metric.number("changePercent")
    Text(if(change==null)"暂无可比较的上周期数据"else if(change==0.0)"较上周期持平"else"较上周期 "+String.format(java.util.Locale.getDefault(),"%+.1f%%",change),fontSize=13.sp,color=muted)
   }
   Panel{
    SectionTitle("其他指标",Icons.Outlined.Insights)
    metrics.filterIndexed{i,_->i!=selected}.forEach{m->
     Column(Modifier.fillMaxWidth().background(softWash,RoundedCornerShape(14.dp)).clickable{selected=metrics.indexOf(m)}.padding(14.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
      Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(m.safeText("label"),fontSize=13.sp,color=muted,modifier=Modifier.weight(1f));Icon(Icons.Outlined.ChevronRight,null,tint=muted,modifier=Modifier.size(15.dp))}
      Text(homeValue(m.opt("currentValue"))+" "+analysisUnit(m),fontSize=21.sp,lineHeight=28.sp,fontWeight=FontWeight.Medium,color=ink)
      Text("上周期 "+homeValue(m.opt("previousValue"))+" "+analysisUnit(m),fontSize=11.sp,lineHeight=17.sp,color=muted)
     }
    }
   }
  }else Panel{Text("当前范围暂无综合分析",fontSize=13.sp,color=muted)}
  data.optJSONObject("analysis")?.let{a->val ad=a.number("adMileage");val human=a.number("humanMileage");if(ad!=null&&human!=null&&ad+human>0)DonutCard("驾驶里程构成",listOf("辅助驾驶" to ad,"人工驾驶" to human),"km")}
 }
}

internal fun analysisUnit(metric:JSONObject)=metric.safeText("unit").ifBlank{when(metric.safeText("label")){
 "活跃天数"->"天";"总补能"->"kWh";"平均能耗"->"Wh/km";"最高车速","平均车速"->"km/h";"行程次数"->"次";else->""
}}

@Composable internal fun HomeDrivingTrends(api:Api,carId:Any,initial:JSONObject?){
 var days by remember{mutableIntStateOf(30)};var data by remember(carId){mutableStateOf(initial)};var loading by remember{mutableStateOf(false)};var issue by remember{mutableStateOf("")};var retry by remember{mutableIntStateOf(0)}
 LaunchedEffect(initial){if(days==30)data=initial}
 LaunchedEffect(carId,days,retry){
  if(days==30&&initial!=null&&retry==0){data=initial;return@LaunchedEffect}
  loading=true;issue=""
  try{val next=api.call("/dashboard/getTrendData",JSONObject().put("carId",carId).put("days",days).put("statDate",LocalDate.now().toString())).optJSONObject("data")?:throw Exception("趋势数据暂不可用");data=next.optJSONObject("drivingTrend")?:next}
  catch(e:Exception){if(e is CancellationException)throw e;issue=e.message?:"趋势读取失败"}finally{loading=false}
 }
 Panel{
  Text("行车趋势",fontSize=17.sp,fontWeight=FontWeight.Medium,color=ink)
  CompactSegments(listOf(30 to "近30天",60 to "近60天",0 to "全部"),days,{days=it})
  if(loading)FlowLoading(label="正在更新趋势…")
  if(issue.isNotBlank()){Text(issue,fontSize=12.sp,color=muted);CinemaOutlineButton({retry++}){Text("重新加载")}}
  if(issue.isBlank()&&!loading){
   CompositionLocalProvider(LocalPalette provides if(LocalGlobalTheme.current.id=="vivid")LocalPalette.current.copy(accent=Color(0xFF168BFF))else LocalPalette.current){InteractiveTrend("行驶里程","km",chartSamples(data?.optJSONArray("mileageStats")?:JSONArray(),"mileage",.001),framed=false)}
   CompositionLocalProvider(LocalPalette provides if(LocalGlobalTheme.current.id=="vivid")LocalPalette.current.copy(accent=Color(0xFFFF6A00))else LocalPalette.current){InteractiveTrend("行程能耗","Wh/km",chartSamples(data?.optJSONArray("energyTrend")?:JSONArray(),"energyConsumption"),framed=false)}
  }
 }
}
