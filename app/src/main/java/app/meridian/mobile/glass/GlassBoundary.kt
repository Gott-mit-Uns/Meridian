// Adapted from user-owned 流影 source; optical implementation retained.
package app.meridian.mobile.glass

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp

/** A hairline occlusion contour outside the brighter inner meniscus. No fill over the centre. */
@Composable internal fun Modifier.glassBoundaryEdge(shape: Shape, dark: Boolean, amount: Float = 1f): Modifier {
    val reflection=normalizedGlassHighlight(LocalAppPreferences.current.glassHighlight)
    val weight=amount.takeIf{it.isFinite()}?.coerceIn(0f,1f) ?: 0f
    return drawWithCache {
        val outerInset=.4.dp.toPx();val innerInset=1.15.dp.toPx()
        fun bounds(inset:Float)=Size((size.width-inset*2).coerceAtLeast(0f),(size.height-inset*2).coerceAtLeast(0f))
        val outer=shape.createOutline(bounds(outerInset),layoutDirection,this)
        val inner=shape.createOutline(bounds(innerInset),layoutDirection,this)
        val occlusion=Brush.verticalGradient(listOf(
            (if(dark)Color.White else Color.Black).copy(alpha=if(dark).12f else .10f),
            (if(dark)Color.White else Color.Black).copy(alpha=if(dark).055f else .115f),
            Color.Black.copy(alpha=if(dark).22f else .22f)))
        val glint=Brush.linearGradient(listOf(Color.White.copy(alpha=((if(dark).12f else .30f)*reflection).coerceIn(0f,1f)),
            Color.Transparent,Color.White.copy(alpha=((if(dark).07f else .15f)*reflection).coerceIn(0f,1f))))
        onDrawBehind {
            if(weight>0f && size.width>innerInset*2 && size.height>innerInset*2) {
                withTransform({translate(outerInset,outerInset)}){drawOutline(outer,occlusion,alpha=weight,style=Stroke(.65.dp.toPx()))}
                withTransform({translate(innerInset,innerInset)}){drawOutline(inner,glint,alpha=weight,style=Stroke(.5.dp.toPx()))}
            }
        }
    }
}
