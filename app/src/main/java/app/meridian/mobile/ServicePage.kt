package app.meridian.mobile
import android.annotation.SuppressLint
import androidx.compose.foundation.background
import android.webkit.*
import android.net.Uri
import android.content.Intent
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@SuppressLint("SetJavaScriptEnabled","ClickableViewAccessibility")
@Composable fun ServicePage(api:Api,path:String,title:String,onClose:()->Unit){
 val context=LocalContext.current;val official=path.startsWith("https://");val url=if(official)path else api.base+path;val host=Uri.parse(url).host
 var pulled by remember{mutableStateOf(false)};var syncTime by remember{mutableLongStateOf(0L)}
 var ready by remember(url){mutableStateOf(official)};var loading by remember(url){mutableStateOf(true)};var error by remember(url){mutableStateOf("")};var view by remember{mutableStateOf<WebView?>(null)}
 LaunchedEffect(loading,pulled){if(pulled&&!loading){kotlinx.coroutines.delay(1800);pulled=false}}
 LaunchedEffect(url){if(!official){val manager=android.webkit.CookieManager.getInstance();manager.setAcceptCookie(true);for(cookie in api.webCookies())suspendCancellableCoroutine<Unit>{c->manager.setCookie(api.base,cookie){if(c.isActive)c.resume(Unit)}};manager.flush();ready=true}}
 Column(Modifier.fillMaxSize().background(LocalPalette.current.background[0]).navigationBarsPadding()){
 NativePageHeader(title,onClose)
 if(pulled)Text((if(loading)"正在刷新"else"刷新结束")+if(syncTime>0)" · 上次同步 "+java.time.Instant.ofEpochMilli(syncTime).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm"))else"",modifier=Modifier.padding(horizontal=18.dp),fontSize=12.sp,color=muted)
 if(loading)FlowLoading(Modifier.fillMaxWidth())
 if(error.isNotBlank())Text(error,modifier=Modifier.padding(12.dp),color=MaterialTheme.colorScheme.error)
 if(ready)AndroidView(factory={ctx->WebView(ctx).apply{view=this;settings.javaScriptEnabled=true;settings.domStorageEnabled=true;settings.allowFileAccess=false;settings.allowContentAccess=false;settings.mixedContentMode=WebSettings.MIXED_CONTENT_NEVER_ALLOW;settings.mediaPlaybackRequiresUserGesture=false;settings.builtInZoomControls=true;settings.displayZoomControls=false;settings.setSupportZoom(true);settings.safeBrowsingEnabled=true
 var downY=0f;var eligible=false;var swiped=false;setOnTouchListener{target,event->when(event.actionMasked){android.view.MotionEvent.ACTION_DOWN->{downY=event.y;eligible=scrollY==0;swiped=false;false};android.view.MotionEvent.ACTION_MOVE->{if(eligible&&!loading&&event.y-downY>100*resources.displayMetrics.density){swiped=true;eligible=false;pulled=true;loading=true;error="";reload();true}else swiped};android.view.MotionEvent.ACTION_UP,android.view.MotionEvent.ACTION_CANCEL->{val consumed=swiped;swiped=false;eligible=false;consumed};else->swiped}}
 webViewClient=object:WebViewClient(){override fun onPageFinished(v:WebView?,u:String?){loading=false;syncTime=System.currentTimeMillis()};override fun onReceivedError(v:WebView?,request:WebResourceRequest?,e:WebResourceError?){if(request?.isForMainFrame==true){loading=false;error="页面暂时无法加载，请重试"}};override fun shouldOverrideUrlLoading(v:WebView?,request:WebResourceRequest?):Boolean{val target=request?.url?:return true;if(target.scheme!="https")return true;if(target.host==host)return false;if(request.hasGesture()){try{context.startActivity(Intent(Intent.ACTION_VIEW,target))}catch(_:Exception){}};return true}}
 loadUrl(url,mapOf("X-Client-Source" to "web"))}},modifier=Modifier.fillMaxWidth().weight(1f))
 }
 DisposableEffect(url){onDispose{view?.apply{stopLoading();loadUrl("about:blank");onPause();destroy()};view=null}}
}
