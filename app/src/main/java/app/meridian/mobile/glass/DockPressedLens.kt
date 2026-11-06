// Adapted from user-owned 流影 source; optical implementation retained.
package app.meridian.mobile.glass

internal data class DockPressedLens(val pressure:Float,val scaleX:Float,val scaleY:Float,val glyphScale:Float) {
    val glyphCounterX get()=glyphScale/scaleX
    val glyphCounterY get()=glyphScale/scaleY
}
internal fun dockPressedLens(value:Float):DockPressedLens {
    val p=value.takeIf{it.isFinite()}?.coerceIn(0f,1f) ?: 0f
    return DockPressedLens(p,1f+.10f*p,1f+.34f*p,1f+.055f*p)
}

internal data class DropletOptics(val bandScale:Float,val oppositeLip:Float,val dispersionDp:Float,val shoulderLight:Float,val centerMagnification:Float)
internal fun dropletOptics(value:Float):DropletOptics {
    val p=value.takeIf{it.isFinite()}?.coerceIn(0f,1f) ?: 0f
    return DropletOptics(1f+.18f*p,.44f*p,.07f+.11f*p,.12f*p,1f+.06f*p)
}
