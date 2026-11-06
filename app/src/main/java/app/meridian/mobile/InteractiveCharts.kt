package app.meridian.mobile

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import org.json.*
import kotlin.math.*

data class ChartSample(val label:String,val value:Double)
fun chartSamples(rows:JSONArray,key:String,scale:Double=1.0,dateKey:String="date")=rows.objects().mapNotNull{row->row.number(key)?.times(scale)?.takeIf{it.isFinite()}?.let{ChartSample(row.safeText(dateKey).take(16),it)}}

@Composable fun InteractiveTrend(title:String,unit:String,samples:List<ChartSample>,framed:Boolean=true){
 val content:@Composable ColumnScope.()->Unit={
  var selected by remember(samples){mutableIntStateOf((samples.size-1).coerceAtLeast(0))}
  val color=accent;val grid=glassEdge;val secondary=muted
  Text(title,fontSize=15.sp,fontWeight=FontWeight.SemiBold,color=ink)
  if(samples.isEmpty())Text("暂无有效数据",fontSize=12.sp,color=muted)else{
   val point=samples[selected.coerceIn(samples.indices)]
   val navigate=LocalChartDateAction.current;val date=chartDate(point.label)
   if(navigate!=null&&date!=null)CinemaTonalButton({navigate(date)}){Icon(Icons.Outlined.Route,null,Modifier.size(16.dp));Spacer(Modifier.width(5.dp));Text("查看当天行程",fontSize=12.sp)}
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.Bottom,horizontalArrangement=Arrangement.SpaceBetween){Column{Text(point.label,fontSize=11.sp,color=muted);Text(numberText(point.value)+if(unit.isBlank())""else" $unit",fontSize=22.sp,fontWeight=FontWeight.SemiBold,color=ink)};Text("轻点 / 横滑查看",fontSize=11.sp,color=muted)}
   val low=min(0.0,samples.minOf{it.value});val high=samples.maxOf{it.value}.coerceAtLeast(low+.01)
   Canvas(Modifier.fillMaxWidth().height(166.dp).padding(vertical=10.dp)
    .pointerInput(samples){detectTapGestures{p->selected=((p.x/size.width.coerceAtLeast(1))*(samples.size-1)).roundToInt().coerceIn(samples.indices)}}
    .pointerInput(samples){detectHorizontalDragGestures{change,_->change.consume();selected=((change.position.x/size.width.coerceAtLeast(1))*(samples.size-1)).roundToInt().coerceIn(samples.indices)}}){
    fun pos(i:Int)=Offset(if(samples.size==1)size.width/2 else size.width*i/(samples.size-1),(size.height*(1-(samples[i].value-low)/(high-low))).toFloat())
    repeat(4){i->val y=size.height*i/3;drawLine(grid,Offset(0f,y),Offset(size.width,y),1.dp.toPx())}
    val path=Path();samples.indices.forEach{i->val p=pos(i);if(i==0)path.moveTo(p.x,p.y)else path.lineTo(p.x,p.y)}
    val area=Path().apply{addPath(path);lineTo(pos(samples.lastIndex).x,size.height);lineTo(pos(0).x,size.height);close()}
    if(samples.size>1)drawPath(area,Brush.verticalGradient(listOf(color.copy(alpha=.18f),Color.Transparent)))
    drawPath(path,color,style=Stroke(2.3.dp.toPx(),cap=StrokeCap.Round,join=StrokeJoin.Round))
    val p=pos(selected.coerceIn(samples.indices));drawLine(secondary.copy(alpha=.4f),Offset(p.x,0f),Offset(p.x,size.height),1.dp.toPx());drawCircle(color.copy(alpha=.12f),8.dp.toPx(),p);drawCircle(color,4.dp.toPx(),p);drawCircle(Color.White,2.dp.toPx(),p)
   }
   if(samples.size>1)CinemaSlider(selected.toFloat(),{selected=it.roundToInt().coerceIn(samples.indices)},Modifier.fillMaxWidth(),0f..samples.lastIndex.toFloat())
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(samples.first().label,fontSize=11.sp,color=muted);Text(samples.last().label,fontSize=11.sp,color=muted)}
  }
 }
 if(framed)Panel(content)else Column(verticalArrangement=Arrangement.spacedBy(12.dp),content=content)
}

@Composable fun ComparisonBars(label:String,current:Double?,previous:Double?,unit:String){
 val peak=max(abs(current?:0.0),abs(previous?:0.0)).coerceAtLeast(.01)
 Column(Modifier.fillMaxWidth().background(softWash,RoundedCornerShape(16.dp)).padding(14.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
  Text(label,fontSize=13.sp,fontWeight=FontWeight.Medium,color=ink)
  listOf(Triple("本周期",current,accent),Triple("上周期",previous,muted.copy(alpha=.5f))).forEach{(name,value,color)->
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(name,fontSize=11.sp,color=muted);Text(numberText(value)+" "+unit,fontSize=12.sp,color=ink)}
   val track=glassEdge;Canvas(Modifier.fillMaxWidth().height(6.dp)){drawRoundRect(track,cornerRadius=androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()));if(value!=null)drawRoundRect(color,size=androidx.compose.ui.geometry.Size(size.width*(abs(value)/peak).toFloat(),size.height),cornerRadius=androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()))}
  }
 }
}

@Composable fun BatteryRangeChart(overview:JSONObject){
 val percent=overview.number("batteryLevel");val color=accent;val track=glassEdge
 Row(Modifier.fillMaxWidth().background(softWash,RoundedCornerShape(20.dp)).padding(18.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(20.dp)){
  Box(Modifier.size(112.dp),contentAlignment=Alignment.Center){Canvas(Modifier.fillMaxSize().padding(8.dp)){drawArc(track,-90f,360f,false,style=Stroke(9.dp.toPx(),cap=StrokeCap.Round));if(percent!=null)drawArc(color,-90f,(percent.coerceIn(0.0,100.0)*3.6).toFloat(),false,style=Stroke(9.dp.toPx(),cap=StrokeCap.Round))};Column(horizontalAlignment=Alignment.CenterHorizontally){Text(numberText(percent)+"%",fontSize=23.sp,fontWeight=FontWeight.SemiBold,color=ink);Text("剩余电量",fontSize=11.sp,color=muted)}}
  Column(verticalArrangement=Arrangement.spacedBy(6.dp)){Icon(Icons.Outlined.Route,null,tint=color,modifier=Modifier.size(21.dp));Text(numberText(overview.number("remainingRange"))+" km",fontSize=22.sp,fontWeight=FontWeight.SemiBold,color=ink);Text("剩余续航",fontSize=11.sp,color=muted)}
 }
}
