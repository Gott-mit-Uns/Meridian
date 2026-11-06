package app.meridian.mobile

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import app.meridian.mobile.glass.LocalAppMotion
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.YearMonth

/** Flow's delayed, quiet three-bar loading language; no platform spinner. */
@Composable fun FlowLoading(modifier:Modifier=Modifier,color:Color=accent,label:String="正在读取…") {
 var visible by remember{mutableStateOf(false)}
 LaunchedEffect(Unit){delay(180);visible=true}
 if(visible) Row(modifier.padding(vertical=6.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)) {
  FlowSpinner(Modifier.size(20.dp),color)
  Text(label,fontSize=12.sp,color=muted)
 }
}
@Composable fun FlowSpinner(modifier:Modifier=Modifier,color:Color=accent,strokeWidth:Dp=2.dp) {
 val full=LocalAppMotion.current.full
 val phase=if(full){val transition=rememberInfiniteTransition(label="flow-loading");val phase by transition.animateFloat(0f,6.283f,infiniteRepeatable(tween(1200,easing=LinearEasing)),label="flow-bars");phase}else 0f
 Canvas(modifier.size(20.dp)) { val width=3.dp.toPx();val gap=3.dp.toPx();val start=(size.width-(width*3+gap*2))/2
  repeat(3){i->val h=(if(i==1)8 else 5).dp.toPx();val a=if(full)(.5f+.22f*kotlin.math.sin(phase-i*.8f))else .65f
   drawRoundRect(color.copy(alpha=a),androidx.compose.ui.geometry.Offset(start+i*(width+gap),(size.height-h)/2),androidx.compose.ui.geometry.Size(width,h),androidx.compose.ui.geometry.CornerRadius(width/2))}
 }
}
@Composable fun FlowProgress(progress:()->Float,modifier:Modifier=Modifier) {
 val color=accent;val track=glassEdge
 Canvas(modifier.fillMaxWidth().height(4.dp)){val fraction=progress().let{if(it.isFinite())it.coerceIn(0f,1f)else 0f};drawRoundRect(track,cornerRadius=androidx.compose.ui.geometry.CornerRadius(size.height/2));drawRoundRect(color,size=androidx.compose.ui.geometry.Size(size.width*fraction,size.height),cornerRadius=androidx.compose.ui.geometry.CornerRadius(size.height/2))}
}

@Composable fun FlowDetailPanel(onDismiss:()->Unit,confirm:@Composable ()->Unit,title:(@Composable ()->Unit)?=null,body:(@Composable ()->Unit)?=null,dismiss:(@Composable ()->Unit)?=null) {
 val (progress,close)=rememberPageMotion(onDismiss);val fullMotion=LocalAppMotion.current.full
 WindowSafeDialog(close,DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
  val view=LocalView.current;val dark=LocalPalette.current.dark
  SideEffect{val window=view.dialogWindow();if(window!=null){prepareFullscreenWindow(window,view,dark)}}

  CompositionLocalProvider(LocalControlBackdrop provides null,LocalControlSettings provides true) {
   Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.32f*progress.value)).clickable(indication=null,interactionSource=remember{androidx.compose.foundation.interaction.MutableInteractionSource()}){close()},contentAlignment=Alignment.BottomCenter) {
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(12.dp),contentAlignment=Alignment.BottomCenter) {
    Column(Modifier.widthIn(max=600.dp).fillMaxWidth().heightIn(max=maxHeight*.94f).graphicsLayer{alpha=progress.value;translationY=if(fullMotion)(1f-progress.value)*96.dp.toPx()else 0f;scaleX=if(fullMotion).98f+.02f*progress.value else 1f;scaleY=scaleX}.clip(RoundedCornerShape(28.dp)).background(glassWhite).border(.7.dp,glassEdge,RoundedCornerShape(28.dp)).clickable(indication=null,interactionSource=remember{androidx.compose.foundation.interaction.MutableInteractionSource()}){}.padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
     Box(Modifier.align(Alignment.CenterHorizontally).width(32.dp).height(4.dp).background(muted.copy(alpha=.25f),CircleShape))
     if(title!=null)ProvideTextStyle(MaterialTheme.typography.titleMedium.copy(fontWeight=FontWeight.SemiBold,color=ink)){title()}
     if(body!=null)Box(Modifier.weight(1f,fill=false).verticalScroll(rememberScrollState())){body()}
     CompositionLocalProvider(LocalFooterAction provides true) {
      Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(8.dp)){
       CompositionLocalProvider(LocalFooterPrimary provides true){confirm()}
       if(dismiss!=null)CompositionLocalProvider(LocalFooterPrimary provides false){dismiss()}
      }
     }
    }
    }
   }
  }
 }
}

@Composable fun FlowDatePicker(initial:LocalDate,onDismiss:()->Unit,latest:LocalDate=LocalDate.now(),earliest:LocalDate?=null,onConfirm:(LocalDate)->Unit) {
 var selected by remember(initial,latest,earliest){mutableStateOf(boundedCalendarDate(initial,latest,earliest))};var month by remember(initial,latest,earliest){mutableStateOf(YearMonth.from(boundedCalendarDate(initial,latest,earliest)))};val today=LocalDate.now();val returnDate=boundedCalendarDate(today,latest,earliest)
 FlowDetailPanel(onDismiss,confirm={CinemaButton({onConfirm(selected)}){Text("确定 · ${selected.monthValue}月${selected.dayOfMonth}日")}},title={
  Column(verticalArrangement=Arrangement.spacedBy(5.dp)){Text("选择日期");Text("${selected.year}年${selected.monthValue}月${selected.dayOfMonth}日",fontSize=13.sp,color=muted)}
 },body={Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(12.dp)){
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
   QuietIconButton(androidx.compose.material.icons.Icons.Outlined.ChevronLeft,"上个月",{month=month.minusMonths(1)},enabled=earliest==null||month>YearMonth.from(earliest))
   Text("${month.year} 年 ${month.monthValue} 月",fontSize=16.sp,fontWeight=FontWeight.Medium,color=ink)
   QuietIconButton(androidx.compose.material.icons.Icons.Outlined.ChevronRight,"下个月",{month=month.plusMonths(1)},enabled=month<YearMonth.from(latest))
  }
  Row(Modifier.fillMaxWidth()){listOf("一","二","三","四","五","六","日").forEachIndexed{i,day->Box(Modifier.weight(1f).height(32.dp),contentAlignment=Alignment.Center){Text(day,fontSize=12.sp,color=if(i>4)accent.copy(alpha=.75f)else muted)}}}
  val start=month.atDay(1).dayOfWeek.value-1
  repeat((start+month.lengthOfMonth()+6)/7){week->Row(Modifier.fillMaxWidth()) {repeat(7){day->
   val n=week*7+day-start+1
   Box(Modifier.weight(1f).height(46.dp),contentAlignment=Alignment.Center){if(n in 1..month.lengthOfMonth()){
    val date=month.atDay(n);val chosen=selected==date
    Box(Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(if(chosen)accent else if(date==today)softWash else Color.Transparent).then(if(date==today&&!chosen)Modifier.border(.8.dp,accent.copy(alpha=.4f),RoundedCornerShape(14.dp))else Modifier).clickable(enabled=date<=latest&&(earliest==null||date>=earliest),onClickLabel="选择 ${date}"){selected=date},contentAlignment=Alignment.Center){
     Text(n.toString(),color=if(chosen){if(accent.luminance()>.179f)Color.Black else Color.White}else if(date>latest||(earliest!=null&&date<earliest))muted.copy(alpha=.35f)else ink,fontSize=14.sp,fontWeight=if(chosen)FontWeight.SemiBold else FontWeight.Normal)
    }
   }}
  }}}
  CinemaTextButton({selected=returnDate;month=YearMonth.from(returnDate)},Modifier.fillMaxWidth()){Text(if(returnDate==today)"回到今天"else"回到可选最近日期")}
 }},dismiss={CinemaTextButton(onDismiss){Text("取消")}})
}
