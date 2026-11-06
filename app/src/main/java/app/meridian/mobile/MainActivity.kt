package app.meridian.mobile

import android.os.Bundle
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import dev.chrisbanes.haze.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import org.json.*
import java.net.*
import java.security.KeyStore
import javax.crypto.*
import javax.crypto.spec.GCMParameterSpec

class Vault(val activity: ComponentActivity) {
 private val prefs=activity.getSharedPreferences("session",0)
 private fun key(): javax.crypto.SecretKey { val ks=KeyStore.getInstance("AndroidKeyStore").apply{load(null)}; if(!ks.containsAlias("meridian")){KeyGenerator.getInstance("AES","AndroidKeyStore").apply{init(KeyGenParameterSpec.Builder("meridian",KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes("GCM").setEncryptionPaddings("NoPadding").build());generateKey()}};return ks.getKey("meridian",null) as javax.crypto.SecretKey }
 fun save(value:String){val c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key());prefs.edit().putString("data",Base64.encodeToString(c.iv+c.doFinal(value.toByteArray()),Base64.NO_WRAP)).apply()}
 fun read():JSONObject=try{val b=Base64.decode(prefs.getString("data","")!!,Base64.NO_WRAP);val c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,b.copyOfRange(0,12)));JSONObject(String(c.doFinal(b.copyOfRange(12,b.size))))}catch(_:Exception){JSONObject()}
 fun clear(){prefs.edit().remove("data").apply()}
 fun theme()=prefs.getString("theme","system")?:"system"
 fun saveTheme(value:String){prefs.edit().putString("theme",value).apply()}
 fun glass()=prefs.getBoolean("glass",true)
 fun saveGlass(value:Boolean){prefs.edit().putBoolean("glass",value).apply()}
}
class ChatMemory { val conversation=mutableStateOf("");val messages=mutableStateListOf<Pair<String,String>>();val metadata=mutableStateMapOf<Int,JSONObject>();val localImages=mutableStateMapOf<Int,List<androidx.compose.ui.graphics.ImageBitmap>>();var bootstrapToken="" }
open class Api {
 val chats=mutableMapOf<String,ChatMemory>()
 internal val pageReadMemory=PageReadMemory()
 var base=""
 var accountScope=""
 var activeCarId=""
 var offlineOnly by mutableStateOf(false)
 fun webCookies():List<String> = cookies.cookieStore.cookies.filter{!it.hasExpired()}.map{it.name+"="+it.value+"; Path="+(it.path?:"/")+(if(it.secure)"; Secure"else"")+(if(it.isHttpOnly)"; HttpOnly"else"")}
 @Volatile private var activeStream:HttpURLConnection?=null
 fun stopStream(){val connection=activeStream;activeStream=null;Thread{connection?.disconnect()}.start()}
 private val cookies=CookieManager(null,CookiePolicy.ACCEPT_ORIGINAL_SERVER)
 suspend fun uploadAssistant(carId:Any,name:String,mime:String,bytes:ByteArray):JSONObject=withContext(Dispatchers.IO){
  if(offlineOnly)throw Exception("离线浏览不可执行联网操作，请先重新连接服务")
  require(bytes.size<=5*1024*1024){"图片不能超过5MB"}
  require(mime in listOf("image/jpeg","image/png","image/webp")){"支持 JPG、PNG、WebP 图片"}
  val boundary="Meridian"+java.util.UUID.randomUUID().toString().replace("-","")
  val uri=URI(base+"/api/car/assistant/attachment/upload");val c=uri.toURL().openConnection() as HttpURLConnection
  c.requestMethod="POST";c.instanceFollowRedirects=false;c.connectTimeout=15000;c.readTimeout=60000;c.doOutput=true
  c.setRequestProperty("Content-Type","multipart/form-data; boundary=$boundary");c.setRequestProperty("X-Client-Source","web")
  cookies.get(uri,emptyMap()).forEach{(k,v)->c.setRequestProperty(k,v.joinToString("; "))}
  val filename=name.replace(Regex("[\\r\\n\\\"\\\\]"),"_").take(120)
  try{
   c.outputStream.buffered().use{out->fun write(s:String){out.write(s.toByteArray(Charsets.UTF_8))};write("--$boundary\r\nContent-Disposition: form-data; name=\"carId\"\r\n\r\n$carId\r\n");write("--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"$filename\"\r\nContent-Type: $mime\r\n\r\n");out.write(bytes);write("\r\n--$boundary--\r\n")}
   require(c.responseCode in 200..299){"图片上传失败（${c.responseCode}）"}
   val response=JSONObject(c.inputStream.bufferedReader(Charsets.UTF_8).use{it.readText()})
   require(response.optInt("code",200)in listOf(0,200)){response.safeText("msg",response.safeText("message","上传失败"))}
   response.optJSONObject("data")?:throw Exception("服务未返回图片凭据")
  }finally{c.disconnect()}
 }
 suspend fun assistantMedia(conversationId:String,messageId:String):Map<String,ByteArray> =withContext(Dispatchers.IO){
  if(offlineOnly)throw Exception("离线浏览不可执行联网操作，请先重新连接服务")
  val uri=URI(base+"/api/car/assistant/media/previews");val c=uri.toURL().openConnection()as HttpURLConnection
  c.requestMethod="POST";c.instanceFollowRedirects=false;c.connectTimeout=15000;c.readTimeout=60000;c.doOutput=true
  c.setRequestProperty("Content-Type","application/json");c.setRequestProperty("Accept","multipart/form-data, application/json");c.setRequestProperty("X-Client-Source","web")
  cookies.get(uri,emptyMap()).forEach{(k,v)->c.setRequestProperty(k,v.joinToString("; "))}
  try{c.outputStream.use{it.write(JSONObject().put("conversationId",conversationId).put("messageId",messageId).toString().toByteArray())};require(c.responseCode in 200..299){"图片读取失败（${c.responseCode}）"};val type=c.contentType.orEmpty();val bytes=c.inputStream.use{it.readCapped(24*1024*1024)};if(type.contains("json")){val j=JSONObject(bytes.toString(Charsets.UTF_8));throw Exception(j.safeText("msg",j.safeText("message","图片暂不可用")))};parseMultipartImages(type,bytes)}finally{c.disconnect()}
 }
 suspend fun imageResource(address:String):ByteArray=withContext(Dispatchers.IO){
  if(offlineOnly)throw Exception("离线浏览不可执行联网操作，请先重新连接服务")
  if(address.startsWith("data:image/")){val encoded=address.substringAfter("base64,","");require(encoded.length<=8*1024*1024);return@withContext Base64.decode(encoded,Base64.DEFAULT)}
  val target=URI(base).resolve(address);require(target.scheme=="https"&&target.userInfo==null){"图片地址无效"}
  val c=target.toURL().openConnection()as HttpURLConnection;c.instanceFollowRedirects=false;c.connectTimeout=15000;c.readTimeout=20000
  if(target.host==URI(base).host)cookies.get(target,emptyMap()).forEach{(k,v)->c.setRequestProperty(k,v.joinToString("; "))}
  try{require(c.responseCode in 200..299){"图片读取失败（${c.responseCode}）"};c.inputStream.use{it.readCapped(6*1024*1024)}}finally{c.disconnect()}
 }
 suspend fun downloadBackup(path:String,destination:(java.io.InputStream)->Unit)=withContext(Dispatchers.IO){
  if(offlineOnly)throw Exception("离线浏览不可执行联网操作，请先重新连接服务")
  require(path.startsWith("/backup/"))
  val uri=URI(base+"/api"+path);val c=uri.toURL().openConnection()as HttpURLConnection
  c.instanceFollowRedirects=false;c.connectTimeout=15000;c.readTimeout=120000
  cookies.get(uri,emptyMap()).forEach{(k,v)->c.setRequestProperty(k,v.joinToString("; "))}
  try{require(c.responseCode in 200..299){"备份下载失败（${c.responseCode}）"};require(!c.contentType.orEmpty().contains("json")){"服务未返回备份文件，请刷新任务状态"};c.inputStream.use(destination)}finally{c.disconnect()}
 }
 suspend fun prepareBackup(name:String,password:String,source:()->java.io.InputStream):JSONObject=withContext(Dispatchers.IO){
  if(offlineOnly)throw Exception("离线浏览不可执行联网操作，请先重新连接服务")
  require(name.endsWith(".xmbak",true)){"请选择 .xmbak 备份文件"};require(password.length>=6){"恢复口令至少6位"}
  val uri=URI(base+"/api/backup/import/prepare");val c=uri.toURL().openConnection()as HttpURLConnection;val boundary="Meridian"+java.util.UUID.randomUUID().toString().replace("-","")
  c.requestMethod="POST";c.doOutput=true;c.instanceFollowRedirects=false;c.connectTimeout=15000;c.readTimeout=120000;c.setChunkedStreamingMode(65536)
  c.setRequestProperty("Content-Type","multipart/form-data; boundary=$boundary");c.setRequestProperty("X-Client-Source","web")
  cookies.get(uri,emptyMap()).forEach{(k,v)->c.setRequestProperty(k,v.joinToString("; "))}
  val filename=name.replace(Regex("[\\r\\n\\\"\\\\]"),"_").take(120)
  try{
   val coroutine=currentCoroutineContext()
   c.outputStream.buffered().use{out->fun text(s:String){out.write(s.toByteArray())};text("--$boundary\r\nContent-Disposition: form-data; name=\"password\"\r\n\r\n$password\r\n--$boundary\r\nContent-Disposition: form-data; name=\"archive\"; filename=\"$filename\"\r\nContent-Type: application/octet-stream\r\n\r\n")
    source().use{input->val buffer=ByteArray(65536);while(true){coroutine.ensureActive();val n=input.read(buffer);if(n<0)break;out.write(buffer,0,n)}};text("\r\n--$boundary--\r\n")}
   require(c.responseCode in 200..299){"上传校验失败（${c.responseCode}），可刷新服务端校验任务查看是否已接收"}
   val response=JSONObject(c.inputStream.use{it.readCapped(2*1024*1024)}.toString(Charsets.UTF_8));require(response.optInt("code",200)in listOf(0,200)){response.safeText("msg",response.safeText("message","校验失败"))}
   response.optJSONObject("data")?:throw Exception("服务未返回校验任务")
  }finally{c.disconnect()}
 }
 fun clear(clearWidget:Boolean=true){pageReadMemory.clear();accountScope="";offlineOnly=false;if(clearWidget)PrivacyAddressBook.clear();if(clearWidget)runCatching{WidgetSnapshot.clear()};stopStream();cookies.cookieStore.removeAll();chats.clear();android.webkit.CookieManager.getInstance().removeAllCookies(null)}
 suspend fun stream(body:JSONObject,onEvent:suspend (JSONObject)->Unit)=streamEndpoint("/car/assistant/stream",body,onEvent)
 suspend fun streamEndpoint(path:String,body:JSONObject,onEvent:suspend (JSONObject)->Unit)=withContext(Dispatchers.IO){
  if(offlineOnly)throw Exception("离线浏览不可执行联网操作，请先重新连接服务")
  val uri=URI(base+"/api"+path);val c=uri.toURL().openConnection() as HttpURLConnection
  activeStream=c;c.requestMethod="POST";c.connectTimeout=15000;c.readTimeout=120000;c.instanceFollowRedirects=false;c.doOutput=true
  c.setRequestProperty("Content-Type","application/json");c.setRequestProperty("Accept","text/event-stream");c.setRequestProperty("X-Client-Source","web")
  cookies.get(uri,emptyMap()).forEach{(k,v)->c.setRequestProperty(k,v.joinToString("; "))}
  try{c.outputStream.use{it.write(body.toString().toByteArray())};if(c.responseCode !in 200..299)throw Exception("助手连接失败：${c.responseCode}")
   c.inputStream.bufferedReader(Charsets.UTF_8).use{reader->
    val block=StringBuilder()
    suspend fun emit(){if(block.isNotEmpty()){val event=runCatching{JSONObject(block.toString())}.getOrNull();block.clear();if(event!=null)withContext(Dispatchers.Main){onEvent(event)}}}
    while(currentCoroutineContext().isActive){val line=reader.readLine()?:break;if(line.isEmpty())emit()else if(line.startsWith("data:")){if(block.isNotEmpty())block.append("\n");block.append(line.substring(5).trimStart())}}
    currentCoroutineContext().ensureActive();emit()
   }
  }finally{if(activeStream===c)activeStream=null;c.disconnect()}
 }

 open suspend fun call(path:String,body:JSONObject?=null):JSONObject{
  val requestScope=accountScope;val requestBase=base
  if(offlineOnly){val cached=withContext(Dispatchers.IO){peekRead(path,body)}?:throw Exception("离线浏览：本机没有这项缓存，连接服务后重试");cached.put("_meridianOffline",true);cached.optJSONObject("data")?.put("_meridianOffline",true);return cached}
  try{val response=remoteCall(path,body,requestScope,requestBase);withContext(Dispatchers.IO){runCatching{saveRead(path,body,response,requestScope,requestBase)}};if(requestScope!=accountScope||requestBase!=base)throw CancellationException("会话已变化");return response}catch(e:Exception){if(e is CancellationException)throw e;if(requestScope!=accountScope||requestBase!=base)throw CancellationException("会话已变化");if(canUseOfflineCache(e)){val cached=withContext(Dispatchers.IO){peekRead(path,body)};if(cached!=null){cached.put("_meridianOffline",true);cached.optJSONObject("data")?.put("_meridianOffline",true);return cached}};throw e}
 }
 private suspend fun remoteCall(path:String,body:JSONObject?,requestScope:String,requestBase:String):JSONObject=withContext(Dispatchers.IO){
  val u=URI(requestBase+"/api"+path);val c=u.toURL().openConnection() as HttpURLConnection
  c.instanceFollowRedirects=false;c.connectTimeout=15000;c.readTimeout=when{path=="/backup/import/confirm"->3900000;path=="/user/xiaomi/qr/poll"->45000;else->30000};c.requestMethod=if(body==null)"GET" else "POST"
  c.setRequestProperty("Accept","application/json");c.setRequestProperty("X-Client-Source","web")
  cookies.get(u,emptyMap()).forEach{(k,v)->c.setRequestProperty(k,v.joinToString("; "))}
  try{if(body!=null){c.doOutput=true;c.setRequestProperty("Content-Type","application/json");c.outputStream.use{it.write(body.toString().toByteArray())}}
   val status=c.responseCode;if(requestScope!=accountScope||requestBase!=base)throw CancellationException("会话已变化");cookies.put(u,c.headerFields.filterKeys{it!=null})
   if(status==401)throw Exception("登录已失效，请重新登录")
   if(status !in 200..299)throw HttpReadFailure(status)
   val text=c.inputStream.bufferedReader().use{it.readText()};val j=try{JSONObject(text)}catch(_:Exception){throw Exception("服务没有返回有效数据，请检查地址")}
   if(j.has("code") && j.optInt("code") !in listOf(0,200))throw Exception(j.optString("msg",j.optString("message","请求失败")))
   j
  }finally{c.disconnect()}
 }
}
class MainActivity:ComponentActivity(){
 var widgetRequest by mutableIntStateOf(0)
 override fun onNewIntent(intent:android.content.Intent){super.onNewIntent(intent);setIntent(intent);if(intent.getStringExtra("meridianDestination")=="home")widgetRequest++}

 override fun onPause(){if(PrivacyMode.enabled&&android.os.Build.VERSION.SDK_INT<33)window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);super.onPause()}
 override fun onResume(){super.onResume();window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);if(android.os.Build.VERSION.SDK_INT>=33)setRecentsScreenshotEnabled(!PrivacyMode.enabled)}
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);if(intent?.getStringExtra("meridianDestination")=="home")widgetRequest++;PrivacyMode.load(this);DeviceData.initialize(this);androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window,false);if(android.os.Build.VERSION.SDK_INT>=29){window.isStatusBarContrastEnforced=false;window.isNavigationBarContrastEnforced=false};window.statusBarColor=android.graphics.Color.TRANSPARENT;window.navigationBarColor=android.graphics.Color.TRANSPARENT;androidx.core.view.WindowCompat.getInsetsController(window,window.decorView).apply{isAppearanceLightStatusBars=true;isAppearanceLightNavigationBars=true};setContent{val vault=remember{Vault(this)};var mode by remember{mutableStateOf(vault.theme())};val appearanceStore=remember{AppearanceStore(this)};var appearance by remember{mutableStateOf(appearanceStore.read())};val colorStore=remember{HomeLayoutStore(this)};var globalTheme by remember{mutableStateOf(colorStore.theme())};MeridianTheme(mode,appearance,globalTheme,{globalTheme=it;colorStore.saveTheme(it)}){Meridian(vault,mode,onTheme={mode=it;vault.saveTheme(it)},appearance=appearance,onAppearance={appearance=it.normalized();appearanceStore.save(appearance)})}}}}

private val blue:Color @Composable get()=accent
@Composable fun Panel(content:@Composable ColumnScope.()->Unit){if(LocalReferenceHome.current){ReferenceCard(content);return};if(LocalControlSettings.current){FlowSettingsGroup{Column(Modifier.padding(horizontal=16.dp,vertical=10.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)};return};Column(Modifier.fillMaxWidth().themeCardShadow(RoundedCornerShape(20.dp)).background(glassWhite,RoundedCornerShape(20.dp)).border(.5.dp,glassEdge.copy(alpha=.65f),RoundedCornerShape(20.dp)).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)}
@Composable fun Meridian(vault:Vault,themeMode:String,onTheme:(String)->Unit,client:Api?=null,appearance:AppearanceOptions=LocalAppearance.current,onAppearance:(AppearanceOptions)->Unit={}){
 val globalColors=LocalGlobalTheme.current;val homeTheme=globalColors.id;var homeEditing by remember{mutableStateOf(false)};var homeDragging by remember{mutableStateOf(false)};var homeResetRequest by remember{mutableIntStateOf(0)};var quickThemeOpen by remember{mutableStateOf(false)};var homeManageOpen by remember{mutableStateOf(false)};var homeLayoutRevision by remember{mutableIntStateOf(0)}
 var nativePage by remember{mutableStateOf("")};var statsReport by remember{mutableStateOf(false)};var statsDays by remember{mutableIntStateOf(30)};var statsRefreshing by remember{mutableStateOf(false)};var statsSynced by remember{mutableLongStateOf(0L)};var statsRangeOpen by remember{mutableStateOf(false)};var statsShareRequest by remember{mutableIntStateOf(0)};var statsMonth by remember{mutableStateOf(java.time.YearMonth.now())};var statsYear by remember{mutableIntStateOf(java.time.LocalDate.now().year)};var statsMode by remember{mutableStateOf("overview")};val useGlass=appearance.glass;var management by remember{mutableStateOf(false)};var health by remember{mutableStateOf(false)};var webRoute by remember{mutableStateOf("")};var webTitle by remember{mutableStateOf("")};var homeDate by remember{mutableStateOf(java.time.LocalDate.now())};val glass=remember{HazeState()};val api=client?:remember{Api()};val scope=rememberCoroutineScope();val saved=remember{vault.read()}
 var address by remember{mutableStateOf(saved.optString("address"))};var user by remember{mutableStateOf(saved.optString("user"))};var password by remember{mutableStateOf(saved.optString("password"))}
 var keep by remember{mutableStateOf(saved.optBoolean("keep"))};var auto by remember{mutableStateOf(saved.optBoolean("auto"))}
 var offlineAllowed by remember{mutableStateOf(false)};var logged by remember{mutableStateOf(false)};var busy by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")};var tab by remember{mutableIntStateOf(0)}
 var assistantOpen by remember{mutableStateOf(false)}
 val assistantOnScreen=assistantOpen
 LaunchedEffect(logged){if(!logged)assistantOpen=false}
 var cars by remember{mutableStateOf(listOf<JSONObject>())};var car by remember{mutableIntStateOf(0)};var data by remember{mutableStateOf<JSONObject?>(null)};var charging by remember{mutableStateOf(false)};var detail by remember{mutableStateOf<JSONObject?>(null)};var detailError by remember{mutableStateOf("")};var filterDays by remember{mutableIntStateOf(0)};var searchDraft by remember{mutableStateOf("")};var keyword by remember{mutableStateOf("")};var refresh by remember{mutableIntStateOf(0)};var page by remember{mutableIntStateOf(1)}
 var tripKind by remember{mutableStateOf("all")};var tripCity by remember{mutableStateOf("")}
 var recordSearchOpen by remember{mutableStateOf(false)};var recordFilterRequest by remember{mutableIntStateOf(0)}
 var recordSort by remember{mutableStateOf("desc")};var chargeKind by remember{mutableStateOf("all")};var recordRange by remember{mutableStateOf<Pair<java.time.LocalDate,java.time.LocalDate>?>(null)}
 var homeCache by remember(cars.getOrNull(car)?.opt("id"),api.base){mutableStateOf(cars.getOrNull(car)?.opt("id")?.let{api.peekRead("/dashboard/get",JSONObject().put("carId",it).put("pageNum",1).put("pageSize",20).put("orderBy","trip_start_time $recordSort").put("sortOrder",recordSort).put("statDate",homeDate.toString()))})}
 val recordSpec=recordReadSpec(cars.getOrNull(car)?.opt("id")?:"",charging,page,recordSort,keyword,tripKind,tripCity,filterDays,recordRange)
 val recordKey=cacheRequestKey(api.base+"|"+api.accountScope,recordSpec.path,recordSpec.body)
 val recordSnapshots=remember(api.base,api.accountScope){mutableStateMapOf<String,JSONObject>()}
 val savedRecords=remember(recordKey){if(logged&&cars.isNotEmpty())api.peekRead(recordSpec.path,recordSpec.body)else null}
 val recordsData=recordSnapshots[recordKey]?:savedRecords
 var completedRecordKey by remember{mutableStateOf("")};var recordIssue by remember{mutableStateOf("")};var recordIssueKey by remember{mutableStateOf("")}
 SideEffect{api.activeCarId=cars.getOrNull(car)?.opt("id")?.toString().orEmpty()}
 val pageScrolls=remember{mutableMapOf<Int,ScrollState>()}
 val activeScroll=pageScrolls.getOrPut(tab){ScrollState(0)}
 val scrolled by remember(activeScroll){derivedStateOf{activeScroll.value>1}}
 val focus=androidx.compose.ui.platform.LocalFocusManager.current;val keyboard=androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
 LaunchedEffect(logged,tab,nativePage,assistantOpen){focus.clearFocus(force=true);keyboard?.hide()}
 BackHandler(enabled=logged&&nativePage.isNotBlank()&&nativePage!="sentry"){nativePage=""}
 val pageMotion=app.meridian.mobile.glass.LocalAppMotion.current
 suspend fun login(){val wasOffline=api.offlineOnly;busy=true;error="";try{val uri=URI(address.trim().trimEnd('/'));require(uri.scheme=="https" && !uri.host.isNullOrBlank() && uri.userInfo==null && uri.query==null && uri.fragment==null){"请输入有效的 HTTPS 服务地址"};api.clear(clearWidget=false);api.base=uri.toString();api.accountScope=user.trim();api.call("/user/login",JSONObject().put("userName",user).put("password",password));val a=api.call("/car/get").optJSONArray("data")?:JSONArray();cars=(0 until a.length()).map{a.getJSONObject(it)};DeviceData.write("cars:"+api.base+"|"+api.accountScope,JSONObject().put("cars",a));offlineAllowed=false;car=0;logged=true;nativePage="";tab=0;health=false;detail=null;management=false;page=1;if(keep)vault.save(JSONObject().put("address",address).put("user",user).put("password",password).put("keep",true).put("auto",auto).toString())else vault.clear()}catch(e:Exception){if(e is CancellationException)throw e;if(wasOffline){api.offlineOnly=true;api.accountScope=user.trim()};offlineAllowed=canUseOfflineCache(e);error=e.message?:"连接失败"}finally{busy=false}}
 LaunchedEffect(Unit){if(auto&&keep&&address.isNotBlank())login()}
 val widgetSignal=(vault.activity as? MainActivity)?.widgetRequest?:0
 val widgetDark=LocalPalette.current.dark
 LaunchedEffect(homeTheme,themeMode,widgetDark){withContext(Dispatchers.IO){runCatching{WidgetSnapshot.update()}}}
 LaunchedEffect(widgetSignal,logged){if(logged&&widgetSignal>0){tab=0;nativePage="";webRoute="";health=false;management=false;detail=null;refresh++}}
 val offlineAction:(()->Unit)?=if(offlineAllowed&&!busy&&saved.optBoolean("keep")&&saved.optString("user")==user&&saved.optString("address")==address){{val previous=DeviceData.read("cars:"+address.trim().trimEnd('/')+"|"+user.trim()).optJSONArray("cars");if(previous!=null&&previous.length()>0){api.base=address.trim().trimEnd('/');api.accountScope=user.trim();api.offlineOnly=true;cars=previous.objects();car=0;logged=true;tab=0;nativePage="";error=""}else error="没有可浏览的本机车辆资料，请先连接服务"}}else null

 LaunchedEffect(logged,tab,car,refresh,homeDate,recordKey){
  if(logged&&cars.isNotEmpty()&&tab in listOf(0,1)){
   busy=true
   if(tab==0){error="";data=homeCache}else{recordIssue="";recordIssueKey=recordKey}
   val path=if(tab==1)recordSpec.path else"/dashboard/get"
   val query=if(tab==1)recordSpec.body else JSONObject().put("carId",cars[car].get("id")).put("pageNum",1).put("pageSize",20).put("orderBy","trip_start_time $recordSort").put("sortOrder",recordSort).put("statDate",homeDate.toString())
   try{val next=api.call(path,query);if(tab==1){recordSnapshots[recordKey]=next;completedRecordKey=recordKey}else{data=next;homeCache=next}}
   catch(e:Exception){if(e is CancellationException)throw e;if(tab==1){recordIssue=e.message?:"行程读取失败";recordIssueKey=recordKey;completedRecordKey=recordKey}else error=e.message?:"读取失败"}
   finally{busy=false}
  }
 }
 LaunchedEffect(logged,tab,car,webRoute,nativePage,homeDate,assistantOpen){if(logged&&!assistantOpen&&tab==0&&webRoute.isBlank()&&nativePage.isBlank()&&cars.isNotEmpty())vault.activity.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED){while(isActive){val chargingNow=data?.optJSONObject("data")?.optJSONObject("overview")?.optInt("chargeState")==2;delay(if(chargingNow)15000L else 60000L);try{val next=api.call("/dashboard/get",JSONObject().put("carId",cars[car].get("id")).put("statDate",homeDate.toString()));data=next;homeCache=next;error=""}catch(e:Exception){if(e is CancellationException)throw e;error=e.message?:"刷新失败，保留上次数据"}}}}
 LaunchedEffect(tab,logged,assistantOpen){if(tab!=0||!logged||assistantOpen){homeEditing=false;homeDragging=false}}
 val sectionToolbar:@Composable (Int)->Unit={tab-> Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){if(tab==3)Box(Modifier.weight(1f)){GlassSelectionMenu(statsMode,listOf("overview" to "用车概况","energy" to "能耗分析","report" to "月度报告","annual" to "年度报告"),{statsMode=it})}else if(tab==1)Box(Modifier.weight(1f)){RecordTypeSelector(charging){charging=it;page=1;keyword="";searchDraft=""}}else Box(Modifier.weight(1f)){val title=cars.getOrNull(car)?.optString("carName","我的爱车")?:"爱车";ToolbarTitleCapsule{Icon(Icons.Outlined.DirectionsCar,null,Modifier.size(24.dp).background(softWash,androidx.compose.foundation.shape.CircleShape).padding(3.dp),tint=accent);Spacer(Modifier.width(6.dp));Text(title,fontSize=17.sp,maxLines=1,overflow=TextOverflow.Ellipsis)}};if(tab==1)ToolbarActionStrip(buildList{if(!charging)add(ToolbarAction(Icons.Outlined.Search,"搜索行程",cars.isNotEmpty(),recordSearchOpen){recordSearchOpen=!recordSearchOpen});add(ToolbarAction(Icons.Outlined.Tune,"筛选记录",cars.isNotEmpty(),filterDays!=0||tripKind!="all"||tripCity.isNotBlank()||chargeKind!="all"){focus.clearFocus();keyboard?.hide();recordFilterRequest++})});if(tab==3)ToolbarActionStrip(buildList{if(statsMode in listOf("report","annual")){add(ToolbarAction(Icons.Outlined.ChevronLeft,"上一周期"){if(statsMode=="report")statsMonth=statsMonth.minusMonths(1)else statsYear--});add(ToolbarAction(Icons.Outlined.ChevronRight,"下一周期",if(statsMode=="report")statsMonth<java.time.YearMonth.now()else statsYear<java.time.LocalDate.now().year){if(statsMode=="report")statsMonth=statsMonth.plusMonths(1)else statsYear++})};if(statsMode !in listOf("report","annual"))add(ToolbarAction(Icons.Outlined.Tune,"统计周期",active=statsDays!=30){statsRangeOpen=true});add(ToolbarAction(Icons.Outlined.IosShare,"分享统计图片",cars.isNotEmpty()){statsShareRequest++})});if(tab==0){ToolbarActionStrip(listOf(ToolbarAction(if(homeEditing)Icons.Outlined.Check else Icons.Outlined.Edit,if(homeEditing)"完成编辑"else"编辑首页",cars.isNotEmpty(),homeEditing){homeEditing=!homeEditing},ToolbarAction(Icons.Outlined.Language,"我的足迹",cars.isNotEmpty()){nativePage="footprint"},ToolbarAction(Icons.Outlined.AccountBalanceWallet,"用车账单",cars.isNotEmpty()){nativePage="ledger"}))};if(cars.size>1)TextButton(onClick={car=(car+1)%cars.size}){Text("切换车辆")}}}
 val controlHaze=remember{HazeState()};val overlays=remember{SceneOverlayHost()}
 CompositionLocalProvider(LocalSceneOverlayHost provides overlays,LocalAppDestination provides {target:String->assistantOpen=false;when(target){"records"->{tab=1;charging=false;filterDays=0;page=1;keyword="";searchDraft="";tripKind="all";tripCity=""};"month"->{statsMode="overview";statsDays=java.time.LocalDate.now().dayOfMonth;tab=3};else->nativePage=target}},LocalChartDateAction provides {selected:java.time.LocalDate->assistantOpen=false;tab=1;charging=false;page=1;keyword="";searchDraft="";filterDays=-1;recordRange=selected to selected;tripKind="all";tripCity="";nativePage=""},LocalControlBackdrop provides controlHaze){
 Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(LocalPalette.current.background[0],LocalPalette.current.background[1],LocalPalette.current.background[2])))){
 Box(Modifier.matchParentSize().hazeSource(controlHaze).hazeSource(glass,zIndex=0f).background(Brush.verticalGradient(LocalPalette.current.background)))
 if(!logged)MeridianLogin(address,{address=it},user,{user=it},password,{password=it},keep,{keep=it;if(!it)auto=false},auto,{auto=it;if(it)keep=true},busy,error,onOffline=offlineAction){scope.launch{login()}}
 else{
 FlowPullRefresh(!assistantOnScreen&&tab in listOf(0,1,3)&&!health&&!homeEditing,activeScroll,if(tab==3)statsRefreshing else busy,{if(!(if(tab==3)statsRefreshing else busy))refresh++},if(tab==3)statsSynced else responseSyncTime(if(tab==1)recordsData else homeCache)){RootTabMotion(tab){displayedTab->
 val activeScroll=pageScrolls.getOrPut(displayedTab){ScrollState(0)}
 val displayedScrolled by remember(activeScroll){derivedStateOf{activeScroll.value>1}}
 Column(Modifier.fillMaxSize().hazeSource(glass,zIndex=1f).verticalScroll(activeScroll,enabled=!homeDragging).padding(horizontal=18.dp).padding(top=if(displayedTab in listOf(0,1,3))72.dp else 10.dp,bottom=(if(homeEditing&&displayedTab==0)70.dp else 0.dp)+navigationBottomSpaceDp(appearance.showNavigationLabels,14f*LocalSystemDensity.current.fontScale,appearance.interfaceScale,appearance.navigationScale,false).dp).navigationBarsPadding().statusBarsPadding(),verticalArrangement=Arrangement.spacedBy(12.dp)){


 if(api.offlineOnly)Panel{Text("离线浏览 · 仅显示本机资料",fontSize=13.sp,color=muted);CinemaTonalButton({scope.launch{login()}}){Text("重新连接服务")}}
 if(displayedTab==1&&recordsData==null&&completedRecordKey!=recordKey)PageSkeleton()else if(displayedTab==0&&busy&&homeCache==null)PageSkeleton()
 if(displayedTab in listOf(0,1))CachedReadStatus(if(displayedTab==1)recordsData else homeCache)
 val visibleIssue=if(displayedTab==1){if(recordIssueKey==recordKey)recordIssue else""}else error
 if(visibleIssue.isNotBlank())Panel{Text(visibleIssue,color=MaterialTheme.colorScheme.error);TextButton(onClick={refresh++}){Text("重新读取")}}

 when(displayedTab){
  0->{if(cars.isEmpty()&&!busy)Panel{SectionTitle("还没有添加车辆",Icons.Outlined.DirectionsCar);Text("添加车辆后查看爱车状态、行程与用车统计。",fontSize=14.sp,lineHeight=22.sp,color=muted);CinemaButton({nativePage="vehicles"},Modifier.fillMaxWidth()){Text("添加车辆")}};run{if(homeCache!=null&&cars.isNotEmpty())CustomLoveCarHome(api,cars[car],homeCache!!.optJSONObject("data")?:JSONObject(),activeScroll,homeEditing,{homeEditing=it},homeTheme,{globalColors.select(it)},{homeDragging=it},onOpen={title,path->when {path=="/app/ledger"->{nativePage="ledger"};path=="/app/sentry"->{nativePage="sentry"};path.startsWith("/app/energy")-> {statsReport=path.contains("view=report");statsMode=if(statsReport)"report"else"overview";tab=3};path.startsWith("/app/trips")-> {tab=1;charging=false;page=1};path=="/app/charge"->{tab=1;charging=true;page=1};else->{webTitle=title;webRoute=path}}},onHealth={health=true},resetRequest=homeResetRequest,layoutRequest=homeLayoutRevision)}}
  3->if(cars.isNotEmpty())Statistics(api,cars[car].get("id"),statsReport,statsMode,statsShareRequest,statsDays,statsMonth,statsYear,refresh,{isBusy,time->statsRefreshing=isBusy;statsSynced=time}){statsShareRequest=0}
  1->{RecordFilters(api,cars.getOrNull(car)?.opt("id"),charging,RecordFilterState(filterDays,recordRange,recordSort,tripKind,tripCity,chargeKind),{refresh++},request=recordFilterRequest,onConsumed={recordFilterRequest=0},showTrigger=false){next->filterDays=next.days;recordRange=next.dates;recordSort=next.order;tripKind=next.tripType;tripCity=next.city;chargeKind=next.chargeType;page=1}
 if(recordSearchOpen&&!charging)Row(Modifier.fillMaxWidth().routeArrival(recordSearchOpen),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(searchDraft,{searchDraft=it},placeholder={Text(if(charging)"搜索充电地点"else"搜索起点或终点",fontSize=13.sp)},singleLine=true,modifier=Modifier.weight(1f),keyboardOptions=androidx.compose.foundation.text.KeyboardOptions(imeAction=androidx.compose.ui.text.input.ImeAction.Search),keyboardActions=androidx.compose.foundation.text.KeyboardActions(onSearch={keyword=searchDraft.trim();page=1}));CinemaIconButton({focus.clearFocus();keyboard?.hide();keyword=searchDraft.trim();page=1}){Icon(Icons.Outlined.ArrowForward,"执行搜索",Modifier.size(19.dp))}};if(keyword.isNotBlank()&&!charging)Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("搜索：$keyword",fontSize=12.sp,color=muted,modifier=Modifier.weight(1f));CinemaTextButton({keyword="";searchDraft="";page=1}){Text("清除",fontSize=12.sp)}};val d=recordsData?.opt("data");if(d is JSONObject)RecordTotals(d,charging);val list=recordRows(recordsData)?:JSONArray();if(completedRecordKey==recordKey&&recordIssue.isBlank()&&recordsData!=null&&list.length()==0)Panel{Text(if(recordRows(recordsData)==null)"未返回有效记录数据"else"暂无${if(charging)"充电" else "行程"}记录")};for(i in 0 until list.length()){
 val item=list.optJSONObject(i)?:continue
 val date=item.safeText(if(charging)"startTime"else"tripStartTime").take(10)
 val previousDate=list.optJSONObject(i-1)?.safeText(if(charging)"startTime"else"tripStartTime")?.take(10)
 if(i==0||date!=previousDate)Row(Modifier.fillMaxWidth().padding(top=6.dp,bottom=2.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(7.dp)){Box(Modifier.size(6.dp).background(accent,CircleShape));Text(date,fontSize=12.sp,color=muted,fontWeight=FontWeight.SemiBold)}
 TripTimelineCard(item,charging){scope.launch{detailError="";try{val key=if(charging)"recordId"else"tripId";val id=item.opt(key)?:item.opt("id")?:throw Exception("记录缺少编号");detail=api.call(if(charging)"/car/charge/detail"else"/car/trip/getDetail",JSONObject().put(key,id)).optJSONObject("data")}catch(e:Exception){if(e is CancellationException)throw e;detailError=e.message?:"详情读取失败"}}}
};Row{TextButton(onClick={if(page>1)page--},enabled=page>1){Text("上一页")};Text("第 $page 页",Modifier.padding(12.dp));TextButton(onClick={page++},enabled=list.length()==20){Text("下一页")}}}
  2->{}
  4->{if(management&&cars.isNotEmpty())Management(api,cars[car],onClose={management=false}) else {
 Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("设置",fontSize=24.sp,fontWeight=FontWeight.Medium,modifier=Modifier.weight(1f));Box(Modifier.width(54.dp)){ProfileHeader(user,api.base)}}
 FlowSettingsGroup{Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
 Image(painterResource(R.drawable.car_su7_yahui_reference),"我的爱车",Modifier.width(100.dp).height(60.dp),contentScale=androidx.compose.ui.layout.ContentScale.Fit)
 Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)){Text(cars.getOrNull(car)?.safeText("carName","我的爱车")?:"我的爱车",fontSize=17.sp,fontWeight=FontWeight.Medium);Text("2026 新一代 SU7 · 雅灰",fontSize=12.sp,color=muted)}
 }}
 SettingsDirectory(cars.isNotEmpty()){nativePage=it}

 TextButton(onClick={api.clear();logged=false;data=null;homeCache=null;cars=emptyList();car=0;password="";auto=false;vault.clear()},modifier=Modifier.fillMaxWidth()){Text("退出登录")};Text("Meridian "+BuildConfig.VERSION_NAME,fontSize=11.sp,color=muted,modifier=Modifier.align(Alignment.CenterHorizontally))
 }}
 }
 }
 if(displayedTab in listOf(0,1,3))PinnedSectionToolbar(glass,Modifier.align(Alignment.TopCenter),displayedScrolled){sectionToolbar(displayedTab)}
 }
 if(detailError.isNotBlank())Text(detailError,color=MaterialTheme.colorScheme.error)
 BackHandler(enabled=nativePage.isBlank()&&(webRoute.isNotBlank()||detail!=null||health||management||tab!=0)){if(nativePage.isNotBlank())nativePage="" else if(webRoute.isNotBlank())webRoute="" else if(detail!=null)detail=null else if(health)health=false else if(management)management=false else tab=0}
 if(detail!=null&&!charging)TripDetailPage(api,detail!!){detail=null}
 if(detail!=null&&charging)ChargeDetailPage(detail!!,api,{refresh++}){detail=null}
 }
 if(statsRangeOpen&&tab==3)FlowDetailPanel({statsRangeOpen=false},title={Text("统计周期")},body={Column(verticalArrangement=Arrangement.spacedBy(10.dp)){listOf(7,30,90).forEach{days->CinemaChoice(statsDays==days,{statsDays=days;statsRangeOpen=false},{Text("近 $days 天")},Modifier.fillMaxWidth())}}},confirm={CinemaTextButton({statsRangeOpen=false}){Text("完成")}})
 if(tab!=2&&tab!=0&&tab!=1&&tab!=3&&!health&&webRoute.isBlank()&&nativePage.isBlank()&&detail==null)StatusBarGlass(glass,Modifier.align(Alignment.TopCenter))
 if(logged)MeridianNavigationDock(tab,glass,useGlass,{if(it==2){assistantOpen=true}else{tab=it;page=1}},Modifier.align(Alignment.BottomCenter),onReselect={scope.launch{if(pageMotion.full)activeScroll.animateScrollTo(0,tween(pageMotion.duration(400),easing=androidx.compose.animation.core.FastOutSlowInEasing))else activeScroll.scrollTo(0)}})
 if(assistantOpen)AssistantScene(onClosed={assistantOpen=false}){close->if(cars.isNotEmpty())Assistant(api,cars[car].get("id"),account=user,onBack={focus.clearFocus(force=true);keyboard?.hide();close()})}
 if(logged&&tab==0&&homeEditing&&nativePage.isBlank()&&webRoute.isBlank()&&!health&&detail==null){
  val dockSpace=navigationBottomSpaceDp(appearance.showNavigationLabels,14f*LocalSystemDensity.current.fontScale,appearance.interfaceScale,appearance.navigationScale,false)
  CompositionLocalProvider(LocalControlBackdrop provides glass){HomeEditorToolbar({homeResetRequest++},{quickThemeOpen=true},{homeEditing=false},Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal=18.dp).padding(bottom=(dockSpace-24f*navigationDensityScale(appearance.interfaceScale,appearance.navigationScale)/normalizedInterfaceScale(appearance.interfaceScale)).dp),onManage={homeManageOpen=true})}
 }
 if(homeManageOpen&&cars.isNotEmpty())HomeCardManager(api.base,cars[car].opt("id")?.toString().orEmpty(),{homeLayoutRevision++},{homeManageOpen=false},dashboard=homeCache?.optJSONObject("data"),car=cars[car])
 if(quickThemeOpen)FlowDetailPanel(onDismiss={quickThemeOpen=false},title={Text("切换主题")},body={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){ThemePresetGrid(homeTheme){globalColors.select(it)}}},confirm={CinemaButton({quickThemeOpen=false}){Text("完成")}})
 if(logged&&(nativePage.isNotBlank()||webRoute.isNotBlank()||health)){
  Box(Modifier.fillMaxSize().clickable(interactionSource=remember{androidx.compose.foundation.interaction.MutableInteractionSource()},indication=null){}){
 if(nativePage=="widget")WidgetSettingsPage{nativePage=""} else if(nativePage=="localArchive"&&cars.isNotEmpty())LocalArchivePage(api,cars[car].get("id")){nativePage=""} else if(nativePage=="ledger"&&cars.isNotEmpty())CostLedger(api,cars[car].get("id")){nativePage=""} else if(nativePage=="library"&&cars.isNotEmpty())TripLibrary(api,cars[car].get("id")){nativePage=""} else if(nativePage=="routes"&&cars.isNotEmpty())RouteComparisonPage(api,cars[car].get("id")){nativePage=""} else if(nativePage=="diagnostics")DeviceDiagnostics(api){nativePage=""} else if(nativePage=="inbox"&&cars.isNotEmpty())InboxPage(api,cars[car].get("id"),{nativePage=""}){target->nativePage="";when{target=="health"->health=true;target.startsWith("report:")->{statsMonth=runCatching{java.time.YearMonth.parse(target.removePrefix("report:"),java.time.format.DateTimeFormatter.ofPattern("yyyyMM"))}.getOrDefault(java.time.YearMonth.now());statsMode="report";tab=3};target=="charge"->{tab=1;charging=true;page=1;filterDays=0;chargeKind="all"};else->nativePage="diagnostics"}} else if(nativePage=="vehicles")VehicleManagementPage(api,cars,{next->cars=next;car=car.coerceAtMost((next.size-1).coerceAtLeast(0));refresh++}){nativePage=""} else if(nativePage in listOf("account","notifications","backup"))AccountServicesPage(api,user,{nativePage="";api.clear();logged=false;data=null;homeCache=null;cars=emptyList();car=0;password="";auto=false;vault.clear()},initialSection=if(nativePage=="account")""else nativePage){nativePage=""} else if(health&&cars.isNotEmpty())Health(api,cars[car].get("id"),onClose={health=false}) else if(nativePage=="about")AboutPage{nativePage=""} else if(nativePage=="appearance")AppearanceScreen(appearance,themeMode,onTheme,onAppearance){nativePage=""} else if(nativePage=="footprint"&&cars.isNotEmpty())FootprintPage(api,cars[car].get("id")){nativePage=""} else if(nativePage=="sentry"&&cars.isNotEmpty())SentryPage(api,cars[car].get("id")){nativePage=""} else if(webRoute.isNotBlank())ServicePage(api,webRoute,webTitle,onClose={webRoute=""})
  }
 }
 SceneOverlayLayer(overlays)


 }
 }
}
}
@Composable fun Metric(label:String,value:String,unit:String=""){Column(Modifier.padding(8.dp)){Text(label,color=muted,fontSize=12.sp);Text(value,fontSize=32.sp,fontWeight=FontWeight.SemiBold,color=ink);if(unit.isNotBlank())Text(unit,fontSize=12.sp,color=blue)}}
@Composable fun Dashboard(d:JSONObject,statistics:Boolean=false){
 val o=d.optJSONObject("overview")?:JSONObject();val status=when{ o.safeText("carMode").isNotBlank()&&o.safeText("carMode") !in listOf("正常模式","不可用")->o.safeText("carMode");o.optDouble("speed",0.0)>0->displayValue("",o.get("speed"))+" km/h";o.optInt("chargeState",0) in listOf(1,2)->"充电中";o.optInt("chargeState",0)==3->"充电完成";else->"静止中"}
 Image(painterResource(R.drawable.car_hero),"车辆展示图",Modifier.fillMaxWidth().height(150.dp))
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){HeroNumber(o.opt("batteryLevel"),"%","剩余电量");Box(Modifier.height(42.dp).width(1.dp).background(Color.White));HeroNumber(o.opt("remainingRange"),"km","剩余续航")}
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text(o.safeText("carLocation","位置暂无数据"),fontSize=11.sp,color=muted,modifier=Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis);Box(Modifier.background(Color(0xFFDCEBFF),RoundedCornerShape(12.dp)).padding(horizontal=10.dp,vertical=5.dp)){Text(status,fontSize=11.sp,color=accent)}}
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){StatusTile("车辆状态",status,Icons.Outlined.DirectionsCar,Modifier.weight(1f));StatusTile("哨兵模式",o.safeText("sentrySwitch","—"),Icons.Outlined.Security,Modifier.weight(1f));StatusTile("车内温度",o.safeText("insideTemp","—")+if(o.has("insideTemp"))"°C"else"",Icons.Outlined.Thermostat,Modifier.weight(1f))}
 if(o.optInt("chargeState",0)==2)Panel{SectionTitle("正在补能",Icons.Outlined.EvStation);Row(Modifier.fillMaxWidth()){InlineMetric("当前功率",o.safeText("currentPowerKw","—")+" kW",Icons.Outlined.Bolt,Modifier.weight(1f));InlineMetric("充电速度",o.safeText("chargeSpeedKmh","—")+" km/h",Icons.Outlined.Speed,Modifier.weight(1f))};Row(Modifier.fillMaxWidth()){InlineMetric("电压",o.safeText("electricVoltageV","—")+" V",Icons.Outlined.ElectricalServices,Modifier.weight(1f));InlineMetric("电流",o.safeText("electricCurrentA","—")+" A",Icons.Outlined.BatteryChargingFull,Modifier.weight(1f))}}
 val brief=d.optJSONArray("dailyBrief");if(brief!=null&&brief.length()>0)Panel{SectionTitle("今日用车",Icons.Outlined.Today);for(chunk in (0 until brief.length()).toList().chunked(2)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){for(index in chunk){val item=brief.optJSONObject(index)?:continue;val title=item.safeText("title");val raw=item.opt("value");val value=if(title.contains("驾驶")&&raw is Number)durationText(raw.toLong()) else if(raw==null||raw==JSONObject.NULL)"—"else displayValue("",raw);InlineMetric(title,value,metricIcon(title),Modifier.weight(1f))}}}}
 val recent=o.optJSONObject("latestTrip")?:d.optJSONObject("latestTrip");if(recent!=null)Panel{SectionTitle("最近行程",Icons.Outlined.Route);Text(recent.safeText("startLocation",recent.safeText("startLocationName","出发地"))+" → "+recent.safeText("endLocation",recent.safeText("endLocationName","目的地")),fontSize=13.sp,fontWeight=FontWeight.Medium,color=ink,maxLines=2,overflow=TextOverflow.Ellipsis);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(prettyTime(recent.safeText("tripStartTime")),fontSize=11.sp,color=muted);Text(recent.safeText("mileageKm","—")+" km",fontSize=15.sp,fontWeight=FontWeight.SemiBold,color=ink)}}
 val charge=o.optJSONObject("latestCharge")?:d.optJSONObject("latestCharge");if(charge!=null)Panel{SectionTitle("最近充电",Icons.Outlined.EvStation);Readable(charge)}
 val analysis=d.optJSONObject("analysisStats");if(analysis!=null){val ad=analysis.optDouble("adMileage",Double.NaN);val human=analysis.optDouble("humanMileage",Double.NaN);if(ad.isFinite()&&human.isFinite()&&ad+human>0)DonutCard("驾驶方式",listOf("辅助驾驶" to ad,"人工驾驶" to human),"km")}
 val trends=d.optJSONObject("drivingTrend");trends?.optJSONArray("mileageStats")?.let{TrendChart("近30天里程","km",it,"mileage",.001,"date")}
 Panel{SectionTitle("近 30 天",Icons.Outlined.Insights);Row{InlineMetric("行驶里程",o.safeText("recent30Mileage","—")+" km",Icons.Outlined.Route,Modifier.weight(1f));InlineMetric("驾驶时长",if(o.has("recent30DrivingDurationMs"))durationText(o.optLong("recent30DrivingDurationMs"))else"—",Icons.Outlined.Schedule,Modifier.weight(1f))}}
}
@Composable fun SelectionContainerCompat(text:String){androidx.compose.foundation.text.selection.SelectionContainer{Text(text.ifBlank{"…"},lineHeight=22.sp)}}
private val labels=mapOf("sentrySwitch" to "哨兵模式","gearPosition" to "挡位","carMode" to "车辆模式","lowEnergyPercent" to "低能耗占比 (%)","midEnergyPercent" to "中能耗占比 (%)","highEnergyPercent" to "高能耗占比 (%)","daytimePercent" to "白天驾驶占比 (%)","nightPercent" to "夜间驾驶占比 (%)","avgDailyDrivingHours" to "日均驾驶时长 (小时)","currentPeriodEnergy" to "本周期能耗","previousPeriodEnergy" to "上周期能耗","totalPowerConsumption" to "累计耗电 (kWh)","idlePowerConsumption" to "怠速耗电 (kWh)","consumedRange" to "消耗预估续航 (km)","consumptionAchievementRate" to "续航达成率 (%)","chargeRange" to "补充续航 (km)","startRange" to "开始续航 (km)","endRange" to "结束续航 (km)","batteryHealthPercent" to "估算电池健康 (%)","avgChargeSpeedKmh" to "平均补能速度 (km/h)","avgElectricVoltageV" to "平均电压 (V)","avgElectricCurrentA" to "平均电流 (A)","peakElectricVoltageV" to "峰值电压 (V)","peakElectricCurrentA" to "峰值电流 (A)","mileageSinceLastChargeKm" to "上次充电后行驶 (km)","currentPowerKw" to "当前功率 (kW)","chargeSpeedKmh" to "补能速度 (km/h)","electricVoltageV" to "电压 (V)","electricCurrentA" to "电流 (A)","insideTemp" to "车内温度 (°C)","outsideTemp" to "车外温度 (°C)","vehicleStatusText" to "车辆状态","carLocation" to "车辆位置","speed" to "速度 (km/h)","anomalyEvents" to "异常事件","chargeEvents" to "充电事件","drivingEvents" to "行驶事件","parkingEvents" to "驻车事件","storySummary" to "用车回顾","monthMileage" to "本月里程 (km)","totalEnergyConsumption" to "总耗电 (kWh)","maxTripMileage" to "最长行程 (km)","parkingCnt" to "泊车次数","hnoaMileage" to "高速领航里程 (km)","cnoaMileage" to "城市领航里程 (km)","basicPilotMileage" to "基础辅助里程 (km)","tripEnergyConsumption" to "百公里能耗 (kWh/100km)","avgSpeed" to "平均速度 (km/h)","maxSpeed" to "最高速度 (km/h)","startRemainingRange" to "出发续航 (km)","endRemainingRange" to "到达续航 (km)","drivingMileage" to "行驶里程 (km)","drivingDuration" to "驾驶时长 (分钟)","startLocation" to "出发地点","endLocation" to "到达地点","startBattery" to "起始电量 (%)","endBattery" to "结束电量 (%)","chargeEnergy" to "充入电量 (kWh)","durationMinutes" to "时长 (分钟)","locationName" to "地点","recent30Mileage" to "近30天里程 (km)","totalTrips" to "行程次数","adMileage" to "辅助驾驶里程 (km)","humanMileage" to "人工驾驶里程 (km)","peakPowerKw" to "峰值功率 (kW)","avgPowerKw" to "平均功率 (kW)","chargeType" to "充电类型","batteryLevel" to "剩余电量 (%)","remainingRange" to "剩余续航 (km)","carName" to "车辆名称","carModel" to "车型","startTime" to "开始时间","endTime" to "结束时间","tripStartTime" to "出发时间","tripEndTime" to "到达时间","startAddress" to "出发地点","endAddress" to "到达地点","startLocationName" to "出发地点","endLocationName" to "到达地点","observedAt" to "数据更新时间","totalTrips" to "行程次数","tripCount" to "行程次数","charging" to "正在充电","sentryModeEnabled" to "哨兵模式","chargeStartTime" to "充电开始","chargeEndTime" to "充电结束","startSoc" to "开始电量 (%)","endSoc" to "结束电量 (%)")
fun displayValue(key:String,value:Any):String {if(value==JSONObject.NULL||value.toString().trim().lowercase() in listOf("null","undefined"))return "—";if(value is Boolean)return if(value)"是" else "否";if(value is Number){val raw=value.toDouble();if(!raw.isFinite())return "—";val v=when(key){"drivingMileage"->raw/1000;"drivingDuration"->raw/60000;else->raw};return if(v==kotlin.math.floor(v))v.toLong().toString() else String.format(java.util.Locale.getDefault(),"%.1f",v)};return value.toString().ifBlank{"—"}}
@Composable fun Readable(j:JSONObject,depth:Int=0){
 var shown=false
 for((key,label) in labels){val v=j.opt(key);if(v!=null&&v!=JSONObject.NULL&&v !is JSONObject&&v !is JSONArray){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(label,color=muted,modifier=Modifier.weight(1f));Text(if(addressField(key))privateAddress(displayValue(key,v))else displayValue(key,v),modifier=Modifier.weight(1f))};shown=true}}
 if(depth<3)for(key in j.keys()){val nested=j.optJSONObject(key);if(nested!=null){Readable(nested,depth+1);shown=true}}
 if(!shown&&depth==0)Text("暂无可展示的详细指标",color=muted)
}

@Composable fun Statistics(api:Api,carId:Any,initialReport:Boolean=false,mode:String=if(initialReport)"report"else"overview",shareRequest:Int=0,days:Int=30,month:java.time.YearMonth=java.time.YearMonth.now(),year:Int=java.time.LocalDate.now().year,refreshRequest:Int=0,onRefreshState:(Boolean,Long)->Unit={_,_->},onShareConsumed:()->Unit={}){
 if(mode=="report"){MonthlyReport(api,carId,shareRequest,onShareConsumed,month,refreshRequest,onRefreshState);return}
 if(mode=="annual"){AnnualReport(api,carId,year,shareRequest,onShareConsumed,refreshRequest,onRefreshState);return}
 val end=java.time.LocalDate.now()
 val request=remember(carId,days,end){JSONObject().put("carId",carId).put("startDate",end.minusDays(days.toLong()-1).toString()).put("endDate",end.toString())}
 val trendRequest=remember(carId,days,end){JSONObject().put("carId",carId).put("days",days).put("statDate",end.toString())}
 var overview by remember(api.base,api.accountScope,carId,days,end){mutableStateOf(api.memoryRead("/statistics/energy-overview",request)?.optJSONObject("data"))}
 var trend by remember(api.base,api.accountScope,carId,days,end){mutableStateOf(api.memoryRead("/dashboard/getTrendData",trendRequest)?.optJSONObject("data"))}
 var loading by remember{mutableStateOf(false)};var issue by remember{mutableStateOf("")};var retry by remember{mutableIntStateOf(0)};val initialRefresh=remember{refreshRequest}
 LaunchedEffect(api.base,api.accountScope,carId,days,end,retry,refreshRequest){
  if(retry==0&&refreshRequest==initialRefresh&&freshPageRead(overview)&&freshPageRead(trend))return@LaunchedEffect
  loading=true;issue=""
  try{
   if(overview==null){val cached=withContext(Dispatchers.IO){api.peekRead("/statistics/energy-overview",request)?.optJSONObject("data") to api.peekRead("/dashboard/getTrendData",trendRequest)?.optJSONObject("data")};overview=cached.first;trend=cached.second}
   supervisorScope{
    val nextOverview=async{api.call("/statistics/energy-overview",request).optJSONObject("data")}
    val nextTrend=async{runCatching{api.call("/dashboard/getTrendData",trendRequest).optJSONObject("data")}.getOrElse{if(it is CancellationException)throw it;issue="统计已更新，趋势暂不可用";trend}}
    val result=nextOverview.await();val chart=nextTrend.await();overview=result;trend=chart
   }
  }catch(e:Exception){if(e is CancellationException)throw e;issue=e.message?:"统计暂不可用"}finally{loading=false}
 }
 SideEffect{onRefreshState(loading,responseSyncTime(overview))}
 CachedReadStatus(overview)
 Text("最近 $days 天",fontSize=12.sp,color=muted)
 if(overview==null&&issue.isBlank())PageSkeleton()
 if(issue.isNotBlank())Panel{Text(issue,color=MaterialTheme.colorScheme.error);TextButton(onClick={retry++}){Text("重试")}}
 val root=overview
 if(root!=null)StatisticsShare(root,trend,days,mode,shareRequest,onShareConsumed)
 if(mode=="energy"){if(root!=null)EnergyAnalysis(root,trend,api,carId,days);return}
 if(root!=null)remember(root){statisticsMetricGroups(root).filter{it.key !in listOf("energy","efficiency")}}.forEach{group->Panel{SectionTitle(group.title,when(group.key){"overview"->Icons.Outlined.Dashboard;"trip"->Icons.Outlined.Route;"charge"->Icons.Outlined.EvStation;else->metricIcon(group.title)});Text(if(group.scope=="lifetime")"累计表现"else"最近 $days 天",fontSize=11.sp,color=muted);StatisticsCards(group.items,trend,days,group.scope)}}

 val ad=root?.optJSONObject("adData")?.optJSONObject("summary");if(ad!=null)Panel{SectionTitle("辅助驾驶",Icons.Outlined.AutoMode);val pairs=listOf("adMileage" to "辅助驾驶里程 (km)","hnoaMileage" to "高速领航 (km)","cnoaMileage" to "城市领航 (km)","basicPilotMileage" to "基础辅助 (km)","apaCnt" to "自动泊车 (次)","avpCnt" to "代客泊车 (次)");for((key,label) in pairs){if(ad.has(key)&&!ad.isNull(key))Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(label,fontSize=12.sp,color=muted);Text(displayValue(key,ad.get(key)),fontSize=14.sp,fontWeight=FontWeight.SemiBold,color=ink)}}}
 val serverCharts=root?.optJSONObject("periodDashboard")?.optJSONArray("charts")?:JSONArray()
 for(i in 0 until serverCharts.length()){val chart=serverCharts.optJSONObject(i)?:continue;val points=chart.optJSONArray("points")?:continue;val key=chart.safeText("key");if(key=="drivingTime"){val values=(0 until points.length()).mapNotNull{n->val p=points.optJSONObject(n);val v=p?.optDouble("value",Double.NaN)?:Double.NaN;if(p!=null&&v.isFinite()&&v>=0)p.safeText("label") to v else null};if(values.sumOf{it.second}>0)DonutCard(chart.safeText("title",if(key=="drivingTime")"驾驶时段"else"能耗分布"),values,"%")}}
 val t=trend?.optJSONObject("drivingTrend")?:trend
 t?.optJSONArray("mileageStats")?.let{TrendChart("里程趋势","km",it,"mileage",.001,"date")}
 if(root!=null&&!loading)Text("统计范围：最近 $days 天；缺失数据保留为空。",fontSize=12.sp,color=muted)
}
@Composable fun TrendChart(title:String,unit:String,rows:JSONArray,key:String,scale:Double,dateKey:String){InteractiveTrend(title,unit,chartSamples(rows,key,scale,dateKey))}

@Composable fun Management(api:Api,car:JSONObject,onClose:()->Unit){
 val scope=rememberCoroutineScope();var running by remember{mutableStateOf(false)};var message by remember{mutableStateOf("")};var progress by remember{mutableStateOf<JSONObject?>(null)};var confirm by remember{mutableStateOf<Pair<String,String>?>(null)}
 TextButton(onClick=onClose){Text("返回我的")}
 Panel{Text(car.optString("carName","我的车辆"),fontSize=22.sp,fontWeight=FontWeight.SemiBold);Text(car.optString("carModel",""),color=blue);Readable(car)}
 Panel{Text("数据同步",fontSize=18.sp,fontWeight=FontWeight.SemiBold);Text("从车辆服务补齐记录。退出本页面不会取消服务端已提交的任务。",color=muted,fontSize=13.sp)
 for((path,title) in listOf("/car/syncDayTripData" to "同步近期行程","/car/syncTripMonthStatsData" to "同步月度报告","/car/syncAllTripData" to "同步全部历史行程")){OutlinedButton(enabled=!running,onClick={confirm=path to title},modifier=Modifier.fillMaxWidth()){Text(title)}}}
 if(confirm!=null)AlertDialog(onDismissRequest={confirm=null},title={Text(confirm!!.second)},text={Text("将为当前车辆提交同步任务。历史同步可能需要较长时间。")},confirmButton={TextButton(onClick={val path=confirm!!.first;confirm=null;scope.launch{running=true;message="正在提交…";progress=null;try{val response=api.call(path,JSONObject().put("id",car.get("id")));val d=response.opt("data");val id=when(d){is JSONObject->d.optString("jobId");is String->d;else->""};message=response.optString("msg","同步请求已提交");if(id.isNotBlank()){message="任务已提交，正在读取进度";var finished=false;repeat(120){if(!finished){delay(2000);val next=api.call("/car/syncProgress?jobId="+URLEncoder.encode(id,"UTF-8")).optJSONObject("data");progress=next;val state=next?.optString("statusName")?:"";if(state in listOf("DONE","FAIL","CANCEL")){message=next?.optString("message")?.takeIf{it.isNotBlank()}?:when(state){"DONE"->"同步完成";"FAIL"->"同步失败";else->"任务已取消"};finished=true}}};if(!finished)message="任务可能仍在后台运行，请稍后到服务端查看"}}catch(e:Exception){if(e is CancellationException)throw e;message=e.message?:"同步请求失败"}finally{running=false}}}){Text("开始同步")}},dismissButton={TextButton(onClick={confirm=null}){Text("取消")}})
 if(message.isNotBlank())Panel{Text(message);if(running&&progress==null)FlowLoading(Modifier.fillMaxWidth());progress?.let{j->val percentage=j.optDouble("percent",0.0).coerceIn(0.0,100.0);FlowProgress(progress={(percentage/100).toFloat()},modifier=Modifier.fillMaxWidth());Text("${percentage.toInt()}% · ${j.optInt("completed")}/${j.optInt("total")}");Text(j.optString("currentStep"),color=muted)}}
}

@Composable fun MonthlyReport(api:Api,carId:Any,shareRequest:Int=0,onShareConsumed:()->Unit={},month:java.time.YearMonth=java.time.YearMonth.now(),refreshRequest:Int=0,onRefreshState:(Boolean,Long)->Unit={_,_->}){
 val request=remember(carId,month){JSONObject().put("carId",carId).put("month",month.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM")))}
 var data by remember(api.base,api.accountScope,carId,month){mutableStateOf(api.memoryRead("/trip-month-stats/get",request)?.optJSONObject("data"))};var loading by remember{mutableStateOf(false)};var issue by remember{mutableStateOf("")};var reload by remember{mutableIntStateOf(0)};val initialRefresh=remember{refreshRequest}
 LaunchedEffect(api.base,api.accountScope,carId,month,reload,refreshRequest){
  if(reload==0&&refreshRequest==initialRefresh&&freshPageRead(data))return@LaunchedEffect
  issue="";loading=true
  try{if(data==null)data=withContext(Dispatchers.IO){api.peekRead("/trip-month-stats/get",request)?.optJSONObject("data")};data=api.call("/trip-month-stats/get",request).optJSONObject("data")}catch(e:Exception){if(e is CancellationException)throw e;issue=e.message?:"月报读取失败"}finally{loading=false}
 }
 SideEffect{onRefreshState(loading,responseSyncTime(data))}
 CachedReadStatus(data)
 Text(month.toString()+" · 月度报告",fontSize=13.sp,color=muted)
 if(data==null&&loading)PageSkeleton()
 if(issue.isNotBlank())Panel{Text(issue);TextButton(onClick={reload++}){Text("重试")}}
 val d=data;if(d==null&&!loading&&issue.isBlank())Panel{Text("该月暂无报告");Text("可到车辆与数据同步中补同步月报。",color=muted)}
 if(d!=null){MonthlyShare(d,month.toString(),shareRequest,onShareConsumed);MonthlyReportContent(d)}
}
@Composable fun ChatBubbleContent(role:String,text:String){val own=role=="user";Row(Modifier.fillMaxWidth(),horizontalArrangement=if(own)Arrangement.End else Arrangement.Start){Column(Modifier.fillMaxWidth(if(own).85f else .96f).clip(RoundedCornerShape(22.dp)).background(if(own)Brush.linearGradient(listOf(accent.copy(alpha=.82f),accent))else Brush.linearGradient(listOf(glassWhite.copy(alpha=.87f),glassWhite.copy(alpha=.6f)))).border(.6.dp,glassEdge,RoundedCornerShape(22.dp)).padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){if(!own)Text("Meridian",fontSize=11.sp,color=blue);CompositionLocalProvider(LocalContentColor provides if(own){themeOnAccent(accent)}else ink){if(own)SelectionContainerCompat(privateProse(text))else MarkdownMessage(text)}}}}
@Composable fun RecordPreview(j:JSONObject,charge:Boolean){
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(prettyTime(j.safeText(if(charge)"startTime"else"tripStartTime")),fontSize=11.sp,color=muted);Icon(if(charge)Icons.Outlined.EvStation else Icons.Outlined.ChevronRight,null,tint=blue,modifier=Modifier.size(17.dp))}
 if(charge){Text(j.safeText("locationName","充电记录"),fontSize=14.sp,fontWeight=FontWeight.Medium);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){MiniMetric("充入电量",j.opt("chargeEnergy"),"kWh");MiniMetric("时长",j.opt("durationMinutes"),"分钟");MiniMetric("结束电量",j.opt("endBattery"),"%")}}
 else{RoutePoint(j.safeText("startLocation","出发地"),accent);RoutePoint(j.safeText("endLocation","目的地"),Color(0xFF9B7CF9));HorizontalDivider(color=Color(0xFFECF0F7));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){MiniMetric("里程",(j.opt("drivingMileage") as? Number)?.toDouble()?.div(1000),"km");MiniMetric("时长",(j.opt("drivingDuration") as? Number)?.toDouble()?.div(60000),"分钟");if(j.has("tripEnergyConsumption")&&!j.isNull("tripEnergyConsumption"))MiniMetric("能耗",j.opt("tripEnergyConsumption"),"kWh/100km")else MiniMetric("均速",j.opt("avgSpeed")?:j.opt("avgSpeedKmh"),"km/h")}}
}
@Composable fun MiniMetric(label:String,value:Any?,unit:String){Column(verticalArrangement=Arrangement.spacedBy(4.dp)){Text(if(value==null||value==JSONObject.NULL)"—"else displayValue("",value),fontSize=17.sp,fontWeight=FontWeight.SemiBold,color=ink);Text("$label · $unit",fontSize=10.sp,color=muted)}}

@Composable fun SettingsEntry(icon:androidx.compose.ui.graphics.vector.ImageVector,title:String,subtitle:String,onClick:()->Unit){
 Row(Modifier.fillMaxWidth().heightIn(min=60.dp).clip(RoundedCornerShape(14.dp)).clickable(onClick=onClick).padding(vertical=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
  Box(Modifier.size(32.dp),contentAlignment=Alignment.Center){Icon(icon,null,tint=ink,modifier=Modifier.size(20.dp))}
  Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)){Text(title,fontSize=15.sp,fontWeight=FontWeight.Medium,color=ink);Text(subtitle,fontSize=12.sp,lineHeight=18.sp,color=muted)}
  Icon(Icons.Outlined.ChevronRight,null,tint=muted.copy(alpha=.65f),modifier=Modifier.size(16.dp))
 }
}

@Composable fun MetricGrid(items:JSONArray){
 for(chunk in (0 until items.length()).toList().chunked(2)){
  Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max),horizontalArrangement=Arrangement.spacedBy(8.dp)){
   for(index in chunk){val item=items.optJSONObject(index)?:continue;val label=item.safeText("label");val raw=item.opt("value");val value=if(raw==null||raw==JSONObject.NULL)"—"else displayValue("",raw).replace("小时","时").replace("分钟","分")
    MetricDataCard(label,value,item.safeText("unit"),metricIcon(label),Modifier.weight(1f).fillMaxHeight())
   }
   if(chunk.size==1)Spacer(Modifier.weight(1f))
  }
 }
}
