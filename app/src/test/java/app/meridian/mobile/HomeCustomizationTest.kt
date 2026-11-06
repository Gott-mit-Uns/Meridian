package app.meridian.mobile
import org.junit.Assert.*
import org.junit.Test
class HomeCustomizationTest{
 @Test fun damagedOrOldOrderRetainsAllCurrentCardsWithoutDuplicates(){val result=normalizedHomeOrder(listOf("brief","removed","brief","range"));assertEquals("brief",result.first());assertEquals(homeCardIds.toSet(),result.toSet());assertEquals(homeCardIds.size,result.size);assertTrue(result.contains("energyTrend"))}
 @Test fun movingCardsPreservesCompleteDataInventory(){val next=moveHomeCard(homeCardIds,"report","range");assertEquals("report",next[1]);assertEquals(homeCardIds.toSet(),next.toSet());assertEquals(homeCardIds.size,next.size)}
 @Test fun unknownMoveCannotRemoveCards(){assertEquals(homeCardIds,moveHomeCard(homeCardIds,"missing","range"));assertEquals(homeCardIds,moveHomeCard(homeCardIds,"range","missing"))}
 @Test fun singleSmallCardKeepsItsOwnSlotAndFullCardsNeverShareRow(){val rows=homeRows(listOf("recentTrip","brief","recentCharge","history","vehicle"));assertEquals(listOf(listOf("recentTrip"),listOf("brief"),listOf("recentCharge","history"),listOf("vehicle")),rows)}
 @Test fun swappingIndependentHalfCardsDoesNotDropOthers(){val order=moveHomeCard(homeCardIds,"recentCharge","recentTrip");assertTrue(order.indexOf("recentCharge")<order.indexOf("recentTrip"));assertEquals(homeCardIds.toSet(),homeRows(order).flatten().toSet())}
 @Test fun threeStatusCardsCanMoveIndependently(){assertEquals(listOf(listOf("condition","ledger"),listOf("sentry","temperature")),homeRows(listOf("condition","ledger","sentry","temperature")));val order=moveHomeCard(homeCardIds,"sentry","report");assertTrue(order.indexOf("sentry")>order.indexOf("brief"));assertEquals(homeCardIds.toSet(),homeRows(order).flatten().toSet())}
 @Test fun everyThemeRetainsPureBlackInDarkMode(){val base=MeridianPalette(true,androidx.compose.ui.graphics.Color.White,androidx.compose.ui.graphics.Color.Gray,androidx.compose.ui.graphics.Color.Blue,listOf(androidx.compose.ui.graphics.Color.Black),androidx.compose.ui.graphics.Color.Black,androidx.compose.ui.graphics.Color.Gray,androidx.compose.ui.graphics.Color.Black);homeThemeNames.keys.forEach{val palette=homePalette(it,base);assertTrue(palette.background.all{color->color==androidx.compose.ui.graphics.Color.Black});assertNotEquals(palette.text,palette.surface)}}
}
