package app.meridian.mobile
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.*
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec
import java.io.IOException
import androidx.compose.runtime.*

internal object DeviceData{
 lateinit var context:android.app.Application
  private set
 var revision by mutableIntStateOf(0)
 fun initialize(c:Context){context=c.applicationContext as android.app.Application}
 private fun key():javax.crypto.SecretKey{val ks=KeyStore.getInstance("AndroidKeyStore").apply{load(null)};if(!ks.containsAlias("meridian-local"))KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply{init(KeyGenParameterSpec.Builder("meridian-local",KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());generateKey()};return ks.getKey("meridian-local",null)as javax.crypto.SecretKey}
 fun hash(value:String)=java.security.MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString(""){"%02x".format(it)}
 @Synchronized fun read(id:String):JSONObject=runCatching{val encoded=context.getSharedPreferences("device-data",0).getString(hash(id),null)?:return JSONObject();val bytes=Base64.decode(encoded,Base64.NO_WRAP);val cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,bytes.copyOfRange(0,12)));JSONObject(String(cipher.doFinal(bytes.copyOfRange(12,bytes.size)),Charsets.UTF_8))}.getOrDefault(JSONObject())
 @Synchronized fun write(id:String,data:JSONObject){if(id.startsWith("inbox:")){val keys=data.keys().asSequence().toList().sortedByDescending{data.optJSONObject(it)?.optLong("time")?:0L};keys.drop(100).forEach{data.remove(it)}};val cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());val bytes=cipher.iv+cipher.doFinal(data.toString().toByteArray(Charsets.UTF_8));context.getSharedPreferences("device-data",0).edit().putString(hash(id),Base64.encodeToString(bytes,Base64.NO_WRAP)).apply();revision++}
 @Synchronized fun clearCache(){PageReadMemory.invalidateAll();val index=context.getSharedPreferences("cache-index",0);val keys=index.getStringSet("keys",emptySet()).orEmpty();val editor=context.getSharedPreferences("device-data",0).edit();keys.forEach{editor.remove(it)};editor.apply();index.edit().clear().apply();revision++}
}
internal val cacheableReads=setOf("/car/trip/routeComparison","/car/status/current","/car/charge/current","/dashboard/get","/dashboard/getTrendData","/car/trip/get","/car/trip/getDetail","/car/charge/history","/car/charge/detail","/statistics/energy-overview","/trip-month-stats/get","/car/status/health","/car/status/history","/car/trip/cityFootprint/get","/car/trip/cityFootprint/detail","/car/trip/trackMap")
internal class HttpReadFailure(val status:Int):IOException("服务返回错误 $status")
internal fun canUseOfflineCache(e:Exception)=e is IOException&&(e !is HttpReadFailure||e.status in 500..599)
internal fun cacheRequestKey(scope:String,path:String,body:JSONObject?):String{val fields=JSONArray();body?.keys()?.asSequence()?.sorted()?.forEach{fields.put(JSONArray().put(it).put(body.opt(it)))};return JSONArray().put(scope).put(path).put(if(body==null)"GET"else fields).toString()}

internal fun Api.peekRead(path:String,body:JSONObject?):JSONObject?{
 if(accountScope.isBlank()||path !in cacheableReads)return null
 val id=cacheRequestKey(cacheScope,path,body);pageReadMemory.get(id)?.let{return it};val item=DeviceData.read("cache:"+id)
 val response=item.optJSONObject("response")?:return if(path=="/dashboard/get"&&body?.has("pageNum")==true)peekRead(path,JSONObject().put("carId",body.opt("carId")).put("statDate",body.optString("statDate",java.time.LocalDate.now().toString())))else null
 PrivacyAddressBook.observe(response)
 val time=item.optLong("time");response.put("_meridianCache",true).put("_meridianUpdatedAt",time);response.optJSONObject("data")?.put("_meridianCache",true)?.put("_meridianUpdatedAt",time)
 pageReadMemory.put(id,response)
 return response
}
internal fun Api.saveRead(path:String,body:JSONObject?,response:JSONObject,requestScope:String=accountScope,requestBase:String=base,requestCacheScope:String=cacheScope){
 synchronized(DeviceData){
 if(requestScope!=accountScope||requestBase!=base||requestCacheScope!=cacheScope)return
 if(accountScope.isBlank()||path !in cacheableReads)return
 if(response.toString().length>1000000){val time=System.currentTimeMillis();response.put("_meridianUpdatedAt",time);response.optJSONObject("data")?.put("_meridianUpdatedAt",time);return}
 PrivacyAddressBook.observe(response)
 val time=System.currentTimeMillis();val id=cacheRequestKey(requestCacheScope,path,body)
 DeviceData.write("cache:"+id,JSONObject().put("time",time).put("response",response))
 val preferences=DeviceData.context.getSharedPreferences("cache-index",0);val keys=preferences.getStringSet("keys",emptySet()).orEmpty().toMutableSet();keys.add(DeviceData.hash("cache:"+id));if(keys.size>100){val remove=keys.first();keys.remove(remove);DeviceData.context.getSharedPreferences("device-data",0).edit().remove(remove).apply()};preferences.edit().putStringSet("keys",keys).apply()
 response.put("_meridianUpdatedAt",time);response.optJSONObject("data")?.put("_meridianUpdatedAt",time)
 pageReadMemory.put(id,response)
 if(requestScope==accountScope&&requestBase==base)runCatching{captureLocalRecords(path,body,response)}
 }
}
internal fun Api.localScope(carId:Any)=JSONArray().put(base).put(accountScope).put(carId.toString()).toString()
internal fun Api.captureLocalRecords(path:String,body:JSONObject?,response:JSONObject){
 val carId=body?.opt("carId")?:activeCarId.takeIf{it.isNotBlank()}?:return;val scope=localScope(carId);val data=response.optJSONObject("data")
 if(path=="/dashboard/get"&&data!=null){val anomaly=data.optJSONObject("latestHealthAnomaly");if(anomaly!=null){val inbox=DeviceData.read("inbox:"+scope);val id="health:"+anomaly.optString("id",anomaly.toString());if(!inbox.has(id)){inbox.put(id,JSONObject().put("title",anomaly.optString("title","车况提醒")).put("time",System.currentTimeMillis()).put("read",false).put("kind","health"));DeviceData.write("inbox:"+scope,inbox)}};if(body?.optString("statDate",java.time.LocalDate.now().toString())==java.time.LocalDate.now().toString())WidgetSnapshot.save(data)}
 if(path=="/trip-month-stats/get"&&data!=null&&(data.has("monthMileage")||data.has("availDays"))){val month=body?.optString("month").orEmpty();val inbox=DeviceData.read("inbox:"+scope);if(month.isNotBlank()&&!inbox.has("report:"+month)){inbox.put("report:"+month,JSONObject().put("title",month+" 月度报告已读取").put("kind","report").put("month",month).put("time",System.currentTimeMillis()).put("read",false));DeviceData.write("inbox:"+scope,inbox)}}
 if(path=="/dashboard/get"&&data!=null){val current=data.optJSONObject("overview")?.optInt("chargeState",-1)?:-1;val previous=DeviceData.read("charge-state:"+scope).optInt("state",-1);if(previous in listOf(1,2)&&current==3){val inbox=DeviceData.read("inbox:"+scope);inbox.put("charge:"+System.currentTimeMillis(),JSONObject().put("title","车辆充电已完成").put("kind","charge").put("time",System.currentTimeMillis()).put("read",false));DeviceData.write("inbox:"+scope,inbox)};DeviceData.write("charge-state:"+scope,JSONObject().put("state",current))}
 if(path=="/car/trip/get"||path=="/car/trip/getDetail"){
  val rows=if(path.endsWith("getDetail"))listOfNotNull(data)else(data?.optJSONArray("list")?:data?.optJSONArray("records")?:response.optJSONArray("data"))?.objects().orEmpty()
  val index=DeviceData.read("trips:"+scope)
  rows.forEach{j->val id=j.opt("id")?:j.opt("tripId");if(id!=null)index.put(id.toString(),tripDigest(j))}
  val keys=index.keys().asSequence().toList().sortedByDescending{index.optJSONObject(it)?.optString("tripStartTime").orEmpty()};keys.drop(500).forEach{index.remove(it)}
  DeviceData.write("trips:"+scope,index)
 }
}

internal fun hasMoreTripRecords(response:JSONObject,received:Int,loaded:Int,requested:Int):Boolean{
 if(received<=0||loaded>=500)return false
 val data=response.optJSONObject("data")?:response
 val total=data.number("total")?:data.number("totalCount")?:response.number("total")
 return if(total!=null&&total>=0)loaded<total else received>=requested
}
