package app.meridian.mobile.glass
import androidx.compose.runtime.*
internal data class MaterialPreferences(val liquidBlur:Float=2f,val liquidRefraction:Float=1f,val glassHighlight:Float=1.15f)
internal val LocalAppPreferences=staticCompositionLocalOf{MaterialPreferences()}
internal val LocalAppMotion=staticCompositionLocalOf{MotionPolicy()}
internal val LocalPlayerMenu=staticCompositionLocalOf{false}
internal val LocalInsideGlassChrome=staticCompositionLocalOf{false}
@Composable internal fun rememberPageVisible()=true
