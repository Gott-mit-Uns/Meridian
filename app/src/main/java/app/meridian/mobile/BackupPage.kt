package app.meridian.mobile

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.*
import kotlinx.coroutines.*
import org.json.*
import java.time.*

@Composable internal fun BackupPage(api:Api,onRestored:()->Unit){
 val context=LocalContext.current;val safety=remember{context.getSharedPreferences("backup-safety",0)};val safetyKey=remember(api.base){java.security.MessageDigest.getInstance("SHA-256").digest(api.base.toByteArray()).joinToString(""){"%02x".format(it)}};val action=rememberServerAction();var settings by remember{mutableStateOf<JSONObject?>(null)}
 var export by remember{mutableStateOf<JSONObject?>(null)};var restore by remember{mutableStateOf<JSONObject?>(null)};var history by remember{mutableStateOf<JSONArray?>(null)};var hasMore by remember{mutableStateOf(false)};var page by remember{mutableIntStateOf(1)}
 var section by remember{mutableStateOf("export")}
 var reload by remember{mutableIntStateOf(0)};var loading by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")}
 var password by remember{mutableStateOf("")};var passwordConfirm by remember{mutableStateOf("")};var importPassword by remember{mutableStateOf("")};var archive by remember{mutableStateOf<Uri?>(null)};var archiveName by remember{mutableStateOf("")}
 var downloadPath by remember{mutableStateOf("")};var pending by remember{mutableStateOf<ServerConfirmation?>(null)};var overwrite by remember{mutableStateOf(false)};var typed by remember{mutableStateOf("")};var restoreSent by remember{mutableStateOf(false)};var completed by remember{mutableStateOf(false)}
 var auto by remember{mutableStateOf(false)};var frequency by remember{mutableStateOf("DAILY")};var time by remember{mutableStateOf("02:00")};var weekday by remember{mutableStateOf("MONDAY")};var monthDay by remember{mutableStateOf("1")};var interval by remember{mutableStateOf("1")};var maxCount by remember{mutableStateOf("7")};var recovery by remember{mutableStateOf("")};var recoveryConfirm by remember{mutableStateOf("")}
 val saveFile=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")){uri->if(uri!=null)action.run("备份已保存到所选位置"){
  require(downloadPath.isNotBlank());api.downloadBackup(downloadPath){input->context.contentResolver.openOutputStream(uri,"w")?.use{output->input.copyTo(output,65536)}?:throw Exception("无法写入所选文件")}
 }}
 val pick=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null){archive=uri;archiveName=runCatching{context.contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use{if(it.moveToFirst())it.getString(0)else null}}.getOrNull().orEmpty()}}
 LaunchedEffect(reload,page){loading=true;error="";try{
  settings=api.call("/user/settings").optJSONObject("data")
  export=api.call("/backup/export/tasks/latest").optJSONObject("data");restore=api.call("/backup/import/tasks/latest").optJSONObject("data");restoreSent=restore?.safeText("restoreId")?.takeIf{it.isNotBlank()}?.let{it==safety.getString(safetyKey,null)}?:false
  val response=api.call("/backup/auto/history?pageNum=$page&pageSize=10");history=response.optJSONArray("data")?:JSONArray();hasMore=response.optBoolean("hasMore")
 }catch(e:Exception){if(e is CancellationException)throw e;error=e.message?:"备份状态读取失败"}finally{loading=false}}
 LaunchedEffect(settings?.toString()){
  val config=settings?.optJSONObject("autoBackup")?:return@LaunchedEffect
  auto=config.optBoolean("enabled");frequency=config.safeText("frequency","DAILY");time=config.safeText("time","02:00");weekday=config.safeText("weekday","MONDAY");monthDay=config.optInt("monthDay",1).toString();interval=config.optInt("intervalHours",1).toString();maxCount=config.optInt("maxCount",7).toString()
 }
 LaunchedEffect(export?.safeText("status"),restore?.safeText("status"),completed){
  if(completed)return@LaunchedEffect
  while(isActive&&(export?.safeText("status")in listOf("PENDING","RUNNING")||restore?.safeText("status")in listOf("PENDING","RUNNING"))){delay(2500);try{
   export=api.call("/backup/export/tasks/latest").optJSONObject("data");restore=api.call("/backup/import/tasks/latest").optJSONObject("data")
  }catch(e:Exception){if(e is CancellationException)throw e;error=e.message?:"进度读取失败";break}}
 }
 CompactSegments(listOf("export" to "导出备份","auto" to "自动计划","restore" to "恢复数据"),section,{section=it})
 if(loading)FlowLoading(label="正在读取备份与校验任务…")
 if(error.isNotBlank())Panel{Text(error,fontSize=12.sp,color=MaterialTheme.colorScheme.error);CinemaOutlineButton({reload++},enabled=!action.busy){Text("重新读取")}}
 ServerNotice(action)
 if(completed){Panel{SectionTitle("恢复已完成",Icons.Outlined.CheckCircle);Text("请立即重启 NAS 上的服务，然后使用备份中的账号重新登录。",fontSize=14.sp,lineHeight=23.sp,color=ink);CinemaButton(onRestored,Modifier.fillMaxWidth()){Text("已知晓，返回登录")}};return}
 if(settings!=null&&settings?.optJSONObject("autoBackup")==null){Panel{Text("当前账号没有服务端备份管理权限",fontSize=13.sp,color=muted)};return}
 if(section=="export")Panel{
  SectionTitle("加密备份",Icons.Outlined.Backup)
  Text("备份包括数据库、字段加密密钥和账号数据。恢复口令请妥善保存。",fontSize=12.sp,lineHeight=20.sp,color=muted)
  OutlinedTextField(password,{password=it},label={Text("备份恢复口令 · 至少6位")},singleLine=true,visualTransformation=PasswordVisualTransformation())
  OutlinedTextField(passwordConfirm,{passwordConfirm=it},label={Text("确认口令")},singleLine=true,visualTransformation=PasswordVisualTransformation())
  CinemaButton({pending=ServerConfirmation("创建加密备份","新任务将替换当前服务端暂存的可下载备份。恢复口令不会随手动备份保存在服务端。"){
   require(password.length>=6&&password==passwordConfirm){"恢复口令至少6位，且两次填写需一致"};export=api.call("/backup/export",JSONObject().put("password",password)).optJSONObject("data");password="";passwordConfirm=""
  }},Modifier.fillMaxWidth(),enabled=!action.busy&&!loading&&settings!=null&&export?.safeText("status")!in listOf("PENDING","RUNNING")){Text("生成加密备份")}
  export?.let{task->BackupTaskSummary(task);if(task.safeText("status")=="READY")CinemaOutlineButton({downloadPath="/backup/export/tasks/"+java.net.URLEncoder.encode(task.get("id").toString(),"UTF-8")+"/download";saveFile.launch("Meridian-${LocalDate.now()}.xmbak")},Modifier.fillMaxWidth(),enabled=!action.busy){Text("保存备份到手机")}}
 }
 if(section=="auto")Panel{
  SectionTitle("自动备份计划",Icons.Outlined.Schedule)
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("启用自动备份",Modifier.weight(1f),fontSize=13.sp);Switch(auto,{auto=it})}
  SmallSelectionMenu(frequency,listOf("HOURLY" to "每小时","DAILY" to "每天","WEEKLY" to "每周","MONTHLY" to "每月"),{frequency=it})
  if(frequency=="HOURLY")OutlinedTextField(interval,{interval=it},label={Text("间隔小时 · 1—24")},singleLine=true)else OutlinedTextField(time,{time=it},label={Text("备份时间 · HH:mm")},singleLine=true)
  if(frequency=="WEEKLY")SmallSelectionMenu(weekday,listOf("MONDAY" to "周一","TUESDAY" to "周二","WEDNESDAY" to "周三","THURSDAY" to "周四","FRIDAY" to "周五","SATURDAY" to "周六","SUNDAY" to "周日"),{weekday=it})
  if(frequency=="MONTHLY")OutlinedTextField(monthDay,{monthDay=it},label={Text("每月日期 · 1—28")},singleLine=true)
  OutlinedTextField(maxCount,{maxCount=it},label={Text("保留备份数量")},singleLine=true)
  Text(if(settings?.optJSONObject("autoBackup")?.optBoolean("recoveryPasswordConfigured")==true)"自动备份口令已配置，留空保留"else"启用计划前需要配置恢复口令",fontSize=11.sp,color=muted)
  OutlinedTextField(recovery,{recovery=it},label={Text("自动备份恢复口令")},singleLine=true,visualTransformation=PasswordVisualTransformation())
  OutlinedTextField(recoveryConfirm,{recoveryConfirm=it},label={Text("确认恢复口令")},singleLine=true,visualTransformation=PasswordVisualTransformation())
  CinemaOutlineButton({pending=ServerConfirmation("保存自动备份计划","自动备份将按此计划在你的服务端执行，并按保留数量清理旧的自动备份。"){
   val body=JSONObject().put("enabled",auto).put("frequency",frequency).put("time",time).put("weekday",weekday).put("monthDay",monthDay.toIntOrNull()?:1).put("intervalHours",interval.toIntOrNull()?:1).put("maxCount",maxCount.toIntOrNull()?:0)
   require((maxCount.toIntOrNull()?:0)in 1..100){"保留数量需为1—100"};if(frequency=="HOURLY")require((interval.toIntOrNull()?:0)in 1..24){"间隔需为1—24小时"}else LocalTime.parse(time)
   if(frequency=="MONTHLY")require((monthDay.toIntOrNull()?:0)in 1..28){"月度日期需为1—28"}
   if(recovery.isNotBlank()){require(recovery.length>=6&&recovery==recoveryConfirm){"口令至少6位且两次填写需一致"};body.put("recoveryPassword",recovery)}
   require(!auto||recovery.isNotBlank()||settings?.optJSONObject("autoBackup")?.optBoolean("recoveryPasswordConfigured")==true){"请先配置自动备份恢复口令"}
   api.call("/user/settings",JSONObject().put("autoBackup",body));recovery="";recoveryConfirm="";reload++
  }},Modifier.fillMaxWidth(),enabled=settings!=null&&!action.busy){Text("保存计划")}
 }
 if(section=="auto")Panel{
  SectionTitle("自动备份历史",Icons.Outlined.History)
  if(history?.length()==0)Text("暂无自动备份记录",fontSize=12.sp,color=muted)
  history?.objects()?.forEach{row->Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
   Text(row.safeText("fileName",row.safeText("created",row.safeText("createdAt","备份记录"))),fontSize=13.sp,color=ink);BackupTaskSummary(row)
   if(row.safeText("status")=="SUCCESS")CinemaOutlineButton({downloadPath="/backup/auto/history/"+java.net.URLEncoder.encode(row.get("id").toString(),"UTF-8")+"/download";saveFile.launch(row.safeText("fileName","Meridian-auto.xmbak"))},Modifier.fillMaxWidth(),enabled=!action.busy){Text("保存此备份")}
  }}
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){CinemaOutlineButton({page--},Modifier.weight(1f),enabled=page>1&&!loading){Text("上一页")};CinemaOutlineButton({page++},Modifier.weight(1f),enabled=hasMore&&!loading){Text("下一页")}}
 }
 if(section=="restore")Panel{
  SectionTitle("从备份恢复",Icons.Outlined.Restore)
  Text("恢复会覆盖整个服务的数据库、加密密钥和所有账号密码。校验通过后仍需单独确认。",fontSize=12.sp,lineHeight=20.sp,color=MaterialTheme.colorScheme.error)
  CinemaOutlineButton({pick.launch(arrayOf("*/*"))},Modifier.fillMaxWidth(),enabled=!action.busy&&!restoreSent){Text(archiveName.ifBlank{"选择 .xmbak 备份"})}
  OutlinedTextField(importPassword,{importPassword=it},label={Text("备份恢复口令")},singleLine=true,visualTransformation=PasswordVisualTransformation())
  CinemaOutlineButton({action.run("备份已提交校验"){
   val uri=archive?:throw Exception("请选择备份文件");restore=api.prepareBackup(archiveName,importPassword){context.contentResolver.openInputStream(uri)?:throw Exception("无法读取备份文件")};importPassword="";archive=null;archiveName="";typed="";overwrite=false;restoreSent=restore?.safeText("restoreId")?.takeIf{it.isNotBlank()}?.let{it==safety.getString(safetyKey,null)}?:false
  }},Modifier.fillMaxWidth(),enabled=archive!=null&&!action.busy&&!restoreSent){Text("上传并校验备份")}
  restore?.let{task->BackupTaskSummary(task)
   if(task.safeText("status")=="READY"&&!restoreSent){
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Checkbox(overwrite,{overwrite=it});Text("我理解这会覆盖所有服务端数据和账号",fontSize=12.sp,lineHeight=19.sp,color=ink,modifier=Modifier.weight(1f))}
    OutlinedTextField(typed,{typed=it},label={Text("输入“恢复”确认")},singleLine=true)
    CinemaButton({val id=task.safeText("restoreId");pending=ServerConfirmation("覆盖全部数据并恢复","当前数据库、加密密钥和全部账号密码将被备份替换。完成后需要立即重启 NAS 上的服务，并使用备份中的账号重新登录。","确认覆盖并恢复"){
     require(!restoreSent){"恢复请求已提交，请先确认服务端状态"};require(overwrite&&typed=="恢复"&&id.isNotBlank());restoreSent=true;withContext(Dispatchers.IO){check(safety.edit().putString(safetyKey,id).commit()){"无法保存恢复操作标记，未发送恢复请求"}}
     try{api.call("/backup/import/confirm",JSONObject().put("restoreId",id));completed=true}catch(e:Exception){if(e is CancellationException)throw e;throw Exception("恢复请求已发送。若服务连接已中断，请检查 NAS 并重新登录确认结果，不要重复提交。")}
    }},Modifier.fillMaxWidth(),enabled=overwrite&&typed=="恢复"&&!action.busy){Text("确认覆盖并恢复")}
   }
  }
  if(restoreSent)CinemaOutlineButton(onRestored,Modifier.fillMaxWidth(),enabled=!action.busy){Text("返回登录")}
  CinemaTextButton({reload++},enabled=!action.busy){Text("刷新备份与校验状态")}
 }
 ConfirmServerChange(pending,action){pending=null}
}

@Composable private fun BackupTaskSummary(task:JSONObject){
 Text(when(task.safeText("status")){"PENDING"->"等待处理";"RUNNING"->"正在处理";"READY"->"已就绪";"SUCCESS"->"已完成";"EXPIRED"->"已过期";"CANCELLED"->"已取消";else->task.safeText("status","状态待更新")},fontSize=13.sp,color=ink)
 task.safeText("message",task.safeText("failureMessage")).takeIf{it.isNotBlank()}?.let{Text(it,fontSize=12.sp,lineHeight=20.sp,color=muted)}
 task.number("fileSize")?.let{Text(numberText(it/1024/1024)+" MB",fontSize=11.sp,color=muted)}
 task.safeText("expiresAt").takeIf{it.isNotBlank()}?.let{Text("有效至 · "+prettyTime(it),fontSize=11.sp,color=muted)}
 if(task.safeText("status")in listOf("PENDING","RUNNING"))FlowLoading(label="服务端正在处理…")
}
