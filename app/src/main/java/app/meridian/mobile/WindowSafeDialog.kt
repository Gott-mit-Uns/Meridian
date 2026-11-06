package app.meridian.mobile

import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.ComponentDialog
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

private class DialogHost(context:android.content.Context,override val window:Window):FrameLayout(context),DialogWindowProvider

/** A real non-floating window. Compose's floating DialogLayout can remeasure to
 * configuration screenHeightDp, leaving the parent's navigation area exposed. */
@Composable internal fun WindowSafeDialog(onDismiss:()->Unit,properties:DialogProperties,content:@Composable ()->Unit){
 val overlay=LocalSceneOverlayHost.current
 if(overlay!=null){
  val owner=remember{Any()};val latest by rememberUpdatedState(content);val dismiss by rememberUpdatedState(onDismiss);val config by rememberUpdatedState(properties);val locals by rememberUpdatedState(currentCompositionLocalContext)
  DisposableEffect(overlay,owner){val entry=SceneOverlayHost.Entry(owner,{CompositionLocalProvider(locals){latest()}},{dismiss()},{config});overlay.entries.add(entry);onDispose{overlay.entries.remove(entry)}}
  return
 }

 val dark=LocalPalette.current.dark;val context=LocalContext.current;val parent=rememberCompositionContext();val latestContent by rememberUpdatedState(content);val dismiss by rememberUpdatedState(onDismiss);val latestProperties by rememberUpdatedState(properties)
 val dialog=remember(context){ComponentDialog(context,R.style.Theme_Meridian_Overlay)}
 DisposableEffect(dialog){
  val window=dialog.window!!
  window.setWindowAnimations(0)
  val host=DialogHost(context,window)
  val compose=ComposeView(context).apply{setParentCompositionContext(parent);setContent{CompositionLocalProvider(LocalOnBackPressedDispatcherOwner provides dialog){latestContent()}}}
  host.addView(compose,FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT))
  dialog.setContentView(host);dialog.setOnCancelListener{dismiss()}
  dialog.onBackPressedDispatcher.addCallback(object:OnBackPressedCallback(true){override fun handleOnBackPressed(){if(latestProperties.dismissOnBackPress)dismiss()}})
  window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
  dialog.show();prepareFullscreenWindow(window,compose,dark);window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT)
  onDispose{dialog.setOnCancelListener(null);dialog.dismiss();compose.disposeComposition()}
 }
 SideEffect{dialog.setCanceledOnTouchOutside(properties.dismissOnClickOutside);dialog.window?.let{w->applyImmersiveBars(w,w.decorView,dark);when(properties.securePolicy){androidx.compose.ui.window.SecureFlagPolicy.SecureOn->w.addFlags(WindowManager.LayoutParams.FLAG_SECURE);androidx.compose.ui.window.SecureFlagPolicy.SecureOff->w.clearFlags(WindowManager.LayoutParams.FLAG_SECURE);else->if((context.hostActivity()?.window?.attributes?.flags?:0).and(WindowManager.LayoutParams.FLAG_SECURE)!=0)w.addFlags(WindowManager.LayoutParams.FLAG_SECURE)else w.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)}}}
}
