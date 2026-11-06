package app.meridian.mobile

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.core.view.WindowCompat

fun Context.hostActivity():Activity?=when(this){is Activity->this;is ContextWrapper->baseContext.hostActivity();else->null}

/** One policy for activity pages and dialog windows; insets belong to their content. */
@Suppress("DEPRECATION")
fun applyImmersiveBars(window:Window,view:View,dark:Boolean){
 WindowCompat.setDecorFitsSystemWindows(window,false)
 window.statusBarColor=Color.TRANSPARENT;window.navigationBarColor=Color.TRANSPARENT
 if(Build.VERSION.SDK_INT>=28&&window.attributes.layoutInDisplayCutoutMode!=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES)window.attributes=window.attributes.apply{layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES}
 if(Build.VERSION.SDK_INT>=29){window.isStatusBarContrastEnforced=false;window.isNavigationBarContrastEnforced=false}
 WindowCompat.getInsetsController(window,view).apply{isAppearanceLightStatusBars=!dark;isAppearanceLightNavigationBars=!dark}
}

@Suppress("DEPRECATION")
fun prepareFullscreenWindow(window:Window,view:View,dark:Boolean){
 window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
 window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
 window.decorView.setPadding(0,0,0,0);window.decorView.elevation=0f
 window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.MATCH_PARENT)
 applyImmersiveBars(window,view,dark)
}

internal fun View.dialogWindow():Window?{
 var current:android.view.ViewParent?=parent
 while(current!=null){if(current is androidx.compose.ui.window.DialogWindowProvider)return current.window;current=current.parent}
 return null
}
