package app.meridian.mobile

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import org.json.JSONObject

internal fun recordSummaryMetrics(data:JSONObject,charge:Boolean):List<HomeMetricValue>{
 val summary=data.optJSONObject("summary")?:data
 fun trend(key:String):String=summary.number(key)?.let{if(it==0.0)"较上周期持平"else"较上周期 "+String.format(java.util.Locale.getDefault(),"%+.1f%%",it)}.orEmpty()
 val result=if(charge)listOf(
  HomeMetricValue("充电次数",homeValue(summary.opt("totalSessions")),"次"),
  HomeMetricValue("充入电量",homeValue(summary.opt("totalEnergy")),"kWh"),
  HomeMetricValue("补能时长",summary.number("totalDurationMinutes")?.let{durationText((it*60000).toLong())}?:"—",""),
  HomeMetricValue("补充续航",homeValue(summary.opt("totalRange")),"km")
 )else listOf(
  HomeMetricValue("行程次数",homeValue(summary.opt("totalTrips")),"次",trend("totalTripsMonthComparePercent")),
  HomeMetricValue("行驶里程",homeValue(summary.number("totalMileage")?.div(1000)),"km",trend("totalMileageMonthComparePercent")),
  HomeMetricValue("驾驶时长",summary.number("totalDuration")?.let{durationText(it.toLong())}?:"—","",trend("totalDurationMonthComparePercent")),
  HomeMetricValue("平均速度",homeValue(summary.opt("avgSpeedKmh")),"km/h",trend("avgSpeedMonthComparePercent"))
 )
 return if(result.all{it.value=="—"})emptyList()else result
}
@Composable fun RecordTotals(data:JSONObject,charge:Boolean){
 val values=recordSummaryMetrics(data,charge)
 if(values.isNotEmpty())RecordOverviewTiles(values)
}
