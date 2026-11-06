@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package app.meridian.mobile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import dev.chrisbanes.haze.*
import kotlin.math.roundToInt

@Composable private fun AppearanceSection(title:String,content:@Composable ColumnScope.()->Unit){FlowSettingsGroup{Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text(title,fontSize=16.sp,fontWeight=FontWeight.Medium);content()}}}
@Composable private fun AppearanceToggle(title:String,description:String,value:Boolean,onChange:(Boolean)->Unit){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)){Text(title,color=ink,fontSize=14.sp);Text(description,color=muted,fontSize=12.sp,lineHeight=18.sp)};CinemaSwitch(value,onChange)}}
@Composable private fun AppearanceSlider(title:String,value:Float,range:ClosedFloatingPointRange<Float>,label:String,onChange:(Float)->Unit){Column{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(title,fontSize=13.sp,color=ink);Text(label,fontSize=12.sp,color=muted)};CinemaSlider(value,onChange,Modifier.fillMaxWidth(),valueRange=range)}}

@Composable private fun AppearanceModeTile(id:String,label:String,selected:Boolean,onClick:()->Unit,modifier:Modifier){
 val shape=RoundedCornerShape(14.dp);val ink=MaterialTheme.colorScheme.onSurface
 Box(modifier.heightIn(min=64.dp).clip(shape).background(ink.copy(alpha=if(selected).085f else .025f)).border(if(selected)1.dp else .6.dp,ink.copy(alpha=if(selected).28f else .07f),shape).selectable(selected=selected,role=Role.RadioButton,onClick=onClick).padding(10.dp),contentAlignment=Alignment.Center){
  Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)){Icon(when(id){"dark"->Icons.Outlined.DarkMode;"light"->Icons.Outlined.LightMode;else->Icons.Outlined.BrightnessAuto},null,Modifier.size(20.dp));Text(label,style=UiType.label)}
  if(selected)Icon(Icons.Outlined.Check,null,Modifier.align(Alignment.TopEnd).size(12.dp))
 }
}

@Composable private fun ControlMaterialPreview(){
 val haze=remember{HazeState()};var scene by remember{mutableIntStateOf(1)}
 Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("浅色背景","彩色背景","深色背景").forEachIndexed{i,label->CinemaChoice(scene==i,{scene=i},{Text(label,fontSize=11.sp)},Modifier.weight(1f))}}
 val dark=scene==2;val foreground=if(dark)Color.White else Color(0xFF20252D)
 Box(Modifier.fillMaxWidth().height(144.dp).clip(RoundedCornerShape(20.dp))){
  Box(Modifier.matchParentSize().hazeSource(haze)){
   Canvas(Modifier.fillMaxSize()){drawRect(if(dark)Color(0xFF15171B)else Color(0xFFFAFAFC));if(scene==1){drawRect(Brush.linearGradient(listOf(Color(0xFF638CBE),Color(0xFF9CBDB9),Color(0xFFD6AE91))));repeat(14){i->val x=size.width*i/13f;drawLine(foreground.copy(alpha=.09f),androidx.compose.ui.geometry.Offset(x,0f),androidx.compose.ui.geometry.Offset(x,size.height),strokeWidth=2.dp.toPx())}}}
   Text("MERIDIAN",Modifier.align(Alignment.TopCenter).padding(top=20.dp),color=foreground.copy(alpha=.8f),fontSize=18.sp,fontWeight=FontWeight.Medium)
  }
  CompositionLocalProvider(LocalControlBackdrop provides haze,LocalControlSettings provides false,app.meridian.mobile.glass.LocalLiusuDark provides dark){
   MaterialTheme(colorScheme=MaterialTheme.colorScheme.copy(onSurface=foreground,surface=if(dark)Color(0xFF15171B)else Color.White)){
    Row(Modifier.align(Alignment.BottomCenter).padding(14.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){CinemaTonalButton({}){Text("按压预览",color=foreground)};CinemaIconButton({}){Icon(Icons.Outlined.AutoAwesome,"预览图标按钮",Modifier.size(25.dp),tint=foreground)}}
   }
  }
 }
}

@Composable fun AppearanceScreen(options:AppearanceOptions,theme:String,onTheme:(String)->Unit,onChange:(AppearanceOptions)->Unit,onBack:()->Unit){
 PredictiveSceneFrame(onBack=onBack){
 val globalTheme=LocalGlobalTheme.current
 var section by remember{mutableStateOf("theme")}
 var previewTab by remember{mutableIntStateOf(1)}
 val haze=remember{HazeState()}
 Column(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(LocalPalette.current.background))){
  NativePageHeader("外观与体验",onBack)
  CompactSegments(listOf("theme" to "主题","glass" to "材质","navigation" to "导航","motion" to "动效"),section,{section=it},Modifier.padding(horizontal=18.dp,vertical=6.dp))
  CompositionLocalProvider(LocalControlSettings provides true){
   Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
    if(section=="theme")AppearanceSection("主题"){
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){listOf("system" to "自动","light" to "浅色","dark" to "深色").forEach{(key,label)->AppearanceModeTile(key,label,theme==key,{onTheme(key)},Modifier.weight(1f))}}
     Text("全局配色主题",fontSize=14.sp,color=ink)
     Text("同步爱车、行程、统计、AI 助手、登录、设置和详情页面。",fontSize=12.sp,lineHeight=19.sp,color=muted)
     ThemePresetGrid(globalTheme.id,globalTheme.select)

    }
    if(section=="glass")AppearanceSection("玻璃材质"){
     AppearanceToggle("玻璃效果","与流影相同的高光、边缘与液态材质",options.glass){onChange(options.copy(glass=it))}
     Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("blur" to "模糊","liquid" to "液态玻璃").forEach{(key,label)->CinemaChoice(options.glass&&options.glassStyle==key,{onChange(options.copy(glassStyle=key,glass=true))},{Text(label)},Modifier.weight(1f))}}
     ControlMaterialPreview()
     if(android.os.Build.VERSION.SDK_INT<33&&options.glassStyle=="liquid")Text("此系统使用模糊玻璃兼容显示，液态折射需要 Android 13。",fontSize=11.sp,color=muted)
     AppearanceSlider("玻璃高光",options.glassHighlight,0f..2f,"${(options.glassHighlight*100).roundToInt()}%"){onChange(options.copy(glassHighlight=it))}
     AppearanceSlider("液态模糊",options.liquidBlur,0f..12f,"${options.liquidBlur.roundToInt()} dp"){onChange(options.copy(liquidBlur=it))}
     AppearanceSlider("液态折射",options.liquidRefraction,0f..2f,"${(options.liquidRefraction*100).roundToInt()}%"){onChange(options.copy(liquidRefraction=it))}
     CinemaTextButton({onChange(options.copy(glassHighlight=1.15f,liquidBlur=2f,liquidRefraction=1f))}){Text("重置材质参数")}
    }
    if(section=="theme")AppearanceSection("页面大小"){
     AppearanceSlider("界面缩放",options.interfaceScale,.75f..1.25f,"${(options.interfaceScale*100).roundToInt()}%"){onChange(options.copy(interfaceScale=it))}
     Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){CinemaTonalButton({onChange(options.copy(interfaceScale=.85f))}){Text("紧凑 85%")};CinemaTextButton({onChange(options.copy(interfaceScale=1f))}){Text("默认 100%")}}
    }
    if(section=="navigation")AppearanceSection("导航栏"){
     AppearanceToggle("跟随页面大小","关闭后可单独调整导航栏比例",options.navigationScale<=0f){follow->onChange(if(follow)options.copy(lastNavigationScale=if(options.navigationScale>0f)options.navigationScale else options.lastNavigationScale,navigationScale=0f)else options.copy(navigationScale=options.lastNavigationScale))}
     if(options.navigationScale>0f)AppearanceSlider("导航独立大小",options.navigationScale,.8f..1.4f,"${(options.navigationScale*100).roundToInt()}%"){onChange(options.copy(navigationScale=it,lastNavigationScale=it))}
     AppearanceToggle("显示导航文字","隐藏时保留图标及无障碍名称",options.showNavigationLabels){onChange(options.copy(showNavigationLabels=it))}
     CinemaTextButton({onChange(options.copy(navigationScale=1f,lastNavigationScale=1f))}){Text("重置导航大小 100%")}
     Text("实时预览 · 可点击或滑动切换",fontSize=11.sp,color=muted)
     Box(Modifier.fillMaxWidth().heightIn(min=(navigationBottomSpaceDp(options.showNavigationLabels,14f*LocalSystemDensity.current.fontScale,options.interfaceScale,options.navigationScale,false)+40).dp)){
      Row(Modifier.matchParentSize().hazeSource(haze).clip(RoundedCornerShape(20.dp)).background(softWash),horizontalArrangement=Arrangement.SpaceEvenly){repeat(5){i->Box(Modifier.padding(top=(15+i*9).dp).size(48.dp).background(flowAccentColors[(i+4)%9].copy(alpha=.45f),CircleShape))}}
      MeridianNavigationDock(previewTab,haze,options.glass,{previewTab=it},Modifier.align(Alignment.BottomCenter),preview=true)
     }
    }
    if(section=="motion")AppearanceSection("动效"){
     FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("standard" to "标准","reduced" to "减少动效","off" to "关闭动效").forEach{(key,label)->CinemaChoice(options.motionMode==key,{onChange(options.copy(motionMode=key))},{Text(label)})}}
     Text("设置自动保存，重启后保留。",fontSize=11.sp,color=muted)
    }
   }
  }
 }
}
}
