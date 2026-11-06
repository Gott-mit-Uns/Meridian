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

@Composable fun Health(api:Api,carId:Any,onClose:()->Unit){
 var days by remember{mutableIntStateOf(7)};var page by remember{mutableIntStateOf(1)}
 var summary by remember{mutableStateOf<JSONObject?>(null)};var history by remember{mutableStateOf<JSONObject?>(null)}
 var loading by remember{mutableStateOf(false)};var issue by remember{mutableStateOf("")};var retry by remember{mutableIntStateOf(0)}
 var excludeParking by remember{mutableStateOf(false)};var selected by remember{mutableStateOf("batteryLevel")}
 var brushStart by remember(carId,days,excludeParking){mutableFloatStateOf(0f)};var brushEnd by remember(carId,days,excludeParking){mutableFloatStateOf(100f)}
 var detailWindow by remember(carId,days,excludeParking){mutableStateOf<Pair<String,String>?>(null)}
 var bounds by remember(carId,days,excludeParking){mutableStateOf<Pair<String,String>?>(null)}
 LaunchedEffect(carId,days,page,retry,excludeParking,detailWindow){
  loading=true;issue=""
  try{
   val end=LocalDate.now()
   fun body()=JSONObject().put("carId",carId).put("startDate",end.minusDays(days-1L).toString()).put("endDate",end.toString()).put("excludeParking",excludeParking)
   val request=body();detailWindow?.let{request.put("detailStartTime",it.first).put("detailEndTime",it.second)}
   val next=api.call("/car/status/health",request).optJSONObject("data")?:throw Exception("未返回车况数据")
   summary=next
   if(bounds==null){
    val rows=next.optJSONArray("metricTrail")?.objects().orEmpty()
    val left=next.safeText("metricTrailStartTime",rows.firstOrNull()?.safeText("time").orEmpty()).ifBlank{end.minusDays(days-1L).toString()+" 00:00:00"}
    val right=next.safeText("metricTrailEndTime",rows.lastOrNull()?.safeText("time").orEmpty()).ifBlank{end.toString()+" 23:59:59"}
    if(healthDetailWindow(left,right,0f,100f)!=null)bounds=left to right
   }
   history=api.call("/car/status/history",body().put("pageNum",page).put("pageSize",20)).optJSONObject("data")
  }catch(e:Exception){if(e is CancellationException)throw e;issue=e.message?:"车况读取失败"}finally{loading=false}
 }
 var filterOpen by remember{mutableStateOf(false)}
 if(filterOpen)FlowDetailPanel(onDismiss={filterOpen=false},title={Text("车况筛选")},body={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
  CompactSegments(listOf(7 to "近7天",30 to "近30天"),days,{days=it;page=1})
  CinemaChoice(excludeParking,{excludeParking=!excludeParking;page=1},{Text("隐藏驻车")})
 }},confirm={CinemaButton({filterOpen=false}){Text("完成")}})
 FullscreenDataPage("车况详情",onClose,action={ToolbarActionStrip(listOf(ToolbarAction(Icons.Outlined.Tune,"车况筛选",active=days!=7||excludeParking){filterOpen=true}))},refreshing=loading,onRefresh={retry++},lastSynced=responseSyncTime(summary)){
  CachedReadStatus(summary){retry++}
  Text("近 $days 天"+if(excludeParking)" · 已隐藏驻车"else"",fontSize=12.sp,color=muted)
  if(loading)FlowLoading(label="正在更新车况…")
  if(issue.isNotBlank())Panel{Text(issue,color=muted);CinemaOutlineButton({retry++}){Text("重新加载")}}
  summary?.optJSONObject("eventOverview")?.let{overview->
   Panel{
    Text("状态概览",fontSize=17.sp,fontWeight=FontWeight.Medium,color=ink)
    Text("近 $days 天 · 车辆状态事件",fontSize=12.sp,color=muted)
    val events=listOf(Triple("异常提醒","anomalyEvents",Icons.Outlined.HealthAndSafety),Triple("充电事件","chargeEvents",Icons.Outlined.EvStation),Triple("行驶事件","drivingEvents",Icons.Outlined.Route),Triple("驻车事件","parkingEvents",Icons.Outlined.LocalParking)).filter{!excludeParking||it.second!="parkingEvents"}
    events.chunked(2).forEach{chunk->EqualCardRow(chunk.size,surface={_,m->Box(m.background(softWash,RoundedCornerShape(16.dp)))}){i->
     val e=chunk[i];Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
      Icon(e.third,null,tint=if(e.second=="anomalyEvents"&&overview.optInt(e.second)>0)Color(0xFFD18B58)else Color(0xFF689D91),modifier=Modifier.size(21.dp))
      Text(homeValue(overview.opt(e.second)),fontSize=27.sp,lineHeight=34.sp,fontWeight=FontWeight.Medium,color=ink)
      Text(e.first+" · 次",fontSize=12.sp,lineHeight=18.sp,color=muted)
     }
    }}
   }
  }
  Panel{
   Text("车辆指标轨迹",fontSize=17.sp,fontWeight=FontWeight.Medium,color=ink)
   bounds?.let{limits->
    val chosen=healthDetailWindow(limits.first,limits.second,brushStart,brushEnd)
    Text("时间范围 · "+(chosen?.let{prettyTime(it.first)+" — "+prettyTime(it.second)}?:"请选择有效范围"),fontSize=11.sp,lineHeight=18.sp,color=muted)
    Text("开始",fontSize=11.sp,color=muted)
    CinemaSlider(brushStart,{brushStart=it.coerceAtMost(brushEnd-1f)},Modifier.fillMaxWidth(),0f..99f)
    Text("结束",fontSize=11.sp,color=muted)
    CinemaSlider(brushEnd,{brushEnd=it.coerceAtLeast(brushStart+1f)},Modifier.fillMaxWidth(),1f..100f)
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
     CinemaOutlineButton({brushStart=0f;brushEnd=100f;detailWindow=null},Modifier.weight(1f),enabled=!loading){Text("完整范围")}
     CinemaButton({detailWindow=chosen},Modifier.weight(1f),enabled=chosen!=null&&!loading){Text("应用范围")}
    }
   }
   CompactSegments(listOf("batteryLevel" to "电量","remainingRange" to "续航","insideTemp" to "温度","currentSpeed" to "车速"),selected,{selected=it})
   val labels=mapOf("batteryLevel" to ("电量变化" to "%"),"remainingRange" to ("续航变化" to "km"),"insideTemp" to ("车内温度" to "°C"),"currentSpeed" to ("车辆速度" to "km/h"))
   val metric=labels.getValue(selected)
   InteractiveTrend(metric.first,metric.second,chartSamples(summary?.optJSONArray("metricTrail")?:JSONArray(),selected,dateKey="time"),framed=false)
  }
  val timeline=summary?.optJSONArray("statusTimeline")?.objects().orEmpty()
  if(timeline.isNotEmpty())Panel{
   SectionTitle("状态时间线",Icons.Outlined.Timeline)
   timeline.forEach{entry->HealthTimelineEntry(entry)}
  }
  val rows=(history?.optJSONArray("list")?:history?.optJSONArray("records")?:JSONArray()).objects()
  Panel{
   SectionTitle("状态记录",Icons.Outlined.History)
   if(rows.isEmpty()&&!loading&&issue.isBlank())Text("当前范围暂无记录",fontSize=12.sp,color=muted)
   rows.forEach{entry->HealthTimelineEntry(entry)}
   if(rows.isNotEmpty()||page>1)Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
    CinemaOutlineButton({page--},Modifier.weight(1f),enabled=page>1&&!loading){Text("上一页")}
    Text("$page",fontSize=12.sp,color=muted)
    CinemaOutlineButton({page++},Modifier.weight(1f),enabled=rows.size==20&&!loading){Text("下一页")}
   }
  }
 }
}

@Composable private fun HealthTimelineEntry(entry:JSONObject){
 var expanded by remember(entry){mutableStateOf(false)}
 Column(Modifier.fillMaxWidth().background(softWash,RoundedCornerShape(16.dp)).clickable{expanded=!expanded}.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
   Text(entry.safeText("status",entry.safeText("type","车辆状态")),fontSize=14.sp,fontWeight=FontWeight.Medium,color=ink,modifier=Modifier.weight(1f))
   Icon(if(expanded)Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,"查看记录",modifier=Modifier.size(18.dp),tint=muted)
  }
  Text(prettyTime(entry.safeText("startTime",entry.safeText("observedAt",entry.safeText("createTime"))))+entry.safeText("endTime").takeIf{it.isNotBlank()}?.let{" — "+prettyTime(it)}.orEmpty(),fontSize=11.sp,lineHeight=17.sp,color=muted)
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(14.dp)){
   entry.number("batteryLevel")?.let{Text(numberText(it)+"%",color=ink,fontSize=13.sp)}
   entry.number("remainingRange")?.let{Text(numberText(it)+" km",color=ink,fontSize=13.sp)}
   entry.number("insideTemp")?.let{Text(numberText(it)+"°C",color=muted,fontSize=13.sp)}
  }
  if(expanded)Readable(entry)
 }
}
