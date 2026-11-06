package app.meridian.mobile

import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.*

/** Ported from user-owned 流影 ScrollEdge.kt TopScrollFog; not a full-width glass plate. */
@Composable internal fun FlowTopScrollFog(state:HazeState,visible:Boolean,modifier:Modifier=Modifier){
 val opacity by animateFloatAsState(if(visible)1f else 0f,tween(app.meridian.mobile.glass.LocalAppMotion.current.duration(160,60)),label="topEdgeFog")
 if(opacity<=.001f)return
 val canvas=LocalPalette.current.background[0]
 val height=56.dp+WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
 val end=with(LocalDensity.current){height.toPx()}
 val glass=LocalAppearance.current.glass
 Box(modifier.fillMaxWidth().height(height).then(if(glass)Modifier.hazeEffect(state,HazeStyle(backgroundColor=canvas,tints=listOf(HazeTint(canvas.copy(alpha=.16f))),blurRadius=12.dp,noiseFactor=0f,fallbackTint=HazeTint(canvas.copy(alpha=.4f)))){
  blurEnabled=Build.VERSION.SDK_INT>=31;alpha=opacity
  progressive=HazeProgressive.verticalGradient(startY=0f,startIntensity=1f,endY=end,endIntensity=0f,preferPerformance=true)
  mask=Brush.verticalGradient(listOf(Color.White,Color.Transparent))
 }else Modifier.background(Brush.verticalGradient(listOf(canvas.copy(alpha=.9f*opacity),canvas.copy(alpha=0f))))))
}
