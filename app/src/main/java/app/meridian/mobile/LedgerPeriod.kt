package app.meridian.mobile
import java.time.*
import java.time.temporal.TemporalAdjusters
import org.json.JSONObject
internal data class LedgerPeriod(val kind:String,val start:LocalDate,val end:LocalDate){val label:String get()=if(kind=="day")start.toString()else if(kind=="year")"${start.year}年"else if(kind=="month")"${start.year}年${start.monthValue}月"else"${start.monthValue}月${start.dayOfMonth}日 — ${end.monthValue}月${end.dayOfMonth}日"}
internal fun ledgerPeriod(kind:String,anchor:LocalDate,today:LocalDate=LocalDate.now()):LedgerPeriod{
 val date=minOf(anchor,today);val start=when(kind){"day"->date;"week"->date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));"year"->date.withDayOfYear(1);else->date.withDayOfMonth(1)}
 val fullEnd=when(kind){"day"->start;"week"->start.plusDays(6);"year"->start.withMonth(12).withDayOfMonth(31);else->YearMonth.from(start).atEndOfMonth()}
 return LedgerPeriod(kind,start,minOf(fullEnd,today))
}
internal fun shiftLedgerDate(kind:String,date:LocalDate,step:Long):LocalDate=when(kind){"day"->date.plusDays(step);"week"->date.plusWeeks(step);"year"->date.plusYears(step);else->date.plusMonths(step)}
internal fun previousLedgerPeriod(period:LedgerPeriod):LedgerPeriod=LedgerPeriod(period.kind,shiftLedgerDate(period.kind,period.start,-1),shiftLedgerDate(period.kind,period.end,-1))
internal fun ledgerRows(entries:List<JSONObject>,period:LedgerPeriod):List<JSONObject> = entries.filter{entry->val date=runCatching{LocalDate.parse(entry.optString("date"))}.getOrNull();date!=null&&date>=period.start&&date<=period.end}.sortedWith(compareByDescending<JSONObject>{it.optString("date")}.thenByDescending{it.optLong("createdAt")})
internal fun ledgerSpendSamples(entries:List<JSONObject>,period:LedgerPeriod):List<ChartSample>{
 val groups=ledgerRows(entries,period).groupBy{it.optString("date").take(if(period.kind=="year")7 else 10)}
 return if(period.kind=="year")(1..period.end.monthValue).map{month->val key=YearMonth.of(period.start.year,month).toString();ChartSample(key,groups[key].orEmpty().sumOf{it.optLong("cents")}/100.0)}else generateSequence(period.start){it.plusDays(1)}.takeWhile{it<=period.end}.map{date->ChartSample(date.toString(),groups[date.toString()].orEmpty().sumOf{it.optLong("cents")}/100.0)}.toList()
}
internal fun ledgerCategoryName(id:String)=costCategories.firstOrNull{it.first==id}?.second?:"其他"
internal fun ledgerNote(entry:JSONObject)=(entry.optJSONArray("tags")?.objects().orEmpty().map{it.optString("label")}+entry.optString("note")).filter{it.isNotBlank()}.joinToString(" · ")
internal data class LedgerField(val key:String,val label:String,val numeric:Boolean=false)
internal fun ledgerFields(category:String):List<LedgerField> = when(category){
 "charge"->listOf(LedgerField("place","充电场所"))
 "toll"->listOf(LedgerField("origin","入口 / 出发地"),LedgerField("destination","出口 / 目的地"),LedgerField("distanceKm","通行里程 · km",true),LedgerField("payment","支付方式，如 ETC"))
 "repair","maintenance"->listOf(LedgerField("provider","服务商 / 门店"),LedgerField("odometerKm","服务时车辆里程 · km",true))
 "accessories"->listOf(LedgerField("itemName","物品名称"),LedgerField("quantity","数量",true),LedgerField("itemUnitPrice","物品单价 · 元",true))
 "parking"->listOf(LedgerField("place","停车场所"),LedgerField("durationMinutes","停车时长 · 分钟",true))
 "insurance"->listOf(LedgerField("provider","保险公司"),LedgerField("insuranceType","险种"),LedgerField("coverageStart","保障开始 · YYYY-MM-DD"),LedgerField("coverageEnd","保障结束 · YYYY-MM-DD"))
 "wash"->listOf(LedgerField("provider","洗车门店"),LedgerField("serviceName","服务项目"))
 else->listOf(LedgerField("itemName","费用名称"))
}
internal fun validateLedgerFields(category:String,values:Map<String,String>){
 ledgerFields(category).filter{it.numeric&&values[it.key].orEmpty().isNotBlank()}.forEach{field->val value=values[field.key]!!.toBigDecimalOrNull();require(value!=null&&value>=java.math.BigDecimal.ZERO&&value<=java.math.BigDecimal("10000000")){"${field.label}需为有效非负数字"}}
 for(key in listOf("coverageStart","coverageEnd")){values[key]?.takeIf{it.isNotBlank()}?.let{require(runCatching{LocalDate.parse(it)}.isSuccess){"保障日期需为 YYYY-MM-DD"}}}
 val start=values["coverageStart"].orEmpty();val end=values["coverageEnd"].orEmpty();require(start.isBlank()||end.isBlank()||LocalDate.parse(end)>=LocalDate.parse(start)){"保障结束日期不能早于开始日期"}
}
