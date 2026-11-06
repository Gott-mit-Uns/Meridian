package app.meridian.mobile
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*

@Composable internal fun HomeEditorToolbar(onReset:()->Unit,onTheme:()->Unit,onDone:()->Unit,modifier:Modifier=Modifier,onManage:()->Unit={}){
 BoxWithConstraints(modifier.fillMaxWidth()){
 val compact=maxWidth<300.dp||androidx.compose.ui.platform.LocalDensity.current.fontScale>1.2f
 Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
  CinemaIconButton(onManage){Icon(Icons.Outlined.DashboardCustomize,"管理卡片",Modifier.size(19.dp))}
  if(compact){CinemaIconButton(onReset){Icon(Icons.Outlined.RestartAlt,"恢复布局",Modifier.size(19.dp))};CinemaIconButton(onTheme){Icon(Icons.Outlined.Palette,"切换主题",Modifier.size(19.dp))}}
  else{CinemaTonalButton(onReset,Modifier.weight(1f)){Icon(Icons.Outlined.RestartAlt,null,Modifier.size(16.dp));Spacer(Modifier.width(4.dp));Text("恢复",fontSize=13.sp)};CinemaTonalButton(onTheme,Modifier.weight(1f)){Icon(Icons.Outlined.Palette,null,Modifier.size(16.dp));Spacer(Modifier.width(4.dp));Text("主题",fontSize=13.sp)}}
  CinemaButton(onDone,Modifier.weight(1f)){Icon(Icons.Outlined.Check,null,Modifier.size(16.dp));Spacer(Modifier.width(4.dp));Text("完成",fontSize=13.sp)}
 }
 }
}
