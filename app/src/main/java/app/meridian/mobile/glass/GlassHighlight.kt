// Adapted from user-owned 流影 source; optical implementation retained.
package app.meridian.mobile.glass

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp

/** Primary light and opposing environment reflection share the bevel; centre remains untouched. */
@androidx.compose.runtime.Composable
internal fun Modifier.subtleGlassHighlight(shape:Shape,dark:Boolean,strength:Float=1f):Modifier {
    val gain=normalizedGlassHighlight(LocalAppPreferences.current.glassHighlight)
    return drawWithCache {
    val inset=.45.dp.toPx()
    val bounds=Size((size.width-2*inset).coerceAtLeast(0f),(size.height-2*inset).coerceAtLeast(0f))
    val outline=shape.createOutline(bounds,layoutDirection,this)
    val peak=(if(dark).44f else .74f)*strength.coerceIn(0f,1f)
    fun light(fraction:Float)=Color.White.copy(alpha=(peak*gain*fraction).coerceIn(0f,1f))
    val reflection=Brush.linearGradient(0f to light(1f),
        .28f to light(.62f),.50f to light(.16f),
        .72f to light(.24f),.90f to light(.46f),1f to light(.62f),
        start=Offset.Zero,end=Offset(size.width.coerceAtLeast(1f),size.height.coerceAtLeast(1f)))
    onDrawBehind {
        if(bounds.width>0f && bounds.height>0f)withTransform({translate(inset,inset)}) {
            drawOutline(outline,reflection,style=Stroke(1.dp.toPx()))
        }
    }
}
}
