package app.meridian.mobile
import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject
class TollEstimateTest{
 @Test fun missingFeeIsNotFree(){assertNull(tollCents(null));assertNull(tollCents(JSONObject.NULL));assertNull(tollCents(""));assertNull(tollCents("NaN"));assertNull(tollCents("-1"));assertEquals(0L,tollCents("0"))}
 @Test fun decimalFeeUsesMoneyRounding(){assertEquals(1235L,tollCents("12.345"));assertEquals(95L,tollPayableCents(100,true,"95","undiscounted"));assertEquals(101L,tollPayableCents(106,true,"95","undiscounted"))}
 @Test fun uncertainOrAlreadyDiscountedFeeIsNeverDiscountedAgain(){assertEquals(100L,tollPayableCents(100,true,"95","unknown"));assertEquals(100L,tollPayableCents(100,true,"95","discounted"));assertEquals(100L,tollPayableCents(100,false,"95","undiscounted"))}
 @Test fun invalidPercentCannotCreateNegativeOrInflatedBill(){for(value in listOf("0","-1","101","NaN")){try{tollPayableCents(100,true,value,"undiscounted");fail()}catch(_:IllegalArgumentException){}}}
 @Test fun freeTollCanBeArchivedWithoutInventingPayment(){val entry=JSONObject().put("id","x").put("date","2026-11-06").put("category","toll").put("cents",0).put("tollEstimate",JSONObject().put("payableCents",0));assertTrue(validLedgerArchiveEntry(entry));entry.remove("tollEstimate");assertFalse(validLedgerArchiveEntry(entry))}
 @Test fun sameTripCannotBeChargedTwiceButCanBeEdited(){val a=JSONObject().put("id","a").put("category","toll").put("tollEstimate",JSONObject().put("tripId",12));validateTollDuplicate(a,listOf(a));val b=JSONObject(a.toString()).put("id","b");try{validateTollDuplicate(b,listOf(a));fail()}catch(_:IllegalArgumentException){}}
 @Test fun changedAmountIsActualAndRetainsReference(){val entry=JSONObject().put("cents",100).put("tollEstimate",JSONObject().put("payableCents",100));assertTrue(tollEntrySource(entry).contains("估算"));entry.put("cents",200);assertTrue(tollEntrySource(entry).contains("实付"))}
}
