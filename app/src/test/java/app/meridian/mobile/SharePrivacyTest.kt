package app.meridian.mobile

import org.json.*
import org.junit.Assert.*
import org.junit.Test

class SharePrivacyTest {
 private fun trip()=JSONObject("""{"tripStartTime":"2026-11-05T08:12:13","tripEndTime":"2026-11-05T09:01:00","startLocation":"私人住址A","endLocation":"工作地点B","tripStartLatitude":30.1,"tripStartLongitude":120.1,"tripEndLatitude":30.2,"tripEndLongitude":120.2,"track":{"points":[{"latitude":30.1,"longitude":120.1},{"latitude":30.2,"longitude":120.2}]},"mileageKm":12.5,"vin":"secret-vin","meta":"私人住址A"}""")
 @Test fun defaultExportContainsNoPlaceRoutePreciseTimeOrFreeText(){
  val source=trip();val before=source.toString();val snapshot=prepareShareSnapshot(source,JSONObject().put("summary","私人住址A，08:12到达"),SharePrivacy())
  assertFalse(snapshot.data.toString().contains("私人住址A"));assertFalse(snapshot.data.toString().contains("工作地点B"));assertFalse(snapshot.data.toString().contains("08:12"));assertFalse(snapshot.data.has("vin"));assertNull(snapshot.insight)
  assertTrue(trackPositions(snapshot.data).isEmpty());assertTrue(endpointPositions(snapshot.data).isEmpty());assertEquals(12.5,snapshot.data.getDouble("mileageKm"),0.0);assertEquals(before,source.toString())
 }
 @Test fun eachPrivacyOptionCanBeConfiguredWithoutMutatingInput(){
  val snapshot=prepareShareSnapshot(trip(),null,SharePrivacy(hideAddresses=true,hideRoute=false,hideTime=false))
  assertEquals("起点地址已隐藏",snapshot.data.getString("startLocation"));assertEquals(2,trackPositions(snapshot.data).size);assertTrue(snapshot.data.getString("tripStartTime").contains("08:12"))
 }
 @Test fun protectedExportsAlwaysOmitPotentiallyIdentifyingInsight(){
  for(p in listOf(SharePrivacy(true,false,false),SharePrivacy(false,true,false),SharePrivacy(false,false,true)))assertNull(prepareShareSnapshot(trip(),JSONObject().put("summary","私人住址A"),p).insight)
 }
 @Test fun explicitUnprotectedExportRetainsSelectedInformation(){
  val result=prepareShareSnapshot(trip(),JSONObject().put("summary","完整洞察"),SharePrivacy(false,false,false))
  assertEquals("私人住址A",result.data.getString("startLocation"));assertEquals(2,trackPositions(result.data).size);assertEquals("完整洞察",result.insight!!.getString("summary"));assertFalse(result.data.has("vin"))
 }
}
