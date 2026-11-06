package app.meridian.mobile

import java.time.*
import java.time.format.DateTimeFormatter

private fun localHealthTime(value:String):LocalDateTime?=runCatching{LocalDateTime.parse(value.replace(' ','T'))}.getOrNull()
 ?:runCatching{OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()}.getOrNull()

internal fun healthDetailWindow(start:String,end:String,from:Float,to:Float):Pair<String,String>?{
 val begin=localHealthTime(start)?:return null;val finish=localHealthTime(end)?:return null
 if(!from.isFinite()||!to.isFinite()||!finish.isAfter(begin))return null
 val a=from.coerceIn(0f,100f);val b=to.coerceIn(0f,100f);if(a>=b)return null
 val seconds=Duration.between(begin,finish).seconds
 val format=DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
 val left=begin.plusSeconds((seconds*a/100).toLong());val right=begin.plusSeconds((seconds*b/100).toLong());if(!right.isAfter(left))return null
 return left.format(format) to right.format(format)
}
