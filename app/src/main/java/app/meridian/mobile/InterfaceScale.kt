package app.meridian.mobile

import kotlin.math.roundToInt

/** Smaller density exposes more content without changing Android system settings. */
internal fun normalizedInterfaceScale(value:Float):Float =
    if(value.isFinite())value.coerceIn(.75f,1.25f) else 1f

internal fun scaledInterfaceDp(systemDp:Int,scale:Float):Int =
    if(systemDp<=0)systemDp else (systemDp/normalizedInterfaceScale(scale)).roundToInt().coerceAtLeast(1)

/** Zero preserves the previous follow-page behavior; positive values use system density. */
internal fun normalizedNavigationScale(value:Float):Float =
    if(!value.isFinite() || value<=0f)0f else value.coerceIn(.8f,1.4f)

internal fun navigationDensityScale(page:Float,navigation:Float):Float =
    normalizedNavigationScale(navigation).takeIf{it>0f} ?: normalizedInterfaceScale(page)

/** Keep vertical touch targets at least 48 system dp, including compact page layouts. */
internal fun navigationRailHeightDp(labels:Boolean,lineHeight:Float,scale:Float):Float =
    maxOf(if(labels)maxOf(60f,42f+lineHeight) else 44f,48f/(scale.takeIf{it.isFinite() && it>0f} ?: 1f))

/** Return page dp so scrolling reserves the same physical space as the independently sized dock. */
internal fun navigationBottomSpaceDp(labels:Boolean,lineHeight:Float,page:Float,navigation:Float,compact:Boolean):Float {
    val scale=navigationDensityScale(page,navigation)
    return (navigationRailHeightDp(labels,lineHeight,scale)+8f+(if(compact)24f else 32f)+24f)*scale/normalizedInterfaceScale(page)
}
