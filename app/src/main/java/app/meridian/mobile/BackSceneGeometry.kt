package app.meridian.mobile
internal fun safeBackProgress(value:Float):Float=if(value.isFinite())value.coerceIn(0f,1f)else 0f
internal data class BackSceneGeometry(val scale:Float,val translationX:Float,val radiusDp:Float,val translationY:Float=0f,val alpha:Float=1f)
internal fun backSceneGeometry(progress:Float,direction:Float,width:Float,density:Float,fullMotion:Boolean):BackSceneGeometry{
 val p=if(fullMotion)safeBackProgress(progress)else 0f
 val w=if(width.isFinite())width.coerceAtLeast(0f)else 0f
 val d=if(density.isFinite())density.coerceAtLeast(0f)else 0f
 return BackSceneGeometry(1f-.015f*p,(if(direction<0)-1f else 1f)*minOf(12f*d,w*.025f)*p,18f*p,64f*d*p,1f-p)
}
