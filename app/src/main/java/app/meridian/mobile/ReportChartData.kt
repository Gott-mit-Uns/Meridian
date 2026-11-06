package app.meridian.mobile

import org.json.JSONObject
internal data class ReportChart(val title:String,val unit:String,val samples:List<ChartSample>,val kind:String="line")
internal fun reportCharts(root:JSONObject,trend:JSONObject?,mode:String):List<ReportChart>{
 val result=mutableListOf<ReportChart>();val source=trend?.optJSONObject("drivingTrend")?:trend
 source?.optJSONArray("mileageStats")?.let{if(mode!="energy")result.add(ReportChart("每日里程","km",chartSamples(it,"mileage",.001),"bar"))}
 source?.optJSONArray("energyTrend")?.let{rows->
  val samples=chartSamples(rows,"energyConsumption").filter{it.value>=0};result.add(ReportChart("每日能耗","Wh/km",samples))
  if(mode=="energy"&&samples.size>=2){val low=samples.minOf{it.value};val high=samples.maxOf{it.value};val step=(high-low)/4
   val histogram=if(step<=0)listOf(ChartSample(numberText(low)+" Wh/km",samples.size.toDouble()))else (0..3).map{index->val start=low+step*index;val end=low+step*(index+1);ChartSample(numberText(start)+"–"+numberText(end)+" Wh/km",samples.count{it.value>=start&&(it.value<end||index==3&&it.value<=end)}.toDouble())}
   result.add(ReportChart("按日能耗区间分布","天",histogram,"bars"))
  }
 }
 val dashboard=root.optJSONObject("periodDashboard")
 dashboard?.optJSONArray("charts")?.objects()?.forEach{chart->
  val key=chart.safeText("key");val points=chart.optJSONArray("points")?.objects()?.mapNotNull{row->row.number("value")?.takeIf{it>=0}?.let{ChartSample(row.safeText("label").take(30),it)}}.orEmpty()
  if(key in listOf("drivingTime","energyDistribution","energyStructure","efficiencyDistribution"))result.add(ReportChart(chart.safeText("title","周期分布"),chart.safeText("unit"),points,if(key=="drivingTime")"donut"else"bars"))
 }
 root.optJSONObject("adData")?.optJSONObject("summary")?.let{ad->
  val samples=listOf("高速领航" to "hnoaMileage","城市领航" to "cnoaMileage","基础辅助" to "basicPilotMileage").mapNotNull{(label,key)->ad.number(key)?.takeIf{it>=0}?.let{ChartSample(label,it)}}
  if(mode!="energy")result.add(ReportChart("辅助驾驶里程结构","km",samples,"bars"))
 }
 return result.filter{it.samples.isNotEmpty()&&it.samples.any{v->v.value>0}}.distinctBy{it.title}.take(6)
}
internal fun monthlyReportCharts(source:JSONObject):List<ReportChart>{
 val charts=mutableListOf(ReportChart("每日里程","km",chartSamples(flexibleArray(source.opt("dayMileages")),"mileage"),"bar"))
 val energy=listOf("行驶" to "drivingEnergyPercent","温控" to "tempCtrlEnergyPercent","设备" to "deviceEnergyPercent","其他" to "otherEnergyPercent").mapNotNull{(label,key)->source.number(key)?.takeIf{it>=0}?.let{ChartSample(label,it)}}
 charts.add(ReportChart("能耗组成","%",energy,"donut"))
 val ad=listOf("高速领航" to "hnoaMileage","城市领航" to "cnoaMileage","基础辅助" to "basicPilotMileage").mapNotNull{(label,key)->source.number(key)?.takeIf{it>=0}?.let{ChartSample(label,it)}}
 charts.add(ReportChart("辅助驾驶里程","km",ad,"bars"))
 source.optJSONObject("periodComparison")?.let{j->listOf(Triple("里程 · 上月与本月","previousMileage" to "currentMileage","km"),Triple("能耗 · 上月与本月","previousAvgEnergy" to "currentAvgEnergy","kWh/100km"),Triple("耗电 · 上月与本月","previousTotalEnergy" to "currentTotalEnergy","kWh")).forEach{(title,keys,unit)->val previous=j.number(keys.first);val current=j.number(keys.second);if(previous!=null&&current!=null&&previous>=0&&current>=0)charts.add(ReportChart(title,unit,listOf(ChartSample("上月",previous),ChartSample("本月",current)),"bars"))}}
 return charts.filter{it.samples.isNotEmpty()&&it.samples.any{v->v.value>0}}
}
