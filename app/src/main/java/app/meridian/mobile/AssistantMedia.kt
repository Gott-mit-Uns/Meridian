package app.meridian.mobile

import android.graphics.BitmapFactory
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.*
import kotlinx.coroutines.*
import org.json.*

internal fun assistantThumbnail(bytes:ByteArray):ImageBitmap?{
 val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true};BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds)
 if(bounds.outWidth<=0||bounds.outHeight<=0)return null
 val options=BitmapFactory.Options().apply{var n=1;while(maxOf(bounds.outWidth,bounds.outHeight)/n>1800)n*=2;inSampleSize=n}
 var bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.size,options)?:return null
 val orientation=runCatching{android.media.ExifInterface(java.io.ByteArrayInputStream(bytes)).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION,1)}.getOrDefault(1)
 val transform=android.graphics.Matrix().apply{when(orientation){2->postScale(-1f,1f);3->postRotate(180f);4->{postRotate(180f);postScale(-1f,1f)};5->{postRotate(90f);postScale(-1f,1f)};6->postRotate(90f);7->{postRotate(270f);postScale(-1f,1f)};8->postRotate(270f)}}
 if(!transform.isIdentity)bitmap=android.graphics.Bitmap.createBitmap(bitmap,0,0,bitmap.width,bitmap.height,transform,true)
 return bitmap.asImageBitmap()
}

@Composable internal fun AssistantMediaGallery(api:Api,conversation:String,refs:JSONArray){
 var images by remember(conversation,refs.toString()){mutableStateOf<Map<String,ImageBitmap>>(emptyMap())};var loading by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")};var retry by remember{mutableIntStateOf(0)};var expanded by remember{mutableStateOf<ImageBitmap?>(null)}
 LaunchedEffect(conversation,refs.toString(),retry){
  if(conversation.isBlank())return@LaunchedEffect;loading=true;error=""
  try{
   val result=linkedMapOf<String,ImageBitmap>()
   for((message,items)in refs.objects().groupBy{it.safeText("messageId")}){
    require(message.isNotBlank()){"图片缺少消息编号"};val parts=api.assistantMedia(conversation,message)
    for(item in items){val key="media-"+item.optInt("index");val bytes=parts[key]?:continue;withContext(Dispatchers.Default){assistantThumbnail(bytes)}?.let{result[message+":"+item.optInt("index")]=it}}
   };images=result;if(result.isEmpty())throw Exception("当前消息暂无可用图片")
  }catch(e:Exception){if(e is CancellationException)throw e;error=e.message?:"图片加载失败"}finally{loading=false}
 }
 if(loading)FlowLoading(label="正在读取图片…")
 if(error.isNotBlank())Column(verticalArrangement=Arrangement.spacedBy(8.dp)){Text(error,fontSize=12.sp,color=muted);CinemaOutlineButton({retry++}){Text("重新读取图片")}}
 refs.objects().forEach{ref->val bitmap=images[ref.safeText("messageId")+":"+ref.optInt("index")];if(bitmap!=null)Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
  Image(bitmap,ref.safeText("label","助手图片"),Modifier.fillMaxWidth().heightIn(max=280.dp).clip(RoundedCornerShape(18.dp)).clickable(onClickLabel="放大图片"){expanded=bitmap})
  ref.safeText("label").takeIf{it.isNotBlank()}?.let{Text(it,fontSize=11.sp,color=muted)}
 }}
 expanded?.let{ZoomableAssistantImage(it){expanded=null}}
}

@Composable internal fun ZoomableAssistantImage(image:ImageBitmap,onClose:()->Unit){
 var scale by remember{mutableFloatStateOf(1f)};var x by remember{mutableFloatStateOf(0f)};var y by remember{mutableFloatStateOf(0f)}
 FullscreenDataPage("图片预览",onClose){
  Text("双指缩放，拖动查看细节",fontSize=12.sp,color=muted)
  Box(Modifier.fillMaxWidth().height(430.dp).clip(RoundedCornerShape(20.dp)).background(softWash).pointerInput(Unit){detectTransformGestures{_,pan,zoom,_->scale=(scale*zoom).coerceIn(1f,5f);x=(x+pan.x).coerceIn(-size.width*(scale-1)/2,size.width*(scale-1)/2);y=(y+pan.y).coerceIn(-size.height*(scale-1)/2,size.height*(scale-1)/2)}},contentAlignment=Alignment.Center){Image(image,"图片",Modifier.fillMaxSize().graphicsLayer{scaleX=scale;scaleY=scale;translationX=x;translationY=y})}
  CinemaOutlineButton({scale=1f;x=0f;y=0f},Modifier.fillMaxWidth()){Text("重置视图")}
 }
}
