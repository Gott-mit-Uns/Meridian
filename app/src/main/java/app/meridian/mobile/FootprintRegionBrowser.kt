package app.meridian.mobile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.coroutines.CancellationException
import org.json.*

internal fun regionTripFilters(base:JSONObject,level:String,row:JSONObject,province:String,city:String):JSONObject=JSONObject(base.toString()).apply{
 remove("level");remove("keyword");remove("pageNum");remove("pageSize")
 remove("provinceName");remove("cityName");remove("districtName")
 val name=row.safeText("name");require(name.isNotBlank()){"区域缺少名称"}
 when(level){
  "PROVINCE"->put("provinceName",name)
  "CITY"->{val parent=row.safeText("parentName",province);require(parent.isNotBlank()){"缺少所属省份"};put("provinceName",parent);put("cityName",name)}
  "DISTRICT"->{val p=row.safeText("grandParentName",province);val c=row.safeText("parentName",city);require(p.isNotBlank()&&c.isNotBlank()){"缺少区县所属省市"};put("provinceName",p);put("cityName",c);put("districtName",name)}
  else->throw IllegalArgumentException("不支持的区域层级")
 }
}

@Composable internal fun FootprintRegionBrowser(api:Api,base:JSONObject,initialLevel:String,province:String,city:String,onClose:()->Unit){
 var level by remember{mutableStateOf(initialLevel)};var query by remember{mutableStateOf("")};var keyword by remember{mutableStateOf("")};var page by remember{mutableIntStateOf(1)};var retry by remember{mutableIntStateOf(0)}
 var data by remember{mutableStateOf<JSONObject?>(null)};var loading by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")}
 var selected by remember{mutableStateOf<JSONObject?>(null)};var selectionError by remember{mutableStateOf("")}
 LaunchedEffect(base.toString(),level,keyword,page,retry){loading=true;error="";data=null;try{
  val body=JSONObject(base.toString()).put("level",level).put("pageNum",page).put("pageSize",20)
  if(level!="PROVINCE"&&province.isNotBlank())body.put("provinceName",province)
  if(level=="DISTRICT"&&city.isNotBlank())body.put("cityName",city)
  if(keyword.isNotBlank())body.put("keyword",keyword)
  data=api.call("/car/trip/cityFootprint/detail",body).optJSONObject("data")?:throw Exception("未获得区域排行")
 }catch(e:Exception){if(e is CancellationException)throw e;error=e.message?:"区域读取失败"}finally{loading=false}}
 FullscreenDataPage("到访区域",onClose){
  val options=buildList{add("PROVINCE" to "省份");add("CITY" to "城市");if(city.isNotBlank())add("DISTRICT" to "区县")}
  CompactSegments(options,level,{level=it;page=1})
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically){OutlinedTextField(query,{query=it},Modifier.weight(1f),placeholder={Text("搜索到访区域")},singleLine=true);SearchAction{keyword=query.trim();page=1}}
  if(loading)FlowLoading(label="正在读取到访排行…")
  if(error.isNotBlank())Panel{Text(error,fontSize=13.sp,color=muted);CinemaOutlineButton({retry++}){Text("重新加载")}}
  if(selectionError.isNotBlank())Text(selectionError,fontSize=12.sp,color=MaterialTheme.colorScheme.error)
  val rows=data?.optJSONArray("list")?.objects().orEmpty()
  if(rows.isEmpty()&&!loading&&error.isBlank())Panel{Text("当前范围没有匹配区域",color=muted,fontSize=13.sp)}
  rows.forEachIndexed{i,row->Panel{
   Row(Modifier.fillMaxWidth().clickable{try{selected=regionTripFilters(base,level,row,province,city);selectionError=""}catch(e:Exception){selectionError=e.message?:"无法读取区域行程"}},verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
    Text("${(page-1)*20+i+1}",fontSize=22.sp,color=accent)
    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)){
     Text(row.safeText("name"),fontSize=16.sp,color=ink)
     Text("到访 "+row.safeText("visitTripCount","—")+" 次",fontSize=12.sp,color=muted)
     Text("最近 "+prettyTime(row.safeText("lastVisitTime")),fontSize=11.sp,color=muted)
     row.safeText("firstVisitTime").takeIf{it.isNotBlank()}?.let{Text("首次 "+prettyTime(it),fontSize=11.sp,color=muted)}
    }
    Icon(Icons.Outlined.ChevronRight,"查看关联行程",tint=muted,modifier=Modifier.size(18.dp))
   }
  }}
  if(data!=null)Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically){CinemaOutlineButton({page--},Modifier.weight(1f),enabled=page>1&&!loading){Text("上一页")};Text("$page",fontSize=12.sp,color=muted);CinemaOutlineButton({page++},Modifier.weight(1f),enabled=!loading&&rows.isNotEmpty()&&(if(data!!.has("size"))page*20<data!!.optInt("size")else rows.size==20)){Text("下一页")}}
 }
 selected?.let{RegionRelatedTrips(api,it){selected=null}}
}

@Composable private fun RegionRelatedTrips(api:Api,filters:JSONObject,onClose:()->Unit){
 var page by remember{mutableIntStateOf(1)};var retry by remember{mutableIntStateOf(0)};var data by remember{mutableStateOf<JSONObject?>(null)};var error by remember{mutableStateOf("")};var loading by remember{mutableStateOf(false)}
 var detail by remember{mutableStateOf<JSONObject?>(null)};val action=rememberServerAction()
 LaunchedEffect(page,retry){loading=true;error="";data=null;try{data=api.call("/car/trip/get",JSONObject(filters.toString()).put("pageNum",page).put("pageSize",10)).optJSONObject("data")}catch(e:Exception){if(e is CancellationException)throw e;error=e.message?:"行程读取失败"}finally{loading=false}}
 FullscreenDataPage("区域关联行程",onClose){
  Text(listOf("provinceName","cityName","districtName").map{filters.safeText(it)}.filter{it.isNotBlank()}.joinToString(" · "),fontSize=13.sp,color=muted)
  if(loading)FlowLoading()
  if(error.isNotBlank()){Text(error,fontSize=12.sp,color=MaterialTheme.colorScheme.error);CinemaOutlineButton({retry++}){Text("重试")}}
  ServerNotice(action)
  val rows=data?.optJSONArray("list")?.objects().orEmpty()
  if(rows.isEmpty()&&!loading&&error.isBlank())Text("当前范围暂无关联行程",color=muted)
  rows.forEach{row->ActionPanel(prettyTime(row.safeText("tripStartTime")),Icons.Outlined.Route,{action.run{detail=api.call("/car/trip/getDetail",JSONObject().put("tripId",row.get("id"))).optJSONObject("data")}}){Text(row.safeText("startLocation","出发地")+" → "+row.safeText("endLocation","目的地"),fontSize=13.sp,color=ink);Text(homeValue(row.number("drivingMileage")?.div(1000))+" km",fontSize=12.sp,color=muted)}}
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){CinemaOutlineButton({page--},Modifier.weight(1f),enabled=page>1&&!loading){Text("上一页")};CinemaOutlineButton({page++},Modifier.weight(1f),enabled=!loading&&footprintHasNext(data,page)){Text("下一页")}}
 }
 detail?.let{TripDetailPage(api,it){detail=null}}
}
