package app.meridian.mobile
import org.json.*
import org.junit.Assert.*
import org.junit.Test
import java.util.Base64
class LocalArchiveTest{
 @Test fun encryptedArchiveRoundTripsChineseNotes(){val source=JSONObject().put("notes",JSONObject().put("42",JSONObject().put("note","周末旅行")));val encrypted=archiveEncrypt(source,"test-password");assertFalse(String(encrypted).contains("周末旅行"));assertEquals("周末旅行",archiveDecrypt(encrypted,"test-password").getJSONObject("notes").getJSONObject("42").getString("note"))}
 @Test(expected=Exception::class) fun wrongPasswordIsRejected(){archiveDecrypt(archiveEncrypt(JSONObject().put("ledger",1),"test-password"),"another-password")}
 @Test(expected=Exception::class) fun modifiedCiphertextIsRejected(){val envelope=JSONObject(String(archiveEncrypt(JSONObject().put("notes","secret"),"test-password")));val bytes=Base64.getDecoder().decode(envelope.getString("data"));bytes[0]=(bytes[0].toInt() xor 1).toByte();envelope.put("data",Base64.getEncoder().encodeToString(bytes));archiveDecrypt(envelope.toString().toByteArray(),"test-password")}
 @Test(expected=IllegalArgumentException::class) fun shortPasswordIsRejected(){archiveEncrypt(JSONObject(),"123")}
}
