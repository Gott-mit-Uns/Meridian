package app.meridian.mobile

import android.graphics.*
import android.text.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.*
import kotlinx.coroutines.*
import org.json.JSONObject

internal fun statisticsShareMetrics(root:JSONObject,days:Int,mode:String):List<HomeMetricValue> = statisticsMetricGroups(root).filter{!emptyChargeGroup(it)}.filter{if(mode=="energy")it.key in listOf("energy","efficiency") else it.key !in listOf("energy","efficiency")}.flatMap{group->group.items.objects().mapNotNull{item->
 val label=item.safeText("label");val raw=item.opt("value")
 if(label.isBlank()||listOf("地址","账号","车架","车牌","电话","姓名","位置").any{label.contains(it)}||raw is JSONObject||raw is org.json.JSONArray||raw is String&&!raw.matches(Regex("[0-9\\s.,:%+−—/小时分钟天次年月日kmWhKCM°℃()（）→-]*")))null else HomeMetricValue(label.take(40),homeValue(raw).take(80),item.safeText("unit").take(24),if(group.scope=="lifetime")"累计指标"else"近 $days 天")
}}.filter(::shareMetricAvailable)


@Composable internal fun StatisticsShare(root:JSONObject,trend:JSONObject?,days:Int,mode:String,request:Int=0,onConsumed:()->Unit={},reportTitle:String=if(mode=="energy")"能耗分析"else"用车统计",periodLabel:String=java.time.LocalDate.now().minusDays(days.toLong()-1).toString()+" — "+java.time.LocalDate.now()){
 val colors=posterTheme(LocalPalette.current)
 val context=LocalContext.current;val action=rememberServerAction();var open by remember{mutableStateOf(false)};var image by remember{mutableStateOf<Bitmap?>(null)};var error by remember{mutableStateOf("")};var fullReport by remember{mutableStateOf(false)};var generating by remember{mutableStateOf(false)};var readyKey by remember{mutableStateOf("")};var coverStyle by remember{mutableStateOf("classic")};var nickname by remember{mutableStateOf("")}
 val exportKey=listOf(root.toString(),trend.toString(),days,mode,colors,reportTitle,periodLabel,fullReport,PrivacyMode.enabled,coverStyle,nickname).toString()
 LaunchedEffect(request){if(request>0){open=true;onConsumed()}}
 if(open)FlowDetailPanel(onDismiss={open=false},title={Text("统计分享")},body={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
 CompactSegments(listOf(false to "精简分享",true to "完整报告"),fullReport,{if(fullReport!=it){generating=true;fullReport=it}})
 CompactSegments(listOf("classic" to "经典","minimal" to "留白","gallery" to "画廊"),coverStyle,{generating=true;coverStyle=it})
 OutlinedTextField(nickname,{generating=true;nickname=it.take(22)},placeholder={Text("车辆昵称（可选，不含车辆标识）",fontSize=12.sp)},singleLine=true,modifier=Modifier.fillMaxWidth())
 Text(if(fullReport)"全部有效指标与图表"else"重点指标与最多3张图表",fontSize=12.sp,color=muted)
 Text("仅分享统计指标与趋势，不含账号、车辆标识、地址和轨迹。累计指标会单独标注。",fontSize=12.sp,lineHeight=19.sp,color=muted)
 LaunchedEffect(exportKey){generating=true;error="";try{delay(220);val metrics=statisticsShareMetrics(root,days,mode);val source=trend?.optJSONObject("drivingTrend")?:trend;val charts=reportCharts(root,trend,mode);val selection=shareReportContent(metrics,charts,fullReport);image=withContext(Dispatchers.Default){usagePoster(reportTitle,periodLabel,selection.metrics.map{if(it.trend.startsWith("近 "))it.copy(trend=periodLabel)else it},visualCharts=selection.charts,colors=colors,coverStyle=coverStyle,nickname=nickname)};readyKey=exportKey}catch(e:Exception){if(e is CancellationException)throw e;error="图片生成失败，请重试"}finally{generating=false}}
 Box(Modifier.fillMaxWidth().height(24.dp)){if(generating)FlowLoading(label="正在排版统计图片…")}
 if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
 image?.let{Image(it.asImageBitmap(),"统计分享预览",Modifier.fillMaxWidth())};ServerNotice(action)
 }},confirm={CinemaButton({image?.let{bitmap->action.run{shareTripPoster(context,bitmap,"统计图片")}}},enabled=image!=null&&readyKey==exportKey&&!action.busy&&!generating&&error.isBlank()){Text("分享图片")}},dismiss={CinemaTextButton({open=false}){Text("取消")}})
}
