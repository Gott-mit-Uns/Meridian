package app.meridian.mobile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.*

@Composable internal fun MeridianLogin(address:String,onAddress:(String)->Unit,account:String,onAccount:(String)->Unit,password:String,onPassword:(String)->Unit,rememberLogin:Boolean,onRemember:(Boolean)->Unit,automatic:Boolean,onAutomatic:(Boolean)->Unit,busy:Boolean,error:String,onOffline:(()->Unit)?=null,onConnect:()->Unit){
 val dark=LocalPalette.current.dark
 CompositionLocalProvider(LocalControlSettings provides true){
 Column(Modifier.fillMaxSize().systemBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal=24.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
   Image(painterResource(R.drawable.brand),"Meridian",Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)))
   Text("MERIDIAN",fontSize=15.sp,fontWeight=FontWeight.Medium,letterSpacing=2.sp,color=ink)
  }
  Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
   Text("连接每一次出发",fontSize=27.sp,lineHeight=35.sp,fontWeight=FontWeight.Medium,color=ink)
   Text("连接你的车辆服务，开启专属用车视界。",fontSize=13.sp,lineHeight=20.sp,color=muted)
  }

  FlowSettingsGroup{
   Column(Modifier.padding(horizontal=18.dp,vertical=10.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){Icon(Icons.Outlined.Link,null,Modifier.size(19.dp),tint=muted);Text("连接服务",fontSize=18.sp,fontWeight=FontWeight.Medium)}
    LoginField("服务地址",Icons.Outlined.Dns){OutlinedTextField(address,onAddress,placeholder={Text("https://你的服务地址",fontSize=14.sp,color=muted)},singleLine=true,enabled=!busy,modifier=Modifier.fillMaxWidth())}
    LoginField("账号",Icons.Outlined.PersonOutline){OutlinedTextField(account,onAccount,placeholder={Text("输入服务账号",fontSize=14.sp,color=muted)},singleLine=true,enabled=!busy,modifier=Modifier.fillMaxWidth())}
    LoginField("密码",Icons.Outlined.LockOpen){OutlinedTextField(password,onPassword,placeholder={Text("输入服务密码",fontSize=14.sp,color=muted)},visualTransformation=PasswordVisualTransformation(),singleLine=true,enabled=!busy,modifier=Modifier.fillMaxWidth())}
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(14.dp)){
     Row(Modifier.weight(1f).heightIn(min=44.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){Text("记住信息",fontSize=12.sp,color=ink);CinemaSwitch(rememberLogin,{if(!busy)onRemember(it)})}
     Row(Modifier.weight(1f).heightIn(min=44.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){Text("自动登录",fontSize=12.sp,color=ink);CinemaSwitch(automatic,{if(!busy)onAutomatic(it)})}
    }
    if(error.isNotBlank())Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.error.copy(alpha=.07f),RoundedCornerShape(12.dp)).padding(12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){Icon(Icons.Outlined.Info,null,Modifier.size(17.dp),tint=MaterialTheme.colorScheme.error);Text(error,fontSize=13.sp,lineHeight=20.sp,color=MaterialTheme.colorScheme.error,modifier=Modifier.weight(1f))}
    if(onOffline!=null)CinemaOutlineButton(onOffline,Modifier.fillMaxWidth()){Text("离线浏览本机资料")}
    CinemaButton(onConnect,Modifier.fillMaxWidth().heightIn(min=52.dp),enabled=!busy&&address.isNotBlank()&&account.isNotBlank()&&password.isNotBlank()){
     Text(if(busy)"正在连接…"else"连接并登录",fontSize=15.sp);Spacer(Modifier.width(9.dp));Icon(if(busy)Icons.Outlined.Sync else Icons.Outlined.ArrowForward,null,Modifier.size(18.dp))
    }
   }
  }
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center,verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.VerifiedUser,null,Modifier.size(14.dp),tint=muted);Spacer(Modifier.width(6.dp));Text("私有部署 · 凭据加密 · 数据由你的服务提供",fontSize=11.sp,lineHeight=18.sp,color=muted)}
  Spacer(Modifier.height(2.dp))
 }
 }
}
@Composable private fun LoginField(label:String,icon:androidx.compose.ui.graphics.vector.ImageVector,field:@Composable ()->Unit){Column(verticalArrangement=Arrangement.spacedBy(7.dp)){Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){Icon(icon,null,Modifier.size(15.dp),tint=muted);Text(label,fontSize=13.sp,color=muted)};field()}}
@Composable private fun LoginToggle(title:String,description:String,value:Boolean,onChange:(Boolean)->Unit){Row(Modifier.fillMaxWidth().heightIn(min=48.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(2.dp)){Text(title,fontSize=14.sp,color=ink);Text(description,fontSize=11.sp,lineHeight=17.sp,color=muted)};CinemaSwitch(value,onChange)}}
