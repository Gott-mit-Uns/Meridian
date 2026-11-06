package app.meridian.mobile

import android.os.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.doOnLayout
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.json.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.MapTileProviderBase
import org.osmdroid.tileprovider.tilesource.*
import org.osmdroid.util.*
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.*
import kotlin.math.*

data class TrackPosition(val latitude:Double,val longitude:Double)
fun flexibleArray(raw:Any?):JSONArray=when(raw){is JSONArray->raw;is String->try{JSONArray(raw)}catch(_:Exception){try{flexibleArray(JSONObject(raw))}catch(_:Exception){JSONArray()}};is JSONObject->flexibleArray(raw.opt("value"));else->JSONArray()}
fun trackPositions(j:JSONObject):List<TrackPosition>{
 val track=j.optJSONObject("track")?:j
 val rows=flexibleArray(track.opt("locationTrack")?:track.opt("points")?:j.opt("locationTrack"))
 return rows.objects().mapNotNull{p->val lat=p.optDouble("latitude",Double.NaN);val lon=p.optDouble("longitude",Double.NaN);if(lat.isFinite()&&lon.isFinite()&&lat in -85.0..85.0&&lon in -180.0..180.0)TrackPosition(lat,lon)else null}
}
fun endpointPositions(j:JSONObject):List<TrackPosition>{val a=j.optDouble("tripStartLatitude",Double.NaN);val b=j.optDouble("tripStartLongitude",Double.NaN);val c=j.optDouble("tripEndLatitude",Double.NaN);val d=j.optDouble("tripEndLongitude",Double.NaN);return if(listOf(a,b,c,d).all{it.isFinite()}&&a in -85.0..85.0&&c in -85.0..85.0&&b in -180.0..180.0&&d in -180.0..180.0)listOf(TrackPosition(a,b),TrackPosition(c,d))else emptyList()}
private fun amapTiles()=object:OnlineTileSourceBase("Meridian-Amap-8",3,18,256,".png",arrayOf("https://webrd02.is.autonavi.com/appmaptile"),"© Amap"){
 override fun getTileURLString(index:Long)=baseUrl+"?lang=zh_cn&size=1&scale=1&style=8&x="+MapTileIndex.getX(index)+"&y="+MapTileIndex.getY(index)+"&z="+MapTileIndex.getZoom(index)
}

@Composable fun RouteTrackMap(data:JSONObject,modifier:Modifier=Modifier,height:Dp=245.dp){
 val context=LocalContext.current;val owner=LocalLifecycleOwner.current;val palette=LocalPalette.current
 var failures by remember{mutableIntStateOf(0)};var loaded by remember{mutableIntStateOf(0)}
 val tileStatus=remember{Handler(Looper.getMainLooper()){m->when(m.what){MapTileProviderBase.MAPTILE_SUCCESS_ID->loaded++;MapTileProviderBase.MAPTILE_FAIL_ID->failures++};false}}
 val map=remember{Configuration.getInstance().apply{userAgentValue="Meridian/"+BuildConfig.VERSION_NAME+" (Android)";osmdroidBasePath=java.io.File(context.cacheDir,"maps");osmdroidTileCache=java.io.File(context.cacheDir,"maps/tiles")};MapView(context).apply{setTileSource(amapTiles());setMultiTouchControls(true);setBuiltInZoomControls(false);controller.setCenter(GeoPoint(34.0,105.0));controller.setZoom(4.0);tileProvider.tileRequestCompleteHandlers.add(tileStatus);setOnTouchListener{v,event->v.parent?.requestDisallowInterceptTouchEvent(event.action!=MotionEvent.ACTION_UP&&event.action!=MotionEvent.ACTION_CANCEL);false}}}
 val groups=remember(data){(data.optJSONArray("tracks")?.objects()?:listOf(data)).map{row->val real=trackPositions(row);Triple(row,if(real.size>=2)real else endpointPositions(row),real.size>=2)}}
 fun fit(){val all=groups.flatMap{it.second}.map{GeoPoint(it.latitude,it.longitude)};if(all.isNotEmpty())map.doOnLayout{if(it.width>0&&it.height>0){val box=BoundingBox.fromGeoPoints(all);if(box.latitudeSpan<.00001&&box.longitudeSpan<.00001){map.controller.setCenter(all.first());map.controller.setZoom(15.0)}else map.zoomToBoundingBox(box,false,40,17.0,null)}}}
 DisposableEffect(map,owner){val observer=LifecycleEventObserver{_,e->when(e){Lifecycle.Event.ON_RESUME->map.onResume();Lifecycle.Event.ON_PAUSE->map.onPause();else->Unit}};owner.lifecycle.addObserver(observer);map.onResume();onDispose{owner.lifecycle.removeObserver(observer);map.tileProvider.tileRequestCompleteHandlers.remove(tileStatus);tileStatus.removeCallbacksAndMessages(null);map.onPause();map.onDetach()}}
 LaunchedEffect(data,palette.dark){map.setTileSource(amapTiles());map.overlayManager.tilesOverlay.setColorFilter(if(palette.dark)TilesOverlay.INVERT_COLORS else null);map.overlays.clear();groups.forEach{(row,coords,real)->if(coords.size>=2){val points=coords.map{GeoPoint(it.latitude,it.longitude)};map.overlays.add(Polyline(map).apply{setPoints(points);outlinePaint.color=0x40FF7900;outlinePaint.strokeWidth=15f});map.overlays.add(Polyline(map).apply{setPoints(points);outlinePaint.color=0xFFFF7900.toInt();outlinePaint.strokeWidth=7f;if(!real)outlinePaint.pathEffect=android.graphics.DashPathEffect(floatArrayOf(16f,12f),0f)})
   listOf(points.first() to true,points.last() to false).forEach{(point,start)->val px=(30*context.resources.displayMetrics.density).toInt();val bmp=android.graphics.Bitmap.createBitmap(px,px,android.graphics.Bitmap.Config.ARGB_8888);val canvas=android.graphics.Canvas(bmp);val paint=android.graphics.Paint(3);paint.color=if(start)0xFF2AC574.toInt()else 0xFFFF713A.toInt();canvas.drawCircle(px/2f,px/2f,px*.45f,paint);paint.color=android.graphics.Color.WHITE;paint.textSize=px*.45f;paint.textAlign=android.graphics.Paint.Align.CENTER;paint.typeface=android.graphics.Typeface.DEFAULT_BOLD;canvas.drawText(if(start)"起"else"终",px/2f,px*.65f,paint);map.overlays.add(Marker(map).apply{position=point;title=(if(start)"起点："+row.safeText("startLocation")else"终点："+row.safeText("endLocation"));icon=android.graphics.drawable.BitmapDrawable(context.resources,bmp);setAnchor(.5f,.5f)})}

   val samples=(row.optJSONObject("track")?.optJSONArray("speedSamples")?:row.optJSONArray("speedSamples")?:JSONArray()).objects().filter{it.number("speed")!=null&&it.number("speed")!!>=0&&it.number("latitude")!=null&&it.number("longitude")!=null}
   val stride=(samples.size/4).coerceAtLeast(1)
   samples.filterIndexed{index,_->index%stride==0}.take(4).forEach{sample->val text=numberText(sample.number("speed"),0)+" km/h";val density=context.resources.displayMetrics.density;val paint=android.graphics.Paint(3).apply{textSize=11*density;typeface=android.graphics.Typeface.DEFAULT_BOLD};val w=(paint.measureText(text)+20*density).toInt();val h=(27*density).toInt();val bitmap=android.graphics.Bitmap.createBitmap(w,h,android.graphics.Bitmap.Config.ARGB_8888);val canvas=android.graphics.Canvas(bitmap);paint.color=android.graphics.Color.WHITE;canvas.drawRoundRect(0f,0f,w.toFloat(),h.toFloat(),h/2f,h/2f,paint);paint.color=0xFF24AE77.toInt();canvas.drawText(text,10*density,18*density,paint);map.overlays.add(Marker(map).apply{position=GeoPoint(sample.number("latitude")!!,sample.number("longitude")!!);title=text;icon=android.graphics.drawable.BitmapDrawable(context.resources,bitmap);setAnchor(.5f,.5f)})}
  }};map.invalidate();fit()}
 Column(modifier,verticalArrangement=Arrangement.spacedBy(6.dp)){
  Box(Modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(16.dp)).background(softWash).border(.7.dp,glassEdge,RoundedCornerShape(16.dp))){
   AndroidView(factory={map},modifier=Modifier.fillMaxSize())
   Column(Modifier.align(Alignment.TopStart).padding(8.dp).background(glassWhite.copy(alpha=.95f),RoundedCornerShape(12.dp))){TextButton(onClick={map.controller.zoomIn()}){Icon(Icons.Outlined.Add,"放大地图",modifier=Modifier.size(17.dp))};TextButton(onClick={map.controller.zoomOut()}){Icon(Icons.Outlined.Remove,"缩小地图",modifier=Modifier.size(17.dp))}}
   Row(Modifier.align(Alignment.BottomEnd).padding(6.dp).background(glassWhite.copy(alpha=.95f),RoundedCornerShape(12.dp))){TextButton(onClick={fit()}){Icon(Icons.Outlined.CenterFocusStrong,"定位轨迹",modifier=Modifier.size(18.dp))}}
   if(failures>3&&loaded==0)Text("高德底图暂未加载，请检查网络",fontSize=11.sp,color=ink,modifier=Modifier.align(Alignment.TopCenter).padding(8.dp).background(glassWhite,RoundedCornerShape(10.dp)).padding(8.dp))
  }
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("© Amap",fontSize=9.sp,color=muted);if(groups.any{!it.third&&it.second.size>=2})Text("虚线仅连接起终点",fontSize=9.sp,color=muted)}
 }
}

@Composable fun TripRouteThumbnail(item:JSONObject,modifier:Modifier=Modifier){
 val coords=remember(item){trackPositions(item)};val tint=accent;val grid=glassEdge
 Box(modifier.background(softWash,RoundedCornerShape(12.dp)).border(.7.dp,grid,RoundedCornerShape(12.dp)),contentAlignment=Alignment.Center){
  if(coords.size>=2)Canvas(Modifier.fillMaxSize().padding(9.dp)){val minX=coords.minOf{it.longitude};val maxX=coords.maxOf{it.longitude};val minY=coords.minOf{it.latitude};val maxY=coords.maxOf{it.latitude};val dx=(maxX-minX).coerceAtLeast(.00001);val dy=(maxY-minY).coerceAtLeast(.00001);val scale=min(size.width/dx,size.height/dy);val ox=(size.width-dx*scale)/2;val oy=(size.height-dy*scale)/2;val points=coords.map{Offset((ox+(it.longitude-minX)*scale).toFloat(),(size.height-oy-(it.latitude-minY)*scale).toFloat())};val path=Path().apply{moveTo(points.first().x,points.first().y);points.drop(1).forEach{lineTo(it.x,it.y)}};drawPath(path,tint.copy(alpha=.15f),style=Stroke(6.dp.toPx()));drawPath(path,tint,style=Stroke(2.dp.toPx()));drawCircle(Color(0xFF31C581),3.dp.toPx(),points.first());drawCircle(Color(0xFFFF793B),3.dp.toPx(),points.last())}
  else Icon(Icons.Outlined.Route,null,tint=tint.copy(alpha=.6f),modifier=Modifier.size(24.dp))
 }
}
