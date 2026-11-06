package app.meridian.mobile

import org.junit.Assert.*
import org.junit.Test

class NavigationTapTrackerTest {
 @Test fun doubleTapCurrentTabScrollsOnce(){
  val taps=NavigationTapTracker()
  assertFalse(taps.tap(0,0,1000));assertTrue(taps.tap(0,0,1200));assertFalse(taps.tap(0,0,1300))
 }
 @Test fun slowRepeatedTapsDoNotScroll(){
  val taps=NavigationTapTracker()
  assertFalse(taps.tap(1,1,1000));assertFalse(taps.tap(1,1,1400))
 }
 @Test fun switchThenSecondTapCanScrollNewPage(){
  val taps=NavigationTapTracker()
  assertFalse(taps.tap(3,0,1000));assertTrue(taps.tap(3,3,1150))
 }
 @Test fun alternatingTabsNeverAccidentallyScroll(){
  val taps=NavigationTapTracker()
  assertFalse(taps.tap(0,0,1000));assertFalse(taps.tap(1,0,1100));assertFalse(taps.tap(0,1,1200))
 }
}
