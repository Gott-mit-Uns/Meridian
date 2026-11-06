package app.meridian.mobile

import androidx.compose.ui.unit.*
import org.json.*
import org.junit.Assert.*
import org.junit.Test

class MenuShareTest{
 @Test fun popupFollowsTextInsteadOfScreenOrigin(){
  assertEquals(IntOffset(22,128),menuPosition(IntRect(30,100,160,124),IntSize(400,800),IntSize(210,160),4,10))
 }
 @Test fun popupFlipsAndClampsAtEdges(){
  assertEquals(IntOffset(180,536),menuPosition(IntRect(370,700,399,724),IntSize(400,800),IntSize(210,160),4,10))
 }
 private fun root()=JSONObject("""{"periodDashboard":{"metricGroups":[{"key":"energy","items":[{"label":"平均能耗","value":13.2,"unit":"kWh/100km"},{"label":"地址","value":"secret"},{"label":"设备耗电","value":"住址A"}]}]},"lifetimeDashboard":{"metricGroups":[{"key":"trip","items":[{"label":"累计里程","value":1234,"unit":"km"}]}]},"vin":"secret-vin","startLocation":"private"}""")
 @Test fun sharePreservesScopeAndExcludesIdentity(){
  val data=root();val before=data.toString();val metrics=statisticsShareMetrics(data,30,"overview")
  assertEquals(1,metrics.size);assertEquals("累计指标",metrics.first().trend);assertFalse(metrics.toString().contains("secret"));assertEquals(before,data.toString())
 }
 @Test fun energyShareExcludesFreeTextAndOtherModes(){
  val metrics=statisticsShareMetrics(root(),7,"energy")
  assertEquals(1,metrics.size);assertEquals("近 7 天",metrics.first().trend);assertEquals("kWh/100km",metrics.first().unit)
 }
 @Test fun monthlyShareRequiresExplicitStoryConsent(){
  val data=JSONObject().put("monthMileage",123).put("storySummary","私人地点").put("vin","secret").put("address","private")
  assertFalse(monthlyShareValues(data,false).toString().contains("私人"));assertFalse(monthlyShareValues(data,true).has("vin"));assertEquals("私人地点",monthlyShareValues(data,true).getString("storySummary"))
 }
}
