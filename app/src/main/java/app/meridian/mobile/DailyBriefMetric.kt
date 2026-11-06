package app.meridian.mobile
import org.json.JSONObject
internal fun dailyBriefMetric(m:JSONObject,isToday:Boolean):HomeMetricValue{
 val label=m.safeText("title",m.safeText("label"));val raw=m.opt("value");val duration=label.contains("驾驶")||label.contains("时长");val numeric=when(raw){is Number->raw.toDouble();is String->raw.trim().toDoubleOrNull();else->null}?.takeIf{it.isFinite()&&it>=0}
 val declared=m.safeText("unit").lowercase();val minutes=if(numeric==null)null else when(declared){"小时","时","h","hour","hours"->numeric*60;"秒","s","second","seconds"->numeric/60;"毫秒","ms"->numeric/60000;else->numeric}
 val value=if(duration&&minutes!=null)durationText((minutes*60000).toLong())else homeValue(raw)
 val hasTimeUnit=duration&&Regex("小时|分钟|时|分|秒|h|min",RegexOption.IGNORE_CASE).containsMatchIn(value)
 val unit=when{duration&&hasTimeUnit->"";duration->m.safeText("unit").ifBlank{"分钟"};label.contains("行程")->m.safeText("unit").ifBlank{"次"};label.contains("里程")->m.safeText("unit").ifBlank{"km"};label.contains("电耗")||label.contains("能耗")->m.safeText("unit").ifBlank{"kWh/100km"};else->m.safeText("unit")}
 return HomeMetricValue(if(isToday)label else label.replace("今日","当日"),value,unit,m.safeText("trend",m.safeText("subtitle")).let{if(isToday)it else it.replace("较昨日","较前日")})
}
