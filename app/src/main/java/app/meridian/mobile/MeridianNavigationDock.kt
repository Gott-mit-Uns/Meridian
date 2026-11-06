package app.meridian.mobile

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.*
import dev.chrisbanes.haze.HazeState
import app.meridian.mobile.glass.*
import kotlin.math.roundToInt

/** Five-slot adaptation of 流影 LiquidDock; shared optics/bevel/shaders are copied unchanged. */
@Composable fun MeridianNavigationDock(tab:Int,haze:HazeState,enabled:Boolean,onTab:(Int)->Unit,modifier:Modifier=Modifier,preview:Boolean=false,onReselect:()->Unit={}){
 val dark=LocalPalette.current.dark;val foreground=ink;val highlight=accent
 val options=LocalAppearance.current
 val navScale=navigationDensityScale(options.interfaceScale,options.navigationScale)
 val baseDensity=LocalSystemDensity.current
 val motion=LocalAppMotion.current
 CompositionLocalProvider(LocalDensity provides Density(baseDensity.density*navScale,baseDensity.fontScale)){
  val liquid=enabled&&options.glassStyle=="liquid"&&android.os.Build.VERSION.SDK_INT>=33
  val rail=navigationRailHeightDp(options.showNavigationLabels,14f*baseDensity.fontScale,navScale).dp
  val currentTab by rememberUpdatedState(tab);val change by rememberUpdatedState(onTab);val scrollTop by rememberUpdatedState(onReselect)
  val tapTracker=remember{NavigationTapTracker()}
  val select:(Int)->Unit={chosen->
   if(tapTracker.tap(chosen,currentTab,android.os.SystemClock.uptimeMillis()))scrollTop()
   if(chosen!=currentTab)change(chosen)
  }
  var held by remember{mutableStateOf(false)};var drag by remember{mutableStateOf<Float?>(null)};var press by remember{mutableStateOf<Float?>(null)}
  val pressure=animateFloatAsState(if(held&&liquid)1f else 0f,if(motion.full)spring(dampingRatio=.94f,stiffness=650f)else snap(),label="navigation-pressure")
  val landing by animateFloatAsState(tab.toFloat(),if(motion.full)spring(dampingRatio=.92f,stiffness=460f)else snap(),label="navigation-cursor")
  val cursor=drag?:press?:landing;val lensState=dockPressedLens(pressure.value)
  Box(modifier.then(if(preview)Modifier else Modifier.navigationBarsPadding()).padding(horizontal=16.dp).padding(top=8.dp,bottom=10.dp).widthIn(max=430.dp).fillMaxWidth()){
   GlassChrome(haze,Modifier.matchParentSize(),CircleShape,cover=.14f,optical=true,captureAll=true,navigationFrame=true){}
   BoxWithConstraints(Modifier.padding(4.dp).fillMaxWidth().height(rail).selectableGroup()
    .pointerInput(Unit){try{awaitPointerEventScope{while(true){val e=awaitPointerEvent(PointerEventPass.Final);val down=e.changes.firstOrNull{it.pressed};held=down!=null;press=down?.let{dockPositionAt(it.position.x,size.width.toFloat(),5).roundToInt().toFloat()}}}}finally{held=false;press=null}}
    .pointerInput(Unit){detectHorizontalDragGestures(onDragStart={drag=dockPositionAt(it.x,size.width.toFloat(),5)},onHorizontalDrag={change,_->change.consume();drag=dockPositionAt(change.position.x,size.width.toFloat(),5)},onDragEnd={val chosen=(drag?:currentTab.toFloat()).roundToInt().coerceIn(0,4);drag=null;select(chosen)},onDragCancel={drag=null})}){
    val width=maxWidth;val slot=width/5;val density=androidx.compose.ui.platform.LocalDensity.current;val slotPx=with(density){slot.toPx()};val left=slotPx*cursor
    if(liquid)DockItems(tab,null,foreground,highlight,Modifier.fillMaxSize(),select)
    val lens=Modifier.offset{IntOffset(left.roundToInt(),0)}.width(slot).height(rail).graphicsLayer{scaleX=lensState.scaleX;scaleY=lensState.scaleY}
    if(liquid)GlassChrome(haze,lens,DockDropletShape,lens=true,cover=.06f,elevated=false,captureAll=true,pressed=held,showRim=false,quietLens=true,droplet=true,pressureOverride=pressure){
     Box(Modifier.matchParentSize().background(foreground.copy(alpha=(if(dark).12f else .085f)*(1f-lensState.pressure))))
     Box(Modifier.matchParentSize().clearAndSetSemantics{}.glassOptics(true,DockDropletShape,strength=.35f,rimStrength=0f,pressureSource=pressure,droplet=true,pressedOnly=true)){
      DockItems(tab,cursor.roundToInt().coerceIn(0,4),foreground,highlight,Modifier.wrapContentSize(Alignment.TopStart,unbounded=true).requiredWidth(width).height(rail).offset{IntOffset(-left.roundToInt(),0)}.graphicsLayer{scaleX=lensState.glyphCounterX;scaleY=lensState.glyphCounterY},{},true)
     }
     Box(Modifier.matchParentSize().dockDropletLighting(DockDropletShape,dark,pressure.value))
    }else{
     Box(lens.background(foreground.copy(alpha=.09f),CircleShape))
     DockItems(tab,tab,foreground,highlight,Modifier.fillMaxSize(),select)
    }
   }


   Box(Modifier.fillMaxWidth().offset(y=(-9).dp),contentAlignment=Alignment.TopCenter){Box(Modifier.size(50.dp).clickable(interactionSource=remember{androidx.compose.foundation.interaction.MutableInteractionSource()},indication=null,onClickLabel="AI助手"){select(2)},contentAlignment=Alignment.Center){AssistantOrb(size=48.dp,quiet=true)}}


  }
 }
}
@Composable private fun DockItems(tab:Int,accentIndex:Int?,foreground:Color,highlight:Color,modifier:Modifier,onTab:(Int)->Unit,mirror:Boolean=false){
 val labels=listOf("爱车","行程","AI助手","统计","我的");val icons=listOf(Icons.Filled.DirectionsCar,Icons.Filled.Route,Icons.Filled.AutoAwesome,Icons.Filled.BarChart,Icons.Filled.Person)
 Row(modifier){labels.forEachIndexed{i,label->Column(Modifier.weight(1f).fillMaxHeight().semantics{contentDescription=label}.then(if(mirror)Modifier else Modifier.selectable(selected=tab==i,role=Role.Tab,onClick={onTab(i)})),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
  Box(Modifier.size(34.dp),contentAlignment=Alignment.Center){if(i==2)Spacer(Modifier.size(34.dp))else Icon(icons[i],null,tint=if(accentIndex==i)highlight else foreground,modifier=Modifier.size(24.dp))};if(LocalAppearance.current.showNavigationLabels){Spacer(Modifier.height(3.dp));Text(label,color=if(accentIndex==i)highlight else foreground,fontSize=11.sp,lineHeight=14.sp,fontWeight=FontWeight.Medium)}
 }}}
}
