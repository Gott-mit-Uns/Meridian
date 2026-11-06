package app.meridian.mobile

import org.json.JSONObject

/** Region trip drill-down uses the trip API, not the region ranking API. */
internal fun footprintTripRequest(base:JSONObject,province:String,city:String,district:String,page:Int):JSONObject =
 JSONObject(base.toString()).apply {
  require(province.isNotBlank() && city.isNotBlank() && district.isNotBlank())
  put("provinceName",province);put("cityName",city);put("districtName",district)
  put("pageNum",page.coerceAtLeast(1));put("pageSize",10)
 }

internal fun footprintHasNext(data:JSONObject?,page:Int):Boolean {
 val count=data?.optJSONArray("list")?.length()?:0
 return count>0 && if(data?.has("size")==true)page.toLong()*10<data.optLong("size") else count==10
}
