package app.meridian.mobile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import org.json.*

@Composable fun MonthlyReportContent(d:JSONObject){
 val context=LocalContext.current
 Panel{
  Text("月度用车回顾",fontSize=18.sp,fontWeight=FontWeight.Bold,color=ink)
  d.safeText("monthlyTag").takeIf{it.isNotBlank()}?.let{Text(it,fontSize=11.sp,color=accent)}
  val metrics=listOf(Triple("用车天数",d.number("availDays"),"天"),Triple("月度总里程",d.number("monthMileage"),"km"),Triple(if(d.optBoolean("_teslamate"))"已知估算耗电"else"月度总耗电",d.number(if(d.optBoolean("_teslamate"))"knownEnergyConsumption"else"totalEnergyConsumption"),"kWh"),Triple("平均能耗",d.number("avgEnergyConsumption"),"kWh/100km"))
  d.safeText("energyCoverage").takeIf{it.isNotBlank()}?.let{Text(it,fontSize=12.sp,color=muted)}
  metrics.chunked(2).forEach{chunk->Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max),horizontalArrangement=Arrangement.spacedBy(9.dp)){chunk.forEach{(label,value,unit)->DetailMetric(label,numberText(value),unit,metricIcon(label),Modifier.weight(1f))}}}
 }
 d.optJSONObject("periodComparison")?.let{comparison->Panel{SectionTitle("本月亮点",Icons.Outlined.Insights);for((label,keys,unit)in listOf(Triple("月度里程","currentMileage" to "previousMileage","km"),Triple("平均能耗","currentAvgEnergy" to "previousAvgEnergy","kWh/100km"),Triple("月度耗电","currentTotalEnergy" to "previousTotalEnergy","kWh"))){val current=comparison.number(keys.first);val previous=comparison.number(keys.second);if(current!=null){Row(Modifier.fillMaxWidth().padding(vertical=7.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)){Text(label,fontSize=13.sp,color=ink,fontWeight=FontWeight.Medium);Text(if(previous!=null)"较上月 "+String.format(java.util.Locale.getDefault(),"%+.1f",current-previous)+" "+unit else"暂无上月对比",fontSize=11.sp,color=if(label=="平均能耗"&&previous!=null&&current<previous)Color(0xFF24AA76)else accent)};Column(horizontalAlignment=Alignment.End){Text(numberText(current),fontSize=21.sp,fontWeight=FontWeight.Bold,color=ink);Text(unit,fontSize=11.sp,color=muted)}}}}}}
 val days=flexibleArray(d.opt("dayMileages"));Panel{SectionTitle("每日里程趋势",Icons.Outlined.BarChart);if(d.number("maxTripMileage")!=null||d.number("maxTripDuration")!=null)Text("单次最远 "+numberText(d.number("maxTripMileage"))+" km · 单次最久 "+(d.number("maxTripDuration")?.let{durationText(it.toLong())}?:"—"),fontSize=11.sp,color=muted);MileageBars(days)}
 if(!d.optBoolean("_teslamate"))Panel{SectionTitle("辅助驾驶",Icons.Outlined.AutoMode)
  val keys=listOf("hnoaMileage","cnoaMileage","basicPilotMileage");val available=keys.mapNotNull{d.number(it)};if(available.isNotEmpty()){Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max),horizontalArrangement=Arrangement.spacedBy(8.dp)){DetailMetric("辅助驾驶总里程",numberText(available.sum()),"km",Icons.Outlined.AutoMode,Modifier.weight(1f));DetailMetric("高速领航",numberText(d.number("hnoaMileage")),"km",Icons.Outlined.Route,Modifier.weight(1f))};Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max),horizontalArrangement=Arrangement.spacedBy(8.dp)){DetailMetric("城市领航",numberText(d.number("cnoaMileage")),"km",Icons.Outlined.LocationCity,Modifier.weight(1f));DetailMetric("基础辅助",numberText(d.number("basicPilotMileage")),"km",Icons.Outlined.DirectionsCar,Modifier.weight(1f))}}else Text("暂无辅助驾驶数据",fontSize=12.sp,color=muted)
  if(d.has("apaCnt")||d.has("parkingCnt"))Text("自动泊车 "+numberText(d.number("apaCnt")?:d.number("parkingCnt"))+" 次",fontSize=12.sp,color=ink)
  d.number("avpCnt")?.let{Text("代客泊车 "+numberText(it)+" 次",fontSize=12.sp,color=ink)}
 }
 val energy=listOf("行驶" to "drivingEnergyPercent","温控" to "tempCtrlEnergyPercent","设备" to "deviceEnergyPercent","其他" to "otherEnergyPercent").mapNotNull{(label,key)->d.number(key)?.takeIf{it>=0}?.let{label to it}}
 if(!d.optBoolean("_teslamate"))Panel{SectionTitle("能耗分布",Icons.Outlined.DonutLarge);if(energy.sumOf{it.second}>0)EnergyRing(energy,numberText(d.number("totalEnergyConsumption")),"kWh")else Text("暂无有效能耗分布数据",fontSize=12.sp,color=muted)}
 d.safeText("storySummary").takeIf{it.isNotBlank()}?.let{Panel{SectionTitle("月度回顾",Icons.Outlined.Article);Text(it,fontSize=13.sp,lineHeight=22.sp,color=ink)}}
}
@Composable private fun MileageBars(rows:JSONArray){
 val points=rows.objects().mapNotNull{j->j.number("mileage")?.takeIf{it>=0}?.let{j.safeText("date").takeLast(2) to it}}
 if(points.isEmpty()){Text("暂无每日里程记录",fontSize=12.sp,color=muted);return}
 val fill=accent;val grid=glassEdge;val label=muted;val paint=remember{android.graphics.Paint(3).apply{textAlign=android.graphics.Paint.Align.CENTER}}
 Canvas(Modifier.fillMaxWidth().height(190.dp)){
  val left=30.dp.toPx();val bottom=size.height-24.dp.toPx();val top=12.dp.toPx();val chartHeight=bottom-top;val max=points.maxOf{it.second}.coerceAtLeast(1.0);val step=(size.width-left)/points.size
  paint.textSize=9.sp.toPx();paint.color=label.toArgb()
  for(i in 0..3){val y=bottom-chartHeight*i/3;drawLine(grid,androidx.compose.ui.geometry.Offset(left,y),androidx.compose.ui.geometry.Offset(size.width,y),1f);drawContext.canvas.nativeCanvas.drawText(numberText(max*i/3,0),left/2,y+3.dp.toPx(),paint)}
  points.forEachIndexed{i,p->val h=(p.second/max*chartHeight).toFloat();val x=left+step*i+step*.25f;val w=(step*.5f).coerceAtMost(18.dp.toPx());if(h>0)drawRoundRect(Brush.verticalGradient(listOf(fill,fill.copy(alpha=.15f))),topLeft=androidx.compose.ui.geometry.Offset(x,bottom-h),size=androidx.compose.ui.geometry.Size(w,h),cornerRadius=androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()));if(points.size<=12||i%3==0)drawContext.canvas.nativeCanvas.drawText(p.first,x+w/2,size.height-5.dp.toPx(),paint)}
 }
}
