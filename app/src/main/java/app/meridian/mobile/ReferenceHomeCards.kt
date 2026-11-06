package app.meridian.mobile

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import org.json.*
import kotlin.math.*

internal val LocalHomeCardTint=staticCompositionLocalOf{Color.Transparent}
internal val LocalReferenceHome=staticCompositionLocalOf{false}
@Composable internal fun ReferenceCard(content:@Composable ColumnScope.()->Unit){Column(Modifier.fillMaxWidth().fillMaxHeight().themeCardShadow(RoundedCornerShape(18.dp)).background(Brush.verticalGradient(listOf(if(LocalHomeCardTint.current==Color.Transparent)glassWhite else androidx.compose.ui.graphics.lerp(glassWhite,LocalHomeCardTint.current,if(LocalPalette.current.dark).22f else .24f),glassWhite)),RoundedCornerShape(18.dp)).border(.6.dp,glassEdge,RoundedCornerShape(18.dp)).padding(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp),content=content)}
@Composable internal fun HomeMiniTrend(rows:JSONArray,title:String="近30天里程 · km"){
 val samples=chartSamples(rows,"mileage",.001);var selected by remember(rows.toString()){mutableIntStateOf(-1)};val tint=accent;val grid=glassEdge
 if(samples.isNotEmpty()){
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(title,fontSize=11.sp,color=muted);if(selected in samples.indices)Text(numberText(samples[selected].value)+" km",fontSize=11.sp,color=ink)}
  Canvas(Modifier.fillMaxWidth().height(83.dp).pointerInput(samples){detectTapGestures{p->selected=((p.x/size.width)*(samples.size-1)).roundToInt().coerceIn(samples.indices)}}){
   val max=samples.maxOf{it.value}.coerceAtLeast(1.0);val bottom=size.height-4.dp.toPx();fun position(i:Int)=Offset(if(samples.size==1)size.width/2 else i*size.width/(samples.size-1),bottom-(samples[i].value/max*(bottom-8.dp.toPx())).toFloat())
   repeat(3){i->val y=bottom*i/2;drawLine(grid.copy(alpha=.7f),Offset(0f,y),Offset(size.width,y),.6.dp.toPx())}
   val line=Path();samples.indices.forEach{i->val q=position(i);if(i==0)line.moveTo(q.x,q.y)else line.lineTo(q.x,q.y)}
   if(samples.size>1){val area=Path().apply{addPath(line);lineTo(size.width,bottom);lineTo(0f,bottom);close()};drawPath(area,Brush.verticalGradient(listOf(tint.copy(alpha=.2f),Color.Transparent)));drawPath(line,tint,style=Stroke(1.6.dp.toPx()))}else drawCircle(tint,3.dp.toPx(),position(0))
   if(selected in samples.indices){val q=position(selected);drawLine(tint.copy(alpha=.4f),Offset(q.x,0f),Offset(q.x,bottom),1.dp.toPx());drawCircle(tint,3.dp.toPx(),q)}
  }
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text((if(selected in samples.indices)samples[selected]else samples.first()).label.takeLast(5),fontSize=11.sp,color=muted);Text(samples.last().label.takeLast(5),fontSize=11.sp,color=muted)}
 }else Text("暂无里程趋势",fontSize=12.sp,color=muted)
}
@Composable internal fun CompactDrivingCard(ad:Double?,human:Double?){
 val valid=ad!=null&&human!=null&&ad>=0&&human>=0&&ad+human>0
 ReferenceCard{Text("驾驶方式",fontSize=14.sp,fontWeight=FontWeight.Medium,color=ink)
  if(valid){val ratio=ad!!/(ad+human!!);val tint=accent;val track=glassEdge
   Box(Modifier.fillMaxWidth().height(96.dp),contentAlignment=Alignment.Center){Canvas(Modifier.size(88.dp).padding(7.dp)){drawArc(track,-90f,360f,false,style=Stroke(10.dp.toPx()));drawArc(tint,-90f,(ratio*360).toFloat(),false,style=Stroke(10.dp.toPx()))};Column(horizontalAlignment=Alignment.CenterHorizontally){Text("辅助",fontSize=11.sp,color=muted);Text("${(ratio*100).roundToInt()}%",fontSize=20.sp,fontWeight=FontWeight.Medium)}}
   Text("辅助 "+numberText(ad)+" km",fontSize=11.sp,color=accent);Text("人工 "+numberText(human)+" km",fontSize=11.sp,color=muted)
  }else Text("暂无驾驶方式数据",fontSize=12.sp,color=muted)
 }
}
@Composable internal fun ReferenceAnalysis(metrics:JSONArray,onOpen:()->Unit){
 ReferenceCard{
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("综合分析",fontSize=16.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f));Text("近30天",fontSize=12.sp,color=muted)}
  val items=metrics.objects();if(items.isEmpty())Text("暂无分析数据",fontSize=13.sp,color=muted)
  items.take(3).forEach{item->val label=item.safeText("label");Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
   Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)){Text(label,fontSize=12.sp,color=muted);Text(homeValue(item.opt("currentValue"))+" "+analysisUnit(item),fontSize=17.sp,fontWeight=FontWeight.Medium)}
   Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)){Text(item.number("changePercent")?.let{String.format(java.util.Locale.getDefault(),"较上周期 %+.1f%%",it)}?:"暂无对比",fontSize=11.sp,color=accent);val a=item.number("currentValue");val b=item.number("previousValue");val color=accent;val track=glassEdge
    if(a!=null&&b!=null&&a>=0&&b>=0&&max(a,b)>0)Canvas(Modifier.fillMaxWidth().height(6.dp)){drawRoundRect(track,cornerRadius=androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()));drawRoundRect(color,size=androidx.compose.ui.geometry.Size(size.width*(a/max(a,b)).toFloat(),size.height),cornerRadius=androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))}
   }
  }}
  SolidActionButton(onOpen,Modifier.fillMaxWidth()){Icon(Icons.Outlined.Insights,null,Modifier.size(16.dp));Spacer(Modifier.width(6.dp));Text("查看完整分析",fontSize=13.sp);Spacer(Modifier.weight(1f));Icon(Icons.Outlined.ChevronRight,null,Modifier.size(16.dp))}
 }
}

@Composable internal fun CompactDailyMetrics(values:List<HomeMetricValue>){
 Column(Modifier.fillMaxWidth().background(softWash,RoundedCornerShape(12.dp)).border(.5.dp,glassEdge,RoundedCornerShape(12.dp))){values.chunked(2).forEachIndexed{i,row->if(i>0)HorizontalDivider(color=glassEdge,thickness=.5.dp);Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max)){row.forEachIndexed{j,v->if(j>0)Box(Modifier.fillMaxHeight().width(.5.dp).background(glassEdge));Column(Modifier.weight(1f).padding(10.dp),verticalArrangement=Arrangement.spacedBy(3.dp)){Text(androidx.compose.ui.text.buildAnnotatedString{append(v.value);if(v.unit.isNotBlank()){append(" ");pushStyle(androidx.compose.ui.text.SpanStyle(fontSize=12.sp,color=muted,fontWeight=FontWeight.Normal));append(v.unit);pop()}},fontSize=18.sp,lineHeight=24.sp,fontWeight=FontWeight.Medium);Text(v.label,fontSize=11.sp,color=muted);if(v.trend.isNotBlank())Text(v.trend,fontSize=11.sp,lineHeight=14.sp,color=muted)}};if(row.size==1)Spacer(Modifier.weight(1f))}}}
}
