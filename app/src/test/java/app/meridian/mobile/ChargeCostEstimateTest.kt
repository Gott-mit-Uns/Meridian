package app.meridian.mobile
import org.junit.Assert.*
import org.junit.Test
import org.json.*
import java.time.YearMonth
import java.math.BigDecimal
class ChargeCostEstimateTest{
 private val month=YearMonth.of(2026,11)
 private fun record(id:Int=1,energy:Double=20.0)=JSONObject().put("id",id).put("chargeEnergy",energy).put("startTime","2026-11-05T10:00:00").put("endTime","2026-11-05T11:00:00")
 @Test fun multiplicationRoundsToNearestCent(){assertEquals(1541L,estimatedChargeCents(23.7,electricityPrice("0.65")))}
 @Test fun freeChargingIsAllowed(){assertEquals(0L,estimatedChargeCents(20.0,electricityPrice("0")))}
 @Test fun repeatedSyncDoesNotDuplicate(){val first=mergeChargeCosts(emptyList(),listOf(record()),month,electricityPrice("1"),emptySet());val next=mergeChargeCosts(first,listOf(record()),month,electricityPrice("1"),emptySet());assertEquals(1,next.size);assertEquals(2000L,next.first().getLong("cents"))}
 @Test fun historicPriceStaysUnlessRepriceRequested(){val first=mergeChargeCosts(emptyList(),listOf(record()),month,electricityPrice("1"),emptySet());assertEquals(2000L,mergeChargeCosts(first,listOf(record()),month,electricityPrice("2"),emptySet()).first().getLong("cents"));assertEquals(4000L,mergeChargeCosts(first,listOf(record()),month,electricityPrice("2"),emptySet(),true).first().getLong("cents"))}
 @Test fun manualEditsAndDeletedRecordsStayProtected(){val manual=JSONObject().put("id","auto-charge:1").put("cents",555);assertEquals(555L,mergeChargeCosts(listOf(manual),listOf(record()),month,electricityPrice("2"),emptySet(),true).first().getLong("cents"));assertTrue(mergeChargeCosts(emptyList(),listOf(record()),month,electricityPrice("1"),setOf("auto-charge:1")).isEmpty())}
 @Test fun incompleteOrDifferentMonthIsNotBilled(){val open=record().put("endTime","");val older=record().put("startTime","2026-10-31T10:00:00");assertTrue(mergeChargeCosts(emptyList(),listOf(open,older,record(3,-1.0)),month,electricityPrice("1"),emptySet()).isEmpty())}
 @Test fun priceRejectsInvalidNumbers(){for(value in listOf("-1","NaN","0.12345","101")){try{electricityPrice(value);fail(value)}catch(_:IllegalArgumentException){}}}
 @Test fun widgetFieldsStayValidAndBounded(){assertEquals(4,normalizedWidgetFields(listOf("bad")).size);assertEquals(listOf("range","battery","today","temperature"),normalizedWidgetFields(listOf("range","battery","battery")));assertEquals(4,normalizedWidgetFields(widgetFieldNames.keys.toList()).size)}
 @Test fun batteryIndicatorsOnlyComeFromActualRecords(){val plain=mergeChargeCosts(emptyList(),listOf(record()),month,electricityPrice("1"),emptySet()).first();assertFalse(plain.has("startBattery"));assertFalse(plain.has("endBattery"));val measured=mergeChargeCosts(emptyList(),listOf(record().put("startBattery",25).put("endBattery",80).put("durationMinutes",60)),month,electricityPrice("1"),emptySet()).first();assertEquals(25.0,measured.getDouble("startBattery"),0.0);assertEquals(80.0,measured.getDouble("endBattery"),0.0)}
}
