package app.meridian.mobile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.*
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.*
import org.json.*
import java.time.*

@Composable internal fun AccountServicesPage(api:Api,account:String,onCredentialsChanged:()->Unit,initialSection:String="",onBack:()->Unit){
 var page by remember{mutableStateOf(initialSection)};var refreshRequest by remember{mutableIntStateOf(0)};var refreshing by remember{mutableStateOf(false)};var syncTime by remember{mutableLongStateOf(0L)}
 ImmersiveScrollingPage("账户与服务",onBack){
  Panel{Text(account,fontSize=25.sp,color=ink);Text("管理账户、安全授权和服务连接",fontSize=12.sp,color=muted)}
  FlowSettingsGroup{
   SettingsEntry(Icons.Outlined.Person,"账户信息","修改登录名称和密码"){page="profile"}
   SettingsEntry(Icons.Outlined.Link,"小米汽车授权","扫码登录、设备选择与授权检测"){page="xiaomi"}
   SettingsEntry(Icons.Outlined.Notifications,"通知订阅","渠道、订阅事件与测试发送"){page="notifications"}
   SettingsEntry(Icons.Outlined.Backup,"数据备份与恢复","加密备份、自动计划与恢复校验"){page="backup"}
   SettingsEntry(Icons.Outlined.Key,"API Key 管理","创建、查看、禁用与删除"){page="keys"}
   SettingsEntry(Icons.Outlined.Map,"地图与扩展设置","地图密钥与通勤天气提醒"){page="settings"}
  }
 }
 if(page.isNotBlank())FullscreenDataPage(when(page){"profile"->"账户信息";"xiaomi"->"小米汽车授权";"keys"->"API Key";"notifications"->"通知订阅";"backup"->"备份与恢复";else->"服务端设置"},{if(initialSection.isNotBlank())onBack()else page=""},refreshing=refreshing,onRefresh=if(page=="notifications")({refreshRequest++})else null,lastSynced=syncTime){
  CompositionLocalProvider(LocalControlSettings provides true){when(page){"profile"->AccountForm(api,account,onCredentialsChanged);"xiaomi"->XiaomiAuthorization(api);"keys"->ApiKeysContent(api);"notifications"->NotificationSettings(api,refreshRequest){loading,time->refreshing=loading;syncTime=time};"backup"->BackupPage(api,onCredentialsChanged);else->ServerSettingsContent(api)}}
 }
}

@Composable private fun AccountForm(api:Api,account:String,onCredentialsChanged:()->Unit){
 var name by remember{mutableStateOf(account)};var password by remember{mutableStateOf("")};var repeat by remember{mutableStateOf("")}
 val action=rememberServerAction();var pending by remember{mutableStateOf<ServerConfirmation?>(null)}
 Panel{
  OutlinedTextField(name,{name=it},label={Text("登录名称")},singleLine=true)
  OutlinedTextField(password,{password=it},label={Text("新密码")},placeholder={Text("留空则保留当前密码")},singleLine=true,visualTransformation=PasswordVisualTransformation())
  OutlinedTextField(repeat,{repeat=it},label={Text("确认新密码")},singleLine=true,visualTransformation=PasswordVisualTransformation())
  ServerNotice(action)
  CinemaButton({action.run{
   require(name.isNotBlank()){"请输入登录名称"};require(password==repeat){"两次密码不一致"}
   pending=ServerConfirmation("更新账户信息","更新登录名称或密码后需要重新登录。请确认已记住新凭据。"){
    val body=JSONObject().put("userName",name.trim());if(password.isNotBlank())body.put("password",password)
    api.call("/user/change",body);password="";repeat="";onCredentialsChanged()
   }
  }},Modifier.fillMaxWidth(),enabled=!action.busy){Text("保存修改")}
 }
 ConfirmServerChange(pending,action){pending=null}
}

@Composable private fun ApiKeysContent(api:Api){
 val context=LocalContext.current;val action=rememberServerAction();var keys by remember{mutableStateOf<JSONArray?>(null)};var reload by remember{mutableIntStateOf(0)}
 var creating by remember{mutableStateOf(false)};var name by remember{mutableStateOf("")};var expiry by remember{mutableStateOf<LocalDate?>(null)};var time by remember{mutableStateOf("23:59")};var calendar by remember{mutableStateOf(false)}
 var secret by remember{mutableStateOf("")};var pending by remember{mutableStateOf<ServerConfirmation?>(null)}
 LaunchedEffect(reload){action.run{keys=api.call("/user/api-key/list").optJSONArray("data")?:JSONArray()}}
 ServerNotice(action)
 Panel{Text("服务访问密钥",fontSize=23.sp,color=ink);Text("最多保留5个有效密钥。完整密钥仅在创建后显示一次。",fontSize=12.sp,lineHeight=20.sp,color=muted)
  CinemaButton({creating=true;name="";expiry=null;time="23:59"},Modifier.fillMaxWidth(),enabled=!action.busy&&(keys?.objects()?.count{it.safeText("status")=="ACTIVE"}?:0)<5){Text("创建 API Key")}
  CinemaOutlineButton({reload++},Modifier.fillMaxWidth(),enabled=!action.busy){Text("刷新列表")}
 }
 if(keys?.length()==0)Panel{Text("还没有访问密钥",fontSize=13.sp,color=muted)}
 keys?.objects()?.forEach{key->Panel{
  SectionTitle(key.safeText("name","未命名密钥"),Icons.Outlined.Key)
  Text(key.safeText("keyPrefix")+"••••••••",fontSize=15.sp,color=ink)
  Text(when(key.safeText("status")){"ACTIVE"->"使用中";"EXPIRED"->"已过期";"REVOKED"->"已禁用";else->key.safeText("status")},fontSize=12.sp,color=muted)
  Text("到期 · "+key.safeText("expiresAt").ifBlank{"长期有效"},fontSize=11.sp,color=muted)
  Text("最后使用 · "+key.safeText("lastUsedAt").ifBlank{"尚未使用"},fontSize=11.sp,color=muted)
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
   CinemaOutlineButton({pending=ServerConfirmation("禁用密钥","禁用“${key.safeText("name")}”后，使用该密钥的连接将立即失效，无法恢复。"){api.call("/user/api-key/revoke",JSONObject().put("id",key.get("id")));reload++}},Modifier.weight(1f),enabled=!action.busy&&key.safeText("status")=="ACTIVE"){Text("禁用")}
   CinemaOutlineButton({pending=ServerConfirmation("删除密钥","将永久删除“${key.safeText("name")}”。有效密钥会立即失效。"){api.call("/user/api-key/delete",JSONObject().put("id",key.get("id")));reload++}},Modifier.weight(1f),enabled=!action.busy){Text("删除",color=MaterialTheme.colorScheme.error)}
  }
 }}
 if(creating)FlowDetailPanel({if(!action.busy)creating=false},title={Text("创建 API Key")},body={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
  OutlinedTextField(name,{name=it.take(64)},label={Text("用途名称")},placeholder={Text("例如：家庭车辆面板")},singleLine=true)
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("设置到期时间",Modifier.weight(1f),fontSize=13.sp);Switch(expiry!=null,{expiry=if(it)LocalDate.now().plusDays(30)else null})}
  expiry?.let{CinemaOutlineButton({calendar=true},Modifier.fillMaxWidth()){Text("到期日期 · $it")};OutlinedTextField(time,{time=it},label={Text("到期时间 · HH:mm")},singleLine=true)}
  ServerNotice(action)
 }},confirm={CinemaButton({action.run{
  require(name.isNotBlank()){"请填写用途名称"};val body=JSONObject().put("name",name.trim())
  expiry?.let{val at=it.atTime(LocalTime.parse(time));require(at.isAfter(LocalDateTime.now())){"到期时间需晚于当前时间"};body.put("expiresAt",at.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")))}
  secret=api.call("/user/api-key/create",body).optJSONObject("data")?.safeText("apiKey").orEmpty();require(secret.isNotBlank()){"服务未返回完整密钥，请关闭后刷新列表，避免重复创建"};creating=false;reload++
 }},enabled=!action.busy&&name.isNotBlank()){Text("创建密钥")}})
 if(calendar)FlowDatePicker(expiry?:LocalDate.now().plusDays(30),{calendar=false},latest=LocalDate.now().plusYears(20),earliest=LocalDate.now()){expiry=it;calendar=false}
 if(secret.isNotBlank())FlowDetailPanel({secret=""},title={Text("请保存访问密钥")},body={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
  Text("关闭后无法再次查看完整密钥。仅向信任的应用提供。",fontSize=13.sp,lineHeight=21.sp,color=muted)
  SelectionContainer{Text(secret,fontSize=14.sp,lineHeight=22.sp,color=ink)}
 }},confirm={CinemaButton({(context.getSystemService(Context.CLIPBOARD_SERVICE)as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Meridian API Key",secret).apply{if(android.os.Build.VERSION.SDK_INT>=33)description.extras=android.os.PersistableBundle().apply{putBoolean(android.content.ClipDescription.EXTRA_IS_SENSITIVE,true)}})}){Text("复制密钥")}},dismiss={CinemaTextButton({secret=""}){Text("已保存，关闭")}})
}

@Composable private fun XiaomiAuthorization(api:Api){
 val action=rememberServerAction();var account by remember{mutableStateOf("")};var password by remember{mutableStateOf("")};var eui by remember{mutableStateOf("")};var device by remember{mutableStateOf("")}
 var devices by remember{mutableStateOf<JSONArray?>(null)};var session by remember{mutableStateOf<JSONObject?>(null)};var polling by remember{mutableStateOf(false)};var status by remember{mutableStateOf("")};var qr by remember{mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null)}
 var needsDevice by remember{mutableStateOf(false)};var pending by remember{mutableStateOf<ServerConfirmation?>(null)};val owner=LocalLifecycleOwner.current
 fun body()=JSONObject().apply{if(account.isNotBlank())put("xiaomiAccount",account.trim());if(password.isNotBlank())put("xiaomiPassword",password);if(eui.isNotBlank())put("xiaomiEui",eui.trim());if(device.isNotBlank())put("xiaomiDeviceId",device.trim())}
 LaunchedEffect(session?.safeText("sessionId"),polling){
  val id=session?.safeText("sessionId").orEmpty();if(id.isBlank()||!polling)return@LaunchedEffect
  owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED){
   while(isActive&&polling){try{
    val result=api.call("/user/xiaomi/qr/poll",JSONObject().put("sessionId",id)).optJSONObject("data")?:throw Exception("未返回扫码状态")
    status=result.safeText("message","等待扫码确认")
    when(result.safeText("status")){
     "SUCCESS"->{polling=false;password="";needsDevice=result.optBoolean("needSelectDevice");if(needsDevice){devices=api.call("/user/xiaomi/device/list",JSONObject()).optJSONArray("data")?:JSONArray();status="扫码成功，请选择车辆设备"}else status="小米授权已保存"}
     "EXPIRED","FAILED","ERROR"->polling=false
    }
   }catch(e:Exception){if(e is CancellationException)throw e;status=e.message?:"扫码状态读取失败，可重新获取二维码"};delay(3000)}
  }
 }
 LaunchedEffect(session?.safeText("qrImageUrl")){
  qr=null;val url=session?.safeText("qrImageUrl").orEmpty();if(url.isNotBlank())try{val bytes=api.imageResource(url);qr=withContext(Dispatchers.Default){BitmapFactory.decodeByteArray(bytes,0,bytes.size)?.asImageBitmap()};if(qr==null)status="二维码图片无法解码，请刷新"}catch(e:Exception){if(e is CancellationException)throw e;status=e.message?:"二维码加载失败"}
 }
 Panel{
  SectionTitle("扫码授权",Icons.Outlined.QrCode)
  Text("使用小米汽车 App 扫码并确认授权",fontSize=13.sp,color=muted)
  qr?.let{Image(it,"小米登录二维码",Modifier.fillMaxWidth().height(220.dp))}
  if(status.isNotBlank())Text(status,fontSize=13.sp,lineHeight=21.sp,color=muted)
  CinemaButton({action.run{polling=false;needsDevice=false;session=null;session=api.call("/user/xiaomi/qr/init",JSONObject()).optJSONObject("data")?:throw Exception("未获得二维码");polling=true;status="等待扫码确认"}},Modifier.fillMaxWidth(),enabled=!action.busy){Text(if(session==null)"获取登录二维码"else"刷新二维码")}
  if(polling)CinemaTextButton({polling=false}){Text("停止等待")}
 }
 Panel{
  SectionTitle("授权设备",Icons.Outlined.Devices)
  CinemaOutlineButton({action.run{devices=api.call("/user/xiaomi/device/list",JSONObject()).optJSONArray("data")?:JSONArray()}},Modifier.fillMaxWidth(),enabled=!action.busy){Text("读取设备列表")}
  devices?.objects()?.forEach{entry->val id=entry.safeText("deviceId",entry.safeText("id"));CinemaChoice(device==id,{device=id},{Column{Text(entry.safeText("deviceName",entry.safeText("name",id)));Text(id,fontSize=11.sp,color=muted)}},Modifier.fillMaxWidth())}
  OutlinedTextField(device,{device=it},label={Text("设备 ID")},singleLine=true)
  if(needsDevice&&session!=null&&!polling&&device.isNotBlank())CinemaButton({pending=ServerConfirmation("确认授权设备","将使用所选设备完成本次小米账号授权。"){api.call("/user/xiaomi/qr/confirmDevice",JSONObject().put("sessionId",session!!.getString("sessionId")).put("deviceId",device.trim()));status="设备授权已保存";needsDevice=false;session=null}}){Text("确认所选设备")}
 }
 Panel{
  SectionTitle("手动填写授权",Icons.Outlined.ManageAccounts)
  OutlinedTextField(account,{account=it},label={Text("小米登录账号")},singleLine=true)
  OutlinedTextField(password,{password=it},label={Text("小米登录密码")},singleLine=true,visualTransformation=PasswordVisualTransformation())
  OutlinedTextField(eui,{eui=it},label={Text("EUI")},singleLine=true)
  CinemaButton({pending=ServerConfirmation("保存小米授权","将把填写的小米账号、密码、EUI 和设备 ID 提交给你配置的服务端。"){require(body().length()>0){"请填写需要保存的授权信息"};api.call("/user/change",body());password=""}},Modifier.fillMaxWidth(),enabled=!action.busy){Text("保存授权信息")}
  CinemaOutlineButton({pending=ServerConfirmation("测试并保存授权","服务端将测试填写的账号信息，并保存获取到的小米授权。"){api.call("/user/testToken",body());password=""}},Modifier.fillMaxWidth(),enabled=!action.busy){Text("测试并保存授权")}
 }
 ServerNotice(action);ConfirmServerChange(pending,action){pending=null}
}

@Composable private fun ServerSettingsContent(api:Api){
 val action=rememberServerAction();var settings by remember{mutableStateOf<JSONObject?>(null)};var amap by remember{mutableStateOf("")};var tianditu by remember{mutableStateOf("")};var overrides by remember{mutableStateOf<JSONObject?>(null)}
 var weekday by remember{mutableStateOf("MONDAY")};var pending by remember{mutableStateOf<ServerConfirmation?>(null)}
 LaunchedEffect(Unit){action.run{settings=api.call("/user/settings").optJSONObject("data");overrides=JSONObject(settings?.optJSONObject("commuteReminder")?.optJSONObject("weekdayOverrides")?.toString()?:"{}")}}
 Panel{
  SectionTitle("地图服务",Icons.Outlined.Map)
  Text("高德 · "+if(settings?.optBoolean("amapKeyConfigured")==true)"已配置"else"未配置",fontSize=12.sp,color=muted)
  OutlinedTextField(amap,{amap=it},label={Text("高德 Key")},placeholder={Text("留空保留已有配置")},singleLine=true,visualTransformation=PasswordVisualTransformation())
  Text("天地图 · "+if(settings?.optBoolean("tiandituTkConfigured")==true)"已配置"else"未配置",fontSize=12.sp,color=muted)
  OutlinedTextField(tianditu,{tianditu=it},label={Text("天地图 Token")},placeholder={Text("留空保留已有配置")},singleLine=true,visualTransformation=PasswordVisualTransformation())
 }
 Panel{
  SectionTitle("通勤天气提醒",Icons.Outlined.CalendarMonth)
  Text("按星期调整早晚通勤提醒，保存后由服务端执行推送。",fontSize=12.sp,lineHeight=20.sp,color=muted)
  val days=listOf("MONDAY" to "周一","TUESDAY" to "周二","WEDNESDAY" to "周三","THURSDAY" to "周四","FRIDAY" to "周五","SATURDAY" to "周六","SUNDAY" to "周日")
  Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){days.forEach{(key,label)->CinemaChoice(weekday==key,{weekday=key},{Text(label)})}}
  val plan=overrides?.optJSONObject(weekday)?:settings?.optJSONObject("commuteReminder")?.optJSONObject("weekdayPlans")?.optJSONObject(weekday)?:JSONObject()
  fun update(key:String,value:Any){val next=JSONObject(overrides?.toString()?:"{}");next.put(weekday,JSONObject(plan.toString()).put(key,value));overrides=next}
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("早间提醒",Modifier.weight(1f),fontSize=13.sp);Switch(plan.optBoolean("enabled"),{update("enabled",it)})}
  OutlinedTextField(plan.safeText("reminderTime"),{update("reminderTime",it)},label={Text("早间时间 · HH:mm")},singleLine=true)
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("晚间提醒",Modifier.weight(1f),fontSize=13.sp);Switch(plan.optBoolean("eveningEnabled"),{update("eveningEnabled",it)})}
  OutlinedTextField(plan.safeText("eveningReminderTime"),{update("eveningReminderTime",it)},label={Text("晚间时间 · HH:mm")},singleLine=true)
  settings?.optJSONObject("commuteReminder")?.safeText("updatedAt")?.takeIf{it.isNotBlank()}?.let{Text("规律更新 · "+prettyTime(it),fontSize=11.sp,color=muted)}
 }
 ServerNotice(action)
 CinemaButton({pending=ServerConfirmation("保存扩展设置","更新服务端地图配置与通勤天气提醒。未填写的地图密钥会保留原值。"){
  val body=JSONObject();if(amap.isNotBlank())body.put("amapKey",amap.trim());if(tianditu.isNotBlank())body.put("tiandituTk",tianditu.trim());overrides?.let{body.put("commuteReminder",JSONObject().put("weekdayOverrides",it))}
  validateCommuteOverrides(overrides);api.call("/user/settings",body);amap="";tianditu="";settings=api.call("/user/settings").optJSONObject("data")
 }},Modifier.fillMaxWidth(),enabled=!action.busy&&settings!=null){Text("保存服务设置")}

 ConfirmServerChange(pending,action){pending=null}
}
