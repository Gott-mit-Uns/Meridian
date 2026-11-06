package app.meridian.mobile
import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject
class PrivacyModeTest{
 @Test fun preservesProvinceAndCity(){assertEquals("浙江省杭州市*****",cityOnlyAddress("浙江省杭州市钱塘区源隆商业大厦"))}
 @Test fun municipalAddressHidesDistrict(){assertEquals("北京市*****",cityOnlyAddress("北京市朝阳区建国路88号"))}
 @Test fun unknownCityFailsClosed(){assertEquals("*****",cityOnlyAddress("源隆商业大厦"))}
 @Test fun coarseCityAndPlaceholdersStayReadable(){assertEquals("杭州市",cityOnlyAddress("杭州市"));assertEquals("—",cityOnlyAddress("—"));assertEquals("出发地",cityOnlyAddress("出发地"))}
 @Test fun toggleDoesNotMutateRawData(){val j=JSONObject().put("startLocation","浙江省杭州市钱塘区源隆商业大厦");try{PrivacyMode.enabled=true;assertEquals("浙江省杭州市*****",j.safeText("startLocation"));assertEquals("浙江省杭州市钱塘区源隆商业大厦",j.getString("startLocation"));PrivacyMode.enabled=false;assertEquals(j.getString("startLocation"),j.safeText("startLocation"))}finally{PrivacyMode.enabled=false}}
}
