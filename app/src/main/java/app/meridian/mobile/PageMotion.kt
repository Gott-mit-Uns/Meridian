package app.meridian.mobile
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.TransformOrigin
import app.meridian.mobile.glass.LocalAppMotion
import kotlinx.coroutines.*

// Shared timing curve; travel differs between top-level tabs and child routes.
internal val SceneEasing=CubicBezierEasing(.2f,0f,.2f,1f)

internal val LocalPageMotionHandled=staticCompositionLocalOf{false}

/** Finish the exit animation before removing the window; ignore repeated back taps. */
@Composable fun rememberPageMotion(onClose:()->Unit):Pair<State<Float>,()->Unit>{
 val policy=LocalAppMotion.current;val scope=rememberCoroutineScope();val latest by rememberUpdatedState(onClose)
 val progress=remember{Animatable(if(policy.enabled)0f else 1f)};var closing by remember{mutableStateOf(false)}
 LaunchedEffect(policy.mode){if(!closing){withFrameNanos{};progress.animateTo(1f,tween(policy.duration(360),easing=SceneEasing))}}
 val close:()->Unit={if(!closing){closing=true;scope.launch{progress.animateTo(0f,tween(policy.duration(280),easing=SceneEasing));latest()}}}
 return progress.asState() to close
}
/** Vertical shared-axis presence modeled on the supplied second reference clip. */
@Composable internal fun Modifier.scenePresence(progress:State<Float>):Modifier{
 val policy=LocalAppMotion.current
 return graphicsLayer{
  val p=safeBackProgress(progress.value)
  alpha=if(policy.enabled)p else 1f
  translationX=0f
  translationY=if(policy.full)(1f-p)*64.dp.toPx()else 0f
  scaleX=if(policy.full).985f+.015f*p else 1f;scaleY=scaleX
  transformOrigin=TransformOrigin(.5f,.35f)
 }
}
/** Retain both actual tab compositions while sliding; no bitmap or fake parent. */
@Composable internal fun RootTabMotion(destination:Int,content:@Composable (Int)->Unit){
 val policy=LocalAppMotion.current
 AnimatedContent(targetState=destination,modifier=Modifier.fillMaxSize().clipToBounds(),transitionSpec={
  val direction=if(targetState>initialState)1 else -1
  if(policy.full)(slideInHorizontally(tween(policy.duration(360),easing=SceneEasing)){it*direction} togetherWith slideOutHorizontally(tween(policy.duration(360),easing=SceneEasing)){-it*direction}).using(null)
  else if(policy.enabled)(fadeIn(tween(policy.duration(140,90))) togetherWith fadeOut(tween(policy.duration(90,60)))).using(null)
  else (EnterTransition.None togetherWith ExitTransition.None).using(null)
 },label="root-tab-motion"){page->Box(Modifier.fillMaxSize()){content(page)}}
}
/** Initialize at the first-frame value, rather than showing the final frame then snapping back. */
@Composable internal fun Modifier.routeArrival(destination:Any):Modifier{
 val policy=LocalAppMotion.current;val progress=remember(destination){Animatable(if(policy.enabled)0f else 1f)}
 LaunchedEffect(destination,policy.mode){if(policy.enabled)progress.animateTo(1f,tween(policy.duration(200),easing=SceneEasing))else progress.snapTo(1f)}
 return graphicsLayer{alpha=if(policy.full).9f+.1f*progress.value else if(policy.enabled)progress.value else 1f;translationX=0f;translationY=0f;scaleX=1f;scaleY=1f}
}
@Composable internal fun Modifier.menuArrival():Modifier{
 val policy=LocalAppMotion.current;val progress=remember{Animatable(if(policy.enabled)0f else 1f)}
 LaunchedEffect(policy.mode){progress.animateTo(1f,tween(policy.duration(240,60),easing=SceneEasing))}
 return graphicsLayer{alpha=if(policy.enabled)progress.value else 1f;val form=if(policy.full)progress.value else 1f;scaleX=.98f+.02f*form;scaleY=scaleX;transformOrigin=androidx.compose.ui.graphics.TransformOrigin(.5f,0f);translationY=if(policy.full)(1f-form)*2.dp.toPx()else 0f}
}
@Composable internal fun Modifier.cardClick(enabled:Boolean=true,onClick:()->Unit):Modifier{
 val source=remember{MutableInteractionSource()}
 return motionPress(source).clickable(interactionSource=source,indication=null,enabled=enabled,onClick=onClick)
}
