package app.meridian.mobile
import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject
class PageReadMemoryTest{
 @Test fun boundedCacheEvictsLeastRecentlyRead(){val cache=PageReadMemory(2);cache.put("a",JSONObject().put("id",1));cache.put("b",JSONObject().put("id",2));assertNotNull(cache.get("a"));cache.put("c",JSONObject());assertNull(cache.get("b"));assertNotNull(cache.get("a"));assertNotNull(cache.get("c"))}
 @Test fun clearingLocalCacheInvalidatesEveryClient(){val one=PageReadMemory();val two=PageReadMemory();one.put("a",JSONObject());two.put("b",JSONObject());PageReadMemory.invalidateAll();assertNull(one.get("a"));assertNull(two.get("b"))}
 @Test fun freshnessRejectsMissingFutureAndExpiredTimestamps(){val now=100000L;assertFalse(freshPageRead(null,now));assertFalse(freshPageRead(JSONObject(),now));assertFalse(freshPageRead(JSONObject().put("_meridianUpdatedAt",now+1),now));assertFalse(freshPageRead(JSONObject().put("_meridianUpdatedAt",now-60000),now));assertTrue(freshPageRead(JSONObject().put("_meridianUpdatedAt",now-1),now))}
}
