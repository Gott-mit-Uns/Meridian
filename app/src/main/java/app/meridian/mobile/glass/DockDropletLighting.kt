// Adapted from user-owned 流影 source; optical implementation retained.
package app.meridian.mobile.glass

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.unit.dp

/** Paired meniscus glints and contact shade: nothing is painted across the clear centre. */
@Composable internal fun Modifier.dockDropletLighting(shape:Shape,dark:Boolean,pressure:Float):Modifier {
    val gain=normalizedGlassHighlight(LocalAppPreferences.current.glassHighlight)
    return drawWithCache {
        val p=pressure.coerceIn(0f,1f)
        val inset=.40.dp.toPx();val shoulder=.95.dp.toPx()
        fun outline(padding:Float)=shape.createOutline(Size((size.width-padding*2).coerceAtLeast(0f),(size.height-padding*2).coerceAtLeast(0f)),layoutDirection,this)
        val outer=outline(inset);val inner=outline(shoulder)
        val boundary=Brush.verticalGradient(listOf(Color.Black.copy(alpha=.05f*p),Color.Transparent,Color.Black.copy(alpha=(if(dark).10f else .085f)*p)))
        val reflection=Brush.verticalGradient(0f to Color.White.copy(alpha=((.14f+.36f*p)*gain).coerceIn(0f,1f)),
            .20f to Color.White.copy(alpha=.035f*p),.48f to Color.Transparent,.78f to Color.White.copy(alpha=.025f*p),
            1f to Color.White.copy(alpha=((.06f+.22f*p)*gain).coerceIn(0f,1f)))
        // Colour separation comes from the sampled scene, not painted rainbow strokes.
        onDrawBehind {
            if(size.width>shoulder*2 && size.height>shoulder*2){
                withTransform({translate(inset,inset)}){
                    drawOutline(outer,Color.Black.copy(alpha=.028f*p),style=Stroke(.6.dp.toPx()))
                    drawOutline(outer,boundary,style=Stroke(.65.dp.toPx()))
                }
                withTransform({translate(shoulder,shoulder)}){
                    drawOutline(inner,reflection,style=Stroke((.55f+p*.15f).dp.toPx()))
                }
            }
        }
    }
}
