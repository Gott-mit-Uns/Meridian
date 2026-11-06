package app.meridian.mobile
import org.json.*
import org.junit.Assert.*
import org.junit.Test
class ReportChartDataTest{
 @Test fun overviewConvertsMetresToKmAndKeepsEnergyUnit(){
  val trend=JSONObject("""{"drivingTrend":{"mileageStats":[{"date":"2026-11-05","mileage":12500}],"energyTrend":[{"date":"2026-11-05","energyConsumption":140}]}}""")
  val charts=reportCharts(JSONObject(),trend,"overview");assertEquals(2,charts.size);assertEquals(12.5,charts[0].samples.single().value,0.0);assertEquals("km",charts[0].unit);assertEquals("Wh/km",charts[1].unit)
 }
 @Test fun energyHistogramCountsEachValidDayExactlyOnce(){
  val rows=JSONArray();listOf(100,120,140,160,180).forEach{rows.put(JSONObject().put("date","11-05").put("energyConsumption",it))}
  val charts=reportCharts(JSONObject(),JSONObject().put("energyTrend",rows),"energy");val histogram=charts.first{it.kind=="bars"};assertEquals(5.0,histogram.samples.sumOf{it.value},0.0);assertEquals("天",histogram.unit)
 }
 @Test fun constantConsumptionRemainsOneHonestBin(){
  val rows=JSONArray("""[{"energyConsumption":140},{"energyConsumption":140}]""");val histogram=reportCharts(JSONObject(),JSONObject().put("energyTrend",rows),"energy").first{it.kind=="bars"};assertEquals(1,histogram.samples.size);assertEquals(2.0,histogram.samples.single().value,0.0)
 }
 @Test fun monthlyNeedsRealDataAndDoesNotInventComparisons(){
  assertTrue(monthlyReportCharts(JSONObject()).isEmpty())
  val data=JSONObject("""{"dayMileages":[{"date":"2026-11-05","mileage":32}],"drivingEnergyPercent":70,"tempCtrlEnergyPercent":30,"vin":"secret","periodComparison":{"currentMileage":32}}""")
  val charts=monthlyReportCharts(data);assertEquals(2,charts.size);assertEquals("bar",charts.first().kind);assertEquals(32.0,charts.first().samples.single().value,0.0);assertEquals("donut",charts.last().kind);assertFalse(charts.toString().contains("secret"))
 }
}
