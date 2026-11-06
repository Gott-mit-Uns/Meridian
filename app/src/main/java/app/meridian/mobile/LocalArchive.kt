package app.meridian.mobile
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.*
import org.json.*
import kotlinx.coroutines.*
import java.util.Base64
import javax.crypto.*
import javax.crypto.spec.*
import java.security.SecureRandom

internal fun archiveEncrypt(data:JSONObject,password:String):ByteArray{
 require(password.length>=8){"备份密码至少8位"};val payload=data.toString().toByteArray(Charsets.UTF_8);require(payload.size<=7*1024*1024){"本机资料过大，暂不支持导出"};val salt=ByteArray(16).also{SecureRandom().nextBytes(it)};val spec=PBEKeySpec(password.toCharArray(),salt,120000,256);val key=SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded,"AES");spec.clearPassword();val cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key);val b64=Base64.getEncoder();return JSONObject().put("format","meridian-local").put("version",1).put("salt",b64.encodeToString(salt)).put("iv",b64.encodeToString(cipher.iv)).put("data",b64.encodeToString(cipher.doFinal(payload))).toString().toByteArray(Charsets.UTF_8)
}
internal fun archiveDecrypt(bytes:ByteArray,password:String):JSONObject{
 require(bytes.size<=10*1024*1024){"备份文件过大"};val envelope=JSONObject(String(bytes,Charsets.UTF_8));require(envelope.optString("format")=="meridian-local"&&envelope.optInt("version")==1){"不支持的备份格式"};val decode=Base64.getDecoder();val salt=decode.decode(envelope.getString("salt"));val iv=decode.decode(envelope.getString("iv"));require(salt.size==16&&iv.size==12){"备份格式无效"};val spec=PBEKeySpec(password.toCharArray(),salt,120000,256);val key=SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded,"AES");spec.clearPassword();val cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key,GCMParameterSpec(128,iv));return JSONObject(String(cipher.doFinal(decode.decode(envelope.getString("data"))),Charsets.UTF_8))
}
@Composable internal fun LocalArchivePage(api:Api,carId:Any,onClose:()->Unit){
 val context=LocalContext.current;val scope=api.localScope(carId);val action=rememberServerAction();var password by remember{mutableStateOf("")};var confirm by remember{mutableStateOf("")};var exportBytes by remember{mutableStateOf<ByteArray?>(null)};var incoming by remember{mutableStateOf<JSONObject?>(null)};var overwrite by remember{mutableStateOf(false)}
 val create=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")){uri->if(uri!=null)action.run("本机资料已加密导出"){val bytes=exportBytes?:throw Exception("请重新生成备份");withContext(Dispatchers.IO){context.contentResolver.openOutputStream(uri)?.use{it.write(bytes)}?:throw Exception("无法写入文件")};exportBytes=null}}
 val open=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)action.run{incoming=withContext(Dispatchers.IO){val bytes=context.contentResolver.openInputStream(uri)?.use{it.readCapped(10*1024*1024)}?:throw Exception("无法读取文件");try{archiveDecrypt(bytes,password)}catch(e:Exception){throw Exception("密码不正确，或备份文件已损坏")}}}}
 FullscreenDataPage("本机资料备份",onClose){Panel{SectionTitle("加密保存你的用车记忆",Icons.Outlined.EnhancedEncryption);Text("包含当前车辆的账本、收藏、备注和首页布局。不会导出登录凭据、服务地址、读取缓存或视频。备份密码不保存在设备，遗忘后无法恢复。",fontSize=13.sp,color=muted);OutlinedTextField(password,{password=it.take(128)},label={Text("备份密码（至少8位）")},visualTransformation=PasswordVisualTransformation(),singleLine=true);OutlinedTextField(confirm,{confirm=it.take(128)},label={Text("导出时再次确认密码")},visualTransformation=PasswordVisualTransformation(),singleLine=true)}
 CinemaButton({action.run{require(password==confirm){"两次密码不一致"};val store=HomeLayoutStore(context);val source=JSONObject().put("ledger",DeviceData.read("ledger:"+scope)).put("notes",DeviceData.read("notes:"+scope)).put("order",JSONArray(store.order(api.base,carId.toString()))).put("hidden",JSONArray(store.hidden(api.base,carId.toString()).toList()));exportBytes=withContext(Dispatchers.Default){archiveEncrypt(source,password)};create.launch("Meridian-local-"+java.time.LocalDate.now()+".meridian")}},Modifier.fillMaxWidth(),enabled=!action.busy){Text("生成加密备份")}
 CinemaOutlineButton({if(password.length>=8)open.launch(arrayOf("*/*"))else action.run{throw Exception("请先填写备份密码")}},Modifier.fillMaxWidth(),enabled=!action.busy){Text("从文件导入")};ServerNotice(action)
 incoming?.let{source->FlowDetailPanel({incoming=null},title={Text("导入到当前车辆？")},body={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){Text("账本和手记将合并到当前车辆，首页布局会采用备份中的布局。请确认当前选中的车辆正确。",fontSize=13.sp,color=muted);Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("覆盖相同编号的已有记录",Modifier.weight(1f),fontSize=13.sp);Switch(overwrite,{overwrite=it})}}},confirm={CinemaButton({action.run("本机资料已导入"){withContext(Dispatchers.IO){val oldLedger=DeviceData.read("ledger:"+scope).optJSONArray("entries")?.objects().orEmpty();val newLedger=source.optJSONObject("ledger")?.optJSONArray("entries")?.objects().orEmpty();val merged=linkedMapOf<String,JSONObject>();(if(overwrite)oldLedger+newLedger else newLedger+oldLedger).forEach{entry->val id=entry.optString("id");if(validLedgerArchiveEntry(entry))merged[id]=entry};require(merged.size<=10000){"记账数量过多"};val ledger=DeviceData.read("ledger:"+scope);val imported=source.optJSONObject("ledger");for(field in listOf("autoCharge","unitPrice","ignoredCharges")){if(imported?.has(field)==true&&(overwrite||!ledger.has(field)))ledger.put(field,imported.opt(field))};DeviceData.write("ledger:"+scope,ledger.put("entries",JSONArray(merged.values.toList())));val old=DeviceData.read("notes:"+scope);val fresh=source.optJSONObject("notes")?:JSONObject();fresh.keys().forEach{key->if(overwrite||!old.has(key))fresh.optJSONObject(key)?.let{old.put(key,it)}};DeviceData.write("notes:"+scope,old);val store=HomeLayoutStore(context);source.optJSONArray("order")?.let{store.saveOrder(api.base,carId.toString(),(0 until it.length()).map{n->it.optString(n)})};source.optJSONArray("hidden")?.let{store.saveHidden(api.base,carId.toString(),(0 until it.length()).map{n->it.optString(n)}.filter{it in homeCardIds}.toSet())}};incoming=null}},enabled=!action.busy){Text("确认导入")}},dismiss={CinemaTextButton({incoming=null}){Text("取消")}})}
 }
}
