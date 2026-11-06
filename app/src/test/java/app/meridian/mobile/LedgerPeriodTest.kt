package app.meridian.mobile
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import org.json.JSONObject
class LedgerPeriodTest{
 private val today=LocalDate.of(2026,11,6)
 private fun bill(date:String,cents:Long)=JSONObject().put("date",date).put("cents",cents)
 @Test fun weekUsesMondayAndClipsToToday(){val p=ledgerPeriod("week",today,today);assertEquals(LocalDate.of(2026,11,2),p.start);assertEquals(today,p.end);val old=ledgerPeriod("week",LocalDate.of(2026,10,20),today);assertEquals(LocalDate.of(2026,10,25),old.end)}
 @Test fun annualPeriodExcludesFutureDays(){val p=ledgerPeriod("year",today,today);assertEquals(LocalDate.of(2026,1,1),p.start);assertEquals(today,p.end);assertEquals(today,ledgerPeriod("year",today.plusYears(2),today).end)}
 @Test fun categoryDrillCanKeepEntireYearRatherThanOnlySelectedMonth(){val rows=listOf(bill("2026-01-01",100),bill("2026-05-20",200),bill("2026-11-06",300),bill("2026-11-07",900),bill("2025-12-31",500));val selected=ledgerRows(rows,ledgerPeriod("year",today,today));assertEquals(3,selected.size);assertEquals(600L,selected.sumOf{it.getLong("cents")})}
 @Test fun annualChartHasOneBucketPerElapsedMonthAndReconciles(){val p=ledgerPeriod("year",today,today);val rows=listOf(bill("2026-01-01",100),bill("2026-01-02",50),bill("2026-11-06",250));val samples=ledgerSpendSamples(rows,p);assertEquals(11,samples.size);assertEquals(1.5,samples.first().value,0.0);assertEquals(4.0,samples.sumOf{it.value},.00001)}
 @Test fun weeklyBucketsIncludeZeroSpendDaysAndCorrectBoundary(){val p=ledgerPeriod("week",today,today);val rows=listOf(bill("2026-11-01",900),bill("2026-11-02",123),bill("2026-11-06",200));val samples=ledgerSpendSamples(rows,p);assertEquals(5,samples.size);assertEquals(0.0,samples[1].value,0.0);assertEquals(3.23,samples.sumOf{it.value},.00001)}
 @Test fun sameCalendarComparisonHandlesLeapDay(){val p=LedgerPeriod("year",LocalDate.of(2024,1,1),LocalDate.of(2024,2,29));val prev=previousLedgerPeriod(p);assertEquals(LocalDate.of(2023,2,28),prev.end)}
 @Test fun categoryFieldsRejectInvalidNumericAndCoverageInputs(){for(values in listOf(mapOf("quantity" to "-1"),mapOf("quantity" to "NaN"))){try{validateLedgerFields("accessories",values);fail()}catch(_:IllegalArgumentException){}};try{validateLedgerFields("insurance",mapOf("coverageStart" to "2026-11-06","coverageEnd" to "2026-10-01"));fail()}catch(_:IllegalArgumentException){}}
 @Test fun singleDayDrillHasOnlyThatDaysBills(){val p=LedgerPeriod("day",today,today);assertEquals(1,ledgerRows(listOf(bill("2026-11-05",100),bill("2026-11-06",200)),p).size)}
}
