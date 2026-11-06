package app.meridian.mobile
import org.json.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.*
internal fun electricityPrice(value:String):BigDecimal{val price=value.trim().toBigDecimalOrNull()?:throw IllegalArgumentException("请输入有效电价");require(price>=BigDecimal.ZERO&&price<=BigDecimal("100")&&price.stripTrailingZeros().scale()<=4){"电价需为0–100元，最多4位小数"};return price}
internal fun estimatedChargeCents(kwh:Double,price:BigDecimal):Long{require(kwh.isFinite()&&kwh>0);return BigDecimal.valueOf(kwh).multiply(price).setScale(2,RoundingMode.HALF_UP).movePointRight(2).longValueExact()}
internal fun mergeChargeCosts(existing:List<JSONObject>,records:List<JSONObject>,month:YearMonth,price:BigDecimal,ignored:Set<String>,reprice:Boolean=false):List<JSONObject>{
 val result=linkedMapOf<String,JSONObject>();existing.forEach{result[it.optString("id")]=it}
 records.forEach{record->val recordId=(record.opt("recordId")?:record.opt("id"))?.toString()?:return@forEach;val id="auto-charge:"+recordId;if(id in ignored)return@forEach
  val date=runCatching{LocalDate.parse(record.optString("startTime",record.optString("chargeStartTime")).take(10))}.getOrNull()?:return@forEach
  if(YearMonth.from(date)!=month||runCatching{LocalDate.parse(record.optString("endTime",record.optString("chargeEndTime")).take(10))}.isFailure)return@forEach
  val kwh=record.number("chargeEnergy")?.takeIf{it.isFinite()&&it>0}?:return@forEach;val old=result[id];if(old!=null&&old.optString("source")!="charge")return@forEach
  val rate=if(old!=null&&!reprice)runCatching{electricityPrice(old.optString("unitPrice"))}.getOrDefault(price)else price
  val cents=runCatching{estimatedChargeCents(kwh,rate)}.getOrNull()?:return@forEach
  val next=JSONObject().put("id",id).put("chargeId",recordId).put("date",date.toString()).put("category","charge").put("cents",cents).put("source","charge").put("energyKwh",kwh).put("unitPrice",rate.toPlainString()).put("note","自动估算 · "+numberText(kwh)+" kWh × ¥"+rate.toPlainString()+"/kWh")
  for(key in listOf("startBattery","endBattery","durationMinutes")){record.number(key)?.let{next.put(key,it)}}
  result[id]=next
 }
 return result.values.toList()
}
