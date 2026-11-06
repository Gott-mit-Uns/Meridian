package app.meridian.mobile

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import app.meridian.mobile.glass.*

data class MeridianPalette(val dark:Boolean,val text:Color,val secondary:Color,val accent:Color,val background:List<Color>,val surface:Color,val border:Color,val wash:Color,val chartColors:List<Color>?=null)
private val LightPalette=MeridianPalette(false,Color(0xFF1C1C1E),Color(0xFF74747D),Color(0xFFFF7900),listOf(Color(0xFFF2F2F7),Color(0xFFF2F2F7),Color(0xFFF2F2F7)),Color.White,Color(0xFFE6E6EC),Color(0xFFF6F6F9))
private val DarkPalette=MeridianPalette(true,Color(0xFFE8E8ED),Color(0xFFA1A1AA),Color(0xFF8BAAFF),listOf(Color.Black,Color.Black,Color.Black),Color(0xFF17171B),Color(0xFF303034),Color(0xFF1C1C1F))
val LocalPalette=staticCompositionLocalOf{LightPalette}
val ink:Color @Composable get()=LocalPalette.current.text
val accent:Color @Composable get()=LocalPalette.current.accent
val muted:Color @Composable get()=LocalPalette.current.secondary
val glassWhite:Color @Composable get()=LocalPalette.current.surface
val glassEdge:Color @Composable get()=LocalPalette.current.border
val softWash:Color @Composable get()=LocalPalette.current.wash

internal data class GlobalThemeSelection(val id:String="pearl",val select:(String)->Unit={})
internal val LocalGlobalTheme=staticCompositionLocalOf{GlobalThemeSelection()}
internal fun applicationPalette(id:String,base:MeridianPalette)=if(id=="custom")base else homePalette(id,base)
internal fun themeChartColors(palette:MeridianPalette)=palette.chartColors?:listOf(palette.accent,androidx.compose.ui.graphics.lerp(palette.accent,Color(0xFF66A894),.65f),androidx.compose.ui.graphics.lerp(palette.accent,Color(0xFFA08AC6),.7f),androidx.compose.ui.graphics.lerp(palette.accent,Color(0xFFCBA970),.78f))
internal fun themeOnAccent(color:Color)=if(color.luminance()>.179f)Color.Black else Color.White
@Composable fun MeridianTheme(mode:String,options:AppearanceOptions=AppearanceOptions(),colorTheme:String="pearl",onColorTheme:(String)->Unit={},content:@Composable ()->Unit){
 val dark=when(mode){"dark"->true;"light"->false;else->isSystemInDarkTheme()}
 val p=applicationPalette(colorTheme,(if(dark)DarkPalette else LightPalette).copy(accent=if(dark&&options.accent==1)Color.White else flowAccentColors[options.accent]))
 val density=LocalDensity.current
 val policy=MotionPolicy(if(android.animation.ValueAnimator.areAnimatorsEnabled())options.motionMode else "off")
 val view=LocalView.current
 SideEffect{view.context.hostActivity()?.window?.let{it.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(p.background[0].toArgb()));applyImmersiveBars(it,view,dark)}}
 val seed=if(dark)darkColorScheme()else lightColorScheme()
 val scheme=seed.copy(primary=p.accent,onPrimary=themeOnAccent(p.accent),primaryContainer=p.wash,onPrimaryContainer=p.text,secondary=p.accent,onSecondary=themeOnAccent(p.accent),secondaryContainer=p.wash,onSecondaryContainer=p.text,tertiary=p.accent,onTertiary=themeOnAccent(p.accent),background=p.background[0],surface=p.surface,onSurface=p.text,onBackground=p.text,surfaceVariant=p.wash,onSurfaceVariant=p.secondary,surfaceTint=p.accent,outline=p.border,outlineVariant=p.border)

 CompositionLocalProvider(LocalGlobalTheme provides GlobalThemeSelection(colorTheme,onColorTheme),LocalPalette provides p,LocalAppearance provides options,LocalSystemDensity provides density,LocalDensity provides Density(density.density*options.interfaceScale,density.fontScale),LocalLiusuDark provides dark,LocalNavigationOptions provides GlassOptions(glass=options.glass,liquid=options.glassStyle=="liquid"),LocalAppPreferences provides MaterialPreferences(options.liquidBlur,options.liquidRefraction,options.glassHighlight),LocalAppMotion provides policy){MaterialTheme(colorScheme=scheme){CompositionLocalProvider(LocalContentColor provides p.text,androidx.compose.foundation.LocalIndication provides FlowQuietIndication,content=content)}}
}
