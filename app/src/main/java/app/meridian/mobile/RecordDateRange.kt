package app.meridian.mobile

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import java.time.LocalDate

@Composable internal fun RecordDateRange(value:Pair<LocalDate,LocalDate>?,onSelect:(Pair<LocalDate,LocalDate>)->Unit){
 var open by remember{mutableStateOf(false)};var target by remember{mutableStateOf<Boolean?>(null)}
 var start by remember{mutableStateOf(value?.first?:LocalDate.now().minusDays(29))};var end by remember{mutableStateOf(value?.second?:LocalDate.now())}
 CinemaOutlineButton({start=value?.first?:start;end=value?.second?:end;open=true}){Icon(Icons.Outlined.CalendarMonth,null,Modifier.size(17.dp));Spacer(Modifier.width(6.dp));Text("自定义日期",fontSize=12.sp)}
 if(open)FlowDetailPanel({open=false},title={Text("筛选记录日期")},body={Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
  CinemaOutlineButton({target=true},Modifier.fillMaxWidth()){Text("开始 · $start")}
  CinemaOutlineButton({target=false},Modifier.fillMaxWidth()){Text("结束 · $end")}
  if(start>end)Text("开始日期不能晚于结束日期",fontSize=12.sp,color=MaterialTheme.colorScheme.error)
 }},confirm={CinemaButton({onSelect(start to end);open=false},enabled=start<=end){Text("应用日期")}},dismiss={CinemaTextButton({open=false}){Text("取消")}})
 target?.let{isStart->FlowDatePicker(if(isStart)start else end,{target=null}){if(isStart)start=it else end=it;target=null}}
}
