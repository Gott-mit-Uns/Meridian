package app.meridian.mobile

import org.json.*
import org.junit.Assert.*
import org.junit.Test

class StatisticsMetricGroupsTest {
 private fun metric(label:String,value:Any,unit:String="")=JSONObject().put("label",label).put("value",value).put("unit",unit)
 private fun dashboard(vararg metrics:JSONObject)=JSONObject().put("metricGroups",JSONArray().put(JSONObject().put("title","总览").put("items",JSONArray(metrics.toList()))))

 @Test fun repeatedOverviewAcrossDashboardsAppearsOnce(){
  val four=arrayOf(metric("累计安全行驶",144,"天"),metric("累计总里程",12989,"km"),metric("统计期行程",105,"次"),metric("统计期时长","60时48分"))
  val root=JSONObject().put("periodDashboard",dashboard(*four)).put("lifetimeDashboard",dashboard(*four))
  val original=root.toString();val groups=statisticsMetricGroups(root)
  assertEquals(1,groups.size);assertEquals(4,groups.single().items.length())
  assertEquals("Must not mutate cached server response",original,root.toString())
 }
 @Test fun distinctPeriodAndLifetimeLabelsAreKeptEvenWithEqualValues(){
  val root=JSONObject().put("periodDashboard",dashboard(metric("统计期里程",100,"km"))).put("lifetimeDashboard",dashboard(metric("累计总里程",100,"km")))
  assertEquals(2,statisticsMetricGroups(root).sumOf{it.items.length()})
 }
 @Test fun differentValuesOrUnitsAreNotDiscarded(){
  val root=JSONObject().put("periodDashboard",dashboard(metric("里程",100,"km"),metric("里程",200,"km"),metric("里程",100,"m")))
  assertEquals(3,statisticsMetricGroups(root).single().items.length())
 }
 @Test fun integralAndDecimalRepresentationsOfSameMetricAreMerged(){
  val root=JSONObject().put("periodDashboard",dashboard(metric("里程",100,"km"),metric("里程",100.0,"km")))
  assertEquals(1,statisticsMetricGroups(root).single().items.length())
 }
 @Test fun emptyDashboardsDoNotCreateEmptyPanels(){assertTrue(statisticsMetricGroups(JSONObject()).isEmpty())}
}
