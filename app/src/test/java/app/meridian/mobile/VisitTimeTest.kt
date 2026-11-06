package app.meridian.mobile
import org.junit.Assert.*
import org.junit.Test
class VisitTimeTest{
 @Test fun epochMillisecondsAndSecondsHaveSameHumanDate(){assertEquals(prettyTime("1793839943000"),prettyTime("1793839943"));assertTrue(prettyTime("1793839943000").contains(":"));assertFalse(prettyTime("1793839943000").contains("1793839943"))}
 @Test fun isoAndEmptyRemainReadable(){assertEquals("—",prettyTime("—"));assertEquals("11-05 09:31",prettyTime(java.time.LocalDate.now().year.toString()+"-11-05T09:31:00"))}
}
