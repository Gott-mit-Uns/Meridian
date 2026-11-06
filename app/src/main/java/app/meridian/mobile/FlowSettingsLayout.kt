package app.meridian.mobile

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.*

// Ported from liuying SettingsScreen.kt SettingsGroup / SettingsRow and SurfaceHierarchy.kt.
@Composable internal fun FlowSettingsGroup(content:@Composable ColumnScope.()->Unit){
 val shape=RoundedCornerShape(22.dp);val dark=LocalPalette.current.dark
 Column(Modifier.fillMaxWidth().themeCardShadow(shape).clip(shape).background(glassWhite.copy(alpha=if(dark).90f else .95f)).border(.6.dp,ink.copy(alpha=if(dark).065f else .035f),shape).padding(vertical=6.dp)){
  ProvideTextStyle(MaterialTheme.typography.bodyMedium.copy(fontSize=15.sp,lineHeight=22.sp)){content()}
 }
}
@Composable internal fun FlowSettingsRow(icon:ImageVector,title:String,divider:Boolean=true,onClick:()->Unit){
 val interaction=remember{MutableInteractionSource()}
 Row(Modifier.fillMaxWidth().heightIn(min=60.dp).motionPress(interaction).clickable(interactionSource=interaction,indication=null,role=Role.Button,onClick=onClick).padding(horizontal=16.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
  Box(Modifier.size(32.dp),contentAlignment=Alignment.Center){Icon(icon,null,Modifier.size(20.dp),tint=ink)}
  Text(title,Modifier.weight(1f),fontSize=15.sp,lineHeight=22.sp,color=ink)
  Icon(Icons.Outlined.ChevronRight,null,Modifier.size(17.dp),tint=muted.copy(alpha=.7f))
 }
 if(divider)Box(Modifier.padding(start=60.dp,end=20.dp).fillMaxWidth().height(.5.dp).background(ink.copy(alpha=.06f)))
}
