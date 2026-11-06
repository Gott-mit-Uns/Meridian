package app.meridian.mobile
import android.content.Context
import androidx.compose.runtime.*

/** Presentation-only setting: raw API JSON, coordinates and request payloads stay intact. */
internal object PrivacyMode{
 var enabled by mutableStateOf(false)
 fun load(context:Context){enabled=context.getSharedPreferences("privacy",0).getBoolean("cityOnly",false)}
 fun set(context:Context,value:Boolean){enabled=value;if(android.os.Build.VERSION.SDK_INT>=33)context.hostActivity()?.setRecentsScreenshotEnabled(!value);context.getSharedPreferences("privacy",0).edit().putBoolean("cityOnly",value).apply()}
}
internal fun cityOnlyAddress(value:String):String{
 if(value.isBlank()||value.startsWith("暂无")||value in listOf("—","出发地","目的地","暂无数据","位置暂无数据","起点地址已隐藏","终点地址已隐藏"))return value
 val end=value.indexOf('市')
 if(end>=1){val city=value.substring(0,end+1);return if(end==value.lastIndex)city else city+"*****"}
 return "*****"
}
internal fun privateAddress(value:String)=if(PrivacyMode.enabled)cityOnlyAddress(value)else value
internal fun addressField(key:String)=key.lowercase().let{it.contains("address")||it in setOf("location","carlocation","locationname","startlocation","endlocation","startlocationname","endlocationname","fromlocation","tolocation")}
internal fun privateProse(value:String):String=if(!PrivacyMode.enabled)value else redactKnownPlaces(value,PrivacyAddressBook.places).replace(Regex("[\\p{IsHan}]{2,}市[^\\s，。；！？、\\n<>]*")){cityOnlyAddress(it.value)}
