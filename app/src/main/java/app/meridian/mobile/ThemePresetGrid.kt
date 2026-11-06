package app.meridian.mobile

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.*

@Composable internal fun ThemePresetGrid(selected:String,onSelect:(String)->Unit){
 val base=LocalPalette.current
 val groups=listOf("白底多彩" to listOf("vivid","clarity","citrus","coast","confetti"),"中性材质" to listOf("charcoal","ivory"),"轻雅" to homeThemeNames.keys.take(5),"鲜明" to listOf("cobalt","coral","emerald","electric"),"流彩" to listOf("aurora","sunset","prism"),"分色卡片" to listOf("atelier","riviera","botanical","gallery","sorbet","jewel"))
 groups.forEach{(title,ids)->
  Text(title,fontSize=12.sp,color=muted)
  ids.chunked(2).forEach{row->Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max),horizontalArrangement=Arrangement.spacedBy(10.dp)){
   row.forEach{id->val sample=homePalette(id,base);val shape=RoundedCornerShape(16.dp);val source=remember{MutableInteractionSource()}
    Box(Modifier.weight(1f).fillMaxHeight().clip(shape).motionPress(source).selectable(selected==id,interactionSource=source,indication=null,role=Role.RadioButton,onClick={onSelect(id)}).border(if(selected==id)1.5.dp else .5.dp,if(selected==id)accent else glassEdge,shape)){
     SharedGlassControl(shape,Modifier.matchParentSize())
     Column(Modifier.padding(9.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
      Box(Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(sample.background))){
       Canvas(Modifier.fillMaxSize().padding(9.dp)){val cardTone=homeCardTint(id,"range");val fill=if(cardTone==Color.Transparent)sample.surface.copy(alpha=.85f)else lerp(sample.surface,cardTone,.25f);drawRoundRect(fill,Offset(0f,size.height*.22f),Size(size.width*.62f,size.height*.74f),CornerRadius(5.dp.toPx()));drawRoundRect(sample.accent,Offset(6.dp.toPx(),size.height*.43f),Size(size.width*.36f,4.dp.toPx()),CornerRadius(2.dp.toPx()));drawRoundRect((if(id in splitChartThemes)homeCardColor(id,"time")else sample.accent).copy(alpha=.7f),Offset(6.dp.toPx(),size.height*.67f),Size(size.width*.45f,3.dp.toPx()),CornerRadius(2.dp.toPx()));drawCircle(if(id in splitChartThemes)homeCardColor(id,"efficiency")else sample.accent,size.height*.18f,Offset(size.width*.84f,size.height*.61f))}
      }
      Row(verticalAlignment=Alignment.CenterVertically){Text(homeThemeNames[id].orEmpty(),fontSize=12.sp,color=ink,modifier=Modifier.weight(1f));if(selected==id)Icon(Icons.Outlined.Check,"已选中",Modifier.size(15.dp),tint=accent)}
     }
    }
   }
   if(row.size==1)Spacer(Modifier.weight(1f))
  }}
 }
}
