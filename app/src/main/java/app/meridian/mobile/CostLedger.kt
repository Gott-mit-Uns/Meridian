@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package app.meridian.mobile
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.*
import kotlinx.coroutines.*
import org.json.*
import java.time.*
import java.math.BigDecimal

internal fun validCostCents(value:String):Long{val money=value.trim().toBigDecimalOrNull()?:throw IllegalArgumentException("请填写有效金额");require(money>BigDecimal.ZERO&&money<=BigDecimal("10000000")&&money.stripTrailingZeros().scale()<=2){"金额需大于0，最多两位小数"};return money.movePointRight(2).longValueExact()}
@Composable internal fun CostLedger(api:Api,carId:Any,onClose:()->Unit){
 val scope=api.localScope(carId);val ctx=LocalContext.current;val action=rememberServerAction();val colors=posterTheme(LocalPalette.current);val key="ledger:"+scope
 var ledgerPage by remember{mutableStateOf("bills")};var statsKind by remember{mutableStateOf("month")};var anchor by remember{mutableStateOf(LocalDate.now())};var drillPeriod by remember{mutableStateOf<LedgerPeriod?>(null)};val period=if(ledgerPage=="bills"&&drillPeriod!=null)drillPeriod!!else ledgerPeriod(if(ledgerPage=="stats")statsKind else "month",anchor);val month=YearMonth.from(anchor);var selectedId by remember{mutableStateOf<String?>(null)};var searchOpen by remember{mutableStateOf(false)};var search by remember{mutableStateOf("")};var dayFilter by remember{mutableStateOf<LocalDate?>(null)};var dateOpen by remember{mutableStateOf(false)};var revision by remember{mutableIntStateOf(0)};var reload by remember{mutableIntStateOf(0)};var editing by remember{mutableStateOf<JSONObject?>(null)};var confirmDelete by remember{mutableStateOf<JSONObject?>(null)};var issue by remember{mutableStateOf("")};var mileage by remember(period){mutableStateOf<Double?>(null)};var syncing by remember{mutableStateOf(false)};var syncTime by remember{mutableLongStateOf(0L)};var categoryFilter by remember{mutableStateOf<String?>(null)};var priceOpen by remember{mutableStateOf(false)};var repriceTarget by remember{mutableStateOf<YearMonth?>(null)}
 var ledger by remember(scope){mutableStateOf(JSONObject())};var ledgerLoaded by remember(scope){mutableStateOf(false)}
 LaunchedEffect(scope,revision){ledger=withContext(Dispatchers.IO){DeviceData.read(key)};ledgerLoaded=true}
 val entries=remember(ledger){ledger.optJSONArray("entries")?.objects().orEmpty()};val rows=remember(entries,period){ledgerRows(entries,period)};val total=rows.sumOf{it.optLong("cents")}/100.0;val auto=ledger.optBoolean("autoCharge");val rate=ledger.optString("unitPrice");val totals=remember(ledger,period){costCategoryTotals(rows)};val visibleRows=rows.filter{(categoryFilter==null||costCategory(it)==categoryFilter)&&(dayFilter==null||it.optString("date")==dayFilter.toString())&&(search.isBlank()||(ledgerTitle(it)+ledgerNote(it)+ledgerCategoryName(costCategory(it))+it.optString("date")).contains(search.trim(),ignoreCase=true))}
 fun update(transform:(JSONObject)->JSONObject){synchronized(DeviceData){DeviceData.write(key,transform(DeviceData.read(key)))};revision++}
 LaunchedEffect(scope,period,reload){syncing=true;issue=""
  try{if(period.kind=="month"){val report=api.call("/trip-month-stats/get",JSONObject().put("carId",carId).put("month",month.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"))));mileage=report.optJSONObject("data")?.number("monthMileage");syncTime=responseSyncTime(report)}}catch(e:Exception){if(e is CancellationException)throw e}
  try{val policy=withContext(Dispatchers.IO){DeviceData.read(key)};if(api.isTeslaMate||policy.optBoolean("autoCharge")){val price=if(policy.optBoolean("autoCharge"))electricityPrice(policy.optString("unitPrice"))else BigDecimal.ZERO;val records=mutableListOf<JSONObject>();var page=1;var more=true
   while(more&&page<=10){val raw=api.call("/car/charge/history",JSONObject().put("carId",carId).put("startDate",period.start.toString()).put("endDate",period.end.toString()).put("pageNum",page).put("pageSize",50));val data=raw.optJSONObject("data");val fetched=(data?.optJSONArray("list")?:data?.optJSONArray("records")?:raw.optJSONArray("data"))?.objects().orEmpty();records.addAll(fetched);more=hasMoreTripRecords(raw,fetched.size,records.size,50);if(records.size>=500)issue="本次自动同步最多读取500条充电记录；已有本机账单全部参与统计，可缩小日期范围补入记录。";syncTime=responseSyncTime(raw);page++}
   currentCoroutineContext().ensureActive();synchronized(DeviceData){val latest=DeviceData.read(key);if(api.isTeslaMate||latest.optBoolean("autoCharge")){val ignored=latest.optJSONArray("ignoredCharges")?.let{array->(0 until array.length()).map{array.optString(it)}.toSet()}.orEmpty();val grouped=records.filter{record->runCatching{LocalDate.parse(record.optString("startTime",record.optString("chargeStartTime")).take(10))}.getOrNull()?.let{it>=period.start&&it<=period.end}==true}.groupBy{YearMonth.from(LocalDate.parse(it.optString("startTime",it.optString("chargeStartTime")).take(10)))};var merged=latest.optJSONArray("entries")?.objects().orEmpty();grouped.forEach{(recordMonth,items)->if(api.isTeslaMate)merged=tmMergeLedger(merged,items,ignored);if(latest.optBoolean("autoCharge"))merged=mergeChargeCosts(merged,if(api.isTeslaMate)items.filter{tmCost(it)==null}else items,recordMonth,price,ignored,repriceTarget==recordMonth)};latest.put("entries",JSONArray(merged));DeviceData.write(key,latest)}};revision++;if(repriceTarget==month)repriceTarget=null
  }}catch(e:Exception){if(e is CancellationException)throw e;issue=e.message?:"充电数据读取失败，已保留现有账本"}finally{syncing=false}
 }

 val samples=remember(rows,period){ledgerSpendSamples(rows,period)};val sharePalette=totals.filter{it.cents>0}.map{ledgerColor(it.id).toArgb()}
 fun add(){editing=JSONObject().put("date",minOf(if(period.kind=="month"&&month!=YearMonth.now())month.atEndOfMonth()else LocalDate.now(),LocalDate.now()).toString())}
 fun share(){action.run{
  val bitmap=withContext(Dispatchers.Default){usagePoster("账单统计",period.start.toString()+" — "+period.end,listOf(HomeMetricValue("总支出",numberText(total,2),"元"),HomeMetricValue("账单笔数",rows.size.toString(),"笔")),visualCharts=listOf(ReportChart(if(period.kind=="year")"每月支出"else"每日支出","元",samples,"bar"),ReportChart("分类构成","元",totals.filter{it.cents>0}.map{ChartSample(it.label,it.cents/100.0)},"donut")),colors=if(sharePalette.isEmpty())colors else colors.copy(chartColors=sharePalette))};shareTripPoster(ctx,bitmap,"账单统计")
 }}
 LedgerShell(ledgerPage,{ledgerPage=it;search="";categoryFilter=null;dayFilter=null;drillPeriod=null},onClose,buildList{
  if(ledgerPage=="bills")add(ToolbarAction(Icons.Outlined.Search,"搜索账单",active=searchOpen){searchOpen=!searchOpen})
  add(ToolbarAction(Icons.Outlined.PriceChange,"电价与自动记账"){priceOpen=true})
  if(ledgerPage=="stats")add(ToolbarAction(Icons.Outlined.IosShare,"分享账单统计",rows.isNotEmpty()&&!action.busy){share()})
 },syncing,{reload++},syncTime,{add()},resetKey="${period.start}|${period.end}|$categoryFilter|$dayFilter|$search"){
  item("period"){
   Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
    if(ledgerPage=="stats")CompactSegments(listOf("week" to "周","month" to "月","year" to "年"),statsKind,{statsKind=it;categoryFilter=null;dayFilter=null})
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Box(Modifier.weight(1f)){CinemaTextButton({dateOpen=true}){Text(period.label,fontSize=15.sp,fontWeight=FontWeight.Medium);Spacer(Modifier.width(5.dp));Icon(Icons.Outlined.CalendarMonth,null,Modifier.size(16.dp))}};ToolbarActionStrip(listOf(ToolbarAction(Icons.Outlined.ChevronLeft,"上一周期"){anchor=shiftLedgerDate(period.kind,anchor,-1);if(drillPeriod!=null)drillPeriod=ledgerPeriod(period.kind,anchor);dayFilter=null},ToolbarAction(Icons.Outlined.ChevronRight,"下一周期",ledgerPeriod(period.kind,shiftLedgerDate(period.kind,anchor,1)).start>period.start){anchor=minOf(shiftLedgerDate(period.kind,anchor,1),LocalDate.now());if(drillPeriod!=null)drillPeriod=ledgerPeriod(period.kind,anchor);dayFilter=null}))}
   }
  }
  if(api.isTeslaMate)item("sourceNotice"){Text("金额来源分别标记为原始记录、分时计算、人工估算或本机估算；未知费用不补零。账本按元显示，请与原服务的记账币种保持一致。",fontSize=12.sp,color=muted)}
  item("summary"){if(ledgerLoaded)LedgerSummary(rows,if(api.isTeslaMate)"本期已记金额"else if(ledgerPage=="stats")"所选周期支出"else if(drillPeriod!=null)"所选范围支出"else"本月支出",if(period.kind=="month")mileage else null)}
  if(!ledgerLoaded)item("loading"){PageSkeleton()}else if(ledgerPage=="bills"){
   item("categoryRibbon"){LedgerCategoryRibbon(rows,categoryFilter){categoryFilter=it}}
   if(searchOpen)item("search"){OutlinedTextField(search,{search=it},Modifier.fillMaxWidth(),label={Text("搜索费用名称、分类或备注")},singleLine=true,trailingIcon={if(search.isNotBlank())CinemaIconButton({search=""}){Icon(Icons.Outlined.Close,"清空搜索")}})}
   if(categoryFilter!=null||dayFilter!=null||drillPeriod!=null)item("filter"){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(listOfNotNull(categoryFilter?.let{ledgerCategoryName(it)},dayFilter?.toString()).joinToString(" · "),Modifier.weight(1f),fontSize=13.sp,color=accent);CinemaTextButton({categoryFilter=null;dayFilter=null;drillPeriod=null}){Text("月度账单")}}}
   if(visibleRows.isEmpty())item("empty"){LedgerPaper{Text(if(rows.isEmpty())"所选范围还没有账单"else"没有符合条件的账单",fontSize=17.sp,fontWeight=FontWeight.Medium);Text("点击右下角＋，记录每一次用车支出",fontSize=14.sp,color=muted)}}
   visibleRows.sortedByDescending{it.optString("date")}.groupBy{it.optString("date")}.forEach{(date,dayRows)->
    item("date:$date"){Row(Modifier.fillMaxWidth().padding(top=7.dp,bottom=1.dp),verticalAlignment=Alignment.CenterVertically){Text((if(date==LocalDate.now().toString())"今天"else if(date==LocalDate.now().minusDays(1).toString())"昨天"else date)+" · ${dayRows.size}笔",Modifier.weight(1f),fontSize=14.sp,fontWeight=FontWeight.Medium,color=ink);Text("¥"+numberText(dayRows.sumOf{it.optLong("cents")}/100.0,2),fontSize=15.sp,fontWeight=FontWeight.Medium,color=ink)}}
    items(dayRows,key={"entry:"+it.optString("id")}){entry->LedgerBillRow(entry){selectedId=entry.optString("id")}}
   }
  }else{
   item("comparison"){
    val previous=ledgerRows(entries,previousLedgerPeriod(period)).sumOf{it.optLong("cents")};val current=rows.sumOf{it.optLong("cents")}
    Text(if(previous>0)"较上期同期 "+(if(current>previous)"↑ "else"↓ ")+numberText(kotlin.math.abs((current-previous).toDouble()/previous*100),1)+"%"else"暂无上期同期支出用于对比",fontSize=13.sp,color=muted)
   }
   item("spend"){LedgerSpendChart(samples,period.kind=="year"){key->if(period.kind=="year"){anchor=YearMonth.parse(key).atDay(1);drillPeriod=ledgerPeriod("month",anchor);dayFilter=null}else{anchor=LocalDate.parse(key);drillPeriod=LedgerPeriod("day",anchor,anchor);dayFilter=anchor};categoryFilter=null;ledgerPage="bills"}}
   item("composition"){LedgerComposition(rows){drillPeriod=period;categoryFilter=it;dayFilter=null;ledgerPage="bills"}}
   item("footnote"){Text("${period.start} — ${period.end} · 按记账日期统计；免费账单保留笔数，不占费用扇区。",fontSize=12.sp,lineHeight=19.sp,color=muted)}
  }
  if(issue.isNotBlank())item("issue"){LedgerPaper{Text(issue,color=MaterialTheme.colorScheme.error);CinemaTextButton({reload++}){Text("重试")}}}
  item("notice"){ServerNotice(action)}
 }
 if(dateOpen&&period.kind!="year")FlowDatePicker(minOf(anchor,LocalDate.now()),{dateOpen=false}){anchor=it;dayFilter=null;dateOpen=false}
 if(dateOpen&&period.kind=="year")FlowDetailPanel({dateOpen=false},title={Text("选择年度")},body={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){val first=entries.mapNotNull{runCatching{LocalDate.parse(it.optString("date")).year}.getOrNull()}.minOrNull()?:LocalDate.now().year-5;(LocalDate.now().year downTo minOf(first,LocalDate.now().year-5)).forEach{year->CinemaChoice(anchor.year==year,{anchor=anchor.withYear(year);drillPeriod=null;dayFilter=null;dateOpen=false},{Text("${year}年")},Modifier.fillMaxWidth())}}},confirm={CinemaTextButton({dateOpen=false}){Text("完成")}})
 val selected=selectedId?.let{id->entries.firstOrNull{it.optString("id")==id}}
 if(selected!=null)LedgerEntryDetails(selected,{editing=selected},{confirmDelete=selected},{selectedId=null})
 editing?.let{entry->CostEntryEditor(entry,rate,{editing=null}){next->update{current->val list=current.optJSONArray("entries")?.objects().orEmpty();validateTollDuplicate(next,list);current.put("entries",JSONArray(list.filterNot{it.optString("id")==next.optString("id")}+next))};editing=null}}
  confirmDelete?.let{entry->FlowDetailPanel({confirmDelete=null},title={Text("删除这笔记账？")},body={Text("删除的自动充电记录不会在下次刷新时重复导入。")},confirm={CinemaButton({update{current->val id=entry.optString("id");if(id.startsWith("auto-charge:")){val ignored=current.optJSONArray("ignoredCharges")?:JSONArray();ignored.put(id);current.put("ignoredCharges",ignored)};current.put("entries",JSONArray(current.optJSONArray("entries")?.objects().orEmpty().filterNot{it.optString("id")==id}))};confirmDelete=null;selectedId=null},colors=ButtonDefaults.buttonColors(containerColor=MaterialTheme.colorScheme.error,contentColor=themeOnAccent(MaterialTheme.colorScheme.error))){Text("删除")}},dismiss={CinemaTextButton({confirmDelete=null}){Text("取消")}})}
  if(priceOpen){var value by remember{mutableStateOf(rate)};var enabled by remember{mutableStateOf(auto)};var reprice by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")};FlowDetailPanel({priceOpen=false},title={Text("充电费用估算")},body={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("自动补入充电费用",Modifier.weight(1f));Switch(enabled,{enabled=it})};OutlinedTextField(value,{value=it.take(10)},label={Text("每度电单价 · 元/kWh")},keyboardOptions=androidx.compose.foundation.text.KeyboardOptions(keyboardType=androidx.compose.ui.text.input.KeyboardType.Decimal));Text("0表示按零金额估算。单价用于新增记录，已有记录保留原单价；手动编辑过的记录不会被自动覆盖。关闭不会删除已有估算。",fontSize=12.sp,color=muted);Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("按新单价重算当前月自动记录",Modifier.weight(1f),fontSize=13.sp);Switch(reprice,{reprice=it})};if((ledger.optJSONArray("ignoredCharges")?.length()?:0)>0)CinemaTextButton({update{it.put("ignoredCharges",JSONArray())};reload++;priceOpen=false}){Text("恢复已忽略的充电记录")};if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)}},confirm={CinemaButton({try{val next=if(enabled||value.isNotBlank())electricityPrice(value).toPlainString()else"";update{it.put("unitPrice",next).put("autoCharge",enabled)};repriceTarget=if(reprice)month else null;reload++;priceOpen=false}catch(e:Exception){error=e.message?:"电价无效"}}){Text("保存设置")}},dismiss={CinemaTextButton({priceOpen=false}){Text("取消")}})}
}
internal fun costIcon(id:String)=when(id){"charge"->Icons.Outlined.EvStation;"toll"->Icons.Outlined.Route;"repair"->Icons.Outlined.Build;"maintenance"->Icons.Outlined.Handyman;"accessories"->Icons.Outlined.ShoppingBag;"parking"->Icons.Outlined.LocalParking;"insurance"->Icons.Outlined.Security;"wash"->Icons.Outlined.LocalCarWash;else->Icons.Outlined.ReceiptLong}
@Composable private fun CostEntryEditor(entry:JSONObject,defaultRate:String,onClose:()->Unit,onSave:(JSONObject)->Unit){
 var tollQuote by remember{mutableStateOf(entry.optJSONObject("tollEstimate"))}
 var date by remember{mutableStateOf(entry.optString("date",LocalDate.now().toString()))};var amount by remember{mutableStateOf(if(entry.has("cents"))BigDecimal(entry.optLong("cents")).movePointLeft(2).toPlainString()else"")}
 var category by remember{mutableStateOf(if(entry.has("id"))costCategory(entry)else"")};var note by remember{mutableStateOf(if(entry.optString("source")=="charge")""else entry.optString("note"))};var error by remember{mutableStateOf("")};var choosingDate by remember{mutableStateOf(false)}
 var rate by remember{mutableStateOf(entry.optString("unitPrice").ifBlank{defaultRate})};var energy by remember{mutableStateOf(entry.number("energyKwh")?.let{BigDecimal.valueOf(it).stripTrailingZeros().toPlainString()}.orEmpty())};var byEnergy by remember{mutableStateOf(entry.optString("source")=="charge"||entry.optString("chargeBasis")=="energy")}
 val detailValues=remember{mutableStateMapOf<String,String>().apply{val saved=entry.optJSONObject("details");ledgerFields(category).forEach{put(it.key,saved?.optString(it.key).orEmpty())}}}
 var tags by remember{mutableStateOf(entry.optJSONArray("tags")?.objects().orEmpty().map{it.optString("label")}.toSet())}
 val price=runCatching{electricityPrice(rate)}.getOrNull()
 val estimated=if(category=="charge"&&price!=null)runCatching{if(byEnergy)validChargeEnergy(energy)else estimatedChargeKwh(amount.toBigDecimal().multiply(BigDecimal(100)).longValueExact(),price)}.getOrNull()else null
 val calculated=if(byEnergy&&price!=null)runCatching{estimatedChargeCents(validChargeEnergy(energy).toDouble(),price)}.getOrNull()else null
 FlowDetailPanel(onClose,title={Text(if(entry.has("id"))"编辑账单"else"记一笔")},body={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
  Text("费用项目 · 单选，图表按此分类",fontSize=13.sp,color=muted)
  FlowRow(horizontalArrangement=Arrangement.spacedBy(7.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){costCategories.forEach{(id,label)->CinemaChoice(category==id,{category=id;tags=emptySet();detailValues.clear();tollQuote=null},{Icon(costIcon(id),null,Modifier.size(16.dp));Spacer(Modifier.width(4.dp));Text(label)})}}
  if(category=="charge"){
   CompactSegments(listOf("cost" to "费用反算电量","energy" to "电量计算费用"),if(byEnergy)"energy"else"cost",{byEnergy=it=="energy"})
   OutlinedTextField(rate,{rate=it.take(10)},Modifier.fillMaxWidth(),label={Text("本笔电价 · 元/kWh")},keyboardOptions=androidx.compose.foundation.text.KeyboardOptions(keyboardType=androidx.compose.ui.text.input.KeyboardType.Decimal))
  }
  if(category=="charge"&&byEnergy)OutlinedTextField(energy,{energy=it.take(12)},Modifier.fillMaxWidth(),label={Text("充电量 · kWh")},keyboardOptions=androidx.compose.foundation.text.KeyboardOptions(keyboardType=androidx.compose.ui.text.input.KeyboardType.Decimal))
  else OutlinedTextField(amount,{amount=it.take(12)},Modifier.fillMaxWidth(),label={Text("金额 · 元")},keyboardOptions=androidx.compose.foundation.text.KeyboardOptions(keyboardType=androidx.compose.ui.text.input.KeyboardType.Decimal))
  if(category=="charge")Panel{
   Text(if(byEnergy)"计算费用 · ¥ "+(calculated?.let{numberText(it/100.0,2)}?:"—")else"推算充电量 · "+(estimated?.stripTrailingZeros()?.toPlainString()?:"—")+" kWh",fontSize=18.sp,color=ink)
   Text(if(!byEnergy&&price?.compareTo(BigDecimal.ZERO)==0)"免费电价无法根据费用反算电量，请切换为电量计算费用。"else if(byEnergy)"按充电量×电价计算，未额外加入服务费。"else"按整笔费用和电价推算；含服务费或分时电价时可能与实际电量不同。",fontSize=12.sp,color=muted)
  }
  CinemaOutlineButton({choosingDate=true}){Icon(Icons.Outlined.CalendarMonth,null,Modifier.size(17.dp));Spacer(Modifier.width(5.dp));Text(date)}
  if(choosingDate)FlowDatePicker(runCatching{LocalDate.parse(date)}.getOrDefault(LocalDate.now()),{choosingDate=false}){date=it.toString();choosingDate=false}
  if(costNoteOptions(category).isNotEmpty()){Text("常用备注 · 可多选",fontSize=13.sp,color=muted);FlowRow(horizontalArrangement=Arrangement.spacedBy(7.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){costNoteOptions(category).forEach{label->CinemaChoice(label in tags,{tags=if(label in tags)tags-label else tags+label},{Text(label)})}}}
  ledgerFields(category).forEach{field->OutlinedTextField(detailValues[field.key].orEmpty(),{detailValues[field.key]=it.take(160);if(category=="toll")tollQuote=null},Modifier.fillMaxWidth(),label={Text(field.label+"（可选）")},keyboardOptions=KeyboardOptions(keyboardType=if(field.numeric)KeyboardType.Decimal else KeyboardType.Text))}
  OutlinedTextField(note,{note=it.take(300)},Modifier.fillMaxWidth(),label={Text("补充备注（可选）")});if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
 }},confirm={CinemaButton({try{
  require(category.isNotBlank()){ "请选择费用项目" };val selected=LocalDate.parse(date);require(selected<=LocalDate.now()){ "日期不能晚于今天" }
  require(category!="charge"||rate.isBlank()||price!=null){"请填写有效电价"}
  val cents=if(category=="charge"&&byEnergy){require(price!=null){"请填写有效电价"};estimatedChargeCents(validChargeEnergy(energy).toDouble(),price)}else if(category=="charge"&&entry.optString("id").startsWith("auto-charge:")&&amount.toBigDecimalOrNull()?.compareTo(BigDecimal.ZERO)==0)0L else if(category=="toll"&&tollQuote?.optLong("payableCents",-1)==0L&&amount.toBigDecimalOrNull()?.compareTo(BigDecimal.ZERO)==0)0L else validCostCents(amount)
  validateLedgerFields(category,detailValues)
  val next=JSONObject().put("id",entry.optString("id",java.util.UUID.randomUUID().toString())).put("date",selected.toString()).put("cents",cents).put("category",category).put("categoryVersion",1).put("source","manual").put("note",note).put("tags",JSONArray(tags.map{JSONObject().put("label",it)}))
  val details=JSONObject();ledgerFields(category).forEach{field->detailValues[field.key]?.trim()?.takeIf{it.isNotBlank()}?.let{details.put(field.key,it)}};next.put("details",details).put("createdAt",entry.optLong("createdAt",System.currentTimeMillis()))
  if(category=="toll")tollQuote?.let{quote->next.put("tollEstimate",JSONObject(quote.toString())).put("source",if(quote.optLong("payableCents",-1)==cents)"toll-estimate"else"manual")}
  if(category=="charge"&&price!=null){next.put("unitPrice",price.toPlainString()).put("chargeBasis",if(byEnergy)"energy"else"cost");val kwh=if(byEnergy)validChargeEnergy(energy)else estimatedChargeKwh(cents,price);if(kwh!=null)next.put("energyKwh",kwh.toDouble())}
  onSave(next)
 }catch(e:Exception){error=e.message?:"请检查输入"}}){Text("保存")}},dismiss={CinemaTextButton(onClose){Text("取消")}})

}
