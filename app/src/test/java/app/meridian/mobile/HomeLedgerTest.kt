package app.meridian.mobile
import org.junit.Assert.*
import org.junit.Test
import java.time.YearMonth
import org.json.JSONObject
class HomeLedgerTest{
 @Test fun currentMonthCostDoesNotMixPriorMonth(){val rows=listOf(JSONObject().put("date","2026-11-01").put("cents",1234),JSONObject().put("date","2026-10-31").put("cents",90000),JSONObject().put("date","invalid").put("cents",100));val result=monthlyLedgerCost(rows,YearMonth.of(2026,11));assertEquals(1234L,result.cents);assertEquals(1,result.count);assertEquals(.1234,result.perKm(100.0)!!,.000001)}
 @Test fun missingZeroOrInvalidMileageDoesNotInventUnitCost(){val c=MonthlyLedgerCost(100,1);listOf(null,0.0,-1.0,Double.NaN,Double.POSITIVE_INFINITY).forEach{assertNull(c.perKm(it))};assertEquals(0.0,MonthlyLedgerCost(0,0).perKm(12.0)!!,0.0)}
 @Test fun migrationPlacesNewCardBesideConditionWithoutResettingLayout(){val old=listOf("recentTrip","condition","sentry","range");val next=normalizedHomeOrder(old);assertEquals("recentTrip",next.first());assertEquals(next.indexOf("condition")+1,next.indexOf("ledger"));assertEquals(3,homeSpan("ledger"));assertEquals(3,homeSpan("condition"));val custom=moveHomeCard(next,"ledger","report");assertEquals(custom,normalizedHomeOrder(custom))}
 @Test fun pairedConditionAndLedgerStartTogetherEvenAfterHalfCard(){assertEquals(listOf(listOf("recentTrip"),listOf("condition","ledger")),homeRows(listOf("recentTrip","condition","ledger")))}
}
