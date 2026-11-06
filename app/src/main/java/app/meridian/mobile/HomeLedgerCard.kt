package app.meridian.mobile
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import kotlinx.coroutines.*
import org.json.*
import java.time.*

internal data class MonthlyLedgerCost(val cents:Long,val count:Int){fun perKm(mileage:Double?):Double?=mileage?.takeIf{it.isFinite()&&it>0}?.let{cents/100.0/it}}
internal fun monthlyLedgerCost(entries:List<JSONObject>,month:YearMonth):MonthlyLedgerCost{val rows=entries.filter{runCatching{YearMonth.from(LocalDate.parse(it.optString("date")))}.getOrNull()==month};return MonthlyLedgerCost(rows.sumOf{it.optLong("cents").coerceAtLeast(0)},rows.size)}
@Composable internal fun HomeLedgerCard(api:Api,carId:Any,enabled:Boolean,onOpen:()->Unit){
 val scope=api.localScope(carId);val month=YearMonth.now();val revision=DeviceData.revision
 var cost by remember(scope,month){mutableStateOf<MonthlyLedgerCost?>(null)};var mileage by remember(scope,month){mutableStateOf<Double?>(null)}
 LaunchedEffect(scope,month,revision){cost=withContext(Dispatchers.IO){monthlyLedgerCost(DeviceData.read("ledger:"+scope).optJSONArray("entries")?.objects().orEmpty(),month)}}
 LaunchedEffect(scope,month){try{val result=api.call("/trip-month-stats/get",JSONObject().put("carId",carId).put("month",month.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"))));mileage=result.optJSONObject("data")?.number("monthMileage")}catch(e:CancellationException){throw e}catch(_:Exception){}}
 Box(Modifier.fillMaxWidth().cardClick(enabled=enabled,onClick=onOpen)){ReferenceCard{
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){Icon(Icons.Outlined.AccountBalanceWallet,null,Modifier.size(17.dp),tint=accent);Text("用车账单",Modifier.weight(1f),fontSize=13.sp,fontWeight=FontWeight.Medium);Icon(Icons.Outlined.ChevronRight,null,Modifier.size(14.dp),tint=muted)}
  Text(cost?.let{"¥ "+numberText(it.cents/100.0,2)}?:"—",fontSize=23.sp,fontWeight=FontWeight.SemiBold,color=ink)
  Text("本月已记费用",fontSize=11.sp,color=muted)
  Text(cost?.perKm(mileage)?.let{numberText(it,2)+" 元/km"}?:"每公里费用 · —",fontSize=12.sp,color=accent)
 }}
}
