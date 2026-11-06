package app.meridian.mobile

import android.content.*
import android.graphics.*
import android.text.*
import androidx.core.content.FileProvider
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File
import java.util.UUID
import kotlin.math.*

/** Offline poster with actual GPS geometry; no invented roads or map tile dependency. */
internal suspend fun createTripPoster(data:JSONObject,insight:JSONObject?,privacy:SharePrivacy=SharePrivacy(),colors:PosterTheme=PosterTheme()):Bitmap=withContext(Dispatchers.Default){
 val snapshot=prepareShareSnapshot(data,insight,privacy)
 val data=snapshot.data;val insight=snapshot.insight
 val width=1080;val navy=colors.text;val secondary=colors.muted;val blue=colors.accent
 fun paint(color:Int,size:Float=30f,bold:Boolean=false)=TextPaint(Paint.ANTI_ALIAS_FLAG).apply{this.color=color;textSize=size;typeface=Typeface.create("sans-serif",if(bold)Typeface.BOLD else Typeface.NORMAL)}
 val paragraph=insight?.safeText("summary").orEmpty().take(1200)
 val bodyLayout=if(paragraph.isNotBlank())StaticLayout.Builder.obtain(paragraph,0,paragraph.length,paint(navy,30f),888).setIncludePad(false).setLineSpacing(10f,1f).setMaxLines(12).setEllipsize(TextUtils.TruncateAt.END).build()else null
 val infoBottom=1574f;val insightHeight=if(bodyLayout!=null)110+bodyLayout.height else 0
 val bitmap=Bitmap.createBitmap(width,(infoBottom+insightHeight+144).toInt(),Bitmap.Config.ARGB_8888);val canvas=Canvas(bitmap)
 canvas.drawColor(colors.background.first());if(colors.background.size>1)canvas.drawRect(0f,0f,width.toFloat(),bitmap.height.toFloat(),Paint(Paint.ANTI_ALIAS_FLAG).apply{shader=LinearGradient(0f,0f,width.toFloat(),bitmap.height.toFloat(),colors.background.toIntArray(),null,Shader.TileMode.CLAMP)})
 fun text(value:String,x:Float,y:Float,size:Float=30f,color:Int=navy,bold:Boolean=false){canvas.drawText(value,x,y,paint(color,size,bold))}
 fun card(x:Float,y:Float,w:Float,h:Float,color:Int=colors.card){val p=Paint(Paint.ANTI_ALIAS_FLAG).apply{this.color=color};canvas.drawRoundRect(x,y,x+w,y+h,30f,30f,p)}
 fun paragraph(value:String,x:Float,y:Float,w:Int,size:Float=30f,maxLines:Int=2,color:Int=navy){val l=StaticLayout.Builder.obtain(value,0,value.length,paint(color,size),w).setIncludePad(false).setLineSpacing(5f,1f).setMaxLines(maxLines).setEllipsize(TextUtils.TruncateAt.END).build();canvas.save();canvas.translate(x,y);l.draw(canvas);canvas.restore()}
 text("MERIDIAN",60f,76f,28f,blue,true);text("行程回顾",60f,143f,48f,navy,true)
 text(if(privacy.hideTime)"出行日期 · "+data.safeText("tripStartTime")else prettyTime(data.safeText("tripStartTime"))+" — "+shortTime(data.safeText("tripEndTime")),60f,192f,26f,secondary)
 card(60f,226f,960f,366f,colors.wash)
 val grid=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=colors.border;strokeWidth=1f}
 for(i in 1..8){canvas.drawLine(60f+i*106f,226f,60f+i*106f,592f,grid)}
 for(i in 1..3)canvas.drawLine(60f,226f+i*91f,1020f,226f+i*91f,grid)
 val actual=trackPositions(data);val points=if(actual.size>=2)actual else endpointPositions(data)
 if(points.size>=2){
  val xs=points.map{Math.toRadians(it.longitude)};val ys=points.map{-ln(tan(PI/4+Math.toRadians(it.latitude.coerceIn(-85.0,85.0))/2))}
  val xMin=xs.min();val xMax=xs.max();val yMin=ys.min();val yMax=ys.max();val fit=min(800/(xMax-xMin).coerceAtLeast(1e-6),236/(yMax-yMin).coerceAtLeast(1e-6))
  fun point(i:Int)=PointF((540+(xs[i]-(xMin+xMax)/2)*fit).toFloat(),(409+(ys[i]-(yMin+yMax)/2)*fit).toFloat())
  val path=Path();points.indices.forEach{i->val p=point(i);if(i==0)path.moveTo(p.x,p.y)else path.lineTo(p.x,p.y)}
  val route=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=blue;style=Paint.Style.STROKE;strokeWidth=10f;strokeCap=Paint.Cap.ROUND;strokeJoin=Paint.Join.ROUND;if(actual.size<2)pathEffect=DashPathEffect(floatArrayOf(20f,15f),0f)}
  canvas.drawPath(path,route)
  listOf(0 to Color.rgb(36,181,126),points.lastIndex to Color.rgb(243,105,79)).forEach{(i,color)->val p=point(i);canvas.drawCircle(p.x,p.y,17f,Paint(Paint.ANTI_ALIAS_FLAG).apply{this.color=Color.WHITE});canvas.drawCircle(p.x,p.y,11f,Paint(Paint.ANTI_ALIAS_FLAG).apply{this.color=color})}
  text(if(actual.size>=2)"真实 GPS 轨迹 · 路线示意"else"仅起终点示意",86f,564f,21f,secondary)
 }else text(if(privacy.hideRoute)"隐私保护 · 轨迹已隐藏"else"本次行程暂无轨迹",355f,417f,30f,secondary)
 card(60f,612f,960f,176f)
 canvas.drawCircle(94f,657f,8f,Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.rgb(36,181,126)})
 canvas.drawCircle(94f,739f,8f,Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.rgb(243,105,79)})
 paragraph(data.safeText("startLocation",data.safeText("startAddress","出发地")),123f,636f,860,29f)
 paragraph(data.safeText("endLocation",data.safeText("endAddress","目的地")),123f,718f,860,29f)
 val metrics=listOf(
  Triple("行驶里程",numberText(tripDistance(data)),"km"),Triple("行驶时长",tripDurationText(data),""),
  Triple("平均速度",numberText(data.number("avgSpeed")),"km/h"),Triple("最高速度",numberText(data.number("maxSpeed")),"km/h"),
  Triple("行程能耗",numberText(data.number("tripEnergyConsumption")),"kWh/100km"),Triple("累计耗电",numberText(data.number("totalPowerConsumption")),"kWh"),
  Triple("起止续航",numberText(data.number("startRemainingRange"))+" → "+numberText(data.number("endRemainingRange")),"km"),Triple("续航达成率",numberText(data.number("consumptionAchievementRate")),"%"))
 metrics.forEachIndexed{i,(label,value,unit)->val x=60f+(i%2)*490;val y=814f+(i/2)*184;card(x,y,470f,164f);text(label,x+26,y+40,25f,secondary);text(value,x+26,y+100,if(value.length>10)34f else 42f,navy,true);text(unit,x+26,y+137,22f,blue)}
 if(bodyLayout!=null){card(60f,infoBottom,960f,insightHeight.toFloat());text("行程洞察",88f,infoBottom+47,29f,navy,true);canvas.save();canvas.translate(88f,infoBottom+74);bodyLayout.draw(canvas);canvas.restore()}
 text("Meridian · 连接每一次出发",60f,bitmap.height-61f,25f,secondary)
 text("数据来自车辆服务",760f,bitmap.height-61f,22f,secondary)
 bitmap
}

suspend fun shareTripPoster(context:Context,bitmap:Bitmap,label:String="行程图片"){
 val uri=withContext(Dispatchers.IO){
  val folder=File(context.cacheDir,"trip-shares").apply{mkdirs()};val file=File(folder,"meridian-${if(label=="月报图片")"monthly"else if(label=="统计图片")"statistics"else"trip"}-${UUID.randomUUID()}.png")
  file.outputStream().use{check(bitmap.compress(Bitmap.CompressFormat.PNG,100,it)){"图片导出失败"}}
  FileProvider.getUriForFile(context,context.packageName+".files",file)
 }
 context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="image/png";putExtra(Intent.EXTRA_STREAM,uri);clipData=ClipData.newUri(context.contentResolver,label,uri);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)},"分享$label"))
}
