package app.meridian.mobile

import android.content.Intent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.*
import org.json.*
import kotlin.math.*

fun JSONObject.number(key:String):Double?{val v=opt(key);val n=when(v){is Number->v.toDouble();is String->v.trim().toDoubleOrNull()?:Regex("[-+]?\\d+(?:\\.\\d+)?").find(v)?.value?.toDoubleOrNull();else->null};return n?.takeIf{it.isFinite()}}
fun numberText(v:Double?,precision:Int=1):String=if(v==null)"—"else if(v==floor(v))v.toLong().toString()else String.format(java.util.Locale.getDefault(),"%.${precision}f",v)
fun tripDistance(j:JSONObject)=j.number("mileageKm")?:j.number("drivingMileage")?.div(1000)
fun tripDuration(j:JSONObject)=j.number("durationMinutes")?:j.number("drivingDuration")?.div(60000)
fun tripDurationText(j:JSONObject)=tripDuration(j)?.let{durationText((it*60000).toLong())}?:"—"
fun shortTime(t:String)=t.replace('T',' ').substringAfter(' ',"").take(5).ifBlank{"—"}
fun performance(raw:Any?):List<Pair<String,Double>> =flexibleArray(raw).objects().mapNotNull{j->j.number("score")?.let{j.safeText("item") to it}}

@Composable fun TripTimelineCard(item:JSONObject,charge:Boolean,onClick:()->Unit){
 Box(Modifier.fillMaxWidth().background(glassWhite,RoundedCornerShape(19.dp)).border(.7.dp,glassEdge,RoundedCornerShape(19.dp)).clickable(onClick=onClick).padding(15.dp)){
  if(charge)Column(verticalArrangement=Arrangement.spacedBy(12.dp)){RecordPreview(item,true);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){Text("充电详情",fontSize=13.sp,color=accent);Icon(Icons.Outlined.ChevronRight,null,tint=accent,modifier=Modifier.size(16.dp))}}
  else Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
   Row(verticalAlignment=Alignment.CenterVertically){Text(shortTime(item.safeText("tripStartTime"))+" – "+shortTime(item.safeText("tripEndTime")),fontSize=17.sp,fontWeight=FontWeight.Bold,color=ink,modifier=Modifier.weight(1f));Text(if(item.number("tripEnergyConsumption")!=null)numberText(item.number("tripEnergyConsumption"))+" kWh/100km"else"行程记录",fontSize=12.sp,color=Color(0xFF4386DD),modifier=Modifier.background(Color(0xFF4386DD).copy(alpha=.09f),RoundedCornerShape(5.dp)).padding(horizontal=6.dp,vertical=3.dp));Icon(Icons.Outlined.ChevronRight,null,tint=accent,modifier=Modifier.size(18.dp))}
   Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){RoutePoint(item.safeText("startLocation",item.safeText("startAddress","出发地")),Color(0xFF31C581));RoutePoint(item.safeText("endLocation",item.safeText("endAddress","目的地")),Color(0xFFFF7950))}
    TripRouteThumbnail(item,Modifier.size(68.dp,54.dp))
   }
   HorizontalDivider(color=glassEdge.copy(alpha=.7f))
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){TimelineMetric(Icons.Outlined.Route,numberText(tripDistance(item)),"km");TimelineMetric(Icons.Outlined.Schedule,tripDurationText(item),"");TimelineMetric(Icons.Outlined.Speed,numberText(item.number("avgSpeed")),"km/h")}
  }
 }
}
@Composable private fun TimelineMetric(icon:androidx.compose.ui.graphics.vector.ImageVector,value:String,unit:String){Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)){Icon(icon,null,tint=muted,modifier=Modifier.size(13.dp));Text(value,fontSize=14.sp,fontWeight=FontWeight.Medium,color=ink);if(unit.isNotBlank())Text(unit,fontSize=11.sp,color=muted)}}

@Composable fun TripDetailPage(api:Api,data:JSONObject,onClose:()->Unit){
 val colors=posterTheme(LocalPalette.current)
 val ctx=LocalContext.current;val scope=rememberCoroutineScope();var insight by remember(data){mutableStateOf(data.optJSONObject("insight"))};var generating by remember{mutableStateOf(false)};var issue by remember{mutableStateOf("")}
 val tripId=data.opt("id")?:data.opt("tripId")
 var posterJob by remember{mutableStateOf<Job?>(null)}
 var privateShare by remember{mutableStateOf(SharePrivacy())}
 var poster by remember{mutableStateOf<android.graphics.Bitmap?>(null)};var sharing by remember{mutableStateOf(false)}

 LaunchedEffect(insight?.safeText("status"),tripId){if(tripId!=null&&insight?.safeText("status")in listOf("PENDING","RUNNING")){generating=true;try{api.streamEndpoint("/car/trip/insight/stream",JSONObject().put("tripId",tripId)){event->when(event.safeText("type")){"analysis"->event.optJSONObject("insight")?.let{insight=it};"error"->issue=event.safeText("message","行程洞察暂不可用")}}}catch(e:Exception){if(e is CancellationException)throw e;issue=e.message?:"洞察读取失败"}finally{generating=false}}}
 DisposableEffect(Unit){onDispose{api.stopStream()}}
 FullscreenDataPage("行程详情",onClose,action={QuietIconButton(Icons.Outlined.IosShare,"生成行程图片",{posterJob=scope.launch{sharing=true;issue="";try{poster=createTripPoster(data,insight,privateShare,colors)}catch(e:Exception){if(e is CancellationException)throw e;issue=e.message?:"图片生成失败"}finally{sharing=false}}},enabled=!sharing)}){
   if(sharing)FlowLoading(Modifier.padding(horizontal=18.dp),label="正在生成分享图片…")
   poster?.let{image->AlertDialog(onDismissRequest={posterJob?.cancel();poster=null;sharing=false},title={Text("行程分享预览")},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
    fun updatePrivacy(next:SharePrivacy){if(!sharing){privateShare=next;sharing=true;posterJob=scope.launch{try{poster=createTripPoster(data,insight,next,colors)}catch(e:Exception){if(e is CancellationException)throw e;issue=e.message?:"图片生成失败";poster=null}finally{sharing=false}}}}
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("隐藏起终点地址",Modifier.weight(1f),fontSize=14.sp);Switch(privateShare.hideAddresses,{updatePrivacy(privateShare.copy(hideAddresses=it))})}
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("隐藏路线与位置",Modifier.weight(1f),fontSize=14.sp);Switch(privateShare.hideRoute,{updatePrivacy(privateShare.copy(hideRoute=it))})}
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("隐藏精确时间",Modifier.weight(1f),fontSize=14.sp);Switch(privateShare.hideTime,{updatePrivacy(privateShare.copy(hideTime=it))})}
    if(privateShare.protected)Text("隐私保护开启时，图片不包含自由文本洞察。",fontSize=13.sp,color=muted)
    if(sharing)FlowLoading(label="正在更新预览…")
    Image(image.asImageBitmap(),"行程分享图片",Modifier.fillMaxWidth().aspectRatio(image.width.toFloat()/image.height))
   }},confirmButton={Button(enabled=!sharing,onClick={scope.launch{try{shareTripPoster(ctx,image)}catch(e:Exception){if(e is CancellationException)throw e;issue=e.message?:"图片分享失败"}}}){Text("分享图片")}},dismissButton={TextButton({posterJob?.cancel();poster=null;sharing=false}){Text("取消")}})}


    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){Icon(Icons.Outlined.Schedule,null,tint=muted,modifier=Modifier.size(14.dp));Text(prettyTime(data.safeText("tripStartTime"))+" 至 "+shortTime(data.safeText("tripEndTime")),fontSize=13.sp,color=muted)}
    if(trackPositions(data).size>=2||endpointPositions(data).size>=2)RouteTrackMap(data,height=230.dp)else Panel{SectionTitle("行程地图",Icons.Outlined.Map);Text("本次行程暂无 GPS 轨迹数据",fontSize=14.sp,color=muted)}
    Panel{
     SectionTitle("行程概览",Icons.Outlined.DirectionsCar)
     Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max),horizontalArrangement=Arrangement.spacedBy(8.dp)){DetailMetric("行驶里程",numberText(tripDistance(data)),"km",Icons.Outlined.Route,Modifier.weight(1f));DetailMetric("行驶时长",tripDurationText(data),"",Icons.Outlined.Schedule,Modifier.weight(1f));DetailMetric("平均速度",numberText(data.number("avgSpeed")),"km/h",Icons.Outlined.Speed,Modifier.weight(1f))}
     HorizontalDivider(color=glassEdge)
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("最高速度",fontSize=14.sp,color=muted);Text(numberText(data.number("maxSpeed"))+" km/h",fontSize=14.sp,color=accent,fontWeight=FontWeight.SemiBold)}
     data.number("maxSpeed")?.let{speed->val fill=accent;val track=softWash;Canvas(Modifier.fillMaxWidth().height(5.dp)){drawRoundRect(track,cornerRadius=androidx.compose.ui.geometry.CornerRadius(8f));drawRoundRect(Brush.horizontalGradient(listOf(fill.copy(alpha=.45f),fill)),size=androidx.compose.ui.geometry.Size(size.width*(speed/180).toFloat().coerceIn(0f,1f),size.height),cornerRadius=androidx.compose.ui.geometry.CornerRadius(8f))}}
    }
    TripNotebook(api,data)
    Panel{SectionTitle("行程路径",Icons.Outlined.LocationOn);RouteDetailRow("起点",data.safeText("startLocation","出发地"),shortTime(data.safeText("tripStartTime")),Color(0xFF2EC786));RouteDetailRow("终点",data.safeText("endLocation","目的地"),shortTime(data.safeText("tripEndTime")),Color(0xFFFF685A))}
    Panel{SectionTitle("行程信息",Icons.Outlined.Bolt)
     val rows=listOf(Triple("行程能耗",numberText(data.number("tripEnergyConsumption")),"kWh/100km"),Triple("累计耗电",numberText(data.number("totalPowerConsumption")),"kWh"),Triple("怠速耗电",numberText(data.number("idlePowerConsumption")),"kWh"),Triple("起止续航",numberText(data.number("startRemainingRange"))+" → "+numberText(data.number("endRemainingRange")),"km"),Triple("消耗预估续航",numberText(data.number("consumedRange")),"km"),Triple("续航达成率",numberText(data.number("consumptionAchievementRate")),"%"))
     rows.chunked(2).forEach{chunk->Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max),horizontalArrangement=Arrangement.spacedBy(8.dp)){chunk.forEach{(label,value,unit)->DetailMetric(label,value,unit,metricIcon(label),Modifier.weight(1f))}}}
    }
    Panel{SectionTitle("驾驶评分",Icons.Outlined.VerifiedUser);val scores=performance(data.opt("drivingPerformance"));if(scores.isEmpty())Text("暂无驾驶评分数据",color=muted,fontSize=14.sp)else DrivingRadar(scores)}
    Panel{SectionTitle("能耗分布",Icons.Outlined.DonutLarge);val values=performance(data.opt("energyPerformance")).filter{it.second>=0};if(values.isEmpty())Text("暂无能耗分布数据",fontSize=14.sp,color=muted)else EnergyRing(values,numberText(data.number("tripEnergyConsumption")),"kWh/100km")}
    Panel{
     Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.AutoAwesome,null,tint=accent,modifier=Modifier.size(18.dp));Text("行程洞察",fontSize=15.sp,fontWeight=FontWeight.SemiBold,color=ink,modifier=Modifier.weight(1f).padding(start=8.dp));TextButton(onClick={if(tripId!=null)scope.launch{generating=true;issue="";try{insight=api.call("/car/trip/insight/regenerate",JSONObject().put("tripId",tripId)).optJSONObject("data")}catch(e:Exception){if(e is CancellationException)throw e;issue=e.message?:"生成失败"}finally{generating=false}}},enabled=tripId!=null&&!generating){Icon(Icons.Outlined.Refresh,"重新生成",modifier=Modifier.size(18.dp))}}
     if(generating){FlowLoading(Modifier.fillMaxWidth());Text("正在分析本次行程…",fontSize=14.sp,color=muted)}
     if(issue.isNotBlank())Text(issue,fontSize=14.sp,color=MaterialTheme.colorScheme.error)
     val text=insight?.safeText("summary").orEmpty();if(text.isNotBlank())Text(text,fontSize=14.sp,lineHeight=22.sp,color=ink)else if(!generating)Text("暂无行程洞察，可点击右上角生成",fontSize=14.sp,color=muted)
     val reasons=insight?.optJSONArray("reasons")?:JSONArray();for(i in 0 until reasons.length())Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max),horizontalArrangement=Arrangement.spacedBy(8.dp)){Box(Modifier.padding(top=7.dp).size(4.dp).background(muted,CircleShape));Text(reasons.optString(i),fontSize=14.sp,lineHeight=20.sp,color=muted)}
     insight?.safeText("advice")?.takeIf{it.isNotBlank()}?.let{advice->Column(Modifier.fillMaxWidth().background(accent.copy(alpha=.065f),RoundedCornerShape(13.dp)).padding(13.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){Text("驾驶建议",fontSize=13.sp,color=accent,fontWeight=FontWeight.SemiBold);Text(advice,fontSize=14.sp,lineHeight=20.sp,color=ink)}}
    }
 }
}
@Composable private fun RouteDetailRow(label:String,address:String,time:String,tint:Color){Row(Modifier.fillMaxWidth().background(softWash,RoundedCornerShape(13.dp)).padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)){Box(Modifier.size(7.dp).background(tint,CircleShape));Text(label,color=muted,fontSize=13.sp);Text(address,color=ink,fontSize=14.sp,fontWeight=FontWeight.Medium,modifier=Modifier.weight(1f));Text(time,fontSize=13.sp,color=muted)}}
@Composable fun DetailMetric(label:String,value:String,unit:String,icon:androidx.compose.ui.graphics.vector.ImageVector,modifier:Modifier=Modifier){MetricDataCard(label,value,unit,icon,modifier.fillMaxHeight())}

@Composable fun DrivingRadar(scores:List<Pair<String,Double>>){
 if(scores.size<3){scores.forEach{(label,value)->Text(label+" · "+numberText(value),fontSize=15.sp,color=ink)};return}
 val line=glassEdge;val color=Color(0xFF25C783);val text=muted;val paint=remember{android.graphics.Paint(3).apply{textAlign=android.graphics.Paint.Align.CENTER}}
 Canvas(Modifier.fillMaxWidth().height(220.dp)){
  val center=Offset(size.width/2,size.height/2);val radius=min(size.width,size.height)*.35f;val count=scores.size
  fun vertex(i:Int,factor:Float):Offset{val angle=-PI/2+2*PI*i/count;return center+Offset((cos(angle)*radius*factor).toFloat(),(sin(angle)*radius*factor).toFloat())}
  fun polygon(factor:Float)=Path().apply{val p=vertex(0,factor);moveTo(p.x,p.y);for(i in 1 until count){val v=vertex(i,factor);lineTo(v.x,v.y)};close()}
  for(i in 1..4)drawPath(polygon(i/4f),line,style=Stroke(1.dp.toPx()))
  for(i in scores.indices)drawLine(line,center,vertex(i,1f),1.dp.toPx())
  val values=Path().apply{scores.forEachIndexed{i,v->val p=vertex(i,(v.second/100).toFloat().coerceIn(0f,1f));if(i==0)moveTo(p.x,p.y)else lineTo(p.x,p.y)};close()};drawPath(values,color.copy(alpha=.13f));drawPath(values,color,style=Stroke(2.5.dp.toPx()))
  paint.textSize=11.sp.toPx();paint.color=text.toArgb();scores.forEachIndexed{i,v->val p=vertex(i,1.27f);drawContext.canvas.nativeCanvas.drawText(v.first,p.x,p.y+4.dp.toPx(),paint)}
 }
}
@Composable fun EnergyRing(values:List<Pair<String,Double>>,centerValue:String,unit:String){
 val colors=values.map{(name,_)->when{ name.contains("驱动")||name.contains("行驶")->Color(0xFFFF7900);name.contains("温控")->Color(0xFF2689F4);name.contains("设备")->Color(0xFF7864F6);name.contains("回收")->Color(0xFF2BC68A);else->Color(0xFF9AA5B6)}};val total=values.sumOf{it.second}
 if(total<=0){Text("暂无能耗分布数据",fontSize=14.sp,color=muted);return}
 Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(20.dp)){
  Box(Modifier.size(132.dp),contentAlignment=Alignment.Center){Canvas(Modifier.fillMaxSize().padding(9.dp)){var start=-90f;values.forEachIndexed{i,v->val sweep=(v.second/total*360).toFloat();drawArc(colors[i%colors.size],start+1,sweep.coerceAtLeast(2f)-2,false,style=Stroke(12.dp.toPx()));start+=sweep}};Column(horizontalAlignment=Alignment.CenterHorizontally){Text(centerValue,fontSize=25.sp,fontWeight=FontWeight.Bold,color=ink);Text(unit,fontSize=8.sp,color=muted)}}
  Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(13.dp)){values.forEachIndexed{i,v->Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){Box(Modifier.size(5.dp,12.dp).background(colors[i%colors.size],RoundedCornerShape(2.dp)));Text(v.first,fontSize=13.sp,color=muted,modifier=Modifier.weight(1f));Text(numberText(v.second/total*100,0)+"%",fontSize=14.sp,fontWeight=FontWeight.SemiBold,color=ink)}}}
 }
}
