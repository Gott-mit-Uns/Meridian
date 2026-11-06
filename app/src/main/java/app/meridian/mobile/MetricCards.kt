package app.meridian.mobile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import org.json.*

/** Shared line slots keep peers aligned without clipping larger accessibility fonts. */
@Composable fun MetricDataCard(label:String,value:String,unit:String,icon:ImageVector,modifier:Modifier=Modifier,tint:Color=accent,footer:String="") {
 Column(modifier.background(softWash,RoundedCornerShape(16.dp)).border(.6.dp,glassEdge,RoundedCornerShape(16.dp)).padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
   Icon(icon,null,tint=tint,modifier=Modifier.size(17.dp))
   Text(label,fontSize=13.sp,lineHeight=18.sp,color=muted,modifier=Modifier.weight(1f))
  }
  Text(value,fontSize=20.sp,lineHeight=24.sp,fontWeight=FontWeight.SemiBold,color=ink)
  if(unit.isNotBlank())Text(unit,fontSize=13.sp,lineHeight=19.sp,color=muted)
  if(footer.isNotBlank())Text(footer,fontSize=13.sp,lineHeight=19.sp,color=tint)
 }
}

data class StatisticsMetricGroup(val title:String,val items:JSONArray,val key:String="",val scope:String="period")
/** Consolidate identical server metrics, leaving distinct lifetime / period labels intact. */
fun statisticsMetricGroups(root:JSONObject):List<StatisticsMetricGroup> {
 val seen=mutableSetOf<String>()
 val groups=mutableListOf<StatisticsMetricGroup>()
 for((key,fallback) in listOf("periodDashboard" to "周期用车","lifetimeDashboard" to "累计表现")) {
  val source=root.optJSONObject(key)?.optJSONArray("metricGroups")?:continue
  for(i in 0 until source.length()) {
   val group=source.optJSONObject(i)?:continue
   val groupKey=group.safeText("key")
   if(key=="periodDashboard"&&groupKey in listOf("overview","trip","charge")&&(root.optJSONObject("lifetimeDashboard")?.optJSONArray("metricGroups")?:JSONArray()).objects().any{it.safeText("key")==groupKey})continue
   if(key=="lifetimeDashboard"&&groupKey in listOf("energy","efficiency")&&(root.optJSONObject("periodDashboard")?.optJSONArray("metricGroups")?:JSONArray()).objects().any{it.safeText("key")==groupKey})continue
   val items=group.optJSONArray("items")?:continue;val filtered=JSONArray()
   for(n in 0 until items.length()) {
    val item=items.optJSONObject(n)?:continue
    val label=item.safeText("label").trim();val value=item.opt("value")
    val canonical=when(value){null,JSONObject.NULL->"missing";is Number->value.toDouble().toString();else->value.toString().trim()}
    // No label means there is no safe identity: retain it rather than discard data.
    val identity=label+"\u001f"+item.safeText("unit").trim()+"\u001f"+canonical
    if(label.isBlank()||seen.add(identity))filtered.put(item)
   }
   if(filtered.length()>0)groups.add(StatisticsMetricGroup(group.safeText("title",fallback),filtered,groupKey,if(key=="lifetimeDashboard")"lifetime"else"period"))
  }
 }
 val merged=linkedMapOf<String,StatisticsMetricGroup>()
 for(group in groups){val identity=group.key.ifBlank{group.title};val old=merged[identity];if(old==null)merged[identity]=group else{val items=JSONArray(old.items.toString());group.items.objects().forEach{items.put(it)};merged[identity]=old.copy(items=items)}}
 val order=listOf("overview","trip","energy","efficiency","charge")
 return merged.values.sortedBy{order.indexOf(it.key).let{index->if(index<0)99 else index}}
}
