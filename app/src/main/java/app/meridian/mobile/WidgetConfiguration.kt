package app.meridian.mobile
import android.appwidget.AppWidgetManager
import android.content.*
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.*

class WidgetConfigurationActivity:ComponentActivity(){override fun onCreate(state:Bundle?){super.onCreate(state);setResult(RESULT_CANCELED);val id=intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID)?:AppWidgetManager.INVALID_APPWIDGET_ID;val manager=AppWidgetManager.getInstance(this);if(id==AppWidgetManager.INVALID_APPWIDGET_ID||manager.getAppWidgetInfo(id)?.provider!=ComponentName(this,MeridianWidget::class.java)){finish();return};DeviceData.initialize(this);androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window,false);setContent{val vault=remember{Vault(this)};val appearance=remember{AppearanceStore(this).read()};MeridianTheme(vault.theme(),appearance,HomeLayoutStore(this).theme()){WidgetFieldEditor(id,{finish()}){fields->saveWidgetFields(this,id,fields);MeridianWidget.render(this,manager,id);setResult(RESULT_OK,Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id));finish()}}}}}
@Composable internal fun WidgetFieldEditor(id:Int,onClose:()->Unit,onSave:(List<String>)->Unit){
 val context=LocalContext.current;var selected by remember(id){mutableStateOf(widgetFields(context,id))};var showTime by remember(id){mutableStateOf(context.getSharedPreferences("widget-options",0).getBoolean("time:$id",true))};val revision=DeviceData.revision;val snapshot=remember(revision){DeviceData.read("widget")};val preview=remember(selected,snapshot.toString(),LocalPalette.current,showTime){widgetBitmap(context,snapshot,selected,320,160,showTime)}
 FullscreenDataPage("桌面卡片内容",onClose){
  Text("默认4×2，一项主数据搭配三项辅助数据。选择已有指标会交换位置。",fontSize=13.sp,color=muted)
  Image(preview.asImageBitmap(),"桌面卡片预览",Modifier.fillMaxWidth())
  FlowSettingsGroup{listOf("主数据","上方辅助","左下辅助","右下辅助").forEachIndexed{slot,label->Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically){Text(label,Modifier.weight(1f),fontSize=14.sp);SmallSelectionMenu(selected[slot],widgetFieldNames.map{it.key to it.value}){selected=replaceWidgetField(selected,slot,it)}}}}
  Row(Modifier.fillMaxWidth().padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically){Text("显示快照同步时间",Modifier.weight(1f),fontSize=14.sp);CinemaSwitch(showTime,{showTime=it})}
  SolidActionButton({context.getSharedPreferences("widget-options",0).edit().putBoolean("time:$id",showTime).apply();onSave(selected)},Modifier.fillMaxWidth()){Text("保存桌面卡片")}
 }

}
@Composable internal fun WidgetSettingsPage(onClose:()->Unit){
 val context=LocalContext.current;val manager=AppWidgetManager.getInstance(context);val ids=remember{manager.getAppWidgetIds(ComponentName(context,MeridianWidget::class.java))};var editing by remember{mutableStateOf<Int?>(null)}
 FullscreenDataPage("桌面卡片",onClose){Text("默认4×2，支持缩放和内容自定义。具体网格占位由桌面决定。",fontSize=13.sp,color=muted);if(ids.isEmpty())Panel{Text("尚未添加桌面卡片");Text("长按桌面空白区域，在小组件列表添加Meridian。",fontSize=13.sp,color=muted)};ids.forEachIndexed{index,id->FlowSettingsGroup{FlowSettingsRow(Icons.Outlined.Widgets,"桌面卡片 "+(index+1),false){editing=id}}};editing?.let{id->WidgetFieldEditor(id,{editing=null}){saveWidgetFields(context,id,it);MeridianWidget.render(context,manager,id);editing=null}}}
}
