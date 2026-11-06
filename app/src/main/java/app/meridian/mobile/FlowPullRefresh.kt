package app.meridian.mobile
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import app.meridian.mobile.glass.LocalAppMotion
import kotlinx.coroutines.*
import kotlin.math.exp

internal fun elasticPullOffset(distance:Float,limit:Float):Float=if(limit<=0f||!distance.isFinite())0f else limit*(1f-exp(-distance.coerceAtLeast(0f)*.58f/limit))
@Composable internal fun FlowPullRefresh(enabled:Boolean,scroll:ScrollState,refreshing:Boolean,onRefresh:()->Unit,lastSynced:Long=0,atTop:(()->Boolean)?=null,content:@Composable ()->Unit){
 val density=LocalDensity.current;val view=LocalView.current;val motion=LocalAppMotion.current;val scope=rememberCoroutineScope()
 val trigger=with(density){92.dp.toPx()};val limit=with(density){112.dp.toPx()};val hold=with(density){50.dp.toPx()};val settled=remember{Animatable(0f)}
 var distance by remember{mutableFloatStateOf(0f)};var requested by remember{mutableStateOf(false)};var finished by remember{mutableStateOf(false)};var generation by remember{mutableIntStateOf(0)}
 val active by rememberUpdatedState(enabled);val busy by rememberUpdatedState(refreshing);val callback by rememberUpdatedState(onRefresh);val topCheck by rememberUpdatedState(atTop)
 suspend fun settle(target:Float){settled.animateTo(target,if(motion.full)spring(dampingRatio=.9f,stiffness=420f)else snap())}
 LaunchedEffect(enabled){if(!enabled){distance=0f;requested=false;finished=false;settled.snapTo(0f)}}
 LaunchedEffect(refreshing,requested,generation){if(requested&&!refreshing){delay(350);if(!busy){finished=true;delay(650);settle(0f);requested=false;finished=false}}}
 val connection=remember(scroll,trigger,limit){object:NestedScrollConnection{
  override fun onPreScroll(available:Offset,source:NestedScrollSource):Offset{if(!active||requested||source!=NestedScrollSource.UserInput||distance<=0||available.y>=0)return Offset.Zero;val consumed=maxOf(available.y,-distance);distance+=consumed;return Offset(0f,consumed)}
  override fun onPostScroll(consumed:Offset,available:Offset,source:NestedScrollSource):Offset{if(!active||busy||requested||settled.isRunning||!(topCheck?.invoke()?: (scroll.value==0))||available.y<=0||source!=NestedScrollSource.UserInput)return Offset.Zero;val previous=distance;distance=(distance+available.y).coerceAtMost(trigger*4);if(previous<trigger&&distance>=trigger)view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);return Offset(0f,available.y)}
  override suspend fun onPreFling(available:Velocity):Velocity{if(distance<=0)return Velocity.Zero;val shouldRefresh=active&&!busy&&distance>=trigger;settled.snapTo(elasticPullOffset(distance,limit));distance=0f;if(shouldRefresh){generation++;requested=true;finished=false;scope.launch{settle(hold)};callback()}else scope.launch{settle(0f)};return Velocity(0f,available.y)}
 }}
 val offset=if(distance>0&&!requested)elasticPullOffset(distance,limit)else settled.value
 val progress=(distance/trigger).coerceIn(0f,1f)
 Box(Modifier.fillMaxSize().nestedScroll(connection)){
  Box(Modifier.fillMaxSize().graphicsLayer{translationY=offset}){content()}
  if(enabled&&(offset>1f||requested)){
   val shape=RoundedCornerShape(20.dp)
   Row(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top=66.dp).widthIn(max=252.dp).fillMaxWidth(.78f).graphicsLayer{alpha=if(requested)1f else (progress*1.8f).coerceIn(0f,1f)}.shadow(4.dp,shape,ambientColor=ink.copy(alpha=.04f),spotColor=ink.copy(alpha=.05f)).clip(shape).background(glassWhite).border(.6.dp,glassEdge,shape).padding(horizontal=12.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically){
    Box(Modifier.size(24.dp),contentAlignment=Alignment.Center){if(requested&&!finished)FlowSpinner(Modifier.size(18.dp),accent)else Icon(if(finished)Icons.Outlined.Check else Icons.Outlined.ArrowDownward,null,Modifier.size(18.dp).graphicsLayer{rotationZ=if(finished)0f else progress*180f},tint=accent)}
    Column(Modifier.weight(1f).padding(horizontal=6.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(2.dp)){
     Text(if(finished)"刷新结束"else if(requested)"正在刷新"else if(progress>=1f)"松开刷新"else"下拉刷新",Modifier.fillMaxWidth(),fontSize=13.sp,lineHeight=18.sp,fontWeight=FontWeight.Medium,color=ink,textAlign=TextAlign.Center)
     if(lastSynced>0)Text("上次同步 "+java.time.Instant.ofEpochMilli(lastSynced).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm")),Modifier.fillMaxWidth(),fontSize=11.sp,lineHeight=15.sp,color=muted,textAlign=TextAlign.Center)
    }
    Spacer(Modifier.size(24.dp))
   }
  }
 }
}
