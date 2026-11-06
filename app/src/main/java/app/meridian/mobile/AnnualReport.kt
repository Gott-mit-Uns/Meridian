package app.meridian.mobile

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import kotlinx.coroutines.*
import org.json.JSONObject
import java.time.LocalDate
import java.time.temporal.ChronoUnit

internal data class AnnualPeriod(val start:LocalDate,val end:LocalDate){val days:Int get()=ChronoUnit.DAYS.between(start,end).toInt()+1;val label:String get()="$start — $end"}
internal fun annualPeriod(year:Int,today:LocalDate=LocalDate.now()):AnnualPeriod{
 require(year in 1900..today.year)
 return AnnualPeriod(LocalDate.of(year,1,1),minOf(LocalDate.of(year,12,31),today))
}
@Composable internal fun AnnualReport(api:Api,carId:Any,year:Int,shareRequest:Int,onConsumed:()->Unit,refreshRequest:Int=0,onRefreshState:(Boolean,Long)->Unit={_,_->}){
 val period=remember(year){annualPeriod(year)}
 val request=remember(carId,period){JSONObject().put("carId",carId).put("startDate",period.start.toString()).put("endDate",period.end.toString())}
 val trendRequest=remember(carId,period){JSONObject().put("carId",carId).put("days",period.days).put("statDate",period.end.toString())}
 var overview by remember(api.base,api.accountScope,carId,year){mutableStateOf(api.memoryRead("/statistics/energy-overview",request)?.optJSONObject("data"))}
 var trend by remember(api.base,api.accountScope,carId,year){mutableStateOf(api.memoryRead("/dashboard/getTrendData",trendRequest)?.optJSONObject("data"))}
 var busy by remember{mutableStateOf(false)};var issue by remember{mutableStateOf("")};var retry by remember{mutableIntStateOf(0)};val initialRefresh=remember{refreshRequest}
 LaunchedEffect(api.base,api.accountScope,carId,year,retry,refreshRequest){
  if(retry==0&&refreshRequest==initialRefresh&&freshPageRead(overview)&&freshPageRead(trend))return@LaunchedEffect
  busy=true;issue=""
  try{
   if(overview==null){val cached=withContext(Dispatchers.IO){api.peekRead("/statistics/energy-overview",request)?.optJSONObject("data") to api.peekRead("/dashboard/getTrendData",trendRequest)?.optJSONObject("data")};overview=cached.first;trend=cached.second}
   supervisorScope{val metrics=async{api.call("/statistics/energy-overview",request).optJSONObject("data")?:throw Exception("该年度暂无统计数据")};val charts=async{try{api.call("/dashboard/getTrendData",trendRequest).optJSONObject("data")}catch(e:Exception){if(e is CancellationException)throw e;issue="年度指标已加载，趋势读取失败，请重试";trend}};val next=metrics.await();val nextTrend=charts.await();overview=next;trend=nextTrend}
  }catch(e:Exception){if(e is CancellationException)throw e;issue=e.message?:"年度报告读取失败"}finally{busy=false}
 }
 SideEffect{onRefreshState(busy,responseSyncTime(overview))}
 CachedReadStatus(overview)
 Panel{SectionTitle("$year 年度用车报告",Icons.Outlined.AutoGraph);Text(if(year==LocalDate.now().year)"今年的每一次出发"else"这一年的用车记忆",fontSize=22.sp,fontWeight=FontWeight.Medium);Text(period.label,fontSize=13.sp,color=muted);Text("按所选年度统计；本年度截至今天，车辆累计指标另行标注。",fontSize=12.sp,color=muted)}
 if(busy&&overview==null)PageSkeleton()
 if(issue.isNotBlank())Panel{Text(issue,color=muted);CinemaOutlineButton({retry++}){Text("重新加载")}}
 overview?.let{root->
  StatisticsShare(root,trend,period.days,"overview",shareRequest,onConsumed,"$year 年度用车报告",period.label)
  val groups=statisticsMetricGroups(root).filter{!emptyChargeGroup(it)}
  groups.filter{it.scope!="lifetime"}.forEach{group->Panel{SectionTitle(group.title,metricIcon(group.title));StatisticsCards(group.items,trend,period.days,group.scope,period.label)}}
  val source=trend?.optJSONObject("drivingTrend")?:trend
  source?.optJSONArray("mileageStats")?.let{InteractiveTrend("年度里程趋势","km",chartSamples(it,"mileage",.001))}
  source?.optJSONArray("energyTrend")?.let{InteractiveTrend("年度能耗趋势","Wh/km",chartSamples(it,"energyConsumption"))}
  groups.filter{it.scope=="lifetime"}.forEach{group->Panel{SectionTitle("车辆累计 · "+group.title,Icons.Outlined.History);Text("累计指标，不计作本年新增",fontSize=12.sp,color=muted);StatisticsCards(group.items,null,period.days,"lifetime",period.label)}}
 }
}
