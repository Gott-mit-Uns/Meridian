package app.meridian.mobile

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class FootprintTripRequestTest {
 @Test fun districtUsesTripFiltersAndPreservesDateRange(){
  val base=JSONObject().put("carId",42).put("startDate","2026-11-01").put("endDate","2026-11-05")
  val result=footprintTripRequest(base,"浙江省","杭州市","钱塘区",2)
  assertEquals("钱塘区",result.getString("districtName"))
  assertEquals("浙江省",result.getString("provinceName"));assertEquals("杭州市",result.getString("cityName"))
  assertEquals("2026-11-01",result.getString("startDate"));assertEquals(42,result.getInt("carId"))
  assertEquals(2,result.getInt("pageNum"));assertEquals(10,result.getInt("pageSize"))
  assertFalse(result.has("keyword"));assertFalse(result.has("level"));assertFalse(base.has("districtName"))
 }
 @Test fun paginationUsesTotalRatherThanOnlyFullLastPage(){
  val full=JSONArray((1..10).toList())
  assertTrue(footprintHasNext(JSONObject().put("list",full).put("size",20),1))
  assertFalse(footprintHasNext(JSONObject().put("list",full).put("size",20),2))
  assertFalse(footprintHasNext(JSONObject().put("list",JSONArray()).put("size",30),1))
  assertFalse(footprintHasNext(null,1))
 }
 @Test(expected=IllegalArgumentException::class) fun incompleteRegionCannotBecomeBroadTripQuery(){
  footprintTripRequest(JSONObject(),"浙江省","","钱塘区",1)
 }
}
