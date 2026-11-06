// Adapted from user-owned 流影 source; optical implementation retained.
package app.meridian.mobile.glass

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import android.graphics.Paint
import android.graphics.Canvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.*

/** A shallow rolled capsule edge. Light fades across its width, not a stroked outline.
 * Geometry is cached; no backdrop readback, bitmap allocation, or per-frame mesh rebuild.
 * Both themes retain transmitted backdrop colour; white scenes reveal the opposite shadow.
 */
@androidx.compose.runtime.Composable
internal fun Modifier.navigationGlassBevel(dark:Boolean):Modifier {
    val gain=normalizedGlassHighlight(LocalAppPreferences.current.glassHighlight)
    return drawWithCache {
    val radius=size.height*.5f
    val halfStraight=(size.width-size.height).coerceAtLeast(0f)*.5f
    val centre=Offset(size.width*.5f,size.height*.5f)
    val bevel=minOf(2.2.dp.toPx(),radius*.16f)
    val segments=96
    val bands=8
    val positions=ArrayList<Offset>(segments*bands*6)
    val colors=ArrayList<Color>(segments*bands*6)
    fun vertex(segment:Int,band:Int):Pair<Offset,Color> {
        val angle=(2.0*PI*segment/segments).toFloat()
        val nx=cos(angle);val ny=sin(angle)
        val t=band.toFloat()/bands
        val depth=bevel*t
        val point=Offset(centre.x+(if(nx>=0f)halfStraight else -halfStraight)+(radius-depth)*nx,
            centre.y+(radius-depth)*ny)
        val facing=(-.55f*nx-.835f*ny).coerceIn(-1f,1f)
        // Zero at both boundaries avoids a white tracing line. Specular roll peaks inward.
        val roll=sin(PI.toFloat()*t).coerceAtLeast(0f).pow(1.25f)
        val highlight=maxOf(facing,0f).pow(2f)*roll
        val occlusion=(.22f+.78f*maxOf(-facing,0f))*roll
        val environment=maxOf(-facing,0f).pow(2f)*roll
        val signed=(highlight*(if(dark).23f else .28f)+environment*(if(dark).13f else .16f)+roll*.025f)*gain-occlusion*(if(dark).07f else .135f)
        return point to (if(signed>=0f)Color.White else Color.Black).copy(alpha=abs(signed).coerceIn(0f,1f))
    }
    fun add(v:Pair<Offset,Color>){positions.add(v.first);colors.add(v.second)}
    if(bevel>0f)for(segment in 0 until segments)for(band in 0 until bands){
        val a=vertex(segment,band);val b=vertex(segment+1,band)
        val c=vertex(segment+1,band+1);val d=vertex(segment,band+1)
        add(a);add(b);add(c);add(a);add(c);add(d)
    }
    val mesh=buildGlassBevelBuffers(positions.map{it.x to it.y},colors.map{it.toArgb()})
    val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    var drawingAvailable=true
    onDrawBehind {
        if(drawingAvailable && mesh.coordinates.isNotEmpty())drawIntoCanvas {canvas->
            try {
            // Untextured and unindexed: Android requires null, not an empty array.
            // Compose 1.7 forwards its empty arrays unchanged and Canvas rejects them.
            canvas.nativeCanvas.drawVertices(Canvas.VertexMode.TRIANGLES,mesh.coordinates.size,
                mesh.coordinates,0,null,0,mesh.colors,0,null,0,0,paint)
            } catch(e:RuntimeException){
                drawingAvailable=false
                android.util.Log.w("LiuyingGlass","Bevel unavailable; retaining glass surface",e)
            }
        }
    }
}
}
