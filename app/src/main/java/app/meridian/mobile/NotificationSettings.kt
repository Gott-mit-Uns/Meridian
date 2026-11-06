package app.meridian.mobile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.*
import org.json.*

@Composable internal fun NotificationSettings(api:Api,refreshRequest:Int=0,onRefreshState:(Boolean,Long)->Unit={_,_->}){
 val action=rememberServerAction();var records by remember{mutableStateOf<JSONArray?>(null)};var events by remember{mutableStateOf<JSONArray?>(null)};var reload by remember{mutableIntStateOf(0)}
 var platform by remember{mutableStateOf("WECOM")};var enabled by remember{mutableStateOf(false)};var webhook by remember{mutableStateOf("")};var appId by remember{mutableStateOf("")};var secret by remember{mutableStateOf("")};var agentId by remember{mutableStateOf("")};var recipients by remember{mutableStateOf("")};var receiveType by remember{mutableStateOf("user_id")}
 var syncTime by remember{mutableLongStateOf(0L)}
 SideEffect{onRefreshState(action.busy,syncTime)}
 val selected=remember{mutableStateListOf<String>()};var picking by remember{mutableStateOf(false)};var search by remember{mutableStateOf("")};var pending by remember{mutableStateOf<ServerConfirmation?>(null)};var testResult by remember{mutableStateOf<JSONObject?>(null)}
 LaunchedEffect(reload,refreshRequest){action.run{records=api.call("/notify/subscriptions").optJSONArray("data")?:JSONArray();events=api.call("/notify/events").optJSONArray("data")?:JSONArray();syncTime=System.currentTimeMillis()}}
 val record=records?.objects()?.firstOrNull{it.safeText("platform")==platform}
 LaunchedEffect(platform,records?.toString(),events?.toString()){
  enabled=record?.optInt("enabled")==1;webhook="";appId=record?.safeText("appId").orEmpty();secret="";agentId=record?.safeText("agentId").orEmpty();recipients=record?.safeText("receiveIds").orEmpty();receiveType=record?.safeText("receiveIdType","user_id")?:"user_id"
  selected.clear();val codes=record?.optJSONArray("eventCodes");if(codes==null||codes.length()==0)events?.objects()?.map{it.safeText("code")}?.let{selected.addAll(it)}else for(i in 0 until codes.length())selected.add(codes.optString(i))
 }
 fun validate(){if(enabled){
  require(selected.isNotEmpty()){"请至少选择一个通知事件"}
  if(platform=="WECOM")require(webhook.isNotBlank()||record?.optBoolean("webhookKeyConfigured")==true){"请先填写机器人 Key"}
  else{require(appId.isNotBlank()&&recipients.isNotBlank()){"请填写 App ID 和接收方"};require(secret.isNotBlank()||record?.optBoolean("appSecretConfigured")==true){"请填写应用密钥"};if(platform=="DINGTALK")require(agentId.isNotBlank()){"请填写 Agent ID"}}
 }}
 fun payload():JSONObject=JSONObject().put("platform",platform).put("enabled",if(enabled)1 else 0).put("eventCodes",JSONArray(selected.toList())).apply{
  if(platform=="WECOM"){if(webhook.isNotBlank())put("webhookKey",webhook.trim())}
  else{put("appId",appId.trim());put("receiveIds",recipients.trim());if(secret.isNotBlank())put("appSecret",secret);if(platform=="DINGTALK")put("agentId",agentId.trim())else put("receiveIdType",receiveType)}
 }
 CompactSegments(listOf("WECOM" to "企业微信","DINGTALK" to "钉钉","FEISHU" to "飞书"),platform,{platform=it})
 ServerNotice(action)
 Panel{
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("启用此通知渠道",Modifier.weight(1f),fontSize=14.sp);Switch(enabled,{enabled=it})}
  if(platform=="WECOM"){
   Text(if(record?.optBoolean("webhookKeyConfigured")==true)"机器人密钥已配置，留空保留"else"请填写机器人 Webhook Key",fontSize=12.sp,color=muted)
   OutlinedTextField(webhook,{webhook=it},label={Text("Webhook Key")},singleLine=true,visualTransformation=PasswordVisualTransformation())
  }else{
   OutlinedTextField(appId,{appId=it},label={Text("App ID")},singleLine=true)
   Text(if(record?.optBoolean("appSecretConfigured")==true)"应用密钥已配置，留空保留"else"尚未配置应用密钥",fontSize=12.sp,color=muted)
   OutlinedTextField(secret,{secret=it},label={Text("App Secret")},singleLine=true,visualTransformation=PasswordVisualTransformation())
   if(platform=="DINGTALK")OutlinedTextField(agentId,{agentId=it},label={Text("Agent ID")},singleLine=true)
   OutlinedTextField(recipients,{recipients=it},label={Text("接收方 ID")},placeholder={Text("按服务端规范填写多个接收方")})
   if(platform=="FEISHU")SmallSelectionMenu(receiveType,listOf("user_id" to "User ID","open_id" to "Open ID","union_id" to "Union ID","email" to "邮箱","chat_id" to "群聊 ID"),{receiveType=it})
  }
 }
 Panel{
  SectionTitle("订阅事件",Icons.Outlined.NotificationsActive)
  Text("已选 ${selected.size} 项。关闭渠道可暂停全部通知。",fontSize=12.sp,color=muted)
  CinemaOutlineButton({picking=true},Modifier.fillMaxWidth()){Text("选择订阅事件")}
 }
 CinemaButton({val body=payload();pending=ServerConfirmation("保存通知配置","将更新所选通知渠道和订阅事件。"){
  validate();require(!enabled||selected.isNotEmpty()){"启用渠道时至少选择一个事件"};api.call("/notify/changeSubscription",body);reload++
 }},Modifier.fillMaxWidth(),enabled=records!=null&&!action.busy){Text("保存通知配置")}
 CinemaOutlineButton({val body=payload();val target=platform;pending=ServerConfirmation("保存并发送测试通知","将保存当前配置，并向所配置的接收方发送一条测试通知。请确认接收方正确。"){
  validate();require(selected.isNotEmpty()){"请先选择订阅事件"};api.call("/notify/changeSubscription",body);testResult=api.call("/notify/testSend",JSONObject().put("platform",target)).optJSONObject("data");reload++
 }},Modifier.fillMaxWidth(),enabled=records!=null&&!action.busy){Text("保存并发送测试")}
 testResult?.let{result->Panel{SectionTitle(if(result.optBoolean("success"))"测试发送成功"else"测试发送失败",Icons.Outlined.Info);Text(result.safeText("title"),fontSize=14.sp,color=ink);Text(result.safeText("content"),fontSize=12.sp,lineHeight=20.sp,color=muted);result.safeText("errorMsg").takeIf{it.isNotBlank()}?.let{Text(it,fontSize=12.sp,color=MaterialTheme.colorScheme.error)}}}
 if(picking)FlowDetailPanel({picking=false},title={Text("选择订阅事件")},body={Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
  OutlinedTextField(search,{search=it},placeholder={Text("搜索事件名称")},singleLine=true)
  events?.objects()?.filter{it.safeText("title").contains(search,true)||it.safeText("code").contains(search,true)}?.forEach{event->val code=event.safeText("code");CinemaChoice(code in selected,{if(code in selected)selected.remove(code)else selected.add(code)},{Column{Text(event.safeText("title",code),fontSize=13.sp);event.safeText("description").takeIf{it.isNotBlank()}?.let{Text(it,fontSize=11.sp,color=muted)}}},Modifier.fillMaxWidth())}
 }},confirm={CinemaTextButton({picking=false}){Text("完成选择 · ${selected.size}项")}})
 ConfirmServerChange(pending,action){pending=null}
}
