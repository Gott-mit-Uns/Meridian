package app.meridian.mobile
import androidx.compose.runtime.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.*
import org.json.JSONObject

/** Static artwork from the user's 2026 SU7 reference, no first-generation model. */
@Composable fun VehicleStudio(car:JSONObject) {
 val model=car.safeText("carModel")
 var expanded by remember{mutableStateOf(false)}
 if(model.contains("SU7",true)&&!model.contains("Ultra",true)) {
  Text("2026 新一代 SU7 · 雅灰",fontSize=13.sp,color=muted)
  Box(Modifier.fillMaxWidth().aspectRatio(2.6f).clickable(onClickLabel="查看爱车大图"){expanded=true},contentAlignment=Alignment.Center){
   Canvas(Modifier.fillMaxWidth(.78f).height(18.dp).align(Alignment.BottomCenter)){drawOval(Brush.radialGradient(listOf(Color.Black.copy(alpha=.09f),Color.Transparent),radius=size.width/2))}
   Image(painterResource(R.drawable.car_su7_yahui_reference),"2026 新一代 SU7 雅灰",Modifier.fillMaxSize(),contentScale=ContentScale.Fit)
  }
  if(expanded)AlertDialog({expanded=false},{CinemaTextButton({expanded=false}){Text("完成")}},title={Text("2026 新一代 SU7 · 雅灰")},text={Image(painterResource(R.drawable.car_su7_yahui_reference),"爱车静态展示",Modifier.fillMaxWidth().aspectRatio(2.6f),contentScale=ContentScale.Fit)})
 } else Row(Modifier.fillMaxWidth().padding(vertical=28.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.DirectionsCar,null,tint=accent,modifier=Modifier.size(40.dp));Text(model.ifBlank{"我的爱车"},Modifier.padding(start=12.dp),color=ink)}
}
