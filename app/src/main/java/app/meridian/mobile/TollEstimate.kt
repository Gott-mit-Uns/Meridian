package app.meridian.mobile

import java.math.BigDecimal
import java.math.RoundingMode
import org.json.JSONObject

// Missing tolls are unknown, distinct from an explicitly returned free route.
internal fun tollCents(raw:Any?):Long? = runCatching{
 if(raw==null||raw==JSONObject.NULL)return null
 val value=raw.toString().toBigDecimal();require(value>=BigDecimal.ZERO&&value<=BigDecimal("10000000"))
 value.movePointRight(2).setScale(0,RoundingMode.HALF_UP).longValueExact()
}.getOrNull()
internal fun tollPayableCents(providerCents:Long,applyEtc:Boolean,discountPercent:String,feeBasis:String):Long{
 require(providerCents in 0..1_000_000_000L)
 if(!applyEtc||feeBasis!="undiscounted")return providerCents
 val percent=discountPercent.toBigDecimalOrNull()?:throw IllegalArgumentException("请填写有效 ETC 比例")
 require(percent>BigDecimal.ZERO&&percent<=BigDecimal(100)){"ETC 实付比例需大于0且不超过100%"}
 return BigDecimal(providerCents).multiply(percent).divide(BigDecimal(100),0,RoundingMode.HALF_UP).longValueExact()
}
internal fun tollEntrySource(entry:JSONObject):String{
 val quote=entry.optJSONObject("tollEstimate")?:return "手动记账"
 return if(quote.optLong("payableCents",-1)==entry.optLong("cents",-2))"高德路线 · 估算记账"else"手动实付 · 保留路线参考"
}
internal fun validateTollDuplicate(next:JSONObject,entries:List<JSONObject>){
 val trip=next.optJSONObject("tollEstimate")?.optString("tripId").orEmpty();if(trip.isBlank())return
 require(entries.none{it.optString("id")!=next.optString("id")&&costCategory(it)=="toll"&&it.optJSONObject("tollEstimate")?.optString("tripId")==trip}){"这趟行程已有关联高速费，请编辑原账单，避免重复记账"}
}
