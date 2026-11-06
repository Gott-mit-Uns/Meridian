package app.meridian.mobile
import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject
class TripNoteEditTest{
 private val trip=JSONObject().put("id",12).put("carId",7).put("startLocation","杭州市甲地").put("track","not-a-digest-field")
 @Test fun editingKeepsFavoriteAndOtherTrips(){val original=JSONObject().put("12",JSONObject().put("favorite",true).put("note","旧内容")).put("13",JSONObject().put("note","另一趟"));val next=mergeTripNoteArchive(original,"12",trip,"新内容","旅行");assertTrue(next.getJSONObject("12").getBoolean("favorite"));assertEquals("旅行",next.getJSONObject("12").getString("tag"));assertEquals("另一趟",next.getJSONObject("13").getString("note"));assertEquals("旧内容",original.getJSONObject("12").getString("note"))}
 @Test fun togglingFavoriteDoesNotEraseEditedTextOrTag(){val all=JSONObject().put("12",JSONObject().put("note","已保存").put("tag","工作"));val next=mergeTripNoteArchive(all,"12",trip,favorite=true).getJSONObject("12");assertEquals("已保存",next.getString("note"));assertEquals("工作",next.getString("tag"));assertTrue(next.getBoolean("favorite"))}
 @Test fun clearingTextIsSavedAndDraftIsBounded(){val old=JSONObject().put("12",JSONObject().put("note","旧内容"));assertEquals("",mergeTripNoteArchive(old,"12",trip,text="").getJSONObject("12").getString("note"));val saved=mergeTripNoteArchive(old,"12",trip,text="a".repeat(1100)).getJSONObject("12");assertEquals(1000,saved.getString("note").length);assertFalse(saved.getJSONObject("trip").has("track"))}
}
