package app.meridian.mobile
import androidx.compose.runtime.*
import org.json.*
internal fun redactKnownPlaces(text:String,places:Map<String,String>):String{var result=text;places.entries.sortedByDescending{it.key.length}.forEach{(raw,masked)->if(raw.length>=4)result=result.replace(raw,masked)};return result}
internal object PrivacyAddressBook{
 var places by mutableStateOf<Map<String,String>>(emptyMap());private set
 fun clear(){places=emptyMap()}
 @Synchronized fun observe(value:Any?){val collected=linkedMapOf<String,String>()
  fun add(raw:String){if(raw.length<4||raw.length>200||raw in listOf("位置暂无数据","出发地址暂无数据","目的地址暂无数据"))return;val masked=cityOnlyAddress(raw);collected[raw]=masked;val tail=raw.substringAfterLast('市',raw).substringAfterLast('区').substringAfterLast('县');if(tail.length>=5){collected[tail]=masked;val poi=tail.removeSuffix("附近");if(poi.length>=4)collected[poi]=masked}}
  fun walk(node:Any?){when(node){is JSONObject->node.keys().forEach{key->val child=node.opt(key);if(addressField(key)&&child is String)add(child)else if(key=="route"&&child is String){child.split(Regex("\\s*(?:→|->|➜)\\s*")).forEach{add(it)}}else if(child is JSONObject||child is JSONArray)walk(child)};is JSONArray->for(i in 0 until node.length())walk(node.opt(i))}}
  walk(value);if(collected.isNotEmpty())places=(places+collected).entries.toList().takeLast(400).associate{it.key to it.value}
 }
}
