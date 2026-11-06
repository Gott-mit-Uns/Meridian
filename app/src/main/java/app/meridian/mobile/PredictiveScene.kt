package app.meridian.mobile
import app.meridian.mobile.glass.LocalAppMotion

import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext

@Stable internal class BackGestureMotion {
    val progress=Animatable(0f)
    var generation=0L
    var direction by mutableFloatStateOf(1f)
}
private val LocalBackGestureMotion=staticCompositionLocalOf<BackGestureMotion?>{null}

/** Menus retain ordinary BackHandler priority; only route handlers opt into scene motion. */
@Composable internal fun SceneBackHandler(enabled:Boolean=true,completeExit:Boolean=true,onBack:()->Unit){
    val motion=LocalBackGestureMotion.current
    val latest by rememberUpdatedState(onBack)
    val canBack by rememberUpdatedState(enabled)
    val policy=LocalAppMotion.current
    if(motion==null)BackHandler(enabled){latest()}
    else PredictiveBackHandler(enabled){events->
        val generation=++motion.generation
        motion.progress.stop()
        var committed=false
        try{
            events.collect{event->
                motion.direction=if(event.swipeEdge==0)1f else -1f
                if(policy.full)motion.progress.snapTo(safeBackProgress(event.progress))
            }
            if(canBack && motion.generation==generation){
                if(policy.full){
                    if(completeExit)motion.progress.animateTo(1f,tween(policy.duration(if(motion.progress.value>0f)120 else 280),easing=SceneEasing))
                    else motion.progress.animateTo(0f,spring(dampingRatio=1f,stiffness=550f))
                }
                committed=true;latest()
            }
            // Keep a committed outgoing scene at its final frame until disposal.
            // Restoring it here flashes the child while its parent starts an exit transition.
        }catch(e:CancellationException){
            if(!committed&&motion.generation==generation)withContext(NonCancellable){if(policy.full)motion.progress.animateTo(0f,spring(dampingRatio=1f,stiffness=550f))else motion.progress.snapTo(0f)}
            throw e
        }
    }
}

@Composable internal fun PredictiveSceneFrame(enabled:Boolean=true,onBack:(()->Unit)?=null,completeExitOnBack:Boolean=true,fullHeightBack:Boolean=false,content:@Composable ()->Unit){
    val motion=remember{BackGestureMotion()};val policy=LocalAppMotion.current
    CompositionLocalProvider(LocalBackGestureMotion provides if(enabled)motion else null){
        if(onBack!=null)SceneBackHandler(enabled,completeExitOnBack){onBack()}
        Box(Modifier.fillMaxSize().graphicsLayer{
            val geometry=backSceneGeometry(motion.progress.value,motion.direction,size.width,density,enabled&&policy.full)
            val cover=fullHeightBack&&enabled&&policy.full
            scaleX=if(cover)1f else geometry.scale;scaleY=scaleX
            translationX=if(cover)0f else geometry.translationX
            translationY=if(cover)size.height*safeBackProgress(motion.progress.value)else geometry.translationY
            alpha=if(cover)1f else geometry.alpha
            shape=RoundedCornerShape(if(cover)0.dp else geometry.radiusDp.dp);clip=!cover&&geometry.radiusDp>0f
        }){content()}
    }
}
