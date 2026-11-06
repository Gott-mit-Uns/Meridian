package app.meridian.mobile

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class RecordSummaryTest {
 @Test fun readsNestedWebSummaryRatherThanPaginationEnvelope(){
  val envelope=JSONObject().put("totalTrips",999).put("summary",JSONObject().put("totalTrips",3).put("totalMileage",12000).put("totalDuration",5400000).put("avgSpeedKmh",24))
  val rows=recordSummaryMetrics(envelope,false)
  assertEquals("3",rows[0].value);assertEquals("12",rows[1].value);assertEquals("km",rows[1].unit);assertEquals("1时30分",rows[2].value);assertEquals("24",rows[3].value)
 }
 @Test fun chargeSummaryKeepsRangeAndConvertsMinutesExactlyOnce(){
  val rows=recordSummaryMetrics(JSONObject().put("summary",JSONObject().put("totalSessions",0).put("totalEnergy",30).put("totalDurationMinutes",90).put("totalRange",200)),true)
  assertEquals("0",rows[0].value);assertEquals("1时30分",rows[2].value);assertEquals("200",rows[3].value)
 }
 @Test fun missingSummaryDoesNotInventZeroTotals(){assertTrue(recordSummaryMetrics(JSONObject(),false).isEmpty())}
}
