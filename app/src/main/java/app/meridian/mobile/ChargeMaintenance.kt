package app.meridian.mobile

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.*
import org.json.*

@Composable internal fun ChargeDetection(api:Api,carId:Any,onChanged:()->Unit){
 val action=rememberServerAction();var pending by remember{mutableStateOf<ServerConfirmation?>(null)}
 var result by remember{mutableStateOf("")}
 CinemaOutlineButton({pending=ServerConfirmation("检测并补录充电","将读取当前充电状态。如果检测到正在充电，服务端可能新建充电记录。"){
  val response=api.call("/car/charge/detect",JSONObject().put("carId",carId));onChanged();result=when(response.opt("data")){true->"检测到正在充电，记录已更新";false->"未检测到当前充电";else->"检测完成，已刷新记录"}
 }},Modifier.fillMaxWidth(),enabled=!action.busy){Icon(Icons.Outlined.EvStation,null,Modifier.size(18.dp));Spacer(Modifier.width(6.dp));Text("检测当前充电")}
 if(result.isNotBlank())Text(result,fontSize=12.sp,color=muted)
 ServerNotice(action);ConfirmServerChange(pending,action){pending=null}
}

@Composable internal fun ChargeMaintenance(api:Api,data:JSONObject,onReload:suspend ()->Unit,onDeleted:()->Unit){
 val action=rememberServerAction();var preview by remember{mutableStateOf<JSONObject?>(null)};var pending by remember{mutableStateOf<ServerConfirmation?>(null)};var samplePage by remember{mutableIntStateOf(0)}
 val id=data.opt("recordId")?:data.opt("id")
 Panel{
  SectionTitle("记录维护",Icons.Outlined.Tune)
  Text("补齐采样前先预览变化，删除与手动结束需要再次确认。",fontSize=12.sp,lineHeight=20.sp,color=muted)
  CinemaOutlineButton({action.run{preview=api.call("/car/charge/fix/preview",JSONObject().put("recordId",id)).optJSONObject("data")?:throw Exception("未获得修复预览");samplePage=0}},Modifier.fillMaxWidth(),enabled=id!=null&&!action.busy){Text("检查并预览修复")}
  if(data.safeText("endTime",data.safeText("chargeEndTime")).isBlank())CinemaOutlineButton({pending=ServerConfirmation("手动结束充电记录","将结束本条未完成的充电记录。此操作更新记录，不等同于控制车辆停止充电。"){api.call("/car/charge/manual-finalize",JSONObject().put("id",id));onReload()}},Modifier.fillMaxWidth(),enabled=id!=null&&!action.busy){Text("手动结束记录")}
  CinemaTextButton({pending=ServerConfirmation("删除充电记录","将永久删除这条充电记录，无法通过 App 撤销。请核对地点与起止时间。"){api.call("/car/charge/delete",JSONObject().put("id",id));onDeleted()}},Modifier.fillMaxWidth(),enabled=id!=null&&!action.busy){Text("删除记录",color=MaterialTheme.colorScheme.error)}
  ServerNotice(action)
 }
 preview?.let{p->FullscreenDataPage("充电修复预览",{if(!action.busy)preview=null}){
  Text(if(p.optBoolean("needFix"))"发现可以补齐的数据"else"当前记录无需修复",fontSize=23.sp,color=ink)
  Text("依据车况历史补齐缺失数据，已有采样不会被覆盖。",fontSize=12.sp,lineHeight=20.sp,color=muted)
  val pairs=listOf(Triple("起始电量","StartBattery","%"),Triple("结束电量","EndBattery","%"),Triple("起始续航","StartRange","km"),Triple("结束续航","EndRange","km"),Triple("充入电量","ChargeEnergy","kWh"),Triple("补充电量","ChargeBattery","%"),Triple("补充续航","ChargeRange","km"),Triple("估算容量","EstimatedCapacityKwh","kWh"))
  pairs.forEach{(label,key,unit)->Panel{Text(label,fontSize=14.sp,color=muted);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("修复前 · "+homeValue(p.opt("old$key"))+" $unit",fontSize=13.sp,color=ink);Text("修复后 · "+homeValue(p.opt("new$key"))+" $unit",fontSize=13.sp,color=accent)}}}
  Panel{Text("采样补齐",fontSize=16.sp,color=ink);Text("已有 ${p.optInt("existingSampleCount")} 条 · 新增 ${p.optInt("missingSampleCount")} 条",fontSize=13.sp,color=muted)}
  val samples=p.optJSONArray("missingSamples")?.objects().orEmpty()
  samples.drop(samplePage*20).take(20).forEach{row->Panel{Text(prettyTime(row.safeText("sampleTime")),fontSize=13.sp,color=ink);Readable(row)}}
  if(samples.size>20)Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){CinemaOutlineButton({samplePage--},Modifier.weight(1f),enabled=samplePage>0){Text("上一页")};CinemaOutlineButton({samplePage++},Modifier.weight(1f),enabled=(samplePage+1)*20<samples.size){Text("下一页")}}
  ServerNotice(action)
  CinemaButton({pending=ServerConfirmation("确认修复充电数据","将应用上述修复结果并补齐缺失采样。"){api.call("/car/charge/fix/confirm",JSONObject().put("recordId",id));onReload();preview=null}},Modifier.fillMaxWidth(),enabled=p.optBoolean("needFix")&&!action.busy){Text("应用修复")}
 }}
 ConfirmServerChange(pending,action){pending=null}
}
