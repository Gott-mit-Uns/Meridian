package app.meridian.mobile
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test
class AnnualPeriodTest{
 @Test fun currentYearEndsToday(){val p=annualPeriod(2026,LocalDate.of(2026,11,5));assertEquals(LocalDate.of(2026,1,1),p.start);assertEquals(LocalDate.of(2026,11,5),p.end);assertEquals(309,p.days)}
 @Test fun historicalLeapYearIncludesLeapDay(){val p=annualPeriod(2024,LocalDate.of(2026,11,5));assertEquals(LocalDate.of(2024,12,31),p.end);assertEquals(366,p.days)}
 @Test fun firstDayIsInclusive(){assertEquals(1,annualPeriod(2026,LocalDate.of(2026,1,1)).days)}
 @Test(expected=IllegalArgumentException::class) fun futureYearRejected(){annualPeriod(2027,LocalDate.of(2026,11,5))}
}
