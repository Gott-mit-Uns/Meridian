package app.meridian.mobile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.*
import kotlinx.coroutines.*

internal class ServerAction(private val scope:CoroutineScope){
 var busy by mutableStateOf(false);private set
 var error by mutableStateOf("");private set
 var message by mutableStateOf("");private set
 fun clearFeedback(){if(!busy){error="";message=""}}
 fun run(success:String="",block:suspend ()->Unit){
  if(busy)return
  busy=true;error="";message=""
  scope.launch{try{block();message=success}catch(e:Exception){if(e is CancellationException)throw e;error=e.message?:"操作失败，请重试"}finally{busy=false}}
 }
}
@Composable internal fun rememberServerAction():ServerAction{val scope=rememberCoroutineScope();return remember(scope){ServerAction(scope)}}
@Composable internal fun ServerNotice(action:ServerAction){
 if(action.busy)FlowLoading(label="正在处理…")
 if(action.error.isNotBlank())Text(action.error,color=MaterialTheme.colorScheme.error,fontSize=12.sp,lineHeight=19.sp)
 if(action.message.isNotBlank())Text(action.message,color=muted,fontSize=12.sp,lineHeight=19.sp)
}
internal data class ServerConfirmation(val title:String,val message:String,val confirmLabel:String="确认操作",val perform:suspend ()->Unit)
@Composable internal fun ConfirmServerChange(pending:ServerConfirmation?,action:ServerAction,onClose:()->Unit){
 LaunchedEffect(pending){if(pending!=null)action.clearFeedback()}
 if(pending!=null)FlowDetailPanel(onDismiss={if(!action.busy)onClose()},title={Text(pending.title)},body={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){Text(pending.message,fontSize=14.sp,lineHeight=22.sp,color=muted);ServerNotice(action)}},confirm={CinemaButton({action.run("操作已完成"){pending.perform();onClose()}},enabled=!action.busy,colors=if(pending.title.contains("删除"))ButtonDefaults.buttonColors(containerColor=MaterialTheme.colorScheme.error,contentColor=themeOnAccent(MaterialTheme.colorScheme.error))else null){Text(if(action.busy)"处理中…"else pending.confirmLabel)}},dismiss={CinemaTextButton(onClose,enabled=!action.busy){Text("取消")}})
}
