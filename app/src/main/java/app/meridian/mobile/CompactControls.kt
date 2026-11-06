package app.meridian.mobile

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*

@Composable fun QuietIconButton(icon:ImageVector,label:String,onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true){
 Box(modifier){ToolbarActionStrip(listOf(ToolbarAction(icon,label,enabled){onClick()}))}
}
@Composable fun <T> CompactSegments(options:List<Pair<T,String>>,selected:T,onSelect:(T)->Unit,modifier:Modifier=Modifier){
 Row(modifier.fillMaxWidth().height(IntrinsicSize.Max).heightIn(min=44.dp).then(if(LocalAppearance.current.glass)Modifier.cinemaGlass(RoundedCornerShape(14.dp))else Modifier.background(softWash,RoundedCornerShape(14.dp))).padding(4.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)){
  options.forEach{(id,label)->Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(11.dp)).background(if(id==selected)accent else androidx.compose.ui.graphics.Color.Transparent).clickable(role=Role.RadioButton){onSelect(id)}.semantics{this.selected=id==selected}.padding(horizontal=6.dp,vertical=6.dp),contentAlignment=Alignment.Center){Text(label,fontSize=12.sp,lineHeight=18.sp,fontWeight=if(id==selected)FontWeight.SemiBold else FontWeight.Normal,color=if(id==selected)themeOnAccent(accent) else muted)}
  }
 }
}
@Composable fun RecordTypeSelector(charging:Boolean,onSelect:(Boolean)->Unit){GlassSelectionMenu(charging,listOf(false to "行程记录",true to "充电记录"),onSelect)}

@Composable fun SearchAction(onClick:()->Unit){
 CinemaTonalButton(onClick,Modifier.heightIn(min=48.dp)){Icon(Icons.Outlined.Search,null,Modifier.size(19.dp));Spacer(Modifier.width(5.dp));Text("搜索",fontSize=13.sp)}
}
