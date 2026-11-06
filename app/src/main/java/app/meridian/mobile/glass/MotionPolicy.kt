package app.meridian.mobile.glass

/** Pure motion policy, also used by import/export tests. */
internal fun normalizedMotionMode(value:String,legacyReduce:Boolean=false):String = when {
    value=="off" -> "off"
    value=="reduced" || legacyReduce -> "reduced"
    else -> "standard"
}
internal data class MotionPolicy(val mode:String="standard") {
    val enabled get()=mode!="off"
    val full get()=mode=="standard"
    fun duration(normal:Int,reduced:Int=90)=when(mode){"off"->0;"reduced"->reduced;else->normal}
}
internal fun motionPolicy(mode:String,legacyReduce:Boolean,systemEnabled:Boolean)=
    MotionPolicy(if(systemEnabled)normalizedMotionMode(mode,legacyReduce) else "off")

/** Optical lens support is independent of animation policy. */
internal fun liquidGlassAvailable(style:String,glass:Boolean,sdk:Int,playerMenu:Boolean=false):Boolean =
    style=="liquid" && glass && sdk>=33 && !playerMenu
