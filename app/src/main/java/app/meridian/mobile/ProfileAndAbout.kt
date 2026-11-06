package app.meridian.mobile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import kotlinx.coroutines.*
import java.io.File

@Composable fun ProfileHeader(account:String,service:String){
 val context=LocalContext.current;val scope=rememberCoroutineScope()
 val key=remember(account,service){java.security.MessageDigest.getInstance("SHA-256").digest((service+"\n"+account).toByteArray()).take(12).joinToString(""){"%02x".format(it)}}
 val file=remember(key){File(context.filesDir,"profiles/$key.png")};val preferences=remember{context.getSharedPreferences("profile",0)}
 var revision by remember(key){mutableIntStateOf(0)};var theme by remember(key){mutableIntStateOf(preferences.getInt(key,0).coerceIn(0,3))}
 var bitmap by remember(key){mutableStateOf<ImageBitmap?>(null)};var open by remember{mutableStateOf(false)};var busy by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")}
 LaunchedEffect(key,revision){bitmap=withContext(Dispatchers.IO){if(file.exists())BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()else null}}
 val picker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){uri->if(uri!=null)scope.launch{busy=true;error="";try{withContext(Dispatchers.IO){
  val resolver=context.contentResolver
  val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true};resolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it,null,bounds)}
  require(bounds.outWidth>0&&bounds.outHeight>0){"无法读取这张图片"}
  val options=BitmapFactory.Options().apply{var scale=1;while(maxOf(bounds.outWidth,bounds.outHeight)/scale>2048)scale*=2;inSampleSize=scale}
  var image=resolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it,null,options)}?:throw IllegalArgumentException("图片读取失败")
  val orientation=runCatching{resolver.openInputStream(uri)?.use{ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION,1)}?:1}.getOrDefault(1)
  val matrix=Matrix().apply{when(orientation){3->postRotate(180f);6->postRotate(90f);8->postRotate(270f);2->postScale(-1f,1f);4->{postRotate(180f);postScale(-1f,1f)};5->{postRotate(90f);postScale(-1f,1f)};7->{postRotate(270f);postScale(-1f,1f)}}}
  if(!matrix.isIdentity)image=Bitmap.createBitmap(image,0,0,image.width,image.height,matrix,true)
  val side=minOf(image.width,image.height);val square=Bitmap.createBitmap(image,(image.width-side)/2,(image.height-side)/2,side,side)
  val output=Bitmap.createScaledBitmap(square,512,512,true);file.parentFile?.mkdirs();val atomic=android.util.AtomicFile(file);val stream=atomic.startWrite();try{check(output.compress(Bitmap.CompressFormat.PNG,100,stream));atomic.finishWrite(stream)}catch(e:Exception){atomic.failWrite(stream);throw e}
 };revision++}catch(e:Exception){if(e is CancellationException)throw e;error=e.message?:"头像更新失败"}finally{busy=false}}}
 val colors=listOf(listOf(Color(0xFF7090B5),Color(0xFF394F72)),listOf(Color(0xFFAD96C5),Color(0xFF61527C)),listOf(Color(0xFF80AAA1),Color(0xFF3E6962)),listOf(Color(0xFFB8A084),Color(0xFF766148)))
 Row(Modifier.fillMaxWidth().padding(vertical=10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.End){
  Box(Modifier.size(44.dp).clickable(enabled=!busy,onClickLabel="设置头像"){open=true}){
   Box(Modifier.fillMaxSize().clip(CircleShape).background(Brush.linearGradient(colors[theme])),contentAlignment=Alignment.Center){val image=bitmap;if(image!=null)Image(image,"个人头像",Modifier.fillMaxSize(),contentScale=ContentScale.Crop)else Text(account.take(1).uppercase().ifBlank{"M"},fontSize=20.sp,color=Color.White,fontWeight=FontWeight.Medium)}
   Box(Modifier.align(Alignment.BottomEnd).size(22.dp).background(glassWhite,CircleShape).border(1.dp,glassEdge,CircleShape),contentAlignment=Alignment.Center){Icon(Icons.Outlined.PhotoCamera,null,Modifier.size(12.dp),tint=ink)}
  }

 }
 if(error.isNotBlank())Text(error,fontSize=12.sp,color=MaterialTheme.colorScheme.error)
 if(open)AlertDialog({open=false},{TextButton({open=false}){Text("完成")}},title={Text("设置头像")},text={Column(verticalArrangement=Arrangement.spacedBy(14.dp)){
  Text("头像仅保存在此设备",fontSize=12.sp,color=muted)
  Button({open=false;picker.launch("image/*")},Modifier.fillMaxWidth()){Icon(Icons.Outlined.PhotoLibrary,null,Modifier.size(18.dp));Spacer(Modifier.width(7.dp));Text("从相册选择")}
  Text("默认配色",fontSize=12.sp,color=muted)
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){colors.forEachIndexed{i,pair->Box(Modifier.size(42.dp).clip(CircleShape).background(Brush.linearGradient(pair)).clickable{theme=i;preferences.edit().putInt(key,i).apply();scope.launch{withContext(Dispatchers.IO){file.delete()};revision++}},contentAlignment=Alignment.Center){if(bitmap==null&&theme==i)Icon(Icons.Outlined.Check,null,tint=Color.White,modifier=Modifier.size(18.dp))}}}
  TextButton({scope.launch{withContext(Dispatchers.IO){file.delete()};revision++};theme=0;preferences.edit().putInt(key,0).apply()}){Text("恢复默认头像")}
 }})
}

@Composable fun AboutPage(onBack:()->Unit){
 val context=LocalContext.current;var credits by remember{mutableStateOf(false)}
 if(credits)FullscreenDataPage("开源与设计致谢",{credits=false}){Panel{Text("小米 EV Mate · 车辆数据服务");Text("流影 · 玻璃材质与控件设计");Text("Jetpack Compose · Haze · Markwon · osmdroid",fontSize=13.sp,color=muted);Text("地图边界与底图来源见地图页面标注。",fontSize=12.sp,color=muted)}}
 ImmersiveScrollingPage("关于 Meridian",onBack){
  Column(Modifier.fillMaxWidth().padding(vertical=28.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)){
   Image(painterResource(R.drawable.brand),"Meridian",Modifier.size(82.dp).clip(RoundedCornerShape(22.dp)))
   Text("Meridian",fontSize=31.sp,fontWeight=FontWeight.Medium,color=ink)
   Text("连接每一次出发",fontSize=13.sp,color=muted)
   Text(BuildConfig.VERSION_NAME,fontSize=11.sp,color=muted,modifier=Modifier.background(softWash,CircleShape).padding(horizontal=14.dp,vertical=6.dp))
  }
  Panel{SectionTitle("与你的爱车，一路同行",Icons.Outlined.Route);Text("车辆状态、旅途记录、补能与能耗洞察，以更清晰的方式呈现每一次出发。",fontSize=14.sp,lineHeight=23.sp,color=muted)}
  Panel{SettingsEntry(Icons.Outlined.FavoriteBorder,"开源与设计致谢","感谢每一份构建体验的贡献"){credits=true};HorizontalDivider(color=glassEdge);SettingsEntry(Icons.Outlined.Code,"项目主页","Xiaomi EV Mate"){context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse("https://github.com/Ponyix/xiaomiev-mate")))}}
  Panel{SectionTitle("隐私与本机数据",Icons.Outlined.Shield);Text("登录凭据由系统密钥加密保存在本机。头像仅保存在此设备。分享行程前，可选择隐藏地址、轨迹和精确时间。",fontSize=13.sp,lineHeight=22.sp,color=muted)}
  Text("Meridian · 让旅途留下值得回看的细节",fontSize=11.sp,color=muted,modifier=Modifier.align(Alignment.CenterHorizontally).padding(vertical=22.dp))
 }
}
