package app.meridian.mobile
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.*
import kotlinx.coroutines.*
import java.io.File

@Composable internal fun ChatBubble(role:String,text:String,account:String="",service:String=""){
 val own=role=="user"
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.Top){
  if(!own)AssistantOrb(size=32.dp)
  Box(Modifier.weight(1f)){ChatBubbleContent(role,text)}
  if(own)ChatUserAvatar(account,service)
 }
}
@Composable private fun ChatUserAvatar(account:String,service:String){
 val context=LocalContext.current
 val key=remember(account,service){java.security.MessageDigest.getInstance("SHA-256").digest((service+"\n"+account).toByteArray()).take(12).joinToString(""){"%02x".format(it)}}
 var image by remember(key){mutableStateOf<ImageBitmap?>(null)}
 LaunchedEffect(key){image=withContext(Dispatchers.IO){runCatching{android.graphics.BitmapFactory.decodeFile(File(context.filesDir,"profiles/$key.png").absolutePath)?.asImageBitmap()}.getOrNull()}}
 Box(Modifier.size(32.dp).clip(CircleShape).background(softWash).border(.6.dp,glassEdge,CircleShape),contentAlignment=Alignment.Center){val bitmap=image;if(bitmap!=null)Image(bitmap,"我的头像",Modifier.fillMaxSize(),contentScale=ContentScale.Crop)else Icon(Icons.Outlined.Person,"我的头像",Modifier.size(21.dp),tint=accent)}
}
