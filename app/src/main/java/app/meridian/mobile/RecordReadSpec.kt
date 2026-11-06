package app.meridian.mobile
import org.json.*
import java.time.LocalDate
internal data class RecordReadSpec(val path:String,val body:JSONObject)
internal fun recordReadSpec(carId:Any,charging:Boolean,page:Int,sort:String,keyword:String,kind:String,city:String,days:Int,range:Pair<LocalDate,LocalDate>?,today:LocalDate=LocalDate.now()):RecordReadSpec{
 val body=JSONObject().put("carId",carId).put("pageNum",page).put("pageSize",20).put("orderBy","trip_start_time $sort").put("sortOrder",sort)
 if(!charging){if(keyword.isNotBlank())body.put("keyword",keyword);body.put("tripType",kind);if(city.isNotBlank())body.put("cityName",city)}
 if(days==-1)range?.let{body.put("startDate",it.first.toString()).put("endDate",it.second.toString())}
 if(days>0)body.put("startDate",today.minusDays(days-1L).toString()).put("endDate",today.toString())
 return RecordReadSpec(if(charging)"/car/charge/history"else"/car/trip/get",body)
}
internal fun recordRows(response:JSONObject?):JSONArray?=when(val data=response?.opt("data")){is JSONArray->data;is JSONObject->data.optJSONArray("list")?:data.optJSONArray("records");else->null}
