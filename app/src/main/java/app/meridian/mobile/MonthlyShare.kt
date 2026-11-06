package app.meridian.mobile

import android.graphics.*
import android.text.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.*
import kotlinx.coroutines.*
import org.json.JSONObject

internal fun monthlyShareValues(data:JSONObject,includeStory:Boolean):JSONObject=JSONObject().apply{
 for(key in listOf("_teslamate","knownEnergyConsumption","energyCoverage","month","availDays","monthMileage","totalEnergyConsumption","avgEnergyConsumption","maxTripMileage","maxTripDuration","hnoaMileage","cnoaMileage","basicPilotMileage","apaCnt","parkingCnt","avpCnt","drivingEnergyPercent","tempCtrlEnergyPercent","deviceEnergyPercent","otherEnergyPercent"))if(data.has(key)&&!data.isNull(key))put(key,data.get(key))
 if(includeStory)put("storySummary",data.safeText("storySummary").take(900))
}

private suspend fun createMonthlyPoster(source:JSONObject,month:String,includeStory:Boolean,colors:PosterTheme,fullReport:Boolean,coverStyle:String,nickname:String):Bitmap=withContext(Dispatchers.Default){
 val data=monthlyShareValues(source,includeStory)
 val metrics=listOf(Triple("用车天数","availDays","天"),Triple("行驶里程","monthMileage","km"),Triple(if(data.optBoolean("_teslamate"))"已知估算耗电"else"累计耗电",if(data.optBoolean("_teslamate"))"knownEnergyConsumption"else"totalEnergyConsumption","kWh"),Triple("平均能耗","avgEnergyConsumption","kWh/100km"),Triple("单次最远","maxTripMileage","km"),Triple("单次最长","maxTripDuration",""),Triple("高速领航","hnoaMileage","km"),Triple("城市领航","cnoaMileage","km"),Triple("基础辅助","basicPilotMileage","km"),Triple("自动泊车","apaCnt","次"),Triple("代客泊车","avpCnt","次"),Triple("行驶能耗占比","drivingEnergyPercent","%"),Triple("温控能耗占比","tempCtrlEnergyPercent","%"),Triple("设备能耗占比","deviceEnergyPercent","%"),Triple("其他能耗占比","otherEnergyPercent","%")).filter{data.has(it.second)}.map{(label,key,unit)->HomeMetricValue(label,if(key=="maxTripDuration")data.number(key)?.let{durationText(it.toLong())}?:"—"else homeValue(data.opt(key)),unit)}
 val rows=flexibleArray(source.opt("dayMileages"))
 val selection=shareReportContent(metrics,monthlyReportCharts(source),fullReport)
 val story=listOf(data.safeText("energyCoverage"),data.safeText("storySummary")).filter{it.isNotBlank()}.joinToString("\n");val excerpt=if(fullReport||story.length<=180)story else story.take(180)+"…"
 usagePoster("月度用车回顾",month,selection.metrics,story=excerpt,visualCharts=selection.charts,colors=colors,coverStyle=coverStyle,nickname=nickname)
}

@Composable internal fun MonthlyShare(data:JSONObject,month:String,request:Int=0,onConsumed:()->Unit={}){
 val colors=posterTheme(LocalPalette.current)
 val action=rememberServerAction();val context=LocalContext.current;var open by remember{mutableStateOf(false)};var includeStory by remember{mutableStateOf(false)};var poster by remember{mutableStateOf<Bitmap?>(null)}
 var fullReport by remember{mutableStateOf(false)};var generating by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")};var readyKey by remember{mutableStateOf("")};var coverStyle by remember{mutableStateOf("classic")};var nickname by remember{mutableStateOf("")}
 val exportKey=listOf(data.toString(),includeStory,month,colors,fullReport,PrivacyMode.enabled,coverStyle,nickname).toString()
 LaunchedEffect(request){if(request>0){open=true;onConsumed()}}
 if(open)FlowDetailPanel(onDismiss={open=false;poster=null},title={Text("月报分享")},body={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
  CompactSegments(listOf(false to "精简分享",true to "完整报告"),fullReport,{if(fullReport!=it){generating=true;fullReport=it}})
  CompactSegments(listOf("classic" to "经典","minimal" to "留白","gallery" to "画廊"),coverStyle,{generating=true;coverStyle=it})
  OutlinedTextField(nickname,{generating=true;nickname=it.take(22)},placeholder={Text("车辆昵称（可选）",fontSize=12.sp)},singleLine=true,modifier=Modifier.fillMaxWidth())
  Text(if(fullReport)"全部有效指标与图表"else"重点指标、最多3张图表，回顾文字显示摘要",fontSize=12.sp,color=muted)
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("包含回顾文字",Modifier.weight(1f),fontSize=13.sp);Switch(includeStory,{if(includeStory!=it){generating=true;includeStory=it}})}
  Text("默认仅展示统计指标，不含账号、车辆标识和地址。回顾文字可能提及地点，请检查后分享。",fontSize=12.sp,lineHeight=20.sp,color=muted)
  LaunchedEffect(exportKey){generating=true;error="";try{delay(220);poster=createMonthlyPoster(data,month,includeStory,colors,fullReport,coverStyle,nickname);readyKey=exportKey}catch(e:Exception){if(e is CancellationException)throw e;error=e.message?:"月报图片生成失败"}finally{generating=false}}
  Box(Modifier.fillMaxWidth().height(24.dp)){if(generating)FlowLoading(label="正在更新月报…")}
  if(error.isNotBlank())Text(error,fontSize=12.sp,color=MaterialTheme.colorScheme.error)
  ServerNotice(action)
  poster?.let{Image(it.asImageBitmap(),"月报图片预览",Modifier.fillMaxWidth())}
 }},confirm={CinemaButton({poster?.let{image->action.run{shareTripPoster(context,image,"月报图片")}}},enabled=poster!=null&&readyKey==exportKey&&!action.busy&&!generating&&error.isBlank()){Text("分享月报图片")}},dismiss={CinemaTextButton({open=false;poster=null}){Text("取消")}})
}
