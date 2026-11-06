package app.meridian.mobile
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.*
import org.json.*
import java.time.LocalDate

internal fun energyBucketMatches(value:Double,bucket:String)=value.isFinite()&&value>=0&&when(bucket){"low"->value<=12;"mid"->value>12&&value<=18;"high"->value>18;else->true}
@Composable internal fun EnergyRecordExplorer(api:Api,carId:Any,days:Int,onClose:()->Unit){
 val revision=DeviceData.revision;val index=remember(api.localScope(carId),revision){DeviceData.read("trips:"+api.localScope(carId))};val end=LocalDate.now();val start=end.minusDays(days-1L);var bucket by remember{mutableStateOf("high")};var detail by remember{mutableStateOf<JSONObject?>(null)};val action=rememberServerAction()
 val rows=index.keys().asSequence().mapNotNull{index.optJSONObject(it)}.filter{j->val date=chartDate(j.optString("tripStartTime"));val value=j.number("tripEnergyConsumption");date!=null&&date>=start&&date<=end&&value!=null&&energyBucketMatches(value,bucket)}.sortedByDescending{it.number("tripEnergyConsumption")}.toList()
 var syncTime by remember{mutableLongStateOf(0L)}
 val reload:()->Unit={action.run{var page=1;var more=true;var loaded=0;while(more&&page<=10){val raw=api.call("/car/trip/get",JSONObject().put("carId",carId).put("startDate",start.toString()).put("endDate",end.toString()).put("pageNum",page).put("pageSize",50).put("sortOrder","desc").put("orderBy","trip_start_time desc"));val data=raw.optJSONObject("data");val count=(data?.optJSONArray("list")?:data?.optJSONArray("records")?:raw.optJSONArray("data"))?.length()?:0;syncTime=responseSyncTime(raw);loaded+=count;more=hasMoreTripRecords(raw,count,loaded,50);page++}}}
 FullscreenDataPage("按能耗查看行程",onClose,refreshing=action.busy,onRefresh=reload,lastSynced=syncTime){Text("$start 至 $end · kWh/100km",fontSize=12.sp,color=muted);SmallSelectionMenu(bucket,listOf("all" to "全部能耗","low" to "≤12","mid" to "12–18","high" to ">18")){bucket=it};Text("基于本机已加载记录；以下数值区间用于筛选，不代表驾驶评分。",fontSize=12.sp,color=muted)
 Text("下拉读取该周期记录（最多500条）",fontSize=12.sp,color=muted);ServerNotice(action)
 if(rows.isEmpty())Panel{Text("没有符合条件的已加载记录");Text("可切换区间，或读取该周期记录。",fontSize=13.sp,color=muted)}
 rows.forEach{trip->TripTimelineCard(trip,false){action.run{val id=trip.opt("id")?:trip.opt("tripId")?:throw Exception("缺少行程编号");detail=api.call("/car/trip/getDetail",JSONObject().put("tripId",id)).optJSONObject("data")}}};detail?.let{TripDetailPage(api,it){detail=null}}
 }
}
