package app.meridian.mobile

import android.view.MotionEvent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.*
import org.json.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.*
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.*
import java.time.LocalDate

fun JSONArray.objects():List<JSONObject> =(0 until length()).mapNotNull{optJSONObject(it)}
fun regionName(s:String)=s.replace(Regex("(维吾尔自治区|壮族自治区|回族自治区|特别行政区|自治区|省|市|地区|自治州|盟)$"),"")
@Composable fun NativePageHeader(title:String,onBack:()->Unit,action:(@Composable ()->Unit)?=null){
 Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal=10.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically){QuietIconButton(Icons.Outlined.ArrowBack,"返回",onBack);Text(title,fontSize=18.sp,lineHeight=26.sp,color=ink,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis,modifier=Modifier.weight(1f));action?.invoke()}
}

@Composable fun FootprintPage(api:Api,carId:Any,onBack:()->Unit){
 var regionBrowser by remember{mutableStateOf(false)}
 var dateTarget by remember{mutableStateOf<Boolean?>(null)}
 val context=LocalContext.current;var customStart by remember{mutableStateOf<LocalDate?>(null)};var customEnd by remember{mutableStateOf<LocalDate?>(null)};var rangeSheet by remember{mutableStateOf(false)}
 if(dateTarget!=null)FlowDatePicker((if(dateTarget==true)customStart else customEnd)?:LocalDate.now(),{dateTarget=null}){if(dateTarget==true)customStart=it else customEnd=it;dateTarget=null}
 var kind by remember{mutableStateOf("cities")};var days by remember{mutableIntStateOf(30)};var refresh by remember{mutableIntStateOf(0)}
 var level by remember{mutableStateOf("NATION")};var province by remember{mutableStateOf("")};var city by remember{mutableStateOf("")}
 var tracks by remember(carId){mutableStateOf<JSONObject?>(null)};var nation by remember(carId){mutableStateOf<JSONObject?>(null)};var error by remember{mutableStateOf("")};var loading by remember{mutableStateOf(false)}
 var record by remember{mutableStateOf<JSONObject?>(null)};var selection by remember{mutableStateOf<JSONObject?>(null)};var regionDetail by remember{mutableStateOf<JSONObject?>(null)};var detailPage by remember{mutableIntStateOf(1)};var detailLoading by remember{mutableStateOf(false)};var detailError by remember{mutableStateOf("")};var detailRetry by remember{mutableIntStateOf(0)};val scope=rememberCoroutineScope()
 fun body():JSONObject=JSONObject().put("carId",carId).apply{if(days==-1&&customStart!=null&&customEnd!=null){put("startDate",customStart.toString());put("endDate",customEnd.toString())};if(days>0){put("startDate",LocalDate.now().minusDays(days.toLong()-1).toString());put("endDate",LocalDate.now().toString())}}
 fun drill(name:String){when(level){"NATION"->{province=name;if(regionName(name)in listOf("北京","天津","上海","重庆","香港","澳门")){city=name;level="CITY"}else{city="";level="PROVINCE"}};"PROVINCE"->{city=name;level="CITY"};else->{regionDetail=null;detailError="";detailPage=1;selection=JSONObject().put("name",name).put("request",footprintTripRequest(body(),province,city,name,1))}}}
 LaunchedEffect(carId,days,level,province,city,refresh,kind,customStart,customEnd){loading=true;error="";if(kind=="cities")nation=null else tracks=null;try{
   if(kind=="cities")nation=api.call("/car/trip/cityFootprint/get",body().put("level",level).apply{if(province.isNotBlank())put("provinceName",province);if(city.isNotBlank())put("cityName",city)}).optJSONObject("data")
   else tracks=api.call("/car/trip/trackMap",body().apply{if(has("startDate")){put("startDate",getString("startDate")+" 00:00:00");put("endDate",getString("endDate")+" 23:59:59")}}).optJSONObject("data")
 }catch(e:Exception){if(e is CancellationException)throw e;error=e.message?:"足迹读取失败"}finally{loading=false}}
 LaunchedEffect(selection,detailPage,detailRetry){
  val selected=selection?:return@LaunchedEffect
  detailLoading=true;detailError="";regionDetail=null
  try{
   val request=JSONObject(selected.getJSONObject("request").toString()).put("pageNum",detailPage)
   regionDetail=api.call("/car/trip/get",request).optJSONObject("data")?:throw Exception("服务未返回到访明细")
  }catch(e:Exception){if(e is CancellationException)throw e;detailError=e.message?:"明细读取失败"}finally{detailLoading=false}
 }

 ImmersiveScrollingPage("我的足迹",onBack,action={ToolbarActionStrip(listOf(ToolbarAction(Icons.Outlined.Tune,"筛选足迹",active=days!=30||kind!="cities"){rangeSheet=true}))},refreshing=loading,onRefresh={refresh++},lastSynced=responseSyncTime(if(kind=="cities")nation else tracks)){
   Text((when(kind){"tracks"->"行驶轨迹";"frequent"->"高频路线";else->"全国足迹"})+" · "+(if(days==-1)"${customStart} 至 ${customEnd}"else if(days==0)"全部日期"else"最近 $days 天"),fontSize=12.sp,color=muted)
   CachedReadStatus(if(kind=="cities")nation else tracks){refresh++}
  if(loading)FlowLoading(Modifier.fillMaxWidth(),color=accent)
   if(error.isNotBlank())Panel{Text(error,color=MaterialTheme.colorScheme.error);TextButton(onClick={refresh++}){Text("重新加载")}}
   if(kind=="cities"){
    val current=nation;val summary=current?.optJSONObject("summary");val regions=current?.optJSONArray("regions")?:JSONArray()
    if(province.isNotBlank())Row(Modifier.horizontalScroll(rememberScrollState()),verticalAlignment=Alignment.CenterVertically){
     CinemaTextButton({level="NATION";province="";city=""}){Icon(Icons.Outlined.ArrowBack,null,Modifier.size(14.dp));Text("返回全图",fontSize=12.sp)}
     if(city.isNotBlank())CinemaTextButton({level="PROVINCE";city=""}){Text(province,fontSize=12.sp)}else Text(province,fontSize=13.sp,color=muted)
     if(city.isNotBlank())Text(" / $city",fontSize=13.sp,color=muted)
    }
    NativeFootprintMap(null,current,level,province,city){drill(it)}
    HomeGlass{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){if(level=="NATION"){MiniMetric("到访省份",summary?.opt("provinceCount"),"个");MiniMetric("到访城市",summary?.opt("cityCount"),"个");MiniMetric("到访区县",summary?.opt("districtCount"),"个")}else{MiniMetric("到访区域",summary?.opt("regionCount"),"个");MiniMetric("关联行程",summary?.opt("visitTripCount"),"次")}}}
    CinemaOutlineButton({regionBrowser=true},Modifier.fillMaxWidth()){Text("查看全部到访排行") }
    val init=current?.optJSONObject("initialization")
    if(init?.optBoolean("needsHistoricalInitialization")==true||init?.safeText("status")=="RUNNING")Panel{Text(init.safeText("message","历史足迹待补全"),fontSize=14.sp,color=muted);Button(onClick={scope.launch{try{api.call("/car/trip/cityFootprint/initialize",JSONObject().put("carId",carId));refresh++}catch(e:Exception){if(e is CancellationException)throw e;error=e.message?:"补全失败"}}},enabled=init.safeText("status")!="RUNNING"){Text("补全历史足迹")}}
    if(init?.safeText("status")=="RUNNING")LaunchedEffect(refresh){delay(2000);refresh++}
    Panel{SectionTitle(if(level=="NATION")"到访省份"else if(level=="PROVINCE")"到访城市"else"到访区县",Icons.Outlined.TravelExplore);if(regions.length()==0&&!loading)Text("当前范围暂无足迹",color=muted,fontSize=14.sp);regions.objects().sortedByDescending{it.optInt("visitTripCount")}.forEach{r->Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable{drill(r.safeText("name"))}.padding(vertical=12.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(r.safeText("name"),color=ink,fontSize=14.sp);Text("最近到访 "+prettyTime(r.safeText("lastVisitTime","—")),fontSize=12.sp,color=muted)};Text(r.safeText("visitTripCount","—")+" 次",fontSize=15.sp,color=accent);Icon(Icons.Outlined.ChevronRight,null,tint=muted,modifier=Modifier.size(18.dp))}}}
   }else{
    val routes=tracks?.optJSONArray("frequentRoutes")?.objects()?:emptyList();val all=tracks?.optJSONArray("tracks")?.objects()?:emptyList();val routeKey:(String)->String={v->v.replace("中国","").replace("省","").replace(Regex("\\s+"),"")};val frequentNames=routes.take(5).map{routeKey(it.safeText("routeName"))}.toSet();val filtered=if(kind=="frequent")all.filter{routeKey(it.safeText("startLocation")+" → "+it.safeText("endLocation"))in frequentNames||routeKey(it.safeText("routeName"))in frequentNames}else all
    NativeFootprintMap(remember(filtered){JSONObject().put("tracks",JSONArray(filtered))},null,"","",""){}
    val summary=tracks?.optJSONObject("summary")
    HomeGlass{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){MiniMetric("轨迹里程",summary?.opt("totalMileageKm"),"km");MiniMetric("轨迹行程",summary?.opt("trackTrips"),"次");MiniMetric("平均速度",summary?.opt("avgSpeedKmh"),"km/h")};if(summary?.has("totalDurationMinutes")==true)Text("累计驾驶 "+durationText((summary.optDouble("totalDurationMinutes")*60000).toLong()),fontSize=13.sp,color=muted)}
    if(kind=="frequent")Panel{SectionTitle("高频路线",Icons.Outlined.Route);routes.forEach{r->Text(r.safeText("routeName"),fontSize=15.sp,color=ink);Text(r.safeText("tripCount",r.safeText("count","—"))+" 次",fontSize=13.sp,color=muted)}}
    if(filtered.isEmpty()&&!loading)Panel{Text("当前范围暂无轨迹记录",color=muted)}
    filtered.forEach{r->ActionPanel(r.safeText("startLocation","出发地")+" → "+r.safeText("endLocation","目的地"),Icons.Outlined.Route,{scope.launch{detailLoading=true;detailError="";try{record=api.call("/car/trip/getDetail",JSONObject().put("tripId",r.opt("tripId")?:r.opt("id"))).optJSONObject("data")}catch(e:Exception){if(e is CancellationException)throw e;detailError=e.message?:"行程详情读取失败"}finally{detailLoading=false}}}){Text(prettyTime(r.safeText("tripStartTime")),fontSize=13.sp,color=muted);Text(homeValue((r.opt("drivingMileage")as?Number)?.toDouble()?.div(1000))+" km",color=accent,fontSize=14.sp)}}
   }
   if(selection==null){if(detailLoading)FlowLoading(Modifier.fillMaxWidth());if(detailError.isNotBlank())Text(detailError,color=MaterialTheme.colorScheme.error,fontSize=14.sp)}
 }
 if(rangeSheet&&dateTarget==null)AlertDialog(onDismissRequest={rangeSheet=false},title={Text("筛选足迹")},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
 Text("视图类型",fontSize=13.sp,color=muted)
 listOf("cities" to "全国足迹","tracks" to "行驶轨迹","frequent" to "高频路线").forEach{(id,label)->CinemaChoice(kind==id,{kind=id},{Text(label)},Modifier.fillMaxWidth())}
 Text("日期范围",fontSize=13.sp,color=muted)
 CompactSegments(listOf(7 to "近7天",30 to "近30天",0 to "全部"),days,{days=it;rangeSheet=false})

  OutlinedButton(onClick={dateTarget=true}){Text("开始日期 · "+(customStart?:"请选择"))}
  OutlinedButton(onClick={dateTarget=false}){Text("结束日期 · "+(customEnd?:"请选择"))}
  if(customStart!=null&&customEnd!=null&&customStart!!>customEnd!!)Text("开始日期不能晚于结束日期",color=MaterialTheme.colorScheme.error,fontSize=14.sp)
 }},confirmButton={Button(onClick={days=-1;rangeSheet=false},enabled=customStart!=null&&customEnd!=null&&customStart!!<=customEnd!!){Text("应用日期")}})
 if(selection!=null)FlowDetailPanel(onDismiss={selection=null;regionDetail=null;detailError=""},title={Text(selection!!.safeText("name")+" · 到访行程")},confirm={CinemaTextButton(onClick={selection=null;regionDetail=null;detailError=""}){Text("完成")}},body={
  Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(12.dp)){
   when{
    detailLoading->FlowLoading(Modifier.fillMaxWidth())
    detailError.isNotBlank()->{
     Text("到访明细暂时无法加载",color=ink)
     Text(detailError,color=muted,fontSize=14.sp)
     CinemaOutlineButton(onClick={detailRetry++}){Text("重新加载")}
    }
    else->{
     val rows=regionDetail?.optJSONArray("list")?:JSONArray()
     if(rows.length()==0)Text("当前日期范围暂无到访行程",color=muted)
     rows.objects().forEach{r->
      Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(LocalPalette.current.wash).clickable{scope.launch{
       try{record=api.call("/car/trip/getDetail",JSONObject().put("tripId",r.get("id"))).optJSONObject("data")}catch(e:Exception){if(e is CancellationException)throw e;detailError=e.message?:"行程详情读取失败"}
      }}.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
       Text(prettyTime(r.safeText("tripStartTime")),color=muted,fontSize=13.sp)
       Text(r.safeText("startLocation","出发地")+" → "+r.safeText("endLocation","目的地"),color=ink,fontSize=15.sp)
       Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
        Text(homeValue((r.opt("drivingMileage")as?Number)?.toDouble()?.div(1000))+" km",color=accent,fontSize=14.sp)
        Icon(Icons.Outlined.ChevronRight,"查看行程详情",tint=muted,modifier=Modifier.size(18.dp))
       }
      }
     }
     if(rows.length()>0||detailPage>1)Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
      CinemaOutlineButton(onClick={detailPage--},enabled=detailPage>1,modifier=Modifier.weight(1f)){Text("上一页")}
      Text("$detailPage",color=muted,fontSize=14.sp)
      CinemaOutlineButton(onClick={detailPage++},enabled=footprintHasNext(regionDetail,detailPage),modifier=Modifier.weight(1f)){Text("下一页")}
     }
    }
   }
  }
 })
 if(regionBrowser)FootprintRegionBrowser(api,body(),when(level){"NATION"->"PROVINCE";"PROVINCE"->"CITY";else->"DISTRICT"},province,city){regionBrowser=false}
 if(record!=null)TripDetailPage(api,record!!){record=null}
}

@Composable fun NativeFootprintMap(trackData:JSONObject?,cityData:JSONObject?,level:String,province:String,city:String,onRegion:(String)->Unit){
 val context=LocalContext.current
 var districtFeatures by remember{mutableStateOf<List<JSONObject>>(emptyList())};var boundaryIssue by remember{mutableStateOf("")}
 LaunchedEffect(level,province,city){districtFeatures=emptyList();boundaryIssue="";if(level=="CITY")try{districtFeatures=withContext(Dispatchers.IO){val municipal=regionName(province)in listOf("北京","天津","上海","重庆","香港","澳门");val name=if(municipal)province else city;val kind=if(municipal)"province"else"citys";val encoded=java.net.URLEncoder.encode(name,"UTF-8").replace("+","%20");val hash=java.security.MessageDigest.getInstance("SHA-256").digest((kind+name).toByteArray()).joinToString(""){"%02x".format(it)};val cached=java.io.File(context.cacheDir,"boundary-$hash.json");val text=if(cached.exists())cached.readText()else{val connection=java.net.URL("https://cdn.jsdelivr.net/gh/zhChuXiao/ChinaGeoJson@7aa2c5fabc6a892015625898c3482874b233f9de/$kind/$encoded.json").openConnection().apply{connectTimeout=12000;readTimeout=12000};connection.getInputStream().bufferedReader().use{it.readText()}.also{require(JSONObject(it).optString("type")=="FeatureCollection");cached.writeText(it)}};JSONObject(text).getJSONArray("features").objects()}}catch(e:Exception){if(e is CancellationException)throw e;boundaryIssue="区县边界暂未加载，可继续查看下方到访明细"}}

 if(level.isNotBlank()){
  NationFootprintMap(cityData,level,province,city,districtFeatures,onRegion)
  if(boundaryIssue.isNotBlank())Text(boundaryIssue,fontSize=12.sp,color=muted)
 }else RouteTrackMap(trackData?:JSONObject(),height=290.dp)
}
