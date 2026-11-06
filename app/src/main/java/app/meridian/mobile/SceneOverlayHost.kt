package app.meridian.mobile
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import android.view.WindowManager

/** Flow's same-window overlay stack: the real parent stays beneath every child scene. */
internal class SceneOverlayHost{
 class Entry(val owner:Any,val render:@Composable ()->Unit,val dismiss:()->Unit,val properties:()->DialogProperties)
 val entries=mutableStateListOf<Entry>()
}
internal val LocalSceneOverlayHost=staticCompositionLocalOf<SceneOverlayHost?>{null}
@Composable internal fun SceneOverlayLayer(host:SceneOverlayHost){
 val top=host.entries.lastOrNull()?:return
 val view=LocalView.current
 DisposableEffect(top.owner,top.properties().securePolicy){
  val window=view.context.hostActivity()?.window;val secure=WindowManager.LayoutParams.FLAG_SECURE
  val previous=window?.attributes?.flags?.and(secure)!=0
  when(top.properties().securePolicy){SecureFlagPolicy.SecureOn->window?.addFlags(secure);SecureFlagPolicy.SecureOff->window?.clearFlags(secure);else->Unit}
  onDispose{if(previous)window?.addFlags(secure)else window?.clearFlags(secure)}
 }
 BackHandler(enabled=top.properties().dismissOnBackPress){top.dismiss()}
 host.entries.forEach{entry->key(entry.owner){
  Box(Modifier.fillMaxSize().then(if(entry!==top)Modifier.clearAndSetSemantics{}else Modifier).clickable(interactionSource=remember{MutableInteractionSource()},indication=null){}){entry.render()}
 }}
}
