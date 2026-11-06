package app.meridian.mobile
import androidx.compose.runtime.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
fun prettyTime(value:String):String{
 val raw=value.trim();val number=raw.toLongOrNull()
 val text=if(number!=null&&raw.length in 10..13)runCatching{java.time.Instant.ofEpochMilli(if(raw.length<=10)number*1000 else number).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}.getOrDefault("—")else raw.replace('T',' ').take(16)
 return text.removePrefix(java.time.LocalDate.now().year.toString()+"-")
}
fun durationText(ms:Long):String {val minutes=ms/60000;return if(minutes>=60)"${minutes/60}时${minutes%60}分" else "${minutes}分钟"}
@Composable fun HeroNumber(value:Any?,unit:String,label:String){Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(4.dp)){Row(verticalAlignment=Alignment.Bottom){Text(if(value==null||value==org.json.JSONObject.NULL)"—" else if(value is Number){val n=value.toDouble();if(n==kotlin.math.floor(n))n.toLong().toString()else String.format(java.util.Locale.getDefault(),"%.1f",n)}else value.toString(),fontSize=36.sp,lineHeight=44.sp,fontWeight=FontWeight.Medium,letterSpacing=(-.8).sp,color=ink);Text(unit,fontSize=13.sp,color=ink,modifier=Modifier.padding(start=3.dp,bottom=6.dp))};Text(label,fontSize=11.sp,color=muted)}}
@Composable fun StatusTile(label:String,value:String,icon:ImageVector,modifier:Modifier=Modifier){Column(modifier.background(glassWhite.copy(alpha=.65f),RoundedCornerShape(17.dp)).border(.7.dp,glassEdge,RoundedCornerShape(17.dp)).padding(horizontal=6.dp,vertical=10.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(5.dp)){Icon(icon,null,tint=blueColor,modifier=Modifier.size(20.dp));Text(label,fontSize=11.sp,color=ink);Text(value,fontSize=10.sp,color=muted,maxLines=1,overflow=TextOverflow.Ellipsis)}}
private val blueColor:Color @Composable get()=accent
@Composable fun SectionTitle(title:String,icon:ImageVector){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){Icon(icon,null,tint=blueColor,modifier=Modifier.size(18.dp));Text(title,fontSize=15.sp,fontWeight=FontWeight.SemiBold,color=ink)}}
@Composable fun InlineMetric(label:String,value:String,icon:ImageVector,modifier:Modifier=Modifier){Row(modifier.padding(vertical=7.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)){Icon(icon,null,tint=blueColor,modifier=Modifier.size(21.dp));Column(verticalArrangement=Arrangement.spacedBy(4.dp)){Text(value,fontSize=20.sp,fontWeight=FontWeight.SemiBold,color=ink,maxLines=1,overflow=TextOverflow.Ellipsis);Text(label,fontSize=10.sp,color=muted)}}}
@Composable fun RoutePoint(label:String,color:Color){Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)){Box(Modifier.size(7.dp).border(2.dp,color,CircleShape));Text(label,fontSize=13.sp,fontWeight=FontWeight.Medium,color=ink,maxLines=1,overflow=TextOverflow.Ellipsis)}}
@Composable fun DonutCard(title:String,values:List<Pair<String,Double>>,unit:String,centerValue:String?=null,centerUnit:String?=null){
 val parts=values.filter{it.second.isFinite()&&it.second>0};val total=parts.sumOf{it.second};if(total<=0)return
 var selected by remember(values){mutableIntStateOf(-1)}
 val colors=themeChartColors(LocalPalette.current)
 Panel{SectionTitle(title,Icons.Outlined.DonutLarge)
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)){
   Box(Modifier.size(112.dp),contentAlignment=Alignment.Center){
    Canvas(Modifier.fillMaxSize().padding(10.dp).pointerInput(parts){detectTapGestures{p->val dx=p.x-size.width/2f;val dy=p.y-size.height/2f;val r=sqrt(dx*dx+dy*dy);if(r>size.width*.23f&&r<size.width*.65f){val angle=(Math.toDegrees(atan2(dy.toDouble(),dx.toDouble()))+450)%360;var end=0.0;selected=parts.indexOfFirst{end+=it.second/total*360;angle<end}}}}){var start=-90f;parts.forEachIndexed{i,v->val sweep=(v.second/total*360).toFloat();drawArc(colors[i%colors.size].copy(alpha=if(selected<0||selected==i)1f else .3f),start,sweep,false,style=Stroke((if(selected==i)18 else 14).dp.toPx()));start+=sweep}}
    Column(horizontalAlignment=Alignment.CenterHorizontally){Text(if(selected in parts.indices)"${(parts[selected].second/total*100).toInt()}%"else centerValue?:"占比",fontSize=20.sp,fontWeight=FontWeight.SemiBold,color=ink);Text(if(selected in parts.indices)parts[selected].first else centerUnit?:"点选分类",fontSize=10.sp,color=muted)}
   }
   Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)){parts.forEachIndexed{i,v->Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if(selected==i)softWash else Color.Transparent).clickable{selected=if(selected==i)-1 else i}.padding(vertical=9.dp,horizontal=6.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(7.dp)){
    Box(Modifier.size(6.dp).background(colors[i%colors.size],CircleShape));Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)){Text(v.first,fontSize=11.sp,color=if(selected==i)ink else muted);if(unit=="km")Text(numberText(v.second)+" km",fontSize=11.sp,color=ink)};Text("${(v.second/total*100).toInt()}%",fontSize=12.sp,fontWeight=FontWeight.SemiBold,color=ink)
   }}}
  }
 }
}
