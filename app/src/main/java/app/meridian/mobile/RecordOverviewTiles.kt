@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package app.meridian.mobile
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*

@Composable internal fun RecordOverviewTiles(values:List<HomeMetricValue>){
 val colors=listOf(Color(0xFF4E80BB),Color(0xFFB57C64),Color(0xFF7E78AD),Color(0xFF528B7E))
 Column(verticalArrangement=Arrangement.spacedBy(10.dp)){values.chunked(2).forEachIndexed{row,chunk->
  EqualCardRow(chunk.size,gap=10.dp,surface={column,m->val color=colors[(row*2+column)%colors.size];Box(m.background(Brush.linearGradient(listOf(lerp(glassWhite,color,if(LocalPalette.current.dark).17f else .10f),glassWhite)),RoundedCornerShape(18.dp)).border(.6.dp,color.copy(alpha=.20f),RoundedCornerShape(18.dp)))}){column->
   val metric=chunk[column];val color=colors[(row*2+column)%colors.size]
   Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(10.dp)){
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(metric.label,fontSize=13.sp,color=muted,modifier=Modifier.weight(1f));Box(Modifier.size(28.dp).background(color.copy(alpha=.12f),RoundedCornerShape(9.dp)),contentAlignment=Alignment.Center){Icon(metricIcon(metric.label),null,Modifier.size(17.dp),tint=color)}}
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement=Arrangement.spacedBy(5.dp),verticalArrangement=Arrangement.spacedBy(2.dp)){
     Text(metric.value,fontSize=26.sp,lineHeight=33.sp,fontWeight=FontWeight.Medium,color=ink)
     if(metric.unit.isNotBlank())Text(metric.unit,fontSize=12.sp,lineHeight=33.sp,color=color)
    }
    Box(Modifier.width(24.dp).height(2.dp).background(color.copy(alpha=.55f),RoundedCornerShape(2.dp)))
   }
  }
 }}
}
