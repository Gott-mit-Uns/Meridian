package app.meridian.mobile
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test
class UiAuditBoundsTest{
 @Test fun historicalCalendarCannotReturnToDisallowedToday(){val latest=LocalDate.of(2025,1,31);assertEquals(latest,boundedCalendarDate(LocalDate.of(2026,11,5),latest,null))}
 @Test fun initialDateIsClampedToBothBounds(){val first=LocalDate.of(2026,1,1);val last=LocalDate.of(2026,1,31);assertEquals(first,boundedCalendarDate(first.minusDays(1),last,first));assertEquals(last,boundedCalendarDate(last.plusDays(1),last,first));assertEquals(first.plusDays(5),boundedCalendarDate(first.plusDays(5),last,first))}
 @Test fun invalidNumbersDoNotBecomeVisibleMetricText(){for(value in listOf("NaN","Infinity","-Infinity","undefined","null"))assertEquals("—",homeValue(value));assertEquals("0",homeValue(0));assertEquals("0",homeValue("0"))}
}
