package app.meridian.mobile
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.CircleShape
import app.meridian.mobile.glass.GlassChrome
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import app.meridian.mobile.glass.LocalInsideGlassChrome
import androidx.compose.ui.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.*
internal data class ToolbarAction(val icon:ImageVector,val label:String,val enabled:Boolean=true,val active:Boolean=false,val run:()->Unit)
@Composable internal fun ToolbarActionStrip(actions:List<ToolbarAction>){
 val source=remember{MutableInteractionSource()};val pressed by source.collectIsPressedAsState();val backdrop=LocalControlBackdrop.current
 Box(Modifier.width((actions.size*40).dp).height(44.dp),contentAlignment=Alignment.Center){
  if(backdrop!=null)GlassChrome(backdrop,Modifier.fillMaxWidth().height(32.dp),shape=CircleShape,cover=.10f,optical=true,captureAll=true,elevated=true,pressed=pressed,navigationFrame=true,refractionStrength=1.1f,edgeWidthDp=5f){}
  else CompositionLocalProvider(LocalInsideGlassChrome provides false){SharedGlassControl(CircleShape,Modifier.fillMaxWidth().height(32.dp),pressed=pressed)}
  Row(Modifier.fillMaxSize()){actions.forEach{action->Box(Modifier.weight(1f).fillMaxHeight().clickable(interactionSource=source,indication=null,enabled=action.enabled,role=Role.Button,onClickLabel=action.label,onClick=action.run),contentAlignment=Alignment.Center){Icon(action.icon,action.label,Modifier.size(18.dp),tint=(if(action.active)accent else ink).copy(alpha=if(action.enabled)1f else .35f))}}}
 }
}

/** A title and its chevron share the same optical material as toolbar actions. */
@Composable internal fun ToolbarTitleCapsule(content:@Composable RowScope.()->Unit){
 val backdrop=LocalControlBackdrop.current
 Box(contentAlignment=Alignment.Center){
  if(backdrop!=null)GlassChrome(backdrop,Modifier.matchParentSize().padding(vertical=5.dp),shape=CircleShape,cover=.10f,optical=true,captureAll=true,navigationFrame=true,refractionStrength=1.1f,edgeWidthDp=5f){}
  else SharedGlassControl(CircleShape,Modifier.matchParentSize().padding(vertical=5.dp))
  Row(Modifier.heightIn(min=44.dp).padding(horizontal=13.dp),verticalAlignment=Alignment.CenterVertically,content=content)
 }
}
