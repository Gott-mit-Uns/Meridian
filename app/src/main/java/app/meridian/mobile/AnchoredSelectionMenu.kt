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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*

internal fun menuPosition(anchor:IntRect,window:IntSize,popup:IntSize,gap:Int,margin:Int):IntOffset{
 val maxX=(window.width-popup.width-margin).coerceAtLeast(margin)
 val below=anchor.bottom+gap
 val y=if(below+popup.height<=window.height-margin)below else anchor.top-popup.height-gap
 return IntOffset((anchor.left-8).coerceIn(margin,maxX),y.coerceIn(margin,(window.height-popup.height-margin).coerceAtLeast(margin)))
}
internal val LocalMenuLabelSize=staticCompositionLocalOf{17.sp}
@Composable internal fun <T> SmallSelectionMenu(value:T,options:List<Pair<T,String>>,onSelect:(T)->Unit){CompositionLocalProvider(LocalMenuLabelSize provides 14.sp){GlassSelectionMenu(value,options,onSelect)}}

/** Popup is anchored to the actual text bounds in its own window coordinate system. */
@Composable fun <T> GlassSelectionMenu(value:T,options:List<Pair<T,String>>,onSelect:(T)->Unit){
 var open by remember{mutableStateOf(false)};val density=LocalDensity.current
 val gap=with(density){4.dp.roundToPx()};val margin=with(density){10.dp.roundToPx()}
 val position=remember(gap,margin){object:PopupPositionProvider{override fun calculatePosition(anchorBounds:IntRect,windowSize:IntSize,layoutDirection:LayoutDirection,popupContentSize:IntSize)=menuPosition(anchorBounds,windowSize,popupContentSize,gap,margin)}}
 ToolbarTitleCapsule { Row(Modifier.heightIn(min=44.dp).clip(RoundedCornerShape(12.dp)).clickable{open=true}.padding(end=6.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){
  Box{
   Text(options.firstOrNull{it.first==value}?.second.orEmpty(),fontSize=LocalMenuLabelSize.current,fontWeight=FontWeight.Medium,color=ink)
   if(open)Popup(popupPositionProvider=position,onDismissRequest={open=false},properties=PopupProperties(focusable=true)){
    val popupView=LocalView.current;val popupContext=LocalContext.current;val blur=LocalAppearance.current.glass
    DisposableEffect(popupView,blur){
     if(blur&&android.os.Build.VERSION.SDK_INT>=31)runCatching{
      val root=popupView.rootView;val params=root.layoutParams as? android.view.WindowManager.LayoutParams
      if(params!=null){params.flags=params.flags or android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND;params.blurBehindRadius=24;(popupContext.getSystemService(android.content.Context.WINDOW_SERVICE) as android.view.WindowManager).updateViewLayout(root,params)}
     }
     onDispose{}
    }
    Column(Modifier.menuArrival().background(glassWhite.copy(alpha=if(blur).97f else 1f),RoundedCornerShape(18.dp)).width(214.dp).heightIn(max=340.dp).then(if(blur)Modifier.cinemaGlass(RoundedCornerShape(18.dp))else Modifier.border(.6.dp,glassEdge,RoundedCornerShape(18.dp))).verticalScroll(rememberScrollState()).padding(6.dp)){
     options.forEach{(id,label)->Row(Modifier.fillMaxWidth().heightIn(min=44.dp).clip(RoundedCornerShape(12.dp)).background(if(id==value)softWash else androidx.compose.ui.graphics.Color.Transparent).clickable(role=Role.RadioButton){open=false;onSelect(id)}.padding(horizontal=12.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
      Text(label,fontSize=14.sp,lineHeight=20.sp,color=ink,modifier=Modifier.weight(1f))
      if(id==value)Icon(Icons.Outlined.Check,null,Modifier.size(16.dp),tint=accent)
     }}
    }
   }
  }
  Icon(Icons.Outlined.ExpandMore,"选择页面",Modifier.size(17.dp),tint=muted)
 }}
}
