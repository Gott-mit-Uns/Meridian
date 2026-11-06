package app.meridian.mobile

import android.app.Instrumentation
import android.content.Intent
import android.graphics.Bitmap
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import org.json.JSONObject
import java.io.File

/** Opt-in device verification against a user-configured service; screenshots stay in app-private storage. */
internal class TeslaMateDeviceCheck(private val instrumentation: Instrumentation, private val address: String) {
    private fun find(node: AccessibilityNodeInfo?, label: String, contains: Boolean = false): AccessibilityNodeInfo? {
        if (node == null) return null
        val values = listOfNotNull(node.text?.toString(), node.contentDescription?.toString()) + node.actionList.mapNotNull { it.label?.toString() }
        if (values.any { if (contains) it.contains(label) else it == label }) return node
        for (i in 0 until node.childCount) find(node.getChild(i), label, contains)?.let { return it }
        return null
    }
    private fun waitFor(label: String, contains: Boolean = false) {
        repeat(60) {
            if(find(instrumentation.uiAutomation.rootInActiveWindow,label,contains)!=null)return
            Thread.sleep(500)
        }
        error("Screen did not show expected control: $label")
    }
    private fun click(label: String) {
        waitFor(label)
        var node=find(instrumentation.uiAutomation.rootInActiveWindow,label)?:error("Missing control")
        while(!node.isClickable)node=node.parent?:error("Control is not clickable: $label")
        check(node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        Thread.sleep(700)
    }
    private fun screenshot(name: String) {
        Thread.sleep(1000)
        val bitmap=instrumentation.uiAutomation.takeScreenshot()?:error("Screenshot unavailable")
        val directory=File(instrumentation.targetContext.filesDir,"teslamate-qa").apply{mkdirs()}
        File(directory,"$name.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
        bitmap.recycle()
    }
    fun run() {
        val activity=instrumentation.startActivitySync(Intent(instrumentation.targetContext,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        val vault=Vault(activity)
        vault.save(JSONObject().put("address",address).put("user","").put("password","").put("backend","TESLAMATE")
            .put("gpsCoordinates",true).put("keep",true).put("auto",true).toString())
        val mode=mutableStateOf("light")
        instrumentation.runOnMainSync {
            PrivacyMode.set(activity,true)
            activity.setContent{MeridianTheme(mode.value){Meridian(vault,mode.value,{mode.value=it})}}
        }
        waitFor("显示 TeslaMate 最近记录",true)
        screenshot("home-light")
        click("行程");waitFor("行程次数");click("查看行程详情");waitFor("行程概览")
        Thread.sleep(2500);screenshot("trip-map-light")
        instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);Thread.sleep(600)
        click("行程记录");click("充电记录");waitFor("充电次数");click("查看充电详情");waitFor("充电详情")
        screenshot("charge-light")
        instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);Thread.sleep(600)
        click("爱车")
        instrumentation.runOnMainSync{mode.value="dark"}
        screenshot("home-dark")
        // Leave a normal saved connection for manual acceptance on this dedicated emulator.
    }
}
