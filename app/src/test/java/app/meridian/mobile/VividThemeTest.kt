package app.meridian.mobile
import org.junit.Assert.*
import org.junit.Test
import androidx.compose.ui.graphics.Color
class VividThemeTest{
 private val base=MeridianPalette(false,Color.Black,Color.Gray,Color.Blue,listOf(Color.White),Color.White,Color.LightGray,Color.White)
 @Test fun referenceThemeHasWhiteCardsGrayCanvasAndBrightOrangeAccent(){val p=homePalette("vivid",base);assertEquals(Color.White,p.surface);assertEquals(List(3){Color(0xFFF4F4F6)},p.background);assertEquals(Color(0xFFFF6A00),p.accent);assertEquals(Color(0xFF232326),p.text)}
 @Test fun chartsKeepSaturatedReferenceColorsInsteadOfMutedBlends(){val p=homePalette("vivid",base);assertEquals(listOf(Color(0xFFFF6A00),Color(0xFF168BFF),Color(0xFF2CC653),Color(0xFFFFC900),Color(0xFF6254DE),Color(0xFFFF3B30)),themeChartColors(p));assertEquals(Color(0xFF2CC653),homeCardPalette("vivid","efficiency",p).accent)}
 @Test fun darkVersionKeepsBlackCanvasAndColoredCharts(){val p=homePalette("vivid",base.copy(dark=true));assertTrue(p.background.all{it==Color.Black});assertEquals(Color(0xFF202125),p.surface);assertEquals(6,themeChartColors(p).size)}
}
