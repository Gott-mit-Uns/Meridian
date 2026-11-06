package app.meridian.mobile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import android.graphics.drawable.ColorDrawable
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import kotlinx.coroutines.*
import org.json.*
import java.time.LocalDate

@Composable fun FullscreenDataPage(title:String,onClose:()->Unit,action:(@Composable ()->Unit)?=null,refreshing:Boolean=false,onRefresh:(()->Unit)?=null,lastSynced:Long=0,content:@Composable ColumnScope.()->Unit){
 val (progress,close)=rememberPageMotion(onClose);val fullMotion=app.meridian.mobile.glass.LocalAppMotion.current.full
 WindowSafeDialog(close,DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)){
  val view=LocalView.current;val dark=LocalPalette.current.dark;val background=LocalPalette.current.background[0]
  SideEffect{view.dialogWindow()?.let{prepareFullscreenWindow(it,view,dark);it.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))}}
  CompositionLocalProvider(LocalControlBackdrop provides null,LocalPageMotionHandled provides true){
   PredictiveSceneFrame(onBack=onClose){
    Box(Modifier.fillMaxSize().scenePresence(progress).background(Brush.verticalGradient(LocalPalette.current.background))){ImmersiveScrollingPage(title,close,action,refreshing,onRefresh,lastSynced,content)}
   }
  }
 }
}

@Composable fun DailyBriefPanel(api:Api,carId:Any,initial:JSONArray,compact:Boolean=false,trend:JSONArray?=null){
 val today=LocalDate.now();var date by remember(carId){mutableStateOf(today)}
 val cache=remember(carId){mutableMapOf(today to initial)}
 var rows by remember(carId){mutableStateOf(initial)};var busy by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")};var reload by remember{mutableIntStateOf(0)}
 var calendar by remember{mutableStateOf(false)};var detail by remember{mutableStateOf(false)}
 LaunchedEffect(initial){cache[today]=initial;if(date==today)rows=initial}
 LaunchedEffect(carId,date,reload){
  error="";val saved=cache[date]
  if(saved!=null){rows=saved;busy=false}else{busy=true;try{val result=api.call("/dashboard/get",JSONObject().put("carId",carId).put("statDate",date.toString())).optJSONObject("data");rows=result?.optJSONArray("dailyBrief")?:JSONArray();cache[date]=rows}catch(e:Exception){if(e is CancellationException)throw e;error=e.message?:"简报读取失败"}finally{busy=false}}
 }
 if(calendar)FlowDatePicker(date,{calendar=false}){date=it;calendar=false}
 if(detail)FullscreenDataPage("${date} · 行车简报",{detail=false}){HomeDetail(JSONObject().put("metrics",rows))}
 Panel{
  if(compact)Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
   Text(if(date==today)"今日概览"else"当日概览",fontSize=16.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.SemiBold,modifier=Modifier.weight(1f))
   Row(Modifier.then(if(LocalAppearance.current.glass)Modifier.cinemaGlass(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))else Modifier.background(softWash,androidx.compose.foundation.shape.RoundedCornerShape(14.dp))),verticalAlignment=Alignment.CenterVertically){
    BriefDateArrow(Icons.Outlined.ChevronLeft,"前一天",true){date=date.minusDays(1)}
    Box(Modifier.heightIn(min=40.dp).clickable{calendar=true}.padding(horizontal=7.dp),contentAlignment=Alignment.Center){Text("${date.monthValue}月${date.dayOfMonth}日"+if(date==today)" · 今天"else"",fontSize=11.sp,color=ink)}
    BriefDateArrow(Icons.Outlined.ChevronRight,"后一天",date<today){date=date.plusDays(1)}
   }
  }else{
   SectionTitle(if(date==today)"今日行车简报"else"当日行车简报",Icons.Outlined.Today)
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){QuietIconButton(Icons.Outlined.ChevronLeft,"前一天",{date=date.minusDays(1)});TextButton({calendar=true},Modifier.weight(1f)){Text(if(date==today)"今天 · $date"else date.toString(),fontSize=12.sp)};QuietIconButton(Icons.Outlined.ChevronRight,"后一天",{date=date.plusDays(1)},enabled=date<today)}
  }
  if(busy)FlowLoading(label="正在读取当天简报…")else if(error.isNotBlank()){Text(error,color=MaterialTheme.colorScheme.error,fontSize=12.sp);TextButton({cache.remove(date);reload++}){Text("重试")}}else{
   if(rows.length()==0)Text("当天暂无用车记录",fontSize=12.sp,color=muted)
   val tiles=rows.objects().map{dailyBriefMetric(it,date==today)}
   if(compact)CompactDailyMetrics(tiles)else HomeMetricTiles(tiles)
   if(compact&&trend!=null)HomeMiniTrend(trend)
  }
 }
}

@Composable fun VisualHomeDetails(j:JSONObject){
 val metrics=j.optJSONArray("metrics")
 if(metrics!=null){
  val comparisons=metrics.objects().filter{it.has("currentValue")||it.has("previousValue")}
  comparisons.forEach{m->val label=m.safeText("label",m.safeText("title"));val unit=m.safeText("unit").ifBlank{when(label){"活跃天数"->"天";"总补能"->"kWh";"平均能耗"->"Wh/km";"最高车速","平均车速"->"km/h";"行程次数"->"次";else->""}};ComparisonBars(label,m.number("currentValue"),m.number("previousValue"),unit)}
  val simple=metrics.objects().filterNot{it in comparisons}
  simple.chunked(2).forEach{chunk->Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max),horizontalArrangement=Arrangement.spacedBy(8.dp)){
   chunk.forEach{m->val label=m.safeText("label",m.safeText("title"));val raw=m.opt("value");val time=label.contains("驾驶")&&raw is Number
    MetricDataCard(label,if(time)durationText(((raw as Number).toDouble()*60000).toLong())else homeValue(raw),if(time)""else m.safeText("unit",when(label){"今日行程"->"次";"今日里程"->"km";"平均电耗"->"kWh/100km";else->""}),metricIcon(label),Modifier.weight(1f).fillMaxHeight(),footer=m.safeText("trend"))}
   if(chunk.size==1)Spacer(Modifier.weight(1f))
  }}
 }
 j.optJSONArray("points")?.let{rows->val mileage=rows.objects().any{it.has("mileage")};InteractiveTrend(if(mileage)"里程趋势"else"能耗趋势",if(mileage)"km"else"Wh/km",chartSamples(rows,if(mileage)"mileage"else"energyConsumption",if(mileage).001 else 1.0))}
 if(j.has("batteryLevel")||j.has("remainingRange"))BatteryRangeChart(j)
 if(j.has("currentPeriodEnergy")||j.has("previousPeriodEnergy"))ComparisonBars("能耗周期对比",j.number("currentPeriodEnergy"),j.number("previousPeriodEnergy"),"Wh/km")
 val ad=j.number("adMileage");val human=j.number("humanMileage")
 if(ad!=null&&human!=null&&ad+human>0)DonutCard("驾驶方式",listOf("辅助驾驶" to ad,"人工驾驶" to human),"%")
 for((title,keys)in listOf("能效分布" to listOf("低能耗" to "lowEnergyPercent","中能耗" to "midEnergyPercent","高能耗" to "highEnergyPercent"),"驾驶时段" to listOf("白天" to "daytimePercent","夜间" to "nightPercent"))){val values=keys.mapNotNull{(label,key)->j.number(key)?.let{label to it}};if(values.sumOf{it.second}>0)DonutCard(title,values,"%")}
 val fields=listOf(Triple("recent30Mileage","近30天里程","km"),Triple("recent30DrivingDurationMs","近30天驾驶",""),Triple("speed","当前车速","km/h"),Triple("insideTemp","车内温度","°C"),Triple("outsideTemp","车外温度","°C"),Triple("currentPowerKw","充电功率","kW"),Triple("electricVoltageV","充电电压","V"),Triple("electricCurrentA","充电电流","A"),Triple("chargeSpeedKmh","补能速度","km/h"))
 fields.filter{j.has(it.first)&&!j.isNull(it.first)}.chunked(2).forEach{chunk->Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max),horizontalArrangement=Arrangement.spacedBy(8.dp)){chunk.forEach{(key,label,unit)->MetricDataCard(label,if(key=="recent30DrivingDurationMs")durationText(j.optLong(key))else homeValue(j.opt(key)),unit,metricIcon(label),Modifier.weight(1f).fillMaxHeight())};if(chunk.size==1)Spacer(Modifier.weight(1f))}}
 if(listOf("carLocation","carMode","sentrySwitch","gearPosition").any{j.has(it)})Panel{SectionTitle("车辆状态",Icons.Outlined.VerifiedUser);for((key,label)in listOf("carLocation" to "车辆位置","carMode" to "车辆模式","sentrySwitch" to "哨兵模式","gearPosition" to "挡位")){j.safeText(key).takeIf{it.isNotBlank()}?.let{Text("$label  ·  $it",fontSize=13.sp,color=ink)}}}
 j.optJSONObject("analysis")?.let{VisualHomeDetails(it)}
 if(metrics==null&&!j.has("points")&&!j.has("batteryLevel")&&fields.none{j.has(it.first)}&&ad==null&&!j.has("currentPeriodEnergy")&&!j.has("lowEnergyPercent")&&!j.has("daytimePercent"))Panel{Readable(j)}
}

@Composable private fun BriefDateArrow(icon:androidx.compose.ui.graphics.vector.ImageVector,label:String,enabled:Boolean,onClick:()->Unit){Box(Modifier.size(36.dp,40.dp).clickable(enabled=enabled,role=androidx.compose.ui.semantics.Role.Button,onClickLabel=label,onClick=onClick),contentAlignment=Alignment.Center){Icon(icon,label,Modifier.size(17.dp),tint=muted.copy(alpha=if(enabled)1f else .3f))}}
