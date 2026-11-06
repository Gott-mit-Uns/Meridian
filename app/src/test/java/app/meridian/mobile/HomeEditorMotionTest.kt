package app.meridian.mobile
import org.junit.Assert.*
import org.junit.Test
import androidx.compose.ui.geometry.*
class HomeEditorMotionTest{
 @Test fun scrollStopsAwayFromEdgesAndScalesByDistance(){assertEquals(0f,homeDragVelocity(500f,100f,1000f,100f,600f),0f);assertEquals(-300f,homeDragVelocity(150f,100f,1000f,100f,600f),0f);assertEquals(300f,homeDragVelocity(950f,100f,1000f,100f,600f),0f);assertEquals(600f,homeDragVelocity(2000f,100f,1000f,100f,600f),0f)}
 @Test fun badViewportDoesNotScroll(){assertEquals(0f,homeDragVelocity(Float.NaN,0f,100f,20f,600f),0f);assertEquals(0f,homeDragVelocity(20f,100f,0f,20f,600f),0f)}
 @Test fun dropHysteresisAvoidsOscillatingAtCardEdge(){val bounds=mapOf("a" to Rect(0f,0f,100f,100f),"b" to Rect(110f,0f,210f,100f));assertNull(homeDropTarget(Offset(112f,50f),"a",listOf("a","b"),bounds));assertEquals("b",homeDropTarget(Offset(150f,50f),"a",listOf("a","b"),bounds));assertNull(homeDropTarget(Offset(50f,50f),"a",listOf("a","b"),bounds))}
 @Test fun whiteMulticolorThemesHaveDistinctSemanticColorsAndBlackDarkBackground(){val light=MeridianPalette(false,androidx.compose.ui.graphics.Color.Black,androidx.compose.ui.graphics.Color.Gray,androidx.compose.ui.graphics.Color.Blue,listOf(androidx.compose.ui.graphics.Color.White),androidx.compose.ui.graphics.Color.White,androidx.compose.ui.graphics.Color.LightGray,androidx.compose.ui.graphics.Color.White);splitChartThemes.forEach{theme->val palette=homePalette(theme,light);assertEquals(androidx.compose.ui.graphics.Color.White,palette.surface);assertNotEquals(homeCardPalette(theme,"ledger",palette).accent,homeCardPalette(theme,"condition",palette).accent);assertNotEquals(homeCardPalette(theme,"time",palette).accent,homeCardPalette(theme,"efficiency",palette).accent);assertTrue(homePalette(theme,palette.copy(dark=true)).background.all{it==androidx.compose.ui.graphics.Color.Black})}}
}
