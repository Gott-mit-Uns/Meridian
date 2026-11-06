// Adapted from user-owned 流影 source; optical implementation retained.
package app.meridian.mobile.glass

import kotlin.math.pow

internal data class GlassRefractionProfile(val displacementDp:Float,val bandDp:Float,val magnification:Float)
internal fun glassRefractionProfile(amount:Float):GlassRefractionProfile {
    val value=if(amount.isFinite())amount.coerceIn(0f,2f) else 1f
    val response=value.pow(1.65f)
    return GlassRefractionProfile(2.4f*response,6f+3f*response,1f+.012f*response)
}
/** Full range is retained on ordinary 32–56dp controls; only very small shapes clamp. */
internal fun glassEdgeDisplacement(amount:Float,heightDp:Float,strength:Float=1f):Float =
    minOf(glassRefractionProfile(amount).displacementDp,heightDp.coerceAtLeast(0f)*.5f*.48f)*strength.coerceIn(0f,1.3f)

internal fun dockPositionAt(x:Float,width:Float,count:Int=4):Float =
    if(width<=0 || count<=1)0f else (x/width*count-.5f).coerceIn(0f,(count-1).toFloat())

/** A foreground lens must bypass both filtering and the offscreen layer when the finger is released. */
internal fun glassEffectActive(geometryReady:Boolean,pressedOnly:Boolean,pressure:Float):Boolean =
    geometryReady && (!pressedOnly || pressure.isFinite() && pressure>.001f)
