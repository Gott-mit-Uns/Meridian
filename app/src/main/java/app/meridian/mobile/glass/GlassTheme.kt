// Adapted from user-owned 流影 source; optical implementation retained.
package app.meridian.mobile.glass

import androidx.compose.runtime.*
import androidx.compose.material3.MaterialTheme

// Adapt the existing Liusu optical surface without importing downloader business code.
internal val LocalLiusuDark = staticCompositionLocalOf { false }
internal data class GlassOptions(val glass: Boolean = true, val reduceTransparency: Boolean = false, val optics: Boolean = true,
    val darkOpacity: Float = 0f, val opacity: Float = GlassSpec.defaultDayCover, val blur: Float = GlassSpec.defaultBlurDp, val liquid:Boolean=true)
internal val LocalNavigationOptions = staticCompositionLocalOf { GlassOptions() }
internal object LiusuVisual {
    val canvas @Composable get() = MaterialTheme.colorScheme.background
    val surface @Composable get() = MaterialTheme.colorScheme.surface
}
