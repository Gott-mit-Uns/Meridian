package app.meridian.mobile
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
class LedgerMetricsTest{
 private fun entry(category:String,cents:Long,note:String="",explicit:Boolean=true)=JSONObject().put("category",category).put("cents",cents).put("note",note).apply{if(explicit)put("categoryVersion",1)}
 @Test fun categoryTotalsReconcileWithLedger(){val rows=listOf(entry("charge",6500),entry("toll",1234),entry("repair",20000),entry("maintenance",10000),entry("accessories",800),entry("unknown",200));val totals=costCategoryTotals(rows);assertEquals(rows.sumOf{it.getLong("cents")},totals.sumOf{it.cents});assertEquals(6,totals.size);assertEquals(1234L,totals.first{it.id=="toll"}.cents);assertEquals(200L,totals.first{it.id=="other"}.cents)}
 @Test fun legacyNotesAreRecognizedWithoutOverridingExplicitChoices(){assertEquals("toll",costCategory(entry("charge",100,"ETC高速通行费",false)));assertEquals("repair",costCategory(entry("charge",100,"轮胎维修",false)));assertEquals("charge",costCategory(entry("charge",100,"高速通行",true)));assertEquals("charge",costCategory(entry("charge",100,"高速通行",false).put("source","charge")));assertEquals("charge",costCategory(entry("charge",100,"高速和维修",false)))}
 @Test fun energyCanBeCalculatedInEitherDirection(){assertEquals(BigDecimal("100.000"),estimatedChargeKwh(6500,BigDecimal("0.65")));assertEquals(6500L,estimatedChargeCents(100.0,BigDecimal("0.65")));assertEquals(BigDecimal("1.667"),estimatedChargeKwh(100,BigDecimal("0.6")))}
 @Test fun freeChargingHasNoReverseQuantity(){assertNull(estimatedChargeKwh(0,BigDecimal.ZERO));assertNull(estimatedChargeKwh(100,BigDecimal.ZERO));assertEquals(0L,estimatedChargeCents(42.5,BigDecimal.ZERO))}
 @Test fun invalidEnergyIsRejected(){for(value in listOf("-1","0","NaN","10001","0.1234")){try{validChargeEnergy(value);fail(value)}catch(_:IllegalArgumentException){}}}
 @Test fun freeManualChargeSurvivesArchiveRestore(){val item=entry("charge",0).put("id","free-1").put("date","2026-11-06").put("energyKwh",35.0).put("unitPrice","0");assertTrue(validLedgerArchiveEntry(item));assertTrue(validLedgerArchiveEntry(JSONObject(item.toString()).put("unitPrice","0.00")));assertFalse(validLedgerArchiveEntry(entry("repair",0).put("id","bad").put("date","2026-11-06")));assertFalse(validLedgerArchiveEntry(JSONObject(item.toString()).put("date","bad")))}
}
