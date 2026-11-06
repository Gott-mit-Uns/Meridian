package app.meridian.mobile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*

internal data class HomeMetricValue(val label:String,val value:String,val unit:String,val trend:String="")

@Composable internal fun HomeMetricTiles(values:List<HomeMetricValue>){
 val tones=listOf(Color(0xFF69889C),Color(0xFF648C81),Color(0xFF8A879F))
 values.chunked(2).forEachIndexed{row,chunk->EqualCardRow(chunk.size,gap=9.dp,surface={_,m->Box(m.background(softWash,RoundedCornerShape(16.dp)))}){column->
  val metric=chunk[column];val tone=tones[(row*2+column)%tones.size]
  Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(7.dp)){
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
    Icon(metricIcon(metric.label),null,tint=tone,modifier=Modifier.size(16.dp))
    Text(metric.label,fontSize=14.sp,lineHeight=21.sp,color=muted,modifier=Modifier.weight(1f))
   }
   Text(metric.value,fontSize=24.sp,lineHeight=32.sp,fontWeight=FontWeight.Medium,color=ink)
   if(metric.unit.isNotBlank())Text(metric.unit,fontSize=13.sp,lineHeight=19.sp,color=muted)
   if(metric.trend.isNotBlank())Text(metric.trend,fontSize=13.sp,lineHeight=20.sp,color=tone)
  }
 }}
}
