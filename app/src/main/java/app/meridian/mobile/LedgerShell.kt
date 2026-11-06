package app.meridian.mobile
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.DialogProperties
import dev.chrisbanes.haze.*
@Composable internal fun LedgerShell(page:String,onPage:(String)->Unit,onClose:()->Unit,actions:List<ToolbarAction>,refreshing:Boolean,onRefresh:()->Unit,lastSynced:Long,onAdd:()->Unit,resetKey:Any?=null,content:LazyListScope.()->Unit){
 val (presence,close)=rememberPageMotion(onClose);val motion=app.meridian.mobile.glass.LocalAppMotion.current
 val billScroll=rememberLazyListState();val statsScroll=rememberLazyListState();val scroll=if(page=="bills")billScroll else statsScroll
 LaunchedEffect(resetKey){if(resetKey!=null)scroll.scrollToItem(0)}
 val scrolled by remember(scroll){derivedStateOf{scroll.firstVisibleItemIndex>0||scroll.firstVisibleItemScrollOffset>1}}
 val haze=remember{HazeState()};val inset=WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
 WindowSafeDialog(close,DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)){
  PredictiveSceneFrame(onBack=onClose){
   Box(Modifier.fillMaxSize().scenePresence(presence).background(Brush.verticalGradient(LocalPalette.current.background))){
    Box(Modifier.matchParentSize().hazeSource(haze,zIndex=0f).background(Brush.verticalGradient(LocalPalette.current.background)))
    FlowPullRefresh(true,rememberScrollState(),refreshing,onRefresh,lastSynced,atTop={scroll.firstVisibleItemIndex==0&&scroll.firstVisibleItemScrollOffset==0}){
     LazyColumn(Modifier.fillMaxSize().hazeSource(haze,zIndex=1f),state=scroll,contentPadding=PaddingValues(start=18.dp,end=18.dp,top=inset+78.dp,bottom=WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()+112.dp),verticalArrangement=Arrangement.spacedBy(14.dp),content=content)
    }
    PinnedSectionToolbar(haze,Modifier.align(Alignment.TopCenter),scrolled){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
     QuietIconButton(Icons.Outlined.ArrowBack,"返回",close)
     Box(Modifier.weight(1f)){GlassSelectionMenu(page,listOf("bills" to "用车账单","stats" to "账单统计"),onPage)}
     ToolbarActionStrip(actions)
    }}
    CompositionLocalProvider(LocalControlBackdrop provides haze){CinemaIconButton(onAdd,Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end=20.dp,bottom=20.dp).size(58.dp)){Icon(Icons.Outlined.Add,"新增记账",Modifier.size(28.dp),tint=accent)}}
   }
  }
 }
}
