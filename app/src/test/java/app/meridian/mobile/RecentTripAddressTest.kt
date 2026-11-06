package app.meridian.mobile
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
class RecentTripAddressTest{
 @Test fun dashboardRouteUsesRealAddresses(){assertEquals("杭州市源隆商业大厦" to "杭州市建友科创园",recentTripAddresses(JSONObject().put("route","杭州市源隆商业大厦 → 杭州市建友科创园")))}
 @Test fun explicitFieldsTakePrecedence(){assertEquals("A" to "B",recentTripAddresses(JSONObject().put("startLocation","A").put("endLocation","B").put("route","X → Y")))}
 @Test fun unsplitRouteRemainsVisible(){assertEquals("原始路线信息" to "",recentTripAddresses(JSONObject().put("route","原始路线信息")))}
 @Test fun arrowVariantsAreSupported(){for(arrow in listOf("->","→","➜","➡"))assertEquals("甲地" to "乙地",recentTripAddresses(JSONObject().put("route","甲地 $arrow 乙地")))}
}
