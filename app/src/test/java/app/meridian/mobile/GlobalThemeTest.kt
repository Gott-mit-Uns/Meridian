package app.meridian.mobile
import androidx.compose.ui.graphics.*
import org.junit.Assert.*
import org.junit.Test
class GlobalThemeTest{
 private fun base(dark:Boolean)=MeridianPalette(dark,if(dark)Color.White else Color.Black,Color.Gray,Color.Red,listOf(if(dark)Color.Black else Color.White),Color.Gray,Color.Gray,Color.Gray)
 @Test fun presetChangesTheGlobalBackgroundAndAccentTogether(){val pearl=applicationPalette("pearl",base(false));val sage=applicationPalette("sage",base(false));assertNotEquals(pearl.background.first(),sage.background.first());assertNotEquals(pearl.accent,sage.accent);assertEquals(sage.accent,themeChartColors(sage).first())}
 @Test fun customAccentSelectionRetainsTheExistingAppearanceColors(){val original=base(false);assertEquals(original,applicationPalette("custom",original))}
 @Test fun allPresetsRetainBlackNightCanvasAndReadablePrimaryContent(){for(id in homeThemeNames.keys)for(dark in listOf(true,false)){val palette=applicationPalette(id,base(dark));if(dark)assertTrue(palette.background.all{it==Color.Black});val bg=palette.accent.luminance();val fg=themeOnAccent(palette.accent).luminance();val contrast=(maxOf(bg,fg)+.05f)/(minOf(bg,fg)+.05f);assertTrue("$id / $dark contrast=$contrast",contrast>=4.5f)}}
 @Test fun navigationHasRoomForFixedIconSlotAndScaledLabel(){for(font in listOf(1f,1.3f,1.6f,2f))for(scale in listOf(.7f,1f,1.5f)){val line=14*font;assertTrue(navigationRailHeightDp(true,line,scale)>=34+3+line+4)}}
}
