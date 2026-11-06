package app.meridian.mobile
import org.json.JSONObject
/** Dashboard uses route; detail responses use separate endpoint fields. Keep raw values intact. */
internal fun recentTripAddresses(data:JSONObject):Pair<String,String>{
 fun first(vararg keys:String)=keys.map{data.optString(it,"").trim()}.firstOrNull{it.isNotBlank()&&it !in listOf("null","undefined","出发地","目的地")}.orEmpty()
 val start=first("startLocation","startAddress","startLocationName");val end=first("endLocation","endAddress","endLocationName")
 val route=first("route");val parts=route.split(Regex("\\s*(?:→|->|➜|➡|—>|\\s至\\s)\\s*"),limit=2)
 return (start.ifBlank{parts.takeIf{it.size==2}?.first().orEmpty()}.ifBlank{route.ifBlank{"出发地址暂无数据"}}) to (end.ifBlank{parts.takeIf{it.size==2}?.last().orEmpty()}.ifBlank{if(route.isBlank())"目的地址暂无数据"else""})
}
