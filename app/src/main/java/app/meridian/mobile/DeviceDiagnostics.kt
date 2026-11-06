package app.meridian.mobile
import android.content.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.*
import androidx.core.content.FileProvider
import java.io.File
import org.json.JSONObject

internal fun shareDiagnostic(context:Context,report:String){val directory=File(context.cacheDir,"trip-shares").apply{mkdirs()};val file=File(directory,"meridian-diagnostics.json");file.writeText(report);val uri=FileProvider.getUriForFile(context,context.packageName+".files",file);context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("application/json").putExtra(Intent.EXTRA_STREAM,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).apply{clipData=ClipData.newRawUri("诊断报告",uri)},"分享脱敏诊断"))}
@Composable internal fun DeviceDiagnostics(api:Api,onClose:()->Unit){
 val context=LocalContext.current;val action=rememberServerAction();var status by remember{mutableStateOf("尚未检测")};var latency by remember{mutableStateOf<Long?>(null)};var clear by remember{mutableStateOf(false)};var widgetHint by remember{mutableStateOf("")}
 val manager=context.getSystemService(Context.CONNECTIVITY_SERVICE)as android.net.ConnectivityManager;val connected=manager.activeNetwork!=null
 val report=JSONObject().put("appVersion",BuildConfig.VERSION_NAME).put("androidApi",android.os.Build.VERSION.SDK_INT).put("networkAvailable",connected).put("serviceHttps",api.base.startsWith("https://")).put("latencyMs",latency?:JSONObject.NULL).put("status",status).toString(2)
 FullscreenDataPage("连接诊断",onClose,action={ToolbarActionStrip(listOf(ToolbarAction(Icons.Outlined.IosShare,"导出脱敏诊断",!action.busy){action.run{shareDiagnostic(context,report)}}))}){
 Panel{SectionTitle("服务连接",Icons.Outlined.NetworkCheck);Text(status,fontSize=19.sp);Text("设备网络："+if(connected)"已连接"else"未连接",fontSize=13.sp,color=muted);latency?.let{Text("服务响应 $it ms",fontSize=13.sp,color=accent)};CinemaButton({action.run{status="检测中";val start=android.os.SystemClock.elapsedRealtime();try{api.call("/car/get");latency=android.os.SystemClock.elapsedRealtime()-start;status="服务连接正常"}catch(e:Exception){status=if(e.message?.contains("登录")==true)"登录已失效，请重新登录"else"未能连接服务，请检查网络与服务地址";throw e}}},enabled=!action.busy){Text("开始检测")};ServerNotice(action)}
 Panel{SectionTitle("桌面小组件",Icons.Outlined.Widgets);Text("在系统桌面长按空白区域，添加 Meridian 爱车小组件。显示最近同步快照，点击打开App刷新。",fontSize=13.sp,color=muted);CinemaTonalButton({val widgets=android.appwidget.AppWidgetManager.getInstance(context);if(widgets.isRequestPinAppWidgetSupported)widgets.requestPinAppWidget(android.content.ComponentName(context,MeridianWidget::class.java),null,null)else widgetHint="当前桌面不支持直接添加，请长按桌面空白区域，从小组件列表选择Meridian。"}){Text("添加到桌面")};if(widgetHint.isNotBlank())Text(widgetHint,fontSize=12.sp,color=muted)}
 Panel{SectionTitle("本机数据",Icons.Outlined.Storage);Text("车辆读取缓存使用系统密钥加密。清理缓存不会删除行程收藏、手记和账本。NAS备份不包含本机新增的账本与手记。",fontSize=13.sp,color=muted);CinemaOutlineButton({clear=true}){Text("清理读取缓存")}}
 Panel{SectionTitle("诊断信息",Icons.Outlined.Shield);Text("导出内容仅含App版本、Android版本、网络可用状态与检测结果；不包含域名、账号、地址、车辆标识、日志正文或凭据。",fontSize=13.sp,color=muted);Text("Meridian "+BuildConfig.VERSION_NAME+" · Android API "+android.os.Build.VERSION.SDK_INT,fontSize=12.sp,color=muted)}
 if(clear)FlowDetailPanel({clear=false},title={Text("清理读取缓存？")},body={Text("下次打开页面将重新读取车辆数据。收藏、手记与账本保留。")},confirm={CinemaButton({DeviceData.clearCache();clear=false}){Text("清理")}},dismiss={CinemaTextButton({clear=false}){Text("取消")}})
 }
}
@Composable internal fun InboxPage(api:Api,carId:Any,onClose:()->Unit,onOpen:(String)->Unit){
 val revision=DeviceData.revision;val key="inbox:"+api.localScope(carId);val messages=remember(key,revision){DeviceData.read(key)};val rows=messages.keys().asSequence().map{it to messages.optJSONObject(it)}.filter{it.second!=null}.sortedByDescending{it.second!!.optLong("time")}.toList()
 FullscreenDataPage("消息中心",onClose,action={ToolbarActionStrip(listOf(ToolbarAction(Icons.Outlined.DoneAll,"全部已读",rows.isNotEmpty()){rows.forEach{it.second?.put("read",true)};DeviceData.write(key,messages)}))}){
 Text("本机消息在车辆同步时生成。后台系统推送仍使用你已配置的企业微信、钉钉或飞书渠道。",fontSize=12.sp,color=muted)
 if(rows.isEmpty())Panel{Text("暂时没有消息");Text("同步后的车况提醒、充电状态变化和已读取报告会汇总在这里。",fontSize=13.sp,color=muted)}
 rows.forEach{(id,item)->val message=item!!;Panel{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Icon(if(message.optBoolean("read"))Icons.Outlined.Drafts else Icons.Outlined.MarkEmailUnread,null,Modifier.size(20.dp),tint=accent);Spacer(Modifier.width(8.dp));Text(privateProse(message.optString("title")),Modifier.weight(1f),fontSize=15.sp)};Text(java.time.Instant.ofEpochMilli(message.optLong("time")).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm")),fontSize=12.sp,color=muted);CinemaTonalButton({message.put("read",true);DeviceData.write(key,messages);onOpen(when(message.optString("kind")){"health"->"health";"report"->"report:"+message.optString("month",id.removePrefix("report:"));"charge"->"charge";else->"diagnostics"})}){Text("查看")}}}
 }
}
