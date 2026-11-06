package app.meridian.mobile
import androidx.compose.ui.graphics.*
import org.junit.Assert.*
import org.junit.Test
class PosterThemeTest{
 private fun base(dark:Boolean)=MeridianPalette(dark,if(dark)Color.White else Color.Black,Color.Gray,Color.Blue,listOf(if(dark)Color.Black else Color.White),if(dark)Color(0xFF17171B)else Color.White,Color.Gray,Color.Gray)
 @Test fun posterReceivesTheSameThemeAsThePage(){for(id in homeThemeNames.keys)for(dark in listOf(true,false)){val p=homePalette(id,base(dark));val poster=posterTheme(p);assertEquals(p.accent.toArgb(),poster.accent);assertEquals(p.text.toArgb(),poster.text);assertEquals(p.surface.toArgb(),poster.card);assertEquals(p.background.map{it.toArgb()},poster.background);assertEquals(if(id=="vivid")6 else 4,poster.chartColors.size)}}
 @Test fun multicolorAndNightBackgroundsAreNotFlattenedToDefaultWhite(){val multicolor=posterTheme(homePalette("aurora",base(false)));assertTrue(multicolor.background.distinct().size>1);val night=posterTheme(homePalette("aurora",base(true)));assertTrue(night.background.all{it==Color.Black.toArgb()});assertNotEquals(night.text,night.card)}
}
