package app.meridian.mobile
import org.json.*
import java.math.BigDecimal
import java.math.RoundingMode
internal val costCategories=listOf("charge" to "充电费","toll" to "高速费","repair" to "维修费","maintenance" to "保养费","accessories" to "配件","parking" to "停车费","insurance" to "保险费","wash" to "洗车费","other" to "其他")
private val costAliases=mapOf("充电" to "charge","充电费" to "charge","高速费" to "toll","高速" to "toll","ETC" to "toll","维修" to "repair","维修费" to "repair","保养" to "maintenance","保养费" to "maintenance","配件" to "accessories","停车" to "parking","停车费" to "parking","保险" to "insurance","保险费" to "insurance","洗车" to "wash","洗车费" to "wash","其他" to "other")
internal fun costCategory(entry:JSONObject):String{
 val raw=entry.optString("category");val stored=if(costCategories.any{it.first==raw})raw else costAliases[raw]?:"other"
 if(entry.optString("source")=="charge")return "charge"
 if(entry.optInt("categoryVersion")>=1||stored !in listOf("charge","other"))return stored
 val note=entry.optString("note");val matches=listOf("toll" to listOf("高速","过路","通行费","ETC"),"repair" to listOf("维修","修车"),"maintenance" to listOf("保养"),"accessories" to listOf("配件","脚垫","车膜"),"parking" to listOf("停车"),"insurance" to listOf("保险"),"wash" to listOf("洗车")).filter{(_,words)->words.any{note.contains(it,ignoreCase=true)}}.map{it.first}
 return matches.singleOrNull()?:stored
}
internal data class CostCategoryTotal(val id:String,val label:String,val cents:Long,val count:Int)
internal fun costCategoryTotals(rows:List<JSONObject>):List<CostCategoryTotal> = costCategories.map{(id,label)->val subset=rows.filter{costCategory(it)==id};CostCategoryTotal(id,label,subset.sumOf{it.optLong("cents").coerceAtLeast(0)},subset.size)}.filter{it.count>0}
internal fun estimatedChargeKwh(cents:Long,price:BigDecimal):BigDecimal?=if(cents<0||price<=BigDecimal.ZERO)null else BigDecimal.valueOf(cents,2).divide(price,3,RoundingMode.HALF_UP)
internal fun validChargeEnergy(value:String):BigDecimal{val energy=value.trim().toBigDecimalOrNull()?:throw IllegalArgumentException("请输入有效充电量");require(energy>BigDecimal.ZERO&&energy<=BigDecimal("10000")&&energy.stripTrailingZeros().scale()<=3){"充电量需大于0，最多三位小数"};return energy}
internal fun costNoteOptions(category:String)=when(category){"charge"->listOf("家充","公共快充","公共慢充","含服务费");"toll"->listOf("ETC","高速通行","跨城出行");"repair"->listOf("钣金喷漆","轮胎维修","故障检修");"maintenance"->listOf("定期保养","空调滤芯","制动养护");"accessories"->listOf("车内配件","轮胎","贴膜");"parking"->listOf("临时停车","月租车位");"insurance"->listOf("交强险","商业险");"wash"->listOf("日常洗车","精洗养护");else->emptyList()}

internal fun validLedgerArchiveEntry(entry:JSONObject):Boolean{
 if(entry.optString("id").isBlank()||runCatching{java.time.LocalDate.parse(entry.optString("date"))}.isFailure)return false
 val cents=entry.optLong("cents",-1);if(cents>0)return true;if(cents!=0L)return false
 if(costCategory(entry)=="toll"&&entry.optJSONObject("tollEstimate")?.optLong("payableCents",-1)==0L)return true
 return entry.optString("source")=="charge"||entry.optString("id").startsWith("auto-charge:")||(costCategory(entry)=="charge"&&entry.number("energyKwh")?.let{it>0}==true&&runCatching{electricityPrice(entry.optString("unitPrice")).signum()==0}.getOrDefault(false))
}
