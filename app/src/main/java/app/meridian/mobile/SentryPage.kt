package app.meridian.mobile
import androidx.compose.ui.graphics.graphicsLayer

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.SurfaceView
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoCanvas
import io.agora.rtc2.internal.EncryptionConfig
import kotlinx.coroutines.*
import org.json.JSONObject

/** Receive-only video session. Never publishes local camera or microphone tracks. */
class SentryController(private val context:Context,private val api:Api,private val carId:Any){
 private val appContext=context.applicationContext
 var session by mutableStateOf<JSONObject?>(null);private set
 var busy by mutableStateOf(false);private set
 var playing by mutableStateOf(false);private set
 var camera by mutableIntStateOf(1);private set
 var issue by mutableStateOf("");private set
 var status by mutableStateOf("远程查看未开启");private set
 private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
 private var job:Job?=null;private var heartbeat:Job?=null;private var engine:RtcEngine?=null;private var generation=0
 private var media:JSONObject?=null;private var host:FrameLayout?=null;private var frame:SurfaceView?=null;private var selectedUid:Int?=null;private var closing=false
 val cameras:List<JSONObject> get()=media?.optJSONArray("remoteCameras")?.objects()?.filter{it.has("cameraId")&&it.has("uid")}?:emptyList()
 private fun body(ref:String)=JSONObject().put("carId",carId).put("sessionRef",ref)
 private fun uid(value:Any?):Int?=value?.toString()?.toLongOrNull()?.takeIf{it in 1..4294967295L}?.toInt()
 fun attach(view:FrameLayout){host=view;bind()}
 private fun bind(){val e=engine?:return;val container=host?:return;val remote=uid(cameras.firstOrNull{it.optInt("cameraId")==camera}?.opt("uid"))?:return
  e.muteAllRemoteVideoStreams(true);e.muteRemoteVideoStream(remote,false);if(selectedUid==remote&&frame?.parent===container)return;selectedUid=remote;container.removeAllViews();val surface=SurfaceView(container.context);frame=surface;container.addView(surface,FrameLayout.LayoutParams(-1,-1));e.setupRemoteVideo(VideoCanvas(surface,Constants.RENDER_MODE_FIT,remote))
 }
 private suspend fun release(){val e=engine;engine=null;media=null;selectedUid=null;playing=false;host?.removeAllViews();frame=null;if(e!=null){e.leaveChannel();withContext(Dispatchers.IO){RtcEngine.destroy()}}}
 private suspend fun connect(next:JSONObject,epoch:Int){
  if(epoch!=generation)return
  val old=media
  if(engine!=null&&old?.safeText("appId")==next.safeText("appId")&&old.safeText("channel")==next.safeText("channel")){
   if(old.safeText("token")!=next.safeText("token"))check(engine!!.renewToken(next.safeText("token"))>=0){"视频会话续期失败"};media=next;bind();return
  }
  release();if(epoch!=generation)return
  require(next.safeText("appId").isNotBlank()&&next.safeText("channel").isNotBlank()&&next.safeText("token").isNotBlank()){ "服务未返回完整视频会话" }
  // RtcEngineConfig exposes getContext(); using `context` inside apply resolves to
  // that initially-null receiver property rather than the controller's Context.
  val config=RtcEngineConfig().apply{mContext=appContext;mAppId=next.safeText("appId");mEventHandler=object:IRtcEngineEventHandler(){
   override fun onJoinChannelSuccess(channel:String?,uid:Int,elapsed:Int){scope.launch{if(epoch==generation){status="等待车辆画面";session?.safeText("sessionRef")?.takeIf{it.isNotBlank()}?.let{try{api.call("/car/sentry/join-state",body(it).put("joinCostTimeMillisecond",elapsed))}catch(e:Exception){if(e is CancellationException)throw e}}}}}
   override fun onUserJoined(uid:Int,elapsed:Int){scope.launch{if(epoch==generation)bind()}}
   override fun onFirstRemoteVideoDecoded(uid:Int,width:Int,height:Int,elapsed:Int){scope.launch{if(epoch==generation&&uid==selectedUid){playing=true;status="实时查看中"}}}
   override fun onRemoteVideoStateChanged(uid:Int,state:Int,reason:Int,elapsed:Int){scope.launch{if(epoch==generation&&uid==selectedUid){playing=state==2;status=if(playing)"实时查看中"else"等待车辆画面"}}}
   override fun onTokenPrivilegeWillExpire(token:String?){scope.launch{if(epoch==generation)refresh(true)}}
   override fun onRequestToken(){scope.launch{if(epoch==generation)refresh(true)}}
   override fun onError(err:Int){scope.launch{if(epoch==generation){issue="视频连接异常（$err），请关闭后重试";playing=false}}}
  }}
  val e=RtcEngine.create(config);engine=e;media=next;e.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);e.setClientRole(Constants.CLIENT_ROLE_BROADCASTER);e.enableVideo();e.disableAudio();e.enableLocalVideo(false);e.enableLocalAudio(false)
  val mode=next.safeText("encryptionMode");val secret=next.safeText("encryptionSecret")
  if(mode.isNotBlank()){
   require(secret.isNotBlank()){ "视频加密信息缺失" }
   val encryption=EncryptionConfig().apply{encryptionKey=secret;encryptionMode=try{EncryptionConfig.EncryptionMode.valueOf(mode.uppercase(java.util.Locale.ROOT).replace('-','_'))}catch(_:Exception){throw IllegalArgumentException("当前视频加密方式暂不支持")}}
   if(mode.endsWith("gcm2",true)){val salt=next.safeText("encryptionSalt",next.safeText("encryptionKdfSalt"));require(salt.isNotBlank()){ "服务未返回视频加密盐值" };val decoded=android.util.Base64.decode(salt,android.util.Base64.DEFAULT);require(decoded.size==32){"视频加密参数无效"};System.arraycopy(decoded,0,encryption.encryptionKdfSalt,0,32)}
   check(e.enableEncryption(true,encryption)>=0){"视频加密初始化失败"}
  }
  val options=ChannelMediaOptions().apply{channelProfile=Constants.CHANNEL_PROFILE_LIVE_BROADCASTING;clientRoleType=Constants.CLIENT_ROLE_BROADCASTER;publishCameraTrack=false;publishMicrophoneTrack=false;autoSubscribeAudio=false;autoSubscribeVideo=false}
  check(e.joinChannel(next.safeText("token"),next.safeText("channel"),uid(next.opt("uid"))?:0,options)>=0){"无法加入车辆视频会话"}
  if(cameras.none{it.optInt("cameraId")==camera})camera=cameras.firstOrNull()?.optInt("cameraId")?:1
  bind()
 }
 private suspend fun accept(next:JSONObject,epoch:Int){if(epoch!=generation)return;session=next;when(next.safeText("status")){"READY"->{val m=next.optJSONObject("media")?:throw Exception("服务未返回视频参数");connect(m,epoch)};"PENDING"->{status=next.safeText("message","车辆正在准备画面")};"UNAVAILABLE"->throw Exception(next.safeText("message","当前车辆无法远程查看"));else->throw Exception(next.safeText("message","远程会话已结束"))}}
 private suspend fun waitReady(initial:JSONObject,epoch:Int){var next=initial;accept(next,epoch);repeat(55){if(next.safeText("status")!="PENDING"||epoch!=generation)return;delay(2000);next=api.call("/car/sentry/status",body(next.safeText("sessionRef"))).optJSONObject("data")?:throw Exception("无法读取视频会话");accept(next,epoch)};if(next.safeText("status")=="PENDING")throw Exception("车辆连接超时，请稍后重试")}
 fun start(){if(busy||session!=null||closing)return;generation++;val epoch=generation;busy=true;issue="";status="正在唤醒车辆视频";job=scope.launch{try{
  val next=withContext(NonCancellable){val result=api.call("/car/sentry/start",JSONObject().put("carId",carId).put("cameraId",1)).optJSONObject("data")?:throw Exception("未获得车辆视频会话");if(epoch!=generation){try{api.call("/car/sentry/stop",body(result.safeText("sessionRef")))}catch(_:Exception){};null}else result}?:return@launch
  waitReady(next,epoch);heartbeat=scope.launch{while(isActive&&epoch==generation){delay(15000);refresh(false)}}
 }catch(e:Exception){if(e is CancellationException)throw e;if(epoch==generation){issue=e.message?:"开启远程查看失败";end(false)}}finally{if(epoch==generation)busy=false}}}
 private suspend fun refresh(force:Boolean){val current=session?:return;val epoch=generation;try{val next=api.call("/car/sentry/status",body(current.safeText("sessionRef")).put("refreshSession",force)).optJSONObject("data")?:throw Exception("会话刷新失败");waitReady(next,epoch)}catch(e:Exception){if(e is CancellationException)throw e;if(epoch==generation){issue=e.message?:"会话刷新失败";end(false)}}}
 fun switchCamera(id:Int){if(busy||id==camera||session?.safeText("status")!="READY")return;val current=session?:return;val epoch=generation;val previous=camera;busy=true;playing=false;status="正在切换视角";job=scope.launch{try{val next=api.call("/car/sentry/switch-camera",body(current.safeText("sessionRef")).put("cameraId",id)).optJSONObject("data")?:throw Exception("视角切换失败");camera=id;waitReady(next,epoch);bind()}catch(e:Exception){if(e is CancellationException)throw e;if(epoch==generation){camera=previous;bind();issue=e.message?:"视角切换失败"}}finally{if(epoch==generation)busy=false}}}
 private suspend fun end(disableSentry:Boolean)=withContext(NonCancellable){val current=session;session=null;heartbeat?.cancel();heartbeat=null;release();status="远程查看未开启";if(current!=null){try{api.call("/car/sentry/stop",body(current.safeText("sessionRef")).put("disableAutoEnabledSentry",disableSentry))}catch(e:Exception){if(e is CancellationException)throw e;issue="本机播放已停止，车辆会话关闭失败，请重试或检查官方 App"}}}
 fun stop(disableSentry:Boolean=false,onDone:()->Unit={}){if(closing)return;closing=true;generation++;job?.cancel();job=null;busy=true;scope.launch{try{end(disableSentry)}finally{busy=false;closing=false;onDone()}}}
 fun dispose(){stop(false){scope.cancel()}}
}

@Composable fun SentryPage(api:Api,carId:Any,onBack:()->Unit){
 val (pageProgress,animatedBack)=rememberPageMotion(onBack);val fullMotion=app.meridian.mobile.glass.LocalAppMotion.current.full
 val context=LocalContext.current;val owner=LocalLifecycleOwner.current;val controller=remember(api,carId){SentryController(context,api,carId)};var confirm by remember{mutableStateOf(false)};var leave by remember{mutableStateOf(false)};var full by remember{mutableStateOf(false)}
 fun close(exit:Boolean){if(controller.session!=null||controller.busy){leave=exit;confirm=true}else if(exit)animatedBack()}

 DisposableEffect(controller,owner){val observer=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_STOP)controller.stop(false)};owner.lifecycle.addObserver(observer);onDispose{owner.lifecycle.removeObserver(observer);controller.dispose()}}
 val chromeView=androidx.compose.ui.platform.LocalView.current;val chromeDark=LocalPalette.current.dark
 SideEffect{chromeView.context.hostActivity()?.window?.let{applyImmersiveBars(it,chromeView,chromeDark||full)}}
 DisposableEffect(Unit){onDispose{chromeView.context.hostActivity()?.window?.let{applyImmersiveBars(it,chromeView,chromeDark)}}}
 PredictiveSceneFrame(onBack={if(full)full=false else if(controller.session!=null||controller.busy)close(true)else onBack()},completeExitOnBack=!full&&controller.session==null&&!controller.busy){
 Column(Modifier.fillMaxSize().scenePresence(pageProgress).background(if(full)Color(0xFF080E16)else LocalPalette.current.background[0]).navigationBarsPadding()){
  if(!full)NativePageHeader("远程查看",{close(true)})
  Column(Modifier.fillMaxSize().then(if(!full)Modifier.verticalScroll(rememberScrollState()).padding(horizontal=18.dp)else Modifier.statusBarsPadding()),verticalArrangement=Arrangement.spacedBy(14.dp)){
   if(!full){Text("守护每一次停泊",fontSize=12.sp,color=muted);Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(7.dp)){Box(Modifier.size(6.dp).background(if(controller.playing)Color(0xFF73A99B)else muted,RoundedCornerShape(3.dp)));Text(controller.status,fontSize=12.sp,color=ink)}}
   Box(Modifier.fillMaxWidth().then(if(full)Modifier.weight(1f)else Modifier.height(260.dp)).clip(RoundedCornerShape(if(full)0.dp else 26.dp)).background(Color(0xFF080E16)),contentAlignment=Alignment.Center){
    AndroidView(factory={ctx->FrameLayout(ctx).apply{controller.attach(this)}},modifier=Modifier.fillMaxSize())
    if(!controller.playing)Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp),modifier=Modifier.padding(24.dp)){Icon(Icons.Outlined.Videocam,null,tint=Color(0xFF8EA4BA),modifier=Modifier.size(38.dp));if(controller.busy)FlowSpinner(Modifier.size(20.dp),color=Color(0xFF9AB9D0),strokeWidth=2.dp);Text(controller.status,color=Color(0xFFDCE5EF),fontSize=14.sp)}
    Box(Modifier.align(Alignment.TopEnd).padding(8.dp).size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color.Black.copy(alpha=.38f)).clickable{full=!full},contentAlignment=Alignment.Center){Icon(if(full)Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen,if(full)"退出全屏"else"全屏",tint=Color.White,modifier=Modifier.size(21.dp))}
   }
   val cameras=controller.cameras;val labels=if(cameras.size==6)listOf("前","后","左上","右上","左下","右下")else listOf("前","后","左","右")
   if(cameras.isNotEmpty())Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal=if(full)18.dp else 0.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){cameras.sortedBy{it.optInt("cameraId")}.forEachIndexed{i,c->FilterChip(controller.camera==c.optInt("cameraId"),{controller.switchCamera(c.optInt("cameraId"))},label={Text(labels.getOrElse(i){"视角 ${i+1}"})})}}
   if(controller.issue.isNotBlank())Text(controller.issue,color=MaterialTheme.colorScheme.error,fontSize=12.sp,modifier=Modifier.padding(horizontal=if(full)18.dp else 0.dp))
   if(controller.session==null&&!controller.busy)Button(onClick={controller.start()},modifier=Modifier.fillMaxWidth()){Icon(Icons.Outlined.PlayArrow,null,modifier=Modifier.size(19.dp));Spacer(Modifier.width(6.dp));Text("开启远程查看")}
   else OutlinedButton(onClick={close(false)},modifier=Modifier.fillMaxWidth()){Text(if(controller.busy)"取消连接"else"结束查看")}
   if(!full)Panel{SectionTitle("车辆视频会话",Icons.Outlined.Security);Text("仅接收车辆摄像头画面。离开页面或退到后台时停止播放，并请求结束会话。",fontSize=12.sp,color=muted);controller.session?.safeText("expiresAt")?.takeIf{it.isNotBlank()}?.let{Text("有效至 "+prettyTime(it),fontSize=11.sp,color=muted)}}
  }
 }
 }
 if(confirm)AlertDialog(onDismissRequest={confirm=false},title={Text("结束远程查看？")},text={Text(if(controller.session?.optBoolean("shouldConfirmCloseSentry")==true)"本次查看自动开启了哨兵模式，你可以保留它继续守护车辆。"else"关闭视频连接后，车辆哨兵状态保持不变。",fontSize=14.sp,color=muted)},confirmButton={TextButton(onClick={confirm=false;controller.stop(false){if(leave)animatedBack()}}){Text("结束查看 · 保留哨兵")}},dismissButton={if(controller.session?.optBoolean("shouldConfirmCloseSentry")==true)TextButton(onClick={confirm=false;controller.stop(true){if(leave)animatedBack()}}){Text("一并关闭哨兵")}})
}
