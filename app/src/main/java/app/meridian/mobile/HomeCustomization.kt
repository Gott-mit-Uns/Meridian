package app.meridian.mobile

import android.content.Context
import androidx.compose.ui.graphics.Color
import org.json.JSONArray

internal val homeCardIds=listOf("vehicle","range","summary","condition","ledger","sentry","temperature","brief","recentTrip","recentCharge","analysis","trends","driving","energyTrend","efficiency","time","comparison","dailyAverage","charging","history","report")
internal fun normalizedHomeOrder(input:List<String>):List<String>{val clean=(input.filter{it in homeCardIds}.distinct()+homeCardIds).distinct().toMutableList();if("ledger" !in input){clean.remove("ledger");clean.add((clean.indexOf("condition")+1).coerceIn(0,clean.size),"ledger")};return clean}
internal fun moveHomeCard(order:List<String>,from:String,to:String):List<String>{val clean=normalizedHomeOrder(order).toMutableList();val a=clean.indexOf(from);val b=clean.indexOf(to);if(a>=0&&b>=0&&a!=b){clean.removeAt(a);clean.add(b,from)};return clean}
internal val homeHalfCards=setOf("recentTrip","recentCharge","history","report","trends","driving")
internal fun homeSpan(id:String)=when(id){"condition","ledger","sentry","temperature"->3;in homeHalfCards->3;else->6}
internal fun homeRows(ids:List<String>):List<List<String>>{
 val result=mutableListOf<List<String>>();var row=mutableListOf<String>();var used=0
 for((index,id) in ids.withIndex()){if(id=="condition"&&ids.getOrNull(index+1)=="ledger"&&used>0){result.add(row.toList());row=mutableListOf();used=0};val span=homeSpan(id);if(used+span>6){if(row.isNotEmpty())result.add(row.toList());row=mutableListOf();used=0};row.add(id);used+=span;if(used==6){result.add(row.toList());row=mutableListOf();used=0}}
 if(row.isNotEmpty())result.add(row);return result
}

internal val homeThemeNames=linkedMapOf("pearl" to "珍珠雾蓝","sage" to "苔绿银灰","sand" to "香槟暖砂","lilac" to "暮光淡紫","graphite" to "冷调石墨","cobalt" to "克莱因蓝","coral" to "珊瑚朱红","emerald" to "翡翠鲜绿","electric" to "电光紫","aurora" to "极光流彩","sunset" to "落日霓虹","prism" to "棱镜糖彩","atelier" to "莫兰迪画室","riviera" to "里维埃拉","botanical" to "花园手记","gallery" to "艺术拼色","sorbet" to "春日调色","jewel" to "宝石光谱","charcoal" to "曜石炭灰","ivory" to "瓷白暖雾","clarity" to "晴日分色","citrus" to "柑橘花园","coast" to "海岸光谱","confetti" to "缤纷画报","vivid" to "明亮橙彩")
internal class HomeLayoutStore(context:Context){
 private val prefs=context.getSharedPreferences("home-layout",0)
 fun theme()=prefs.getString("theme","pearl").takeIf{it in homeThemeNames}?:"pearl"
 fun saveTheme(id:String){prefs.edit().putString("theme",id.takeIf{it in homeThemeNames}?:"pearl").apply()}
 private fun key(service:String,car:String)="order-"+java.security.MessageDigest.getInstance("SHA-256").digest((service+"\n"+car).toByteArray()).take(12).joinToString(""){"%02x".format(it)}
 fun order(service:String,car:String)=normalizedHomeOrder(runCatching{val array=JSONArray(prefs.getString(key(service,car),"[]"));(0 until array.length()).map{array.optString(it)}}.getOrDefault(emptyList()))
 fun hidden(service:String,car:String):Set<String> = prefs.getStringSet(key(service,car)+"-hidden",emptySet()).orEmpty().filter{it in homeCardIds}.toSet()
 fun saveHidden(service:String,car:String,hidden:Set<String>){prefs.edit().putStringSet(key(service,car)+"-hidden",hidden).apply()}
 fun saveOrder(service:String,car:String,order:List<String>){prefs.edit().putString(key(service,car),JSONArray(normalizedHomeOrder(order)).toString()).apply()}
}
internal fun homePalette(theme:String,base:MeridianPalette):MeridianPalette{
 if(theme=="vivid")return base.copy(text=if(base.dark)Color(0xFFF3F3F5)else Color(0xFF232326),secondary=if(base.dark)Color(0xFFABABB3)else Color(0xFF777780),accent=Color(0xFFFF6A00),surface=if(base.dark)Color(0xFF202125)else Color.White,border=if(base.dark)Color(0xFF303137)else Color(0xFFECECF0),wash=if(base.dark)Color(0xFF292A2F)else Color(0xFFF7F7FA),background=List(3){if(base.dark)Color.Black else Color(0xFFF4F4F6)},chartColors=listOf(Color(0xFFFF6A00),Color(0xFF168BFF),Color(0xFF2CC653),Color(0xFFFFC900),Color(0xFF6254DE),Color(0xFFFF3B30)))
 if(theme in splitChartThemes){val tone=homeCardColor(theme,"range");return if(base.dark)base.copy(text=Color(0xFFF2F2F4),secondary=Color(0xFFA5A6AE),accent=androidx.compose.ui.graphics.lerp(tone,Color.White,.18f),surface=Color(0xFF202125),border=Color(0xFF303137),wash=Color(0xFF28292E),background=listOf(Color.Black,Color.Black,Color.Black))else base.copy(text=Color(0xFF242932),secondary=Color(0xFF7E8590),accent=tone,surface=Color.White,border=Color(0xFFE7E9ED),wash=Color(0xFFF2F4F6),background=listOf(Color(0xFFF5F5F3),Color(0xFFF5F5F3),Color(0xFFF5F5F3)))}
 if(theme=="ivory")return if(base.dark)homePalette("charcoal",base)else base.copy(text=Color(0xFF272724),secondary=Color(0xFF82817B),accent=Color(0xFF6F7D8A),surface=Color.White,border=Color(0xFFEAE8E2),wash=Color(0xFFF4F3EF),background=listOf(Color(0xFFF5F3EE),Color(0xFFF5F3EE),Color(0xFFF5F3EE)))
 // Neutral charcoal from the supplied reference: no blue/purple tint in the card material.
 if(theme=="charcoal")return if(base.dark)base.copy(text=Color(0xFFF1F1F3),secondary=Color(0xFFA3A4AA),accent=Color(0xFFCCD1D9),surface=Color(0xFF202125),border=Color(0xFF2D2E33),wash=Color(0xFF2A2B30),background=listOf(Color.Black,Color.Black,Color.Black))else base.copy(text=Color(0xFF202125),secondary=Color(0xFF74757D),accent=Color(0xFF525863),surface=Color.White,border=Color(0xFFE4E5E9),wash=Color(0xFFF0F1F3),background=listOf(Color(0xFFF5F5F7),Color(0xFFF5F5F7),Color(0xFFF5F5F7)))

 val tones=when(theme){"gallery"->listOf(Color(0xFF4167A0),Color(0xFFF1F2F5),Color(0xFF242937));"sorbet"->listOf(Color(0xFF9C5D83),Color(0xFFF8F1F4),Color(0xFF30232C));"jewel"->listOf(Color(0xFF487D7A),Color(0xFFEEF3F2),Color(0xFF1F2D2E));"atelier"->listOf(Color(0xFF7D7489),Color(0xFFF1EEF2),Color(0xFF27232C));"riviera"->listOf(Color(0xFF397F91),Color(0xFFEDF3F5),Color(0xFF19282E));"botanical"->listOf(Color(0xFF63816D),Color(0xFFF0F2EC),Color(0xFF222A24));"cobalt"->listOf(Color(0xFF2457E8),Color(0xFFE8EFFF),Color(0xFF152348));"coral"->listOf(Color(0xFFDA3D50),Color(0xFFFFE9EB),Color(0xFF351D26));"emerald"->listOf(Color(0xFF00885F),Color(0xFFE0F5EB),Color(0xFF102C25));"electric"->listOf(Color(0xFF8142E5),Color(0xFFF0E7FF),Color(0xFF271C3C));"aurora"->listOf(Color(0xFF326CE5),Color(0xFFE6EDFF),Color(0xFF17253A));"sunset"->listOf(Color(0xFFD44A61),Color(0xFFFFE8E7),Color(0xFF321D30));"prism"->listOf(Color(0xFF9753D2),Color(0xFFF4E7FC),Color(0xFF2A1E39));"sage"->listOf(Color(0xFF638A7C),Color(0xFFEAF0EC),Color(0xFF1B2520));"sand"->listOf(Color(0xFFA48662),Color(0xFFF3EEE7),Color(0xFF28231E));"lilac"->listOf(Color(0xFF8B83B3),Color(0xFFEEECF6),Color(0xFF24212E));"graphite"->listOf(Color(0xFF6D8194),Color(0xFFEBEFF2),Color(0xFF1C232A));else->listOf(Color(0xFF6E94CB),Color(0xFFEEF1F8),Color(0xFF1D232E))}
 val colorful=when(theme){"aurora"->listOf(Color(0xFFE4EDFF),Color(0xFFE3FAF1),Color(0xFFF0E5FF));"sunset"->listOf(Color(0xFFFFE7E7),Color(0xFFFFF0DF),Color(0xFFF4E5FF));"prism"->listOf(Color(0xFFF4E6FD),Color(0xFFE3F2FF),Color(0xFFFFE9F0));else->null}
 return if(base.dark)base.copy(accent=androidx.compose.ui.graphics.lerp(tones[0],Color.White,.25f),surface=tones[2],border=Color(0xFF343A44),wash=Color(0xFF20242C),background=listOf(Color.Black,Color.Black,Color.Black))else base.copy(accent=tones[0],background=colorful?:listOf(tones[1],Color(0xFFF7F8FB),Color(0xFFF1F3F8)),surface=Color(0xFFFEFEFF),border=Color(0xFFE1E6EF),wash=tones[1].copy(alpha=.65f))
}

/** Stable semantic colour by card identity, independent of drag order. */
internal fun homeCardTint(theme:String,id:String):Color{
 val colors=when(theme){
  "gallery"->listOf(Color(0xFF4274CE),Color(0xFFD27F5C),Color(0xFFDCB34B),Color(0xFF47A28B),Color(0xFFA97EBC),Color(0xFFCD6A87))
  "sorbet"->listOf(Color(0xFFF285AE),Color(0xFF65A6E9),Color(0xFFE8C45D),Color(0xFF70BC9A),Color(0xFFA292DE),Color(0xFFEA9C69))
  "jewel"->listOf(Color(0xFF3C82B7),Color(0xFFAA76C7),Color(0xFFD09B45),Color(0xFF3FA58B),Color(0xFFC06575),Color(0xFF7487D0))
  "atelier"->listOf(Color(0xFF9B8CA6),Color(0xFFA88C77),Color(0xFF849E96),Color(0xFF8D9DB2))
  "riviera"->listOf(Color(0xFF5696AA),Color(0xFFC0A473),Color(0xFF8E9E78),Color(0xFFB08686))
  "botanical"->listOf(Color(0xFF809A7C),Color(0xFFBD949B),Color(0xFFB8A074),Color(0xFF8F8FAA))
  else->return Color.Transparent
 }
 return colors[homeCardIds.indexOf(id).coerceAtLeast(0)%colors.size]
}

internal val splitChartThemes=setOf("clarity","citrus","coast","confetti","vivid")
internal fun homeCardColor(theme:String,id:String):Color{
 val colors=when(theme){
  "vivid"->listOf(Color(0xFFFF6A00),Color(0xFF2CC653),Color(0xFFFFC900),Color(0xFF6254DE),Color(0xFF168BFF),Color(0xFF00ADC4))
  "clarity"->listOf(Color(0xFF3979D6),Color(0xFF26A56B),Color(0xFFE09A23),Color(0xFF7C62D5),Color(0xFFDD6A58),Color(0xFF279BA4))
  "citrus"->listOf(Color(0xFF29917C),Color(0xFFDB922B),Color(0xFF6D75CC),Color(0xFFD66988),Color(0xFF5099BD),Color(0xFF8EAA43))
  "coast"->listOf(Color(0xFF367FC2),Color(0xFFD88862),Color(0xFF389B8B),Color(0xFF9670BF),Color(0xFFBE9B39),Color(0xFFD1657A))
  "confetti"->listOf(Color(0xFF6859C9),Color(0xFF26A293),Color(0xFFE39A2E),Color(0xFFD46386),Color(0xFF438ACB),Color(0xFF7AA34E))
  else->return Color.Transparent
 }
 if(theme=="vivid")return when(id){"energyTrend","analysis","range","vehicle","summary"->Color(0xFFFF6A00);"trends","ledger","recentTrip","report","comparison"->Color(0xFF168BFF);"condition","efficiency","recentCharge","charging"->Color(0xFF2CC653);"time","dailyAverage"->Color(0xFFFFC900);"driving","sentry"->Color(0xFF6254DE);else->Color(0xFFFF9500)}
 val role=when(id){"vehicle","range","summary"->0;"condition","efficiency"->1;"temperature","time","brief"->2;"sentry","driving","analysis"->3;"ledger","report","comparison","dailyAverage"->4;else->5}
 return colors[role]
}
internal fun homeCardPalette(theme:String,id:String,base:MeridianPalette):MeridianPalette{if(theme !in splitChartThemes)return base;val tone=homeCardColor(theme,id);return base.copy(accent=if(base.dark)androidx.compose.ui.graphics.lerp(tone,Color.White,.22f)else tone)}
