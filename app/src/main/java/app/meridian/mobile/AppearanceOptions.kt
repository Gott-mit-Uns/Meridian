package app.meridian.mobile
import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import org.json.JSONObject

data class AppearanceOptions(val accent:Int=6,val glass:Boolean=true,val glassStyle:String="liquid",val glassHighlight:Float=1.15f,val liquidBlur:Float=2f,val liquidRefraction:Float=1f,val interfaceScale:Float=1f,val navigationScale:Float=0f,val lastNavigationScale:Float=1f,val showNavigationLabels:Boolean=true,val motionMode:String="standard"){
 fun normalized()=copy(accent=accent.coerceIn(0,8),glassStyle=if(glassStyle=="blur")"blur"else"liquid",glassHighlight=bounded(glassHighlight,0f,2f,1.15f),liquidBlur=bounded(liquidBlur,0f,12f,2f),liquidRefraction=bounded(liquidRefraction,0f,2f,1f),interfaceScale=bounded(interfaceScale,.75f,1.25f,1f),navigationScale=if(navigationScale<=0f||!navigationScale.isFinite())0f else navigationScale.coerceIn(.8f,1.4f),lastNavigationScale=bounded(lastNavigationScale,.8f,1.4f,1f),motionMode=motionMode.takeIf{it in listOf("standard","reduced","off")}?:"standard")
 fun json()=JSONObject().put("accent",accent).put("glass",glass).put("glassStyle",glassStyle).put("glassHighlight",glassHighlight).put("liquidBlur",liquidBlur).put("liquidRefraction",liquidRefraction).put("interfaceScale",interfaceScale).put("navigationScale",navigationScale).put("lastNavigationScale",lastNavigationScale).put("showNavigationLabels",showNavigationLabels).put("motionMode",motionMode)
 companion object{fun bounded(v:Float,min:Float,max:Float,fallback:Float)=if(v.isFinite())v.coerceIn(min,max)else fallback
 fun parse(j:JSONObject)=AppearanceOptions(j.optInt("accent",6),j.optBoolean("glass",true),j.optString("glassStyle","liquid"),j.optDouble("glassHighlight",1.15).toFloat(),j.optDouble("liquidBlur",2.0).toFloat(),j.optDouble("liquidRefraction",1.0).toFloat(),j.optDouble("interfaceScale",1.0).toFloat(),j.optDouble("navigationScale",0.0).toFloat(),j.optDouble("lastNavigationScale",1.0).toFloat(),j.optBoolean("showNavigationLabels",true),j.optString("motionMode","standard")).normalized()}
}
class AppearanceStore(context:Context){private val preferences=context.getSharedPreferences("appearance",0)
 fun read():AppearanceOptions=try{AppearanceOptions.parse(JSONObject(preferences.getString("options","{}")?:"{}"))}catch(_:Exception){AppearanceOptions()}
 fun save(options:AppearanceOptions){preferences.edit().putString("options",options.normalized().json().toString()).apply()}
}
val LocalAppearance=staticCompositionLocalOf{AppearanceOptions()}
val LocalSystemDensity=staticCompositionLocalOf{Density(1f)}
val flowAccentColors=listOf(Color(0xFFFFD600),Color(0xFF191919),Color(0xFFE51C23),Color(0xFFE5124A),Color(0xFFEF6C00),Color(0xFF0AA64B),Color(0xFF2466E8),Color(0xFF7B34ED),Color.White)
