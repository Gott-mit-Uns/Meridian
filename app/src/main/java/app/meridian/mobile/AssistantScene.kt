package app.meridian.mobile

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import app.meridian.mobile.glass.LocalAppMotion

/** One owner for the complete lifecycle. The opaque scene leaves before disposal. */
@Composable internal fun AssistantScene(onClosed:()->Unit,content:@Composable (()->Unit)->Unit){
 val (presence,close)=rememberPageMotion(onClosed);val policy=LocalAppMotion.current
 Box(Modifier.fillMaxSize().clickable(interactionSource=remember{MutableInteractionSource()},indication=null){}){
 PredictiveSceneFrame(onBack=onClosed,fullHeightBack=true){
  Box(Modifier.fillMaxSize().graphicsLayer{
   translationY=if(policy.full)(1f-presence.value)*size.height else 0f
   // Alpha on the whole Haze/RenderEffect tree can reveal an intermediate capture.
   // Keep the surface opaque; movement alone reveals the already-mounted parent.
   alpha=1f
  }.background(Brush.verticalGradient(LocalPalette.current.background)).clickable(interactionSource=remember{MutableInteractionSource()},indication=null){}){
   content(close)
  }
 }
 }
}
