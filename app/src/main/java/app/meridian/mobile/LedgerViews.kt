package app.meridian.mobile
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import org.json.JSONObject
import java.time.*
import kotlin.math.*
@Composable internal fun ledgerColor(id:String):Color{
 if(LocalGlobalTheme.current.id=="vivid")return when(id){"charge","insurance"->Color(0xFF2CC653);"toll","parking"->Color(0xFF168BFF);"repair"->Color(0xFFFF3B30);"maintenance"->Color(0xFFFF9500);"accessories"->Color(0xFF6254DE);"wash"->Color(0xFF00ADC4);else->Color(0xFFFF6A00)}
 val base=when(id){"charge"->Color(0xFF32967C);"toll"->Color(0xFF427CD1);"repair"->Color(0xFFC58255);"maintenance"->Color(0xFFC89A48);"accessories"->Color(0xFF8F78BC);"parking"->Color(0xFF708DA7);"insurance"->Color(0xFF6D8B7E);"wash"->Color(0xFF5BA7B5);else->Color(0xFF88909B)}
 val tone=lerp(base,accent,.16f);return if(LocalPalette.current.dark)lerp(tone,Color.White,.17f)else tone
}
@Composable internal fun LedgerPaper(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit){val shape=RoundedCornerShape(22.dp);Column(modifier.fillMaxWidth().themeCardShadow(shape).background(glassWhite,shape).border(.5.dp,glassEdge.copy(alpha=.6f),shape).padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp),content=content)}
internal fun ledgerTitle(entry:JSONObject):String{
 val detail=entry.optJSONObject("details")?:JSONObject();val tags=entry.optJSONArray("tags")?.objects().orEmpty().map{it.optString("label")}
 return detail.optString("itemName").ifBlank{detail.optString("serviceName")}.ifBlank{when(costCategory(entry)){"charge"->if("家充" in tags)"家用充电"else tags.firstOrNull{it.contains("充")}?:"充电补能";"toll"->"高速通行";"repair"->"维修服务";"maintenance"->"定期保养";"parking"->"停车支出";"wash"->"洗车养护";"insurance"->detail.optString("insuranceType").ifBlank{"车辆保险"};else->ledgerCategoryName(costCategory(entry))}}
}
internal fun ledgerSubtitle(entry:JSONObject):String{
 val detail=entry.optJSONObject("details")?:JSONObject()
 val parts=when(costCategory(entry)){
 "charge"->listOfNotNull(entry.number("energyKwh")?.let{numberText(it)+" kWh"},entry.optString("unitPrice").takeIf{it.isNotBlank()}?.let{"¥$it/度"})
 "toll"->listOfNotNull(detail.optString("origin").takeIf{it.isNotBlank()}?.let{privateAddress(it)},detail.optString("destination").takeIf{it.isNotBlank()}?.let{privateAddress(it)}).let{if(it.isNotEmpty())listOf(it.joinToString(" → "))else emptyList()}
 "parking"->listOfNotNull(detail.optString("place").takeIf{it.isNotBlank()}?.let{privateAddress(it)},detail.number("durationMinutes")?.let{numberText(it,0)+" 分钟"})
 "repair","maintenance","wash"->listOfNotNull(detail.optString("provider").takeIf{it.isNotBlank()}?.let{privateProse(it)})
 "accessories"->listOfNotNull(detail.optString("quantity").takeIf{it.isNotBlank()}?.let{"数量 $it"},detail.optString("itemUnitPrice").takeIf{it.isNotBlank()}?.let{"单价 ¥$it"})
 "insurance"->listOfNotNull(detail.optString("coverageEnd").takeIf{it.isNotBlank()}?.let{"保障至 $it"})
 else->emptyList()}
 return parts.joinToString(" · ").ifBlank{privateProse(ledgerNote(entry))}.ifBlank{if(entry.optString("source")=="charge")"充电记录 · 费用估算"else"手动记账"}
}
@Composable internal fun LedgerBillRow(entry:JSONObject,onOpen:()->Unit){val color=ledgerColor(costCategory(entry));val shape=RoundedCornerShape(18.dp)
 Row(Modifier.fillMaxWidth().cardClick(onClick=onOpen).themeCardShadow(shape).background(glassWhite,shape).border(.5.dp,glassEdge.copy(alpha=.65f),shape).padding(14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
  Box(Modifier.size(46.dp).background(color.copy(alpha=.11f),RoundedCornerShape(13.dp)),contentAlignment=Alignment.Center){Icon(costIcon(costCategory(entry)),null,Modifier.size(23.dp),tint=color)}
  Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)){Text(privateProse(ledgerTitle(entry)),fontSize=16.sp,fontWeight=FontWeight.Medium,color=ink,maxLines=1,overflow=TextOverflow.Ellipsis);Text(ledgerSubtitle(entry),fontSize=12.sp,lineHeight=18.sp,color=muted,maxLines=2,overflow=TextOverflow.Ellipsis)}
  Column(horizontalAlignment=Alignment.End,verticalArrangement=Arrangement.spacedBy(3.dp)){Text("¥"+numberText(entry.optLong("cents")/100.0,2),fontSize=21.sp,fontWeight=FontWeight.SemiBold,color=ink);if(entry.optString("source")=="charge")Text("估算",fontSize=11.sp,color=color)}
 }
}
@Composable internal fun LedgerSummary(rows:List<JSONObject>,label:String,mileage:Double?=null){
 val total=rows.sumOf{it.optLong("cents")}/100.0;val shape=RoundedCornerShape(26.dp);val tint=accent;val totals=costCategoryTotals(rows).filter{it.cents>0};val tones=totals.map{ledgerColor(it.id)}
 Column(Modifier.fillMaxWidth().themeCardShadow(shape).background(Brush.linearGradient(listOf(lerp(glassWhite,tint,.13f),glassWhite)),shape).border(.6.dp,glassEdge,shape).padding(22.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)){Text(label,fontSize=14.sp,fontWeight=FontWeight.Medium,color=ink);Text("DRIVING · COST JOURNAL",fontSize=10.sp,letterSpacing=1.2.sp,color=muted)};Box(Modifier.size(42.dp).background(tint.copy(alpha=.1f),RoundedCornerShape(14.dp)),contentAlignment=Alignment.Center){Icon(Icons.Outlined.AccountBalanceWallet,null,Modifier.size(23.dp),tint=tint)}}
  Row(verticalAlignment=Alignment.Bottom,horizontalArrangement=Arrangement.spacedBy(6.dp)){Text("¥",fontSize=22.sp,modifier=Modifier.padding(bottom=6.dp),color=muted);Text(numberText(total,2),fontSize=if(total>=1000000)34.sp else 43.sp,lineHeight=49.sp,letterSpacing=(-1).sp,fontWeight=FontWeight.SemiBold,color=ink)}
  if(total>0)Canvas(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp))){var x=0f;totals.forEachIndexed{i,v->val w=(v.cents/(total*100)*size.width).toFloat();drawRect(tones[i],Offset(x,0f),androidx.compose.ui.geometry.Size(w,size.height));x+=w}}
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
   Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)){Text("账单笔数",fontSize=12.sp,color=muted);Text("${rows.size} 笔",fontSize=18.sp,fontWeight=FontWeight.Medium,color=ink)}
   Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)){Text("每公里费用",fontSize=12.sp,color=muted);Text(mileage?.takeIf{it.isFinite()&&it>0}?.let{"¥"+numberText(total/it,2)}?:"—",fontSize=18.sp,fontWeight=FontWeight.Medium,color=tint)}
   Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)){Text("统计里程",fontSize=12.sp,color=muted);Text(mileage?.takeIf{it.isFinite()&&it>0}?.let{numberText(it,1)+" km"}?:"—",fontSize=16.sp,fontWeight=FontWeight.Medium,color=ink)}
  }
  Text("仅统计已记录费用 · 每公里费用按同期里程计算",fontSize=11.sp,lineHeight=17.sp,color=muted)
 }
}
@Composable internal fun LedgerCategoryRibbon(rows:List<JSONObject>,selected:String?,onCategory:(String?)->Unit){
 val totals=costCategoryTotals(rows).sortedByDescending{it.cents}
 if(totals.isNotEmpty())Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("费用速览",Modifier.weight(1f),fontSize=16.sp,fontWeight=FontWeight.SemiBold,color=ink);if(selected!=null)CinemaTextButton({onCategory(null)}){Text("全部账单")}}
  Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(10.dp)){totals.forEach{item->val tone=ledgerColor(item.id);val shape=RoundedCornerShape(18.dp);Column(Modifier.width(140.dp).cardClick{onCategory(if(selected==item.id)null else item.id)}.background(lerp(glassWhite,tone,if(selected==item.id).2f else .07f),shape).border(if(selected==item.id)1.2.dp else .5.dp,if(selected==item.id)tone else glassEdge,shape).padding(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(7.dp)){Icon(costIcon(item.id),null,Modifier.size(19.dp),tint=tone);Text(item.label,fontSize=13.sp,color=ink)};Text("¥"+numberText(item.cents/100.0,2),fontSize=20.sp,fontWeight=FontWeight.SemiBold,color=ink);Text("${item.count} 笔记录",fontSize=11.sp,color=muted)}}}
 }
}
@Composable internal fun LedgerSpendChart(samples:List<ChartSample>,annual:Boolean,onDrill:(String)->Unit){
 var selected by remember(samples){mutableIntStateOf(-1)};val color=accent;val track=glassEdge
 LedgerPaper{
  Text("支出趋势",fontSize=16.sp,fontWeight=FontWeight.SemiBold,color=ink)
  if(samples.isEmpty())Text("暂无账单数据",fontSize=13.sp,color=muted)else{
   val max=samples.maxOf{it.value}.coerceAtLeast(1.0)
   val paint=remember{android.graphics.Paint(3)};val labelColor=muted
   Canvas(Modifier.fillMaxWidth().height(184.dp).pointerInput(samples){detectTapGestures{p->val start=30.dp.toPx();selected=floor((p.x-start)/((size.width-start)/samples.size)).toInt().coerceIn(samples.indices)}}){
    val left=30.dp.toPx();val bottom=size.height-25.dp.toPx();val top=14.dp.toPx();val h=bottom-top;val step=(size.width-left)/samples.size
    paint.textSize=10.sp.toPx();paint.color=labelColor.toArgb();paint.textAlign=android.graphics.Paint.Align.RIGHT
    repeat(4){i->val y=bottom-h*i/3;drawLine(track.copy(alpha=.6f),Offset(left,y),Offset(size.width,y),.6.dp.toPx());drawContext.canvas.nativeCanvas.drawText(numberText(max*i/3,0),left-5.dp.toPx(),y+3.dp.toPx(),paint)}
    samples.forEachIndexed{i,item->val bar=(step*.58f).coerceAtMost(24.dp.toPx());val height=(item.value/max*h).toFloat();val x=left+step*i+(step-bar)/2
     if(height>0)drawRoundRect(if(selected==i)color else color.copy(alpha=if(selected<0).64f else .25f),Offset(x,bottom-height),androidx.compose.ui.geometry.Size(bar,height),androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
     if(samples.size<=12||i%5==0||i==samples.lastIndex){paint.textAlign=android.graphics.Paint.Align.CENTER;drawContext.canvas.nativeCanvas.drawText(if(annual)item.label.takeLast(2)+"月"else item.label.takeLast(2),x+bar/2,size.height-5.dp.toPx(),paint);paint.textAlign=android.graphics.Paint.Align.RIGHT}
    }
   }
   if(selected in samples.indices){val point=samples[selected];Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(point.label+" · ¥"+numberText(point.value,2),Modifier.weight(1f),fontSize=14.sp,color=ink);CinemaTextButton({onDrill(point.label)}){Text("查看账单")}}}else Text("点选柱形查看金额和对应账单",fontSize=12.sp,color=muted)
  }
 }
}
@Composable internal fun LedgerComposition(rows:List<JSONObject>,onCategory:(String)->Unit){val totals=remember(rows){costCategoryTotals(rows).sortedByDescending{it.cents}};val total=totals.sumOf{it.cents};val colors=totals.map{ledgerColor(it.id)}
 LedgerPaper{Text("分类构成",fontSize=16.sp,fontWeight=FontWeight.SemiBold,color=ink)
  if(totals.isEmpty())Text("暂无分类支出",fontSize=13.sp,color=muted)else{
   if(total>0)Box(Modifier.fillMaxWidth().height(148.dp),contentAlignment=Alignment.Center){Canvas(Modifier.size(140.dp).padding(12.dp)){var start=-90f;totals.forEachIndexed{i,item->val sweep=(item.cents.toDouble()/total*360).toFloat();if(sweep>0)drawArc(colors[i],start,sweep,false,style=Stroke(17.dp.toPx()));start+=sweep}};Column(horizontalAlignment=Alignment.CenterHorizontally){Text("总支出",fontSize=12.sp,color=muted);Text("¥"+numberText(total/100.0,2),fontSize=17.sp,fontWeight=FontWeight.SemiBold,color=ink)}}
   totals.forEachIndexed{i,item->Row(Modifier.fillMaxWidth().cardClick{onCategory(item.id)}.padding(vertical=9.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
    Box(Modifier.size(34.dp).background(colors[i].copy(alpha=.1f),RoundedCornerShape(11.dp)),contentAlignment=Alignment.Center){Icon(costIcon(item.id),null,Modifier.size(19.dp),tint=colors[i])};Column(Modifier.weight(1f)){Text(item.label,fontSize=14.sp,color=ink);Text("${item.count}笔 · "+if(total>0)numberText(item.cents.toDouble()/total*100,1)+"%"else"免费账单",fontSize=12.sp,color=muted)};Text("¥"+numberText(item.cents/100.0,2),fontSize=17.sp,fontWeight=FontWeight.Medium,color=ink);Icon(Icons.Outlined.ChevronRight,null,Modifier.size(16.dp),tint=muted)
   }}
  }
 }
}
@Composable internal fun LedgerEntryDetails(entry:JSONObject,onEdit:()->Unit,onDelete:()->Unit,onClose:()->Unit){val id=costCategory(entry);val color=ledgerColor(id);val fields=entry.optJSONObject("details")?:JSONObject()
 FullscreenDataPage(ledgerCategoryName(id)+"详情",onClose,action={ToolbarActionStrip(listOf(ToolbarAction(Icons.Outlined.Edit,"编辑账单",run=onEdit),ToolbarAction(Icons.Outlined.DeleteOutline,"删除账单",run=onDelete)))}){
  Column(Modifier.fillMaxWidth().padding(vertical=12.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)){Box(Modifier.size(58.dp).background(color.copy(alpha=.12f),RoundedCornerShape(19.dp)),contentAlignment=Alignment.Center){Icon(costIcon(id),null,Modifier.size(32.dp),tint=color)};Text(privateProse(ledgerTitle(entry)),fontSize=19.sp,fontWeight=FontWeight.Medium,color=ink);Text("¥"+numberText(entry.optLong("cents")/100.0,2),fontSize=40.sp,fontWeight=FontWeight.SemiBold,color=ink)}
  when(id){
   "charge"->LedgerPaper{
    Text("充电补能",fontSize=16.sp,fontWeight=FontWeight.SemiBold,color=ink);entry.number("energyKwh")?.let{Text(numberText(it,3)+" kWh",fontSize=30.sp,fontWeight=FontWeight.SemiBold,color=color)}?:Text("未记录充电量",fontSize=14.sp,color=muted)
    if(entry.optString("source")=="charge"){val start=entry.number("startBattery");val end=entry.number("endBattery");if(start!=null&&end!=null){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(numberText(start,0)+"%",color=muted);Text(numberText(end,0)+"%",color=color)};Canvas(Modifier.fillMaxWidth().height(8.dp)){drawRoundRect(color.copy(alpha=.12f),cornerRadius=androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()));drawRoundRect(color, size=androidx.compose.ui.geometry.Size(size.width*(end/100).coerceIn(0.0,1.0).toFloat(),size.height),cornerRadius=androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()))}}}
    entry.optString("unitPrice").takeIf{it.isNotBlank()}?.let{LedgerDetailLine("本笔电价","¥$it / kWh")};if(entry.optString("source")=="charge")entry.number("durationMinutes")?.let{LedgerDetailLine("充电时长",numberText(it)+" 分钟")}
    fields.optString("place").takeIf{it.isNotBlank()}?.let{LedgerDetailLine("充电场所",privateAddress(it))}
    Text(when{entry.optString("source")=="charge"->"电量来自充电记录，费用按设置单价估算";entry.optString("chargeBasis")=="cost"->"电量由费用÷电价推算，含服务费时可能存在差异";else->"费用按填写电量×电价计算"},fontSize=12.sp,lineHeight=19.sp,color=muted)
   }
   "toll"->LedgerPaper{
    Text("通行票据",fontSize=16.sp,fontWeight=FontWeight.SemiBold,color=color)
    val origin=fields.optString("origin");val destination=fields.optString("destination")
    if(origin.isNotBlank()||destination.isNotBlank())Row(Modifier.fillMaxWidth().background(color.copy(alpha=.07f),RoundedCornerShape(16.dp)).padding(16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){Icon(Icons.Outlined.TripOrigin,null,Modifier.size(18.dp),tint=color);Text(if(origin.isBlank())"未填写入口"else privateAddress(origin),Modifier.weight(1f),fontSize=15.sp,color=ink);Icon(Icons.Outlined.ArrowForward,null,Modifier.size(18.dp),tint=color);Text(if(destination.isBlank())"未填写出口"else privateAddress(destination),Modifier.weight(1f),fontSize=15.sp,color=ink)}
    fields.optString("distanceKm").takeIf{it.isNotBlank()}?.let{LedgerDetailLine("通行里程","$it km")};fields.optString("payment").takeIf{it.isNotBlank()}?.let{LedgerDetailLine("支付方式",it)};if(fields.length()==0)Text("可编辑补充入口、出口和ETC等通行信息",fontSize=13.sp,color=muted)
   }
   "repair","maintenance"->LedgerPaper{Text(if(id=="repair")"维修工单"else"保养清单",fontSize=17.sp,fontWeight=FontWeight.SemiBold,color=ink);val tags=entry.optJSONArray("tags")?.objects().orEmpty();tags.forEach{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Icon(Icons.Outlined.CheckCircle,null,Modifier.size(18.dp),tint=color);Text(it.optString("label"),fontSize=15.sp,color=ink)}};LedgerOptionalFields(id,fields);if(tags.isEmpty()&&fields.length()==0)Text("尚未补充服务项目",fontSize=13.sp,color=muted)}
   "accessories"->LedgerPaper{Text("物品清单",fontSize=17.sp,fontWeight=FontWeight.SemiBold,color=ink);LedgerOptionalFields(id,fields);if(fields.length()==0)Text("可补充物品名称、数量和单价",fontSize=13.sp,color=muted)}
   "parking"->LedgerPaper{Text("停车票据",fontSize=17.sp,fontWeight=FontWeight.SemiBold,color=color);LedgerOptionalFields(id,fields);if(fields.length()==0)Text("尚未补充停车场所与时长",fontSize=13.sp,color=muted)}
   "insurance"->LedgerPaper{Text("车辆保障",fontSize=17.sp,fontWeight=FontWeight.SemiBold,color=color);LedgerOptionalFields(id,fields);if(fields.length()==0)Text("可补充险种与保障期限",fontSize=13.sp,color=muted)}
   "wash"->LedgerPaper{Text("养护服务",fontSize=17.sp,fontWeight=FontWeight.SemiBold,color=color);LedgerOptionalFields(id,fields);if(fields.length()==0)Text("可补充服务项目与门店",fontSize=13.sp,color=muted)}
   else->if(fields.length()>0)LedgerPaper{LedgerOptionalFields(id,fields)}
  }
  if(id=="toll")entry.optJSONObject("tollEstimate")?.let{quote->LedgerPaper{
   Text("路线费用参考",fontSize=17.sp,fontWeight=FontWeight.SemiBold,color=color)
   LedgerDetailLine("高德返回费用","¥ "+numberText(quote.optLong("providerCents")/100.0,2))
   LedgerDetailLine("本次估算","¥ "+numberText(quote.optLong("payableCents")/100.0,2))
   LedgerDetailLine("ETC 处理",if(quote.optBoolean("applyEtc"))"按 "+quote.optString("discountPercent")+"% 折算"else"未再次折算")
   quote.optString("roads").takeIf{it.isNotBlank()}?.let{Text(privateProse(it),fontSize=13.sp,color=muted)}
   Text("当前规划路线，仅供费用参考；以实际支付为准。",fontSize=12.sp,color=muted)
  }}
  LedgerPaper{LedgerDetailLine("记账日期",entry.optString("date"));LedgerDetailLine("费用分类",ledgerCategoryName(id));LedgerDetailLine("来源",if(entry.optString("source")=="charge")"充电记录 · 自动估算"else tollEntrySource(entry));ledgerNote(entry).takeIf{it.isNotBlank()}?.let{Text("备注",fontSize=13.sp,color=muted);Text(privateProse(it),fontSize=15.sp,lineHeight=23.sp,color=ink)}}
  CinemaTonalButton(onEdit,Modifier.fillMaxWidth()){Icon(Icons.Outlined.Edit,null,Modifier.size(18.dp));Spacer(Modifier.width(8.dp));Text("编辑账单")}
 }
}
@Composable private fun LedgerOptionalFields(category:String,values:JSONObject){ledgerFields(category).forEach{field->values.optString(field.key).takeIf{it.isNotBlank()}?.let{LedgerDetailLine(field.label,if(field.key in listOf("place","origin","destination"))privateAddress(it)else privateProse(it))}}}
@Composable private fun LedgerDetailLine(label:String,value:String){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(16.dp),verticalAlignment=Alignment.Top){Text(label,Modifier.weight(.85f),fontSize=14.sp,lineHeight=22.sp,color=muted);Text(value,Modifier.weight(1.15f),fontSize=15.sp,lineHeight=22.sp,color=ink,textAlign=androidx.compose.ui.text.style.TextAlign.End)}}
