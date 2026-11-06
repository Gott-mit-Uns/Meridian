package app.meridian.mobile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.*
import org.json.*

@Composable fun ChargeDetailPage(initial:JSONObject,api:Api?=null,onChanged:()->Unit={},onClose:()->Unit){
 var data by remember(initial){mutableStateOf(initial)}
 var chart by remember{mutableStateOf("power")};var expanded by remember{mutableStateOf(false)}
 FullscreenDataPage("充电详情",onClose){
  Panel{SectionTitle(data.safeText("locationName","充电记录"),Icons.Outlined.EvStation);Text(prettyTime(data.safeText("startTime",data.safeText("chargeStartTime")))+" — "+prettyTime(data.safeText("endTime",data.safeText("chargeEndTime"))),fontSize=11.sp,color=muted)
   val start=data.number("startSoc")?:data.number("startBattery");val end=data.number("endSoc")?:data.number("endBattery");val color=accent;val track=glassEdge
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("起始 "+numberText(start)+"%",fontSize=13.sp,color=muted);Text("结束 "+numberText(end)+"%",fontSize=17.sp,color=ink)}
   Canvas(Modifier.fillMaxWidth().height(9.dp)){drawRoundRect(track,cornerRadius=androidx.compose.ui.geometry.CornerRadius(8f));if(end!=null)drawRoundRect(color.copy(alpha=.28f),size=androidx.compose.ui.geometry.Size(size.width*(end/100).toFloat().coerceIn(0f,1f),size.height),cornerRadius=androidx.compose.ui.geometry.CornerRadius(8f));if(start!=null&&end!=null&&end>=start){val x=size.width*(start/100).toFloat().coerceIn(0f,1f);drawRoundRect(color,topLeft=androidx.compose.ui.geometry.Offset(x,0f),size=androidx.compose.ui.geometry.Size((size.width*(end/100).toFloat().coerceIn(0f,1f)-x).coerceAtLeast(0f),size.height),cornerRadius=androidx.compose.ui.geometry.CornerRadius(8f))}}
  }
  val metrics=listOf(Triple("充入电量",numberText(data.number("chargeEnergy")),"kWh"),Triple("充电时长",data.number("durationMinutes")?.let{durationText((it*60000).toLong())}?:"—",""),Triple("峰值功率",numberText(data.number("peakPowerKw")),"kW"),Triple("平均功率",numberText(data.number("avgPowerKw")),"kW"))
  metrics.chunked(2).forEach{chunk->Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max),horizontalArrangement=Arrangement.spacedBy(8.dp)){chunk.forEach{(label,value,unit)->MetricDataCard(label,value,unit,metricIcon(label),Modifier.weight(1f).fillMaxHeight())}}}
  val samples=data.optJSONArray("samples")?:JSONArray()
  if(samples.length()>0){CompactSegments(listOf("power" to "功率","soc" to "电量","voltage" to "电压","current" to "电流"),chart,{chart=it});val key=when(chart){"soc"->"batteryLevel";"voltage"->"electricVoltageV";"current"->"electricCurrentA";else->"currentPowerKw"};val unit=when(chart){"soc"->"%";"voltage"->"V";"current"->"A";else->"kW"};InteractiveTrend("充电过程",unit,chartSamples(samples,key,dateKey="sampleTime"))}
  TextButton({expanded=!expanded}){Text(if(expanded)"收起详细参数"else"查看更多参数")}
  if(expanded)Panel{Readable(data)}
  if(api!=null)ChargeMaintenance(api,data,onReload={
   val id=data.opt("recordId")?:data.opt("id")?:throw Exception("记录缺少编号")
   data=api.call("/car/charge/detail",JSONObject().put("recordId",id)).optJSONObject("data")?:throw Exception("充电详情读取失败");onChanged()
  },onDeleted={onChanged();onClose()})
 }
}
