package app.meridian.mobile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.*
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.unit.dp
import app.meridian.mobile.glass.GlassChrome
import dev.chrisbanes.haze.*

/** Scroll viewport spans the status bar; toolbar samples it instead of cropping it. */
@Composable fun ImmersiveScrollingPage(title:String,onBack:()->Unit,action:(@Composable ()->Unit)?=null,refreshing:Boolean=false,onRefresh:(()->Unit)?=null,lastSynced:Long=0,content:@Composable ColumnScope.()->Unit){
 val inherited=LocalPageMotionHandled.current;val (pageProgress,close)=if(inherited)(rememberUpdatedState(1f) to onBack)else rememberPageMotion(onBack);val fullMotion=app.meridian.mobile.glass.LocalAppMotion.current.full
 val haze=remember{HazeState()};val scroll=rememberScrollState();val scrolled by remember(scroll){derivedStateOf{scroll.value>1}}
 val inset=WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
 PredictiveSceneFrame(enabled=!inherited,onBack=if(inherited)null else onBack){
 Box(Modifier.fillMaxSize().then(if(inherited)Modifier else Modifier.scenePresence(pageProgress))){
  Box(Modifier.matchParentSize().hazeSource(haze,zIndex=0f).background(Brush.verticalGradient(LocalPalette.current.background)))
  FlowPullRefresh(onRefresh!=null,scroll,refreshing,{onRefresh?.invoke()},lastSynced){Column(Modifier.fillMaxSize().imePadding().hazeSource(haze,zIndex=1f).verticalScroll(scroll).navigationBarsPadding().padding(horizontal=18.dp).padding(top=inset+72.dp,bottom=40.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)}
  FlowTopScrollFog(haze,scrolled,Modifier.align(Alignment.TopCenter))
  CompositionLocalProvider(LocalControlBackdrop provides haze){Box(Modifier.align(Alignment.TopCenter)){NativePageHeader(title,close,action)}}
 }
 }
}

@Composable fun StatusBarGlass(haze:HazeState,modifier:Modifier=Modifier){
 val inset=WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
 GlassChrome(haze,modifier.fillMaxWidth().height(inset+10.dp).graphicsLayer{compositingStrategy=CompositingStrategy.Offscreen}.drawWithContent{
  drawContent()
  drawRect(Brush.verticalGradient(0f to Color.Black,.72f to Color.Black,1f to Color.Transparent),blendMode=BlendMode.DstIn)
 },RectangleShape,optical=false,elevated=false,showRim=false,cover=.15f,captureAll=true){}
}

@Composable internal fun PinnedSectionToolbar(haze:HazeState,modifier:Modifier=Modifier,scrolled:Boolean=false,content:@Composable ()->Unit){
 Box(modifier.fillMaxWidth()){
  FlowTopScrollFog(haze,scrolled,Modifier.align(Alignment.TopCenter))
  CompositionLocalProvider(LocalControlBackdrop provides haze){Box(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal=18.dp,vertical=8.dp)){content()}}
 }
}
