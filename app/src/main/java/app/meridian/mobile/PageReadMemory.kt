package app.meridian.mobile
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicLong
internal class PageReadMemory(private val capacity:Int=24){
 companion object{private val revision=AtomicLong(0);fun invalidateAll(){revision.incrementAndGet()}}
 private var epoch=revision.get()
 private val entries=object:LinkedHashMap<String,JSONObject>(16,.75f,true){override fun removeEldestEntry(eldest:MutableMap.MutableEntry<String,JSONObject>?)=size>capacity}
 @Synchronized fun get(key:String):JSONObject?{sync();return entries[key]}
 @Synchronized fun put(key:String,value:JSONObject){sync();entries[key]=value}
 @Synchronized fun clear(){entries.clear();epoch=revision.get()}
 private fun sync(){if(epoch!=revision.get()){entries.clear();epoch=revision.get()}}
}
internal fun Api.memoryRead(path:String,body:JSONObject?):JSONObject?=if(accountScope.isBlank()||path !in cacheableReads)null else pageReadMemory.get(cacheRequestKey(cacheScope,path,body))
internal fun freshPageRead(value:JSONObject?,now:Long=System.currentTimeMillis()):Boolean{val time=responseSyncTime(value);return time>0&&now>=time&&now-time<60000L}
