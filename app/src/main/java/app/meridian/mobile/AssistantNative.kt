package app.meridian.mobile

import dev.chrisbanes.haze.hazeSource
import android.app.Activity
import android.content.Intent
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import kotlinx.coroutines.*
import org.json.*

private data class PendingAssistantImage(val id:String,val name:String,val thumbnail:ImageBitmap?=null,val token:String="",val error:String="")

@Composable fun Assistant(api:Api,carId:Any,account:String="",onBack:()->Unit={}){
 val scope=rememberCoroutineScope();val context=LocalContext.current
 var composerHeight by remember{mutableIntStateOf(0)}
 val composerInset=with(LocalDensity.current){composerHeight.toDp()}
 val assistantGlass=remember{dev.chrisbanes.haze.HazeState()}
 val memory=remember(carId){api.chats.getOrPut(carId.toString()){ChatMemory()}}
 var conversation by memory.conversation;val messages=memory.messages;val metadata=memory.metadata
 var input by remember(carId){mutableStateOf("")};var sending by remember{mutableStateOf(false)};var status by remember{mutableStateOf("")};var failed by remember{mutableStateOf(false)}
 var historyOpen by remember{mutableStateOf(false)};var history by remember{mutableStateOf<JSONArray?>(null)};var historyBusy by remember{mutableStateOf(false)};var historyError by remember{mutableStateOf("")}
 var job by remember{mutableStateOf<Job?>(null)};var welcomeJob by remember{mutableStateOf<Job?>(null)};var welcome by remember{mutableStateOf("")};var welcomeOptions by remember{mutableStateOf<JSONArray?>(null)};var welcomeRevision by remember{mutableIntStateOf(0)}
 val attachments=remember(carId){mutableStateListOf<PendingAssistantImage>()};val uploads=remember{mutableMapOf<String,Job>()}
 val action=rememberServerAction();var pending by remember{mutableStateOf<ServerConfirmation?>(null)}
 fun loadHistory(){if(historyBusy)return;scope.launch{historyBusy=true;historyError="";try{history=api.call("/car/assistant/conversation/list",JSONObject().put("carId",carId)).optJSONArray("data")?:JSONArray()}catch(e:Exception){if(e is CancellationException)throw e;historyError=e.message?:"历史读取失败"}finally{historyBusy=false}}}
 val picker=rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()){uris->
  val available=(3-attachments.size).coerceAtLeast(0)
  uris.take(available).forEach{uri->
   val id=java.util.UUID.randomUUID().toString();val name=runCatching{context.contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use{if(it.moveToFirst())it.getString(0)else null}}.getOrNull()?:"image"
   attachments.add(PendingAssistantImage(id,name))
   uploads[id]=scope.launch{try{
    val bytes=withContext(Dispatchers.IO){context.contentResolver.openInputStream(uri)?.use{it.readCapped(5*1024*1024)}?:throw Exception("无法读取图片")}
    val thumb=withContext(Dispatchers.Default){assistantThumbnail(bytes)}?:throw Exception("无法解码图片")
    val bounds=android.graphics.BitmapFactory.Options().apply{inJustDecodeBounds=true};android.graphics.BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds);val mime=bounds.outMimeType.orEmpty();val indexBefore=attachments.indexOfFirst{it.id==id};if(indexBefore>=0)attachments[indexBefore]=attachments[indexBefore].copy(thumbnail=thumb);val result=api.uploadAssistant(carId,name,mime,bytes);val token=result.safeText("attachmentToken");require(token.isNotBlank()){"上传未返回图片凭据"}
    val index=attachments.indexOfFirst{it.id==id};if(index>=0)attachments[index]=PendingAssistantImage(id,name,thumb,token)
   }catch(e:Exception){if(e is CancellationException)throw e;val index=attachments.indexOfFirst{it.id==id};if(index>=0)attachments[index]=PendingAssistantImage(id,name,error=e.message?:"上传失败")}finally{uploads.remove(id)}}
  }
  if(uris.size>available)status="每次最多发送3张图片，每张不超过5MB"
 }

 fun clearAttachments(){uploads.values.toList().forEach{it.cancel()};uploads.clear();attachments.clear()}
 fun newConversation(){job?.cancel();welcomeJob?.cancel();api.stopStream();conversation="";messages.clear();metadata.clear();memory.localImages.clear();memory.bootstrapToken="";sending=false;input="";status="";clearAttachments();welcomeRevision++}
 DisposableEffect(carId){onDispose{job?.cancel();welcomeJob?.cancel();uploads.values.toList().forEach{it.cancel()};api.stopStream()}}
 LaunchedEffect(carId,welcomeRevision){if(conversation.isNotBlank()||messages.isNotEmpty())return@LaunchedEffect
  welcome="";welcomeOptions=null
  welcomeJob=scope.launch{try{api.streamEndpoint("/car/assistant/welcome",JSONObject().put("carId",carId)){event->if(messages.isEmpty())when(event.safeText("type")){
   "bootstrap"->memory.bootstrapToken=event.safeText("bootstrapToken")
   "delta"->welcome+=event.safeText("text")
   "message"->{welcome=event.safeText("text");welcomeOptions=event.optJSONArray("options")}
   "options"->welcomeOptions=event.optJSONArray("options")
  }}}catch(e:Exception){if(e is CancellationException)throw e;/* Welcome failure must not block chat. */}}
 }
 fun send(){
  if(sending)return;val text=input.trim()
  when(text.lowercase()){
   "/new"->{newConversation();return};"/history"->{input="";historyOpen=true;loadHistory();return};"/upload"->{input="";picker.launch("image/*");return};"/clear"->{input="";clearAttachments();return};"/help"->{input="";status="/new 新会话 · /history 历史 · /upload 图片 · /clear 清空";return}
  }
  if(text.isBlank()&&attachments.isEmpty())return
  if(attachments.any{it.token.isBlank()}){status="请等待图片上传完成，或移除失败的图片";return}
  val selected=attachments.toList();input="";attachments.clear();messages.add("user" to text);val userIndex=messages.lastIndex
  memory.localImages[userIndex]=selected.mapNotNull{it.thumbnail};messages.add("assistant" to "");var replyIndex=messages.lastIndex
  sending=true;failed=false;status="正在思考…"
  job=scope.launch{try{
   welcomeJob?.cancel();api.stopStream();welcomeJob?.join()
   val body=JSONObject().put("carId",carId).put("requestType","chat").put("content",text).put("attachmentTokens",JSONArray(selected.map{it.token}))
   if(conversation.isNotBlank())body.put("conversationId",conversation)else if(memory.bootstrapToken.isNotBlank())body.put("bootstrapToken",memory.bootstrapToken)
   api.stream(body){event->
    fun meta()=JSONObject(metadata[replyIndex]?.toString()?:"{}")
    when(event.safeText("type")){
     "connected"->conversation=event.safeText("conversationId",conversation)
     "progress"->status=event.safeText("text")
     "delta"->{messages[replyIndex]="assistant" to (messages[replyIndex].second+event.safeText("text"));status=""}
     "message"->{val sourceMeta=meta();for(key in listOf("sources","references","citations","evidence")){event.optJSONArray(key)?.let{sourceMeta.put(key,it)}};metadata[replyIndex]=sourceMeta;val text=event.safeText("text");if(text.isNotBlank()){if(messages[replyIndex].second.isNotBlank()&&messages[replyIndex].second!=text){messages.add("assistant" to text);replyIndex=messages.lastIndex}else messages[replyIndex]="assistant" to text};event.optJSONArray("options")?.let{metadata[replyIndex]=meta().put("options",it)};status=""}
     "options"->event.optJSONArray("options")?.let{metadata[replyIndex]=meta().put("options",it)}
     "media_pending"->metadata[replyIndex]=meta().put("mediaPending",true)
     "media"->{metadata[replyIndex]=meta().put("mediaPending",false).put("media",event.optJSONArray("media"));status=""}
     "error"->{failed=true;status=event.safeText("text","助手暂不可用");metadata[replyIndex]=meta().put("mediaPending",false)}
    }
   }
  }catch(e:Exception){if(e !is CancellationException){failed=true;status=e.message?:"助手连接失败"}}finally{sending=false;if(status=="正在思考…")status="";metadata[replyIndex]?.let{metadata[replyIndex]=JSONObject(it.toString()).put("mediaPending",false)}}}
 }
 if(historyOpen)FlowDetailPanel({if(!historyBusy)historyOpen=false},title={Text("历史会话")},body={Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
  if(historyBusy)FlowLoading(label="正在读取会话…")
  if(historyError.isNotBlank()){Text(historyError,fontSize=12.sp,color=MaterialTheme.colorScheme.error);CinemaOutlineButton({loadHistory()}){Text("重新读取")}}
  if(history?.length()==0&&!historyBusy)Text("还没有历史会话",fontSize=13.sp,color=muted)
  history?.objects()?.forEach{entry->Panel{
   Text(entry.safeText("title","新会话"),fontSize=14.sp,color=ink)
   Text(prettyTime(entry.safeText("lastMessageAt",entry.safeText("created"))),fontSize=11.sp,color=muted)
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
    CinemaOutlineButton({action.run{
     val id=entry.safeText("conversationId");val restored=api.call("/car/assistant/conversation/detail",JSONObject().put("conversationId",id)).optJSONObject("data")?:throw Exception("未获得会话")
     welcomeJob?.cancel();api.stopStream();welcomeJob?.join();messages.clear();metadata.clear();memory.localImages.clear()
     restored.optJSONArray("messages")?.objects()?.forEachIndexed{i,m->messages.add((if(m.safeText("role").equals("USER",true))"user"else"assistant") to m.safeText("content"));metadata[i]=m}
     conversation=id;historyOpen=false;status=""
    }},Modifier.weight(1f),enabled=!action.busy&&!sending){Text("打开会话")}
    CinemaTextButton({pending=ServerConfirmation("删除会话","永久删除“${entry.safeText("title","新会话")}”及其聊天记录。"){
     val id=entry.safeText("conversationId");api.call("/car/assistant/conversation/delete",JSONObject().put("conversationId",id));if(conversation==id)newConversation();loadHistory()
    }},enabled=!action.busy&&!sending){Text("删除",color=MaterialTheme.colorScheme.error)}
   }
  }}
  ServerNotice(action)
 }},confirm={CinemaTextButton({historyOpen=false},enabled=!action.busy){Text("完成")}})
 ConfirmServerChange(pending,action){pending=null}
 Box(Modifier.fillMaxSize()){
  Box(Modifier.matchParentSize().hazeSource(assistantGlass,zIndex=0f).background(androidx.compose.ui.graphics.Brush.verticalGradient(LocalPalette.current.background)))
  CompositionLocalProvider(LocalControlBackdrop provides assistantGlass){
 Box(Modifier.fillMaxSize().imePadding().navigationBarsPadding()){
  val scroll=rememberScrollState();val scrolled by remember(scroll){derivedStateOf{scroll.value>1}};val motion=app.meridian.mobile.glass.LocalAppMotion.current
  LaunchedEffect(messages.size,messages.lastOrNull()?.second,composerHeight){withFrameNanos{};if(motion.full)scroll.animateScrollTo(scroll.maxValue)else scroll.scrollTo(scroll.maxValue)}
  Box(Modifier.fillMaxSize()){
  Column(Modifier.fillMaxSize().hazeSource(assistantGlass,zIndex=1f).verticalScroll(scroll).padding(horizontal=18.dp).padding(top=WindowInsets.statusBars.asPaddingValues().calculateTopPadding()+72.dp,bottom=composerInset+18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
   if(messages.isEmpty()){
    Column(Modifier.fillMaxWidth().padding(vertical=20.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)){AssistantOrb(size=58.dp);Text("今天想了解什么？",fontSize=24.sp,fontWeight=FontWeight.Medium,color=ink)}
    if(welcome.isNotBlank())Panel{MarkdownMessage(welcome)}
    val suggestions=(welcomeOptions?.objects()?.map{it.safeText("text") }?.filter{it.isNotBlank()}.orEmpty()+listOf("分析最近的用车表现","当前车辆有哪些状态需要关注？","最近哪些行程最耗电？","帮我分析充电记录和补能习惯","这个月的里程和上个月相比如何？","总结我的辅助驾驶使用情况")).distinct().take(8)
    AssistantSuggestions(suggestions){input=it}
   }
   messages.forEachIndexed{i,(role,text)->
    if(text.isNotBlank())ChatBubble(role,text,account,api.base)
    if(role!="user"&&text.isNotBlank())AssistantEvidence(api,metadata[i]?:JSONObject())
    memory.localImages[i]?.forEach{image->Image(image,"发送的图片",Modifier.fillMaxWidth(.7f).heightIn(max=180.dp).clip(RoundedCornerShape(16.dp)))}
    metadata[i]?.let{m->
     if(m.optBoolean("mediaPending"))FlowLoading(label="正在准备图片…")
     m.optJSONArray("media")?.takeIf{it.length()>0}?.let{AssistantMediaGallery(api,conversation,it)}
     m.optJSONArray("options")?.objects()?.map{it.safeText("text")}?.filter{it.isNotBlank()}?.let{AssistantSuggestions(it){input=it}}
   }
   }
   if(sending&&!failed)FlowLoading(label=status.ifBlank{"正在生成回答…"})else if(status.isNotBlank())Text(status,fontSize=12.sp,lineHeight=19.sp,color=if(failed)MaterialTheme.colorScheme.error else muted)
  }
  PinnedSectionToolbar(assistantGlass,Modifier.align(Alignment.TopCenter),scrolled){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
   QuietIconButton(Icons.Outlined.ArrowBack,"返回",onBack)
   Box(Modifier.weight(1f)){ToolbarTitleCapsule{Text("AI 助手",fontSize=17.sp)}}
   ToolbarActionStrip(listOf(ToolbarAction(Icons.Outlined.History,"历史会话",!sending){historyOpen=true;loadHistory()},ToolbarAction(Icons.Outlined.EditNote,"新会话",!sending){newConversation()}))
  }}
  }

  Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().onSizeChanged{composerHeight=it.height}.background(androidx.compose.ui.graphics.Brush.verticalGradient(0f to androidx.compose.ui.graphics.Color.Transparent,.24f to LocalPalette.current.background[0].copy(alpha=.92f),1f to LocalPalette.current.background[0])).padding(top=18.dp)){
  if(attachments.isNotEmpty())Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal=18.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){
   attachments.toList().forEach{item->Column(Modifier.width(100.dp).background(glassWhite,RoundedCornerShape(16.dp)).padding(8.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
    item.thumbnail?.let{Image(it,item.name,Modifier.fillMaxWidth().height(56.dp))}
    Text(if(item.error.isNotBlank())item.error else if(item.token.isBlank())"上传中…"else item.name,fontSize=11.sp,lineHeight=16.sp,color=muted,maxLines=2)
    CinemaTextButton({uploads.remove(item.id)?.cancel();attachments.removeAll{it.id==item.id}}){Text("移除",fontSize=11.sp)}
   }}
  }
  Row(Modifier.padding(horizontal=14.dp,vertical=10.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(glassWhite).border(.7.dp,glassEdge,RoundedCornerShape(24.dp)).padding(5.dp),verticalAlignment=Alignment.Bottom,horizontalArrangement=Arrangement.spacedBy(2.dp)){
   CinemaIconButton({picker.launch("image/*")},enabled=!sending&&attachments.size<3){Icon(Icons.Outlined.Add,"添加图片",Modifier.size(20.dp))}
   OutlinedTextField(input,{input=it.take(2000)},placeholder={Text("聊聊你的用车问题…",fontSize=13.sp)},modifier=Modifier.weight(1f),maxLines=5,bare=true,keyboardOptions=KeyboardOptions(imeAction=ImeAction.Send),keyboardActions=KeyboardActions(onSend={send()}))
   AssistantSendButton({if(sending){job?.cancel();api.stopStream();status="已停止生成"}else send()},enabled=sending||input.isNotBlank()||attachments.isNotEmpty()){
    Icon(if(sending)Icons.Outlined.Stop else Icons.Outlined.ArrowUpward,if(sending)"停止生成"else"发送",Modifier.size(20.dp),tint=themeOnAccent(accent))
   }
  }
  }
 }
 }}
}
