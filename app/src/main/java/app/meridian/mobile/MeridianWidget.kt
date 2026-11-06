package app.meridian.mobile
import android.app.PendingIntent
import android.appwidget.*
import android.content.*
import android.graphics.*
import android.util.SizeF
import android.os.Bundle
import androidx.core.os.BundleCompat
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import org.json.JSONObject

internal val widgetFieldNames=linkedMapOf("battery" to "剩余电量","range" to "剩余续航","today" to "今日里程","recent30" to "近30天里程","temperature" to "车内温度","state" to "车辆状态")
internal fun normalizedWidgetFields(values:List<String>):List<String> = (values+listOf("battery","range","today","temperature")+widgetFieldNames.keys).filter{it in widgetFieldNames}.distinct().take(4)
internal fun replaceWidgetField(values:List<String>,slot:Int,key:String):List<String>{val result=normalizedWidgetFields(values).toMutableList();if(slot !in result.indices||key !in widgetFieldNames)return result;val old=result[slot];val other=result.indexOf(key);result[slot]=key;if(other>=0&&other!=slot)result[other]=old;return result}
internal fun widgetFields(context:Context,id:Int)=normalizedWidgetFields(context.getSharedPreferences("widget-options",0).getString("fields:$id",null)?.split(',').orEmpty())
internal fun saveWidgetFields(context:Context,id:Int,fields:List<String>){context.getSharedPreferences("widget-options",0).edit().putString("fields:$id",normalizedWidgetFields(fields).joinToString(",")).apply()}
internal object WidgetSnapshot{
 fun save(dashboard:JSONObject?){val overview=dashboard?.optJSONObject("overview")?:return;val today=dashboard.optJSONArray("dailyBrief")?.objects()?.firstOrNull{it.optString("title",it.optString("label")).contains("里程")}?.number("value");val state=if(dashboard.optBoolean("_teslamate"))overview.safeText("carMode","状态待更新")else when{overview.number("speed")?.let{it>0}==true->"行驶中";overview.optInt("chargeState")in listOf(1,2)->"充电中";overview.optInt("chargeState")==3->"充电完成";overview.has("speed")->"已停放";else->"—"};val data=JSONObject().put("battery",overview.opt("batteryLevel")).put("range",overview.opt("remainingRange")).put("today",today?:JSONObject.NULL).put("recent30",overview.opt("recent30Mileage")).put("temperature",overview.opt("insideTemp")).put("state",state).put("time",System.currentTimeMillis());DeviceData.write("widget",data);update()}
 fun clear(){DeviceData.write("widget",JSONObject());update()}
 fun update(){val context=DeviceData.context;val manager=AppWidgetManager.getInstance(context);manager.getAppWidgetIds(ComponentName(context,MeridianWidget::class.java)).forEach{MeridianWidget.render(context,manager,it)}}
}
internal fun widgetBitmap(context:Context,data:JSONObject,fields:List<String>,widthDp:Int,heightDp:Int,showTime:Boolean=true):Bitmap{
 val mode=context.getSharedPreferences("session",0).getString("theme","system");val dark=mode=="dark"||(mode=="system"&&context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK==android.content.res.Configuration.UI_MODE_NIGHT_YES)
 val palette=homePalette(HomeLayoutStore(context).theme(),MeridianPalette(dark,androidx.compose.ui.graphics.Color.White,androidx.compose.ui.graphics.Color.Gray,androidx.compose.ui.graphics.Color.Gray,emptyList(),androidx.compose.ui.graphics.Color.White,androidx.compose.ui.graphics.Color.Gray,androidx.compose.ui.graphics.Color.Gray))
 val w=widthDp.coerceIn(180,600).toFloat();val h=heightDp.coerceIn(110,400).toFloat();val bitmap=Bitmap.createBitmap((w*2).toInt(),(h*2).toInt(),Bitmap.Config.ARGB_8888);val canvas=Canvas(bitmap);canvas.scale(2f,2f);val paint=Paint(Paint.ANTI_ALIAS_FLAG);val pad=16f;val font=context.resources.configuration.fontScale.coerceIn(1f,1.3f)
 paint.color=if(dark)palette.surface.toArgb()else Color.WHITE;canvas.drawRoundRect(0f,0f,w,h,24f,24f,paint)
 fun text(value:String,x:Float,y:Float,size:Float,secondary:Boolean=false,maxWidth:Float=w-32f){paint.color=if(dark){if(secondary)0xFFAAAAB3.toInt()else 0xFFF4F4F6.toInt()}else{if(secondary)0xFF777B82.toInt()else 0xFF252931.toInt()};paint.textSize=size*font;paint.typeface=Typeface.create("sans-serif",Typeface.NORMAL);val clipped=android.text.TextUtils.ellipsize(value,android.text.TextPaint(paint),maxWidth,android.text.TextUtils.TruncateAt.END).toString();canvas.drawText(clipped,x,y,paint)}
 // Compact instrument layout: one hero metric and three supporting readings.
 val keys=normalizedWidgetFields(fields);val color=palette.accent.toArgb();val compact=h<150f
 fun unit(key:String)=when(key){"battery"->"%";"range","today","recent30"->"km";"temperature"->"°C";else->""}
 fun value(key:String)=if(key=="state")data.optString(key,"—")else homeValue(data.opt(key))
 text("MERIDIAN",pad,21f,10f,true)
 paint.color=color;paint.alpha=120;canvas.drawCircle(w-pad-3,17f,2.5f,paint);paint.alpha=255
 val top=30f;val bottom=h-23f;val heroW=if(compact)(w-pad*2-18f)/4f else (w-pad*2)*.36f
 val hero=keys[0]
 if(!compact){
  paint.shader=LinearGradient(pad,top,pad+heroW,bottom,intArrayOf(color and 0x00FFFFFF or (if(dark)0x38000000 else 0x18000000),color and 0x00FFFFFF or 0x06000000),null,Shader.TileMode.CLAMP)
  canvas.drawRoundRect(pad,top,pad+heroW,bottom,16f,16f,paint);paint.shader=null
  text(widgetFieldNames[hero].orEmpty(),pad+10,top+19,10f,true,maxWidth=heroW-20)
  text(value(hero),pad+10,top+(bottom-top)*.65f,if(hero=="state")18f else 30f/font,maxWidth=heroW-20)
  text(unit(hero),pad+10,bottom-10,10f,true,maxWidth=heroW-20)
  if(hero=="battery"&&data.number("battery")!=null){val x=pad+heroW-14;paint.color=color;paint.alpha=35;canvas.drawRoundRect(x,top+13,x+3,bottom-13,2f,2f,paint);paint.alpha=230;val level=(data.number("battery")!!/100).coerceIn(0.0,1.0).toFloat();canvas.drawRoundRect(x,bottom-13-(bottom-top-26)*level,x+3,bottom-13,2f,2f,paint);paint.alpha=255}
  val x=pad+heroW+13;val rest=w-pad-x;val middle=top+(bottom-top)*.51f
  text(widgetFieldNames[keys[1]].orEmpty()+" · "+unit(keys[1]),x,top+12,10f,true,maxWidth=rest)
  text(value(keys[1]),x,middle-5,22f/font,maxWidth=rest)
  paint.color=color;paint.alpha=25;canvas.drawLine(x,middle,w-pad,middle,paint);paint.alpha=255
  val smallW=(rest-9)/2
  keys.drop(2).forEachIndexed{i,key->val cx=x+i*(smallW+9);text(widgetFieldNames[key].orEmpty(),cx,middle+15,9f,true,maxWidth=smallW);text(value(key),cx,bottom-9,18f/font,maxWidth=smallW);text(unit(key),cx,bottom+3,8f,true,maxWidth=smallW)}
 }else{
  val cellW=(w-pad*2-18)/4
  keys.forEachIndexed{i,key->val x=pad+i*(cellW+6);text(value(key),x,top+26,18f/font,maxWidth=cellW);text(widgetFieldNames[key].orEmpty(),x,top+42,9f,true,maxWidth=cellW);text(unit(key),x,top+54,8f,true,maxWidth=cellW)}
 }
 val footer=if(showTime&&data.has("time"))"同步 "+java.time.Instant.ofEpochMilli(data.optLong("time")).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))+" · 点击刷新"else if(data.has("time"))"点击打开爱车刷新"else"打开App同步车辆数据"
 text(footer,pad,h-10f,10f,true);return bitmap
}
class MeridianWidget:AppWidgetProvider(){
 override fun onUpdate(context:Context,manager:AppWidgetManager,ids:IntArray){DeviceData.initialize(context);ids.forEach{render(context,manager,it)}}
 override fun onAppWidgetOptionsChanged(context:Context,manager:AppWidgetManager,id:Int,newOptions:Bundle){DeviceData.initialize(context);render(context,manager,id)}
 override fun onDeleted(context:Context,ids:IntArray){val edit=context.getSharedPreferences("widget-options",0).edit();ids.forEach{edit.remove("fields:$it");edit.remove("time:$it")};edit.apply()}
 companion object{
  fun render(context:Context,manager:AppWidgetManager,id:Int){DeviceData.initialize(context);val data=DeviceData.read("widget");val fields=widgetFields(context,id)
   fun view(w:Int,h:Int):RemoteViews{val views=RemoteViews(context.packageName,R.layout.meridian_widget);views.setImageViewBitmap(R.id.widget_image,widgetBitmap(context,data,fields,w,h,context.getSharedPreferences("widget-options",0).getBoolean("time:$id",true)));views.setContentDescription(R.id.widget_image,"Meridian · "+fields.joinToString("，"){widgetFieldNames[it]+" "+homeValue(data.opt(it))}+"。点击打开爱车刷新");val intent=Intent(context,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra("meridianDestination","home");views.setOnClickPendingIntent(R.id.widget_image,PendingIntent.getActivity(context,id,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE));return views}
   val result=if(android.os.Build.VERSION.SDK_INT>=31){val sizes=BundleCompat.getParcelableArrayList(manager.getAppWidgetOptions(id),AppWidgetManager.OPTION_APPWIDGET_SIZES,SizeF::class.java).orEmpty().filter{it.width>0&&it.height>0}.take(8);if(sizes.isNotEmpty())RemoteViews(sizes.associateWith{size->view(size.width.toInt(),size.height.toInt())})else RemoteViews(linkedMapOf(SizeF(220f,110f)to view(220,110),SizeF(280f,150f)to view(280,150),SizeF(320f,240f)to view(320,240),SizeF(480f,280f)to view(480,280)))}else{val options=manager.getAppWidgetOptions(id);view(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,280).coerceAtLeast(180),options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,150).coerceAtLeast(110))}
   manager.updateAppWidget(id,result)
  }
 }
}
