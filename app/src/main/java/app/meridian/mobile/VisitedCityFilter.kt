package app.meridian.mobile

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.*
import kotlinx.coroutines.CancellationException
import org.json.*
import java.time.LocalDate

@Composable internal fun VisitedCityFilter(api:Api,carId:Any,start:LocalDate?,end:LocalDate?,value:String,onSelect:(String)->Unit){
 var open by remember{mutableStateOf(false)};var query by remember{mutableStateOf("")};var cities by remember{mutableStateOf<JSONArray?>(null)};var loading by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")};var retry by remember{mutableIntStateOf(0)}
 LaunchedEffect(carId,start,end,retry){loading=true;error="";try{
  cities=api.call("/car/trip/visitedCities",JSONObject().put("carId",carId).apply{start?.let{put("startDate",it.toString())};end?.let{put("endDate",it.toString())}}).optJSONArray("data")?:JSONArray()
  if(value.isNotBlank()&&cities?.objects()?.none{it.safeText("name")==value}==true)onSelect("")
 }catch(e:Exception){if(e is CancellationException)throw e;error=e.message?:"到访城市读取失败"}finally{loading=false}}
 CinemaOutlineButton({open=true},Modifier.fillMaxWidth()){Icon(Icons.Outlined.LocationCity,null,Modifier.size(18.dp));Spacer(Modifier.width(6.dp));Text(value.ifBlank{"按到访城市筛选"},fontSize=13.sp)}
 if(open)FlowDetailPanel({open=false},title={Text("到访城市")},body={Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
  OutlinedTextField(query,{query=it},placeholder={Text("搜索城市")},singleLine=true)
  CinemaChoice(value.isBlank(),{onSelect("");open=false},{Text("全部城市")},Modifier.fillMaxWidth())
  if(loading)FlowLoading(label="正在读取城市…")
  if(error.isNotBlank()){Text(error,fontSize=12.sp,color=MaterialTheme.colorScheme.error);CinemaOutlineButton({retry++}){Text("重新读取")}}
  cities?.objects()?.filter{it.safeText("name").contains(query,true)}?.forEach{row->val name=row.safeText("name");CinemaChoice(value==name,{onSelect(name);open=false},{Column{Text(name);Text(row.safeText("visitTripCount","—")+" 次到访",fontSize=11.sp,color=muted)}},Modifier.fillMaxWidth())}
 }},confirm={CinemaTextButton({open=false}){Text("完成")}})
}
