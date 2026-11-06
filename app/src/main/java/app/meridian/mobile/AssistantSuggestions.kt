package app.meridian.mobile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import dev.chrisbanes.haze.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*

@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun AssistantSuggestions(questions:List<String>,onSelect:(String)->Unit){
 FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
  questions.forEachIndexed{i,question->
   val label=when{question.contains("用车表现")->"用车分析";question.contains("当前车辆")->"车辆状态";question.contains("最耗电")->"高耗电行程";question.contains("充电记录")->"充电与补能";question.contains("月")&&question.contains("里程")->"月度里程对比";question.contains("辅助驾驶")->"辅助驾驶";else->question}
   val shape=RoundedCornerShape(22.dp);val source=androidx.compose.runtime.remember{androidx.compose.foundation.interaction.MutableInteractionSource()}
   Box(Modifier.widthIn(max=240.dp).heightIn(min=44.dp).motionPress(source).clip(shape).clickable(interactionSource=source,indication=null,role=Role.Button){onSelect(question)}.semantics{contentDescription=question}){
    SuggestionGlassSurface(shape,Modifier.matchParentSize())
    Row(Modifier.padding(horizontal=11.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
     Icon(when(i%3){0->Icons.Outlined.Insights;1->Icons.Outlined.VerifiedUser;else->Icons.Outlined.Bolt},null,Modifier.size(14.dp),tint=accent)
     Text(label,fontSize=13.sp,lineHeight=20.sp,color=ink,modifier=Modifier.weight(1f,false))
    }
   }
  }
 }
}

@Composable private fun SuggestionGlassSurface(shape:Shape,modifier:Modifier){
 // A single frosted plate. Avoid the general controls' multiple rim/highlight overlays.
 val dark=LocalPalette.current.dark;val backdrop=LocalControlBackdrop.current;val glass=LocalAppearance.current.glass
 val surface=glassWhite;val base=LocalPalette.current.background[0]
 Box(modifier.clip(shape).then(if(glass&&backdrop!=null)Modifier.hazeEffect(backdrop,style=HazeStyle(backgroundColor=base,tints=listOf(HazeTint(surface.copy(alpha=if(dark).58f else .66f))),blurRadius=12.dp,noiseFactor=0f,fallbackTint=HazeTint(surface.copy(alpha=.9f))))else Modifier.background(surface.copy(alpha=if(glass).9f else 1f))).border(.6.dp,glassEdge.copy(alpha=.85f),shape))
}
