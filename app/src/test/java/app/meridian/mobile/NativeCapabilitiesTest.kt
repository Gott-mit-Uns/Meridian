package app.meridian.mobile

import org.json.*
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream

class NativeCapabilitiesTest {
 private fun part(boundary:String,name:String,bytes:ByteArray)=
  ("--$boundary\r\nContent-Disposition: form-data; name=\"$name\"\r\nContent-Type: image/png\r\n\r\n").toByteArray()+bytes+"\r\n".toByteArray()

 @Test fun multipartPreservesBinaryImagesAndIgnoresBoundaryPrefixesInsidePayload(){
  val boundary="meridian-test"
  val image=byteArrayOf(0,-1,-128,10,13)+"\r\n--meridian-test-not-a-delimiter".toByteArray()+byteArrayOf(12,0)
  val response=part(boundary,"media-0",image)+part(boundary,"media-1",byteArrayOf(9,8,7))+"--$boundary--\r\n".toByteArray()
  val result=parseMultipartImages("multipart/form-data; boundary=$boundary",response)
  assertArrayEquals(image,result["media-0"]);assertArrayEquals(byteArrayOf(9,8,7),result["media-1"])
 }
 @Test fun quotedMultipartBoundaryAndWhitespaceAreSupported(){
  val response=part("quoted","media-3",byteArrayOf(0,1))+"--quoted--".toByteArray()
  assertArrayEquals(byteArrayOf(0,1),parseMultipartImages("multipart/form-data; boundary = \"quoted\"",response)["media-3"])
 }
 @Test(expected=IllegalArgumentException::class) fun malformedMultipartIsRejected(){parseMultipartImages("application/json","{}".toByteArray())}
 @Test(expected=IllegalArgumentException::class) fun oversizedUploadsAreRejectedBeforeReadingIntoMemory(){ByteArrayInputStream(ByteArray(100)).readCapped(99)}
 @Test fun cappedReaderAllowsExactLimit(){assertArrayEquals(byteArrayOf(1,2,3),ByteArrayInputStream(byteArrayOf(1,2,3)).readCapped(3))}

 @Test fun jobIdsAcceptTheWebsitesNumericAndObjectResponses(){
  assertEquals("42",syncJobId(42));assertEquals("42",syncJobId(42.0));assertEquals("job-123",syncJobId(JSONObject().put("jobId","job-123")))
 }
 @Test fun invalidJobIdsCannotStartEndlessProgressPolling(){
  for(value in listOf(null,JSONObject.NULL,0,-1,-1.0,Double.NaN,true,"",JSONObject()))assertEquals("",syncJobId(value))
 }

 @Test fun brushingCreatesExactServerTimeFilters(){
  val window=healthDetailWindow("2026-11-05 10:00:00","2026-11-05 11:00:00",25f,75f)
  assertEquals("2026-11-05 10:15:00" to "2026-11-05 10:45:00",window)
 }
 @Test fun invalidBrushRangesNeverBecomeBroadOrReversedQueries(){
  assertNull(healthDetailWindow("bad","bad",0f,100f));assertNull(healthDetailWindow("2026-11-05 11:00:00","2026-11-05 10:00:00",0f,100f))
  assertNull(healthDetailWindow("2026-11-05 10:00:00","2026-11-05 11:00:00",70f,20f))
  assertNull(healthDetailWindow("2026-11-05 10:00:00","2026-11-05 11:00:00",Float.NaN,80f))
 }
 @Test fun brushClampsToActualWindow(){assertEquals("2026-11-05 10:00:00" to "2026-11-05 11:00:00",healthDetailWindow("2026-11-05 10:00:00","2026-11-05 11:00:00",-10f,120f))}

 @Test fun regionTripsKeepDatesAndUseCorrectParents(){
  val base=JSONObject().put("carId",1).put("startDate","2026-11-01")
  val row=JSONObject().put("name","钱塘区").put("parentName","杭州市").put("grandParentName","浙江省")
  val request=regionTripFilters(base,"DISTRICT",row,"","")
  assertEquals("钱塘区",request.getString("districtName"));assertEquals("杭州市",request.getString("cityName"));assertEquals("浙江省",request.getString("provinceName"));assertEquals("2026-11-01",request.getString("startDate"));assertFalse(base.has("districtName"))
 }
 @Test fun provinceQueryDoesNotInheritOldDistrictFilters(){
  val request=regionTripFilters(JSONObject().put("districtName","旧区县").put("cityName","旧城市"),"PROVINCE",JSONObject().put("name","浙江省"),"","")
  assertEquals("浙江省",request.getString("provinceName"));assertFalse(request.has("cityName"));assertFalse(request.has("districtName"))
 }
 @Test(expected=IllegalArgumentException::class) fun districtWithoutParentsIsRejected(){regionTripFilters(JSONObject(),"DISTRICT",JSONObject().put("name","钱塘区"),"","")}

 @Test fun monthlyShareDropsAccountLocationAndFreeTextByDefault(){
  val source=JSONObject().put("carId",42).put("carName","PRIVATE").put("userName","OWNER").put("startLocation","SECRET").put("storySummary","Home SECRET").put("monthMileage",123.4)
  val result=monthlyShareValues(source,false)
  assertEquals(123.4,result.getDouble("monthMileage"),.001);for(key in listOf("carId","carName","userName","startLocation","storySummary"))assertFalse(result.has(key))
  assertTrue(source.has("storySummary"))
 }
 @Test fun monthlyShareAllowsOnlyExplicitlyOptedInStory(){assertEquals("review",monthlyShareValues(JSONObject().put("storySummary","review"),true).getString("storySummary"))}

 @Test fun commuteSettingsUseWeekdayPlansAndValidateEnabledTimes(){validateCommuteOverrides(JSONObject().put("MONDAY",JSONObject().put("enabled",true).put("reminderTime","08:15").put("eveningEnabled",true).put("eveningReminderTime","18:30")))}
 @Test(expected=IllegalArgumentException::class) fun invalidReminderTimeCannotBeSaved(){validateCommuteOverrides(JSONObject().put("MONDAY",JSONObject().put("enabled",true).put("reminderTime","25:00")))}
 @Test(expected=IllegalArgumentException::class) fun dateKeysCannotAccidentallyReplaceWeekdayPlans(){validateCommuteOverrides(JSONObject().put("2026-11-05",true))}
}
