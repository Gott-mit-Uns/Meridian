package app.meridian.mobile
import org.json.*
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class DeviceFeatureTest{
 @Test fun moneyUsesExactCents(){assertEquals(1234L,validCostCents("12.34"));assertEquals(1200L,validCostCents("12.00"))}
 @Test fun invalidMoneyIsRejected(){for(value in listOf("0","-1","NaN","2.345","10000001")){try{validCostCents(value);fail("accepted $value")}catch(_:IllegalArgumentException){}}}
 @Test fun readCacheNeverIncludesMutationOrVideo(){assertFalse(cacheableReads.contains("/user/login"));assertFalse(cacheableReads.contains("/car/trip/cityFootprint/initialize"));assertFalse(cacheableReads.contains("/car/sentry/start"));assertTrue(cacheableReads.contains("/car/trip/get"))}
 @Test fun cacheIsIsolatedByAccountCarAndFilters(){val a=JSONObject().put("carId",1).put("pageNum",2);val b=JSONObject().put("pageNum",2).put("carId",1);assertEquals(cacheRequestKey("A","/car/trip/get",a),cacheRequestKey("A","/car/trip/get",b));assertNotEquals(cacheRequestKey("A","/car/trip/get",a),cacheRequestKey("B","/car/trip/get",a));assertNotEquals(cacheRequestKey("A","/car/trip/get",a),cacheRequestKey("A","/car/trip/get",JSONObject().put("carId",2).put("pageNum",2)))}
 @Test fun authenticationAndPermissionFailuresCannotFallBackToOldData(){assertFalse(canUseOfflineCache(HttpReadFailure(401)));assertFalse(canUseOfflineCache(HttpReadFailure(403)));assertFalse(canUseOfflineCache(HttpReadFailure(404)));assertTrue(canUseOfflineCache(HttpReadFailure(503)));assertTrue(canUseOfflineCache(IOException("offline")))}
 @Test fun digestDoesNotCopyRawTrackOrCredentials(){val source=JSONObject().put("id",1).put("startLocation","杭州市某地").put("track",JSONArray()).put("token","secret");val digest=tripDigest(source);assertTrue(digest.has("id"));assertFalse(digest.has("track"));assertFalse(digest.has("token"))}
 @Test fun energyIntervalsHaveUnambiguousBoundaries(){assertTrue(energyBucketMatches(12.0,"low"));assertFalse(energyBucketMatches(12.0,"mid"));assertTrue(energyBucketMatches(18.0,"mid"));assertFalse(energyBucketMatches(18.0,"high"));assertTrue(energyBucketMatches(18.1,"high"));assertFalse(energyBucketMatches(Double.NaN,"all"))}
 @Test fun knownPlaceNamesCanBeMaskedInFreeText(){assertEquals("到浙江省杭州市*****充电",redactKnownPlaces("到源隆商业大厦充电",mapOf("源隆商业大厦" to "浙江省杭州市*****")))}
 @Test fun assistantOnlyUsesExplicitTripReferences(){val metadata=JSONObject().put("citations",JSONArray().put(JSONObject().put("tripId",42)).put(JSONObject().put("url","https://example.com")));assertEquals(listOf("42"),assistantCitationTripIds(metadata));assertTrue(assistantCitationTripIds(JSONObject()).isEmpty())}
 @Test fun homePresetsAreValidAndDistinct(){for(name in listOf("日常","长途","补能")){val ids=homePreset(name);assertTrue(ids.all{it in homeCardIds});assertEquals(ids.size,ids.distinct().size);assertEquals(homeCardIds.size,normalizedHomeOrder(ids).size)}}
 @Test fun chartDrillDownDoesNotGuessMissingYears(){assertEquals(java.time.LocalDate.of(2026,11,5),chartDate("2026-11-05 11:12"));assertNull(chartDate("11-05"));assertNull(chartDate("总计"))}
 @Test fun cacheKeysEscapeSearchSeparators(){val first=JSONObject().put("keyword","A&pageNum=1");val second=JSONObject().put("keyword","A").put("pageNum",1);assertNotEquals(cacheRequestKey("A","/car/trip/get",first),cacheRequestKey("A","/car/trip/get",second))}
 @Test fun paginationRespectsServerCapsAndTotal(){val response=JSONObject().put("data",JSONObject().put("total",85));assertTrue(hasMoreTripRecords(response,20,20,50));assertFalse(hasMoreTripRecords(response,5,85,50));assertFalse(hasMoreTripRecords(response,0,20,50));assertFalse(hasMoreTripRecords(response,50,500,50))}
}
