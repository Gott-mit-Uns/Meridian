package app.meridian.mobile

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.*
import org.json.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.*

private data class CountryRegion(val name:String,val path:Path,val hit:android.graphics.Region,val center:Offset)
private fun countryRegions(features:List<JSONObject>,width:Float,height:Float):List<CountryRegion>{
 fun mercator(lat:Double)=-ln(tan(PI/4+Math.toRadians(lat.coerceIn(-80.0,80.0))/2))
 val minX=73.0;val maxX=136.0;val minY=mercator(54.0);val maxY=mercator(17.0)
 // Longitude and Mercator latitude use the same angular unit.
 val xFactor=PI/180;val projectedWidth=(maxX-minX)*xFactor;val ySpan=maxY-minY
 val fit=min((width-24)/projectedWidth.toFloat(),(height-24)/ySpan.toFloat());val dx=(width-projectedWidth*fit)/2;val dy=(height-ySpan*fit)/2
 fun point(a:JSONArray)=Offset((dx+(a.optDouble(0)-minX)*xFactor*fit).toFloat(),(dy+(mercator(a.optDouble(1))-minY)*fit).toFloat())
 return features.mapNotNull{f->val g=f.optJSONObject("geometry")?:return@mapNotNull null;val coords=g.optJSONArray("coordinates")?:return@mapNotNull null;val polygons=if(g.safeText("type")=="Polygon")listOf(coords)else (0 until coords.length()).mapNotNull{coords.optJSONArray(it)};val path=Path().apply{fillType=PathFillType.EvenOdd};var center=Offset.Zero;var largest=0f
  polygons.forEach{polygon->for(i in 0 until polygon.length()){val ring=polygon.optJSONArray(i)?:continue;val points=(0 until ring.length()).mapNotNull{j->ring.optJSONArray(j)?.let(::point)};if(points.size<3)continue;path.moveTo(points[0].x,points[0].y);points.drop(1).forEach{path.lineTo(it.x,it.y)};path.close();if(i==0){var area=0f;var cx=0f;var cy=0f;points.indices.forEach{j->val p=points[j];val q=points[(j+1)%points.size];val cross=p.x*q.y-q.x*p.y;area+=cross;cx+=(p.x+q.x)*cross;cy+=(p.y+q.y)*cross};if(abs(area)>largest){largest=abs(area);center=if(abs(area)>.001f)Offset(cx/(3*area),cy/(3*area))else points[0]}}}}
  val bounds=path.getBounds();val clip=android.graphics.Region(floor(bounds.left).toInt(),floor(bounds.top).toInt(),ceil(bounds.right).toInt(),ceil(bounds.bottom).toInt());val hit=android.graphics.Region().apply{setPath(path.asAndroidPath(),clip)};CountryRegion(f.getJSONObject("properties").safeText("name"),path,hit,center)
 }
}

/** Province heat map is rendered from bundled geometry and never waits for an online tile. */
@Composable fun NationFootprintMap(data:JSONObject?,level:String,province:String,city:String,districts:List<JSONObject> = emptyList(),onRegion:(String)->Unit){
 val context=LocalContext.current;val palette=LocalPalette.current;var size by remember{mutableStateOf(IntSize.Zero)};var zoom by remember(level,province,city){mutableFloatStateOf(1f)};var pan by remember(level,province,city){mutableStateOf(Offset.Zero)}
 var all by remember(level=="NATION"){mutableStateOf<List<JSONObject>>(emptyList())}
 LaunchedEffect(level=="NATION"){all=FootprintGeometry.load(context,level=="NATION")}

 val source=remember(all,level,province,city,districts){if(level=="NATION")all.filter{it.getJSONObject("properties").safeText("layer")=="provinces"}else if(level=="CITY"&&districts.isNotEmpty())districts else{val code=all.firstOrNull{it.getJSONObject("properties").safeText("layer")=="provinces"&&regionName(it.getJSONObject("properties").safeText("name"))==regionName(province)}?.getJSONObject("properties")?.safeText("code")?.take(2);all.filter{val p=it.getJSONObject("properties");p.safeText("layer")=="prefectures"&&code!=null&&p.safeText("code").startsWith(code)}}}
 // Local scopes use their real bounds, fitted independently from the national projection.
 var regions by remember(source,size){mutableStateOf<List<CountryRegion>>(emptyList())}
 LaunchedEffect(source,size){regions=if(size.width<=0||source.isEmpty())emptyList()else withContext(Dispatchers.Default){countryRegions(source,size.width.toFloat(),size.height.toFloat())}}

 val localBounds=remember(regions){if(level=="NATION"||regions.isEmpty())null else regions.map{it.path.getBounds()}.reduce{a,b->Rect(min(a.left,b.left),min(a.top,b.top),max(a.right,b.right),max(a.bottom,b.bottom))}}
 val localScale=localBounds?.let{min((size.width-24)/it.width.coerceAtLeast(1f),(size.height-24)/it.height.coerceAtLeast(1f))}?:1f
 val localOffset=localBounds?.let{Offset(size.width/2f-it.center.x*localScale,size.height/2f-it.center.y*localScale)}?:Offset.Zero
 val visits=(data?.optJSONArray("regions")?:JSONArray()).objects().associateBy{regionName(it.safeText("name"))};val maxVisits=visits.values.maxOfOrNull{it.optInt("visitTripCount")}?.coerceAtLeast(1)?:1
 val labelPaint=remember{android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply{textAlign=android.graphics.Paint.Align.CENTER;typeface=android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.NORMAL)}}
 val latestClick by rememberUpdatedState(onRegion)
 Box(Modifier.fillMaxWidth().height(320.dp).clip(RoundedCornerShape(18.dp)).background(if(palette.dark)Color(0xFF19232F)else Color(0xFFF6FAFD)).border(.7.dp,glassEdge,RoundedCornerShape(18.dp))){
  Canvas(Modifier.fillMaxSize().onSizeChanged{size=it}.pointerInput(level,province,city){detectTransformGestures{_,delta,factor,_->zoom=(zoom*factor).coerceIn(1f,7f);val limitX=size.width*(zoom-1)*.6f;val limitY=size.height*(zoom-1)*.6f;pan=Offset((pan.x+delta.x).coerceIn(-limitX,limitX),(pan.y+delta.y).coerceIn(-limitY,limitY))}}.pointerInput(regions,zoom,pan,localScale,localOffset){detectTapGestures{tap->val center=Offset(size.width/2f,size.height/2f);val local=(tap-pan-center)/zoom+center;val geo=(local-localOffset)/localScale;regions.firstOrNull{it.hit.contains(geo.x.toInt(),geo.y.toInt())}?.let{latestClick(it.name)}}}){
   if(!palette.dark)drawRect(Brush.linearGradient(listOf(Color(0xFFFFF4E6).copy(alpha=.65f),Color(0xFFEAF5FE).copy(alpha=.7f))))
   val center=Offset(this.size.width/2,this.size.height/2)
   withTransform({translate(pan.x,pan.y);scale(zoom,zoom,pivot=center);translate(localOffset.x,localOffset.y);scale(localScale,localScale,pivot=Offset.Zero)}){
    regions.forEach{r->val n=visits[regionName(r.name)]?.optInt("visitTripCount")?:0;val intensity=if(n==0)0f else (.15+sqrt(n.toDouble()/maxVisits)*.85).toFloat();val base=if(palette.dark)Color(0xFF2B3746)else Color.White;drawPath(r.path,if(n>0)lerp(if(palette.dark)Color(0xFF71502F)else Color(0xFFFFE7CB),Color(0xFFF07A19),intensity)else base);drawPath(r.path,if(palette.dark)Color(0xFF45586B)else Color(0xFFCDDFEC),style=Stroke(.8.dp.toPx()/(zoom*localScale))) }
   }
   regions.forEach{r->val n=visits[regionName(r.name)]?.optInt("visitTripCount")?:0;if(level!="NATION"||n>0||zoom>1.35f){val raw=r.center*localScale+localOffset;val p=(raw-center)*zoom+center+pan;val text=regionName(r.name)+(if(n>0)" $n"else"");labelPaint.textSize=if(n>0)10.sp.toPx()else 8.sp.toPx();labelPaint.color=(if(palette.dark)Color(0xFFE6EDF5)else if(n>0)Color(0xFFC65E0A)else Color(0xFF526577)).toArgb();if(n>0){val half=labelPaint.measureText(text)/2+5.dp.toPx();drawRoundRect(if(palette.dark)Color(0xFF302A24)else Color.White,topLeft=Offset(p.x-half,p.y-10.dp.toPx()),size=Size(half*2,16.dp.toPx()),cornerRadius=CornerRadius(5.dp.toPx()))};drawContext.canvas.nativeCanvas.drawText(text,p.x,p.y+2.dp.toPx(),labelPaint)}}
  }
  if(regions.isEmpty())Box(Modifier.align(Alignment.Center)){FlowLoading(label="正在准备地图…")}
  Text("到访热度",fontSize=9.sp,color=muted,modifier=Modifier.align(Alignment.TopEnd).padding(12.dp))
  QuietIconButton(Icons.Outlined.CenterFocusStrong,"重置地图",{zoom=1f;pan=Offset.Zero},Modifier.align(Alignment.BottomEnd).padding(4.dp))
  Text("cn-atlas · 可拖动、缩放和点选区域",fontSize=8.sp,color=muted,modifier=Modifier.align(Alignment.BottomStart).padding(10.dp))
 }
}

private object FootprintGeometry {
 private val mutex=Mutex();private var nation:List<JSONObject>?=null;private var all:List<JSONObject>?=null
 suspend fun load(context:android.content.Context,nationOnly:Boolean):List<JSONObject> = withContext(Dispatchers.IO){mutex.withLock{
  (if(nationOnly)nation else all)?:JSONObject(context.applicationContext.assets.open(if(nationOnly)"maps/china-provinces.json"else"maps/china.json").bufferedReader().use{it.readText()}).getJSONArray("features").objects().also{if(nationOnly)nation=it else all=it}
 }}
}
