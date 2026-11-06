package app.meridian.mobile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import kotlinx.coroutines.*
import org.json.*

@Composable internal fun VehicleManagementPage(api:Api,cars:List<JSONObject>,onCarsChanged:(List<JSONObject>)->Unit,onBack:()->Unit){
 val context=LocalContext.current;val jobStore=remember{context.getSharedPreferences("sync-jobs",0)}
 fun jobKey(id:Any)=java.security.MessageDigest.getInstance("SHA-256").digest((api.base+"|"+id.toString()).toByteArray()).joinToString(""){"%02x".format(it)}
 var syncTime by remember{mutableLongStateOf(0L)}
 val action=rememberServerAction();var pending by remember{mutableStateOf<ServerConfirmation?>(null)}
 var editing by remember{mutableStateOf<JSONObject?>(null)};var candidates by remember{mutableStateOf<JSONArray?>(null)};val selected=remember{mutableStateListOf<String>()}
 var models by remember{mutableStateOf<JSONArray?>(null)};var modelPicker by remember{mutableStateOf(false)};var modelQuery by remember{mutableStateOf("")}
 var carName by remember{mutableStateOf("")};var carId by remember{mutableStateOf("")};var carModel by remember{mutableStateOf("")}
 var syncCar by remember{mutableStateOf<JSONObject?>(null)};var months by remember{mutableStateOf<JSONArray?>(null)};var month by remember{mutableStateOf("")}
 var jobId by remember{mutableStateOf("")};var progress by remember{mutableStateOf<JSONObject?>(null)};var pollError by remember{mutableStateOf("")};var pollRetry by remember{mutableIntStateOf(0)}
 suspend fun reload(){onCarsChanged((api.call("/car/get").optJSONArray("data")?:JSONArray()).objects());syncTime=System.currentTimeMillis()}
 fun edit(car:JSONObject){carName=car.safeText("carName");carId=car.safeText("carId");carModel=car.safeText("carModel");editing=car;action.run{models=api.call("/car/models").optJSONArray("data")?:JSONArray()}}
 fun submitSync(path:String,body:JSONObject){pending=ServerConfirmation("提交数据同步","将为 ${syncCar?.safeText("carName","当前车辆")} 提交同步任务。关闭页面后，服务端任务仍会继续。"){
  val result=api.call(path,body);val data=result.opt("data");jobId=syncJobId(data);progress=null;if(jobId.isNotBlank())jobStore.edit().putString(jobKey(body.get("id")),jobId).apply()
  if(jobId.isBlank())throw Exception(result.safeText("msg","服务端未返回任务编号，请查看同步状态"))
 }}
 LaunchedEffect(cars.map{it.opt("id")}){if(jobId.isBlank())cars.firstNotNullOfOrNull{car->car.opt("id")?.let{jobStore.getString(jobKey(it),null)?.takeIf{it.isNotBlank()}}}?.let{jobId=it}}
 LaunchedEffect(jobId,pollRetry){if(jobId.isBlank())return@LaunchedEffect;pollError="";try{
  while(isActive){val next=api.call("/car/syncProgress?jobId="+java.net.URLEncoder.encode(jobId,"UTF-8")).optJSONObject("data")?:throw Exception("未返回同步进度");progress=next;if(next.safeText("statusName")in listOf("DONE","FAIL","CANCEL"))break;delay(2000)}
 }catch(e:Exception){if(e is CancellationException)throw e;pollError=e.message?:"进度读取失败"}}
 CompositionLocalProvider(LocalControlSettings provides true){
 ImmersiveScrollingPage("车辆与数据",onBack,refreshing=action.busy,onRefresh={action.run{reload()}},lastSynced=syncTime){
  ServerNotice(action)
  Panel{Text("你的车库",fontSize=22.sp,fontWeight=FontWeight.Medium,color=ink);Text("管理车辆、车型资料与同步任务",fontSize=12.sp,color=muted)
   CinemaButton({action.run{val data=api.call("/car/candidates").optJSONObject("data");candidates=data?.optJSONArray("cars")?:JSONArray();selected.clear()}},Modifier.fillMaxWidth(),enabled=!action.busy){Text("发现并添加车辆")}
   CinemaOutlineButton({edit(JSONObject())},Modifier.fillMaxWidth(),enabled=!action.busy){Text("手动添加车辆")}
  }
  if(cars.isEmpty())Panel{Text("还没有绑定车辆",color=muted)}
  cars.forEach{car->Panel{
   SectionTitle(car.safeText("carName","未命名车辆"),Icons.Outlined.DirectionsCar)
   Text(car.safeText("carModel","车型待设置"),fontSize=13.sp,color=muted)
   Text("车辆 ID · "+car.safeText("carId","—"),fontSize=11.sp,color=muted)
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
    CinemaOutlineButton({edit(car)},Modifier.weight(1f),enabled=!action.busy){Text("编辑信息")}
    CinemaButton({syncCar=car;months=null},Modifier.weight(1f),enabled=!action.busy){Text("同步数据")}
   }
   if(car.optBoolean("batteryCapacityUnavailable"))CinemaTextButton({pending=ServerConfirmation("上报未知车型","将为“${car.safeText("carModel")}”补充上报车型相关数据，帮助完善车型资料。确认后才会提交。"){val result=api.call("/car/reportUnknownModel",JSONObject().put("id",car.get("id")));if(result.opt("data")==false)throw Exception("当前暂无可上报数据，请稍后再试")}}){Text("协助补全车型资料")}
   CinemaTextButton({pending=ServerConfirmation("删除车辆","将移除“${car.safeText("carName")}”。该操作可能同时影响车辆关联数据，无法通过 App 撤销。"){api.call("/car/deleted",JSONObject().put("id",car.get("id")));reload()}}){Text("删除此车辆",color=MaterialTheme.colorScheme.error)}
  }}
  if(jobId.isNotBlank())Panel{
   SectionTitle("同步任务",Icons.Outlined.Sync)
   Text(progress?.safeText("message",progress?.safeText("currentStep","正在读取进度…")?:"正在读取进度…")?:"正在读取进度…",fontSize=13.sp,color=ink)
   val percent=progress?.number("percent")?.coerceIn(0.0,100.0)
   if(percent!=null){FlowProgress({(percent/100).toFloat()});Text(numberText(percent)+"%",fontSize=12.sp,color=muted)}else FlowLoading(label="服务端正在处理…")
   Text(when(progress?.safeText("statusName")){"DONE"->"同步完成";"FAIL"->"同步失败";"CANCEL"->"已取消";"RUNNING"->"正在同步";else->"等待执行"},fontSize=11.sp,color=muted)
   if(pollError.isNotBlank()){Text(pollError,color=MaterialTheme.colorScheme.error,fontSize=12.sp);CinemaOutlineButton({pollRetry++}){Text("重新读取进度")}}
   CinemaTextButton({val old=jobId;cars.forEach{car->car.opt("id")?.let{val key=jobKey(it);if(jobStore.getString(key,null)==old)jobStore.edit().remove(key).apply()}};jobId="";progress=null;pollError=""}){Text("隐藏任务 · 不取消服务端任务")}
  }
 }
 }
 if(editing!=null)FlowDetailPanel({if(!action.busy)editing=null},title={Text(if(editing!!.has("id"))"编辑爱车"else"添加爱车")},body={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
  OutlinedTextField(carId,{carId=it},label={Text("车辆 ID")},singleLine=true,enabled=!action.busy)
  OutlinedTextField(carName,{carName=it},label={Text("爱车名称")},singleLine=true,enabled=!action.busy)
  OutlinedTextField(carModel,{carModel=it},label={Text("车型")},singleLine=true,enabled=!action.busy)
  CinemaOutlineButton({modelQuery="";modelPicker=true},enabled=!action.busy){Text("从官方车型目录选择")}
  ServerNotice(action)
 }},confirm={CinemaButton({action.run("车辆信息已保存"){
  require(carId.isNotBlank()&&carModel.isNotBlank()){"请填写车辆 ID 和车型"}
  val body=JSONObject().put("carId",carId.trim()).put("carName",carName.trim()).put("carModel",carModel.trim());editing?.opt("id")?.let{body.put("id",it)}
  api.call("/car/change",body);reload();editing=null
 }},enabled=!action.busy){Text("保存车辆")}},dismiss={CinemaTextButton({editing=null},enabled=!action.busy){Text("取消")}})
 if(modelPicker)FlowDetailPanel({modelPicker=false},title={Text("选择车型")},body={Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
  OutlinedTextField(modelQuery,{modelQuery=it},placeholder={Text("搜索车型")},singleLine=true)
  models?.objects()?.map{it.safeText("model") }?.filter{it.isNotBlank()&&it.contains(modelQuery,true)}?.forEach{model->CinemaOutlineButton({carModel=model;modelPicker=false},Modifier.fillMaxWidth()){Text(model)}}
  if(models?.length()==0)Text("目录暂不可用，可返回手动填写",color=muted,fontSize=12.sp)
 }},confirm={CinemaTextButton({modelPicker=false}){Text("完成")}})
 if(candidates!=null)FlowDetailPanel({if(!action.busy)candidates=null},title={Text("选择要添加的车辆")},body={Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
  if(candidates!!.length()==0)Text("没有发现可添加车辆，可返回手动添加。",color=muted,fontSize=13.sp)
  candidates!!.objects().forEach{car->val id=car.safeText("vid");CinemaChoice(id in selected,{if(id in selected)selected.remove(id)else selected.add(id)},{Column{Text(car.safeText("carName","未命名车辆"));Text(car.safeText("carModel"),fontSize=11.sp,color=muted)}},Modifier.fillMaxWidth(),enabled=car.optBoolean("selectable",false)&&id.isNotBlank()&&!action.busy)}
  ServerNotice(action)
 }},confirm={CinemaButton({action.run("车辆已添加"){
  val chosen=candidates!!.objects().filter{it.safeText("vid")in selected}.map{JSONObject().put("vid",it.get("vid")).put("carName",it.opt("carName")).put("carModel",it.opt("carModel"))}
  require(chosen.isNotEmpty()){"请选择车辆"};api.call("/car/saveSelected",JSONObject().put("cars",JSONArray(chosen)));reload();candidates=null
 }},enabled=selected.isNotEmpty()&&!action.busy){Text("添加所选车辆")}})
 if(syncCar!=null)FlowDetailPanel({if(!action.busy)syncCar=null},title={Text(syncCar!!.safeText("carName","车辆")+" · 数据同步")},body={Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
  listOf("/car/syncDayTripData" to "近期行程","/car/syncTripMonthStatsData" to "月度报告","/car/syncAllTripData" to "全部历史行程").forEach{(path,title)->CinemaOutlineButton({submitSync(path,JSONObject().put("id",syncCar!!.get("id")))},Modifier.fillMaxWidth(),enabled=!action.busy){Text("同步$title")}}
  CinemaOutlineButton({action.run{months=api.call("/car/syncAvailableMonths",JSONObject().put("id",syncCar!!.get("id"))).optJSONArray("data")?:JSONArray();month=months?.optString(0).orEmpty()}},Modifier.fillMaxWidth(),enabled=!action.busy){Text("选择指定月份同步")}
  months?.let{values->if(values.length()==0)Text("服务端暂无可同步月份",fontSize=12.sp,color=muted)else{
   values.let{(0 until it.length()).map{n->it.getString(n)}}.forEach{m->CinemaChoice(month==m,{month=m},{Text(m)},Modifier.fillMaxWidth())}
   CinemaButton({submitSync("/car/syncSpecifiedMonthData",JSONObject().put("id",syncCar!!.get("id")).put("month",month))},Modifier.fillMaxWidth(),enabled=month.isNotBlank()&&!action.busy){Text("同步所选月份")}
  }}
  ServerNotice(action)
 }},confirm={CinemaTextButton({syncCar=null},enabled=!action.busy){Text("完成")}})
 ConfirmServerChange(pending,action){pending=null}
}
