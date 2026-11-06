package app.meridian.mobile
import androidx.compose.foundation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import app.meridian.mobile.glass.LocalAppMotion
import kotlinx.coroutines.*

internal fun homeDragVelocity(y:Float,top:Float,bottom:Float,edge:Float,maxSpeed:Float):Float{
 if(!listOf(y,top,bottom,edge,maxSpeed).all{it.isFinite()}||edge<=0||bottom<=top)return 0f
 return when{y<top+edge->-((top+edge-y)/edge).coerceIn(0f,1f)*maxSpeed;y>bottom-edge->((y-bottom+edge)/edge).coerceIn(0f,1f)*maxSpeed;else->0f}
}
internal fun homeDropTarget(pointer:Offset,dragged:String,ids:List<String>,bounds:Map<String,Rect>):String?=ids.firstOrNull{id->id!=dragged&&bounds[id]?.let{r->r.deflate(minOf(r.width,r.height)*.12f).contains(pointer)}==true}

@Composable internal fun EditableHomeGrid(ids:List<String>,order:List<String>,editing:Boolean,scroll:ScrollState,onMove:(String,String)->Unit,onDrag:(Boolean)->Unit,onDrop:()->Unit,onRemove:(String)->Unit={},content:@Composable (String)->Unit){
 val bounds=remember{mutableMapOf<String,Rect>()};var boundsVersion by remember{mutableIntStateOf(0)};var gridOrigin by remember{mutableStateOf(Offset.Zero)};val currentIds by rememberUpdatedState(ids)
 var dragged by remember{mutableStateOf<String?>(null)};var pointer by remember{mutableStateOf(Offset.Zero)};var grab by remember{mutableStateOf(Offset.Zero)};var lastMove by remember{mutableLongStateOf(0L)}
 val motion=LocalAppMotion.current;val animationScope=rememberCoroutineScope();val shifts=remember{mutableMapOf<String,Animatable<Offset,AnimationVector2D>>()};val previousSlots=remember{mutableMapOf<String,Offset>()}
 val density=LocalDensity.current;val view=LocalView.current;val currentMove by rememberUpdatedState(onMove);val currentDrop by rememberUpdatedState(onDrop);val currentDrag by rememberUpdatedState(onDrag);val currentRemove by rememberUpdatedState(onRemove)
 val latestContent by rememberUpdatedState(content);val movable=remember{mutableMapOf<String,@Composable ()->Unit>()}
 fun finish(){
  val id=dragged
  if(id!=null){view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);val delta=bounds[id]?.let{pointer-grab-it.topLeft}?:Offset.Zero;val shift=shifts.getOrPut(id){Animatable(Offset.Zero,Offset.VectorConverter)}
   animationScope.launch(start=CoroutineStart.UNDISPATCHED){shift.snapTo(delta);dragged=null;if(motion.full)shift.animateTo(Offset.Zero,spring(dampingRatio=.88f,stiffness=500f))else shift.snapTo(Offset.Zero)}
  }
  currentDrag(false);currentDrop()
 }
 fun moveIfNeeded(){val id=dragged?:return;val now=android.os.SystemClock.uptimeMillis();if(now-lastMove<110)return;val target=homeDropTarget(pointer,id,currentIds,bounds)?:return;currentMove(id,target);view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);lastMove=now}
 LaunchedEffect(editing,ids){if(dragged!=null&&(!editing||dragged !in ids))finish();bounds.keys.filter{it !in ids}.toList().forEach{bounds.remove(it)}}
 DisposableEffect(Unit){onDispose{currentDrag(false)}}
 LaunchedEffect(dragged){if(dragged!=null){var last=withFrameNanos{it};while(isActive&&dragged!=null){val now=withFrameNanos{it};val dt=((now-last)/1_000_000_000f).coerceIn(0f,.032f);last=now
  val edge=with(density){72.dp.toPx()};val top=with(density){100.dp.toPx()};val bottom=view.height-with(density){155.dp.toPx()};val speed=homeDragVelocity(pointer.y,top,bottom,edge,with(density){650.dp.toPx()})
  if(speed!=0f){scroll.scrollBy(speed*dt);moveIfNeeded()}
 }}}
 val rows=homeRows(order.filter{it in ids})
 Column(Modifier.fillMaxWidth().onGloballyPositioned{gridOrigin=it.positionInRoot()}.pointerInput(editing){if(editing)detectDragGesturesAfterLongPress(onDragStart={local->
  val root=local+gridOrigin;val removeSize=with(density){44.dp.toPx()};val id=currentIds.firstOrNull{id->bounds[id]?.let{r->r.contains(root)&&!(root.x>r.right-removeSize&&root.y<r.top+removeSize)}==true};val card=id?.let{bounds[it]}
  if(id!=null&&card!=null){pointer=root;grab=root-card.topLeft;dragged=id;shifts[id]?.let{shift->animationScope.launch{shift.snapTo(Offset.Zero)}};lastMove=android.os.SystemClock.uptimeMillis();view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);currentDrag(true)}
 },onDragEnd={finish()},onDragCancel={finish()}){change,delta->if(dragged!=null){change.consume();pointer+=delta;moveIfNeeded()}}},verticalArrangement=Arrangement.spacedBy(10.dp)){
  rows.forEach{row->key(row.joinToString("|")){
   Row(Modifier.fillMaxWidth().zIndex(if(dragged in row)20f else 0f).then(if(row.size>1||homeSpan(row.first())<6)Modifier.height(IntrinsicSize.Max)else Modifier),horizontalArrangement=Arrangement.spacedBy(10.dp)){
    row.forEach{id->key(id){
     val moving=id==dragged;val shift=shifts.getOrPut(id){Animatable(Offset.Zero,Offset.VectorConverter)}
     Box(Modifier.weight(homeSpan(id).toFloat()).fillMaxHeight().onGloballyPositioned{val root=it.positionInRoot();val nextBounds=Rect(root,Size(it.size.width.toFloat(),it.size.height.toFloat()));if(bounds[id]!=nextBounds){bounds[id]=nextBounds;boundsVersion++};val slot=Offset(root.x,root.y+scroll.value);val previous=previousSlots.put(id,slot)
      if(previous!=null&&previous!=slot&&!moving){val delta=previous-slot;if(editing&&motion.full)animationScope.launch{shift.snapTo(shift.value+delta);shift.animateTo(Offset.Zero,spring(dampingRatio=.9f,stiffness=480f))}else if(dragged==null&&shift.value==Offset.Zero)previousSlots[id]=slot}
     }.zIndex(if(moving)20f else 0f)){
      if(moving){val color=accent;Canvas(Modifier.matchParentSize()){drawRoundRect(color.copy(alpha=.07f),cornerRadius=CornerRadius(18.dp.toPx()));drawRoundRect(color.copy(alpha=.4f),cornerRadius=CornerRadius(18.dp.toPx()),style=Stroke(1.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(),5.dp.toPx()))))}}
      Box(Modifier.fillMaxWidth().fillMaxHeight().graphicsLayer{val rect=boundsVersion.let{bounds[id]};alpha=if(moving).97f else 1f;scaleX=if(moving)1.015f else 1f;scaleY=scaleX;shape=RoundedCornerShape(18.dp);shadowElevation=if(moving)12.dp.toPx()else 0f;translationX=if(moving&&rect!=null)pointer.x-grab.x-rect.left else shift.value.x;translationY=if(moving&&rect!=null)pointer.y-grab.y-rect.top else shift.value.y}){
       val card=movable.getOrPut(id){movableContentOf{latestContent(id)}};card()
       if(editing){
        Box(Modifier.matchParentSize().semantics{contentDescription="编辑卡片："+homeCardTitle(id);customActions=listOf(CustomAccessibilityAction("上移"){val index=order.indexOf(id);if(index>0){currentMove(id,order[index-1]);currentDrop();true}else false},CustomAccessibilityAction("下移"){val index=order.indexOf(id);if(index<order.lastIndex){currentMove(id,order[index+1]);currentDrop();true}else false},CustomAccessibilityAction("移除卡片"){currentRemove(id);true})}.clickable(interactionSource=remember{MutableInteractionSource()},indication=null){})
        Box(Modifier.align(Alignment.TopEnd).size(44.dp).clickable(interactionSource=remember{MutableInteractionSource()},indication=null,enabled=dragged==null,onClickLabel="移除"+homeCardTitle(id)){currentRemove(id)},contentAlignment=Alignment.Center){
         Box(Modifier.size(25.dp).background(glassWhite,CircleShape).border(.7.dp,glassEdge,CircleShape),contentAlignment=Alignment.Center){Icon(Icons.Outlined.Remove,"移除"+homeCardTitle(id),Modifier.size(17.dp),tint=ink)}
        }
       }
      }
     }
    }}
    val remaining=6-row.sumOf{homeSpan(it)};if(remaining>0)Spacer(Modifier.weight(remaining.toFloat()))
   }
  }}
 }
}
internal fun homeCardTitle(id:String)=mapOf("vehicle" to "爱车展示","range" to "电量与续航","summary" to "30天摘要","condition" to "车况","ledger" to "用车账单","sentry" to "哨兵模式","temperature" to "车内温度","brief" to "今日概览","recentTrip" to "最近行程","recentCharge" to "最近充电","analysis" to "综合分析","trends" to "行车趋势","driving" to "驾驶方式","energyTrend" to "完整行车趋势","efficiency" to "能效分布","time" to "驾驶时段","comparison" to "周期对比","dailyAverage" to "日均驾驶","charging" to "实时充电","history" to "车况历史","report" to "月度报告")[id]?:id
