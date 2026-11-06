package app.meridian.mobile

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import java.time.LocalDate

internal data class RecordFilterState(val days:Int=0,val dates:Pair<LocalDate,LocalDate>?=null,val order:String="desc",val tripType:String="all",val city:String="",val chargeType:String="all")

@Composable internal fun RecordFilters(api:Api,carId:Any?,charging:Boolean,value:RecordFilterState,onRefresh:()->Unit,request:Int=0,onConsumed:()->Unit={},showTrigger:Boolean=true,onApply:(RecordFilterState)->Unit){
 var open by remember{mutableStateOf(false)};var draft by remember{mutableStateOf(value)}
 LaunchedEffect(request){if(request>0){draft=value;open=true;onConsumed()}}
 val range=when{value.days==-1&&value.dates!=null->"${value.dates.first} — ${value.dates.second}";value.days>0->"最近${value.days}天";else->"全部日期"}
 if(showTrigger)Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
  Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)){Text(range,fontSize=12.sp,color=muted);val tags=listOf(if(value.order=="asc")"时间正序"else"时间倒序",if(charging)when(value.chargeType){"fast"->"快充";"slow"->"慢充";else->"全部类型"}else when(value.tripType){"long"->"长途";"short"->"短途";else->"全部行程"},if(charging)""else value.city).filter{it.isNotBlank()};Text(tags.joinToString(" · "),fontSize=11.sp,color=muted)}
  if(showTrigger)CinemaOutlineButton({draft=value;open=true},enabled=carId!=null){Icon(Icons.Outlined.Tune,null,Modifier.size(17.dp));Spacer(Modifier.width(5.dp));Text("筛选",fontSize=13.sp)}
 }
 if(open)FlowDetailPanel({open=false},title={Text(if(charging)"筛选充电记录"else"筛选行程")},body={Column(verticalArrangement=Arrangement.spacedBy(14.dp)){
  CompactSegments(listOf(0 to "全部日期",7 to "近7天",30 to "近30天"),draft.days,{draft=draft.copy(days=it)})
  RecordDateRange(draft.dates){draft=draft.copy(dates=it,days=-1)}
  if(draft.days==-1)draft.dates?.let{Text("${it.first} 至 ${it.second}",fontSize=12.sp,color=muted)}
  SmallSelectionMenu(draft.order,listOf("desc" to "时间倒序","asc" to "时间正序"),{draft=draft.copy(order=it)})
  if(charging){CompactSegments(listOf("all" to "全部类型","fast" to "快充","slow" to "慢充"),draft.chargeType,{draft=draft.copy(chargeType=it)});if(carId!=null)ChargeDetection(api,carId,onRefresh)}
  else{
   CompactSegments(listOf("all" to "全部行程","long" to "长途","short" to "短途"),draft.tripType,{draft=draft.copy(tripType=it)})
   val end=if(draft.days==-1)draft.dates?.second else if(draft.days>0)LocalDate.now()else null
   val start=if(draft.days==-1)draft.dates?.first else if(draft.days>0)LocalDate.now().minusDays(draft.days-1L)else null
   if(carId!=null)VisitedCityFilter(api,carId,start,end,draft.city){draft=draft.copy(city=it)}
  }
  CinemaTextButton({draft=RecordFilterState()}){Text("重置筛选")}
 }},confirm={CinemaButton({onApply(draft);open=false}){Text("应用筛选")}},dismiss={CinemaTextButton({open=false}){Text("取消")}})
}
