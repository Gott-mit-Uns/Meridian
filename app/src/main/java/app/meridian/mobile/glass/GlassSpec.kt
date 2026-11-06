// Adapted from user-owned 流影 source; optical implementation retained.
package app.meridian.mobile.glass

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

/** Single source of truth; values are surface opacity, not transparency percentages. */
internal object GlassSpec {
    const val actionGapDp=8;const val menuGapDp=6;const val menuInsetDp=10
    const val cardElevationDp=3;const val controlElevationDp=2;const val floatingElevationDp=14
    const val menuRowHeightDp=52;const val menuTextInsetDp=16
    const val controlDay=.72f;const val controlNight=.065f
    const val tintedDay=.86f;const val tintedNight=.24f
    const val sheenDay=.40f;const val sheenNight=.025f
    const val controlMidSheenNight=.005f;const val controlDisabledNight=.04f
    const val controlLensStrengthNight=.45f;const val controlEdgeWidthNightDp=3f
    const val edgeDay=.72f;const val edgeNight=.16f
    const val disabled=.42f;const val pressedMix=.065f
    const val selectedDay=.12f;const val selectedNight=.19f
    const val groupDay=.28f;const val groupNight=.055f
    const val panelDay=.19f;const val panelNight=.055f
    const val panelDayPreference=.30f;const val panelNightPreference=.28f
    const val panelMaxDay=.55f;const val panelMaxNight=.16f
    const val panelSheenDay=.065f;const val panelSheenNight=.045f
    const val panelEdgeDay=.70f;const val panelEdgeNight=.23f
    const val minContrast=4.5f;const val indicatorContrast=3f;const val statusBlurDp=8f;const val panelMinBlurDp=14f
    const val edgeDp=.7f;const val panelEdgeDp=.65f
    const val chromeNoise=.008f;const val panelNoise=.006f;const val fallbackAlpha=.96f
    const val defaultBlurDp=28f;const val minimumBlurDp=12f;const val maximumBlurDp=40f
    const val defaultDayCover=.36f;const val defaultNightCover=0f
    const val chromeRimTopDay=.90f;const val chromeRimTopNight=.30f
    const val chromeRimMidDay=.28f;const val chromeRimMidNight=.06f
    const val chromeRimBottomDay=.65f;const val chromeRimBottomNight=.15f
    const val chromeSheenDay=.075f
    fun backdropBlur(preference:Float)= (preference*.72f).coerceIn(14f,30f)
    fun chromeCover(preference:Float)=(preference*.68f).coerceIn(.12f,.62f)
    const val subtleSheen=.025f;const val edgeBottomDay=.20f;const val edgeBottomNight=.035f
    const val inputDay=.55f;const val inputNight=.56f;const val inputFocused=.82f
    const val inputSheenDay=.38f;const val inputSheenNight=.055f;const val inputFocusEdge=.30f
    fun controlAlpha(dark:Boolean,tinted:Boolean,dayCover:Float,nightCover:Float):Float = when {
        dark&&tinted -> (tintedNight+nightCover*.25f).coerceIn(.20f,.40f)
        dark -> (controlNight+nightCover*.35f).coerceIn(.06f,.21f)
        tinted -> (tintedDay+(dayCover-defaultDayCover)*.12f).coerceIn(.78f,.96f)
        else -> (controlDay+(dayCover-defaultDayCover)*.30f).coerceIn(.50f,.92f)
    }
    fun sheen(dark:Boolean)=if(dark)sheenNight else sheenDay
}
