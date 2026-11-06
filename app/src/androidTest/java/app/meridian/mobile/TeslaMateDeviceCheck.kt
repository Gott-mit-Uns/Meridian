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
        screenshot("failure")
        error("Screen did not show expected control: $label")
    }
    private fun click(label: String) {
        val candidates=mutableListOf<AccessibilityNodeInfo>()
        fun collect(node:AccessibilityNodeInfo?){if(node==null)return
            val values=listOfNotNull(node.text?.toString(),node.contentDescription?.toString())+node.actionList.mapNotNull{it.label?.toString()}
            if(label in values)candidates.add(node)
            for(i in 0 until node.childCount)collect(node.getChild(i))
        }
        fun scroll(node:AccessibilityNodeInfo?):Boolean {
            if(node==null)return false
            if(node.isScrollable&&node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD))return true
            for(i in 0 until node.childCount)if(scroll(node.getChild(i)))return true
            return false
        }
        for(attempt in 0 until 20){
            candidates.clear();collect(instrumentation.uiAutomation.rootInActiveWindow)
            val node=candidates.mapNotNull{candidate->var parent:AccessibilityNodeInfo?=candidate;while(parent!=null&&!parent.isClickable)parent=parent.parent;parent?.takeIf{it.isEnabled&&it.isVisibleToUser}}
                .minByOrNull{android.graphics.Rect().also(it::getBoundsInScreen).top}
            if(node!=null){
                node.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN.id)
                Thread.sleep(300)
                if(node.performAction(AccessibilityNodeInfo.ACTION_CLICK)){Thread.sleep(700);return}
            }
            if(candidates.isEmpty()&&attempt%2==1)scroll(instrumentation.uiAutomation.rootInActiveWindow)
            Thread.sleep(500)
        }
        screenshot("failure")
        error("Control is not clickable: $label")
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
        instrumentation.runOnMainSync{activity.setContent{}}
        instrumentation.waitForIdleSync()
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
        click("统计");waitFor("用车概况");waitFor("行驶里程")
        screenshot("statistics-dark")
        click("用车概况");click("月度报告");waitFor("月度用车回顾")
        screenshot("monthly-dark")
        click("月度报告");click("年度报告");waitFor("年度用车报告",true)
        screenshot("annual-dark")
        click("爱车");click("我的足迹");waitFor("查看全部到访排行")
        screenshot("footprint-dark")
        click("返回");click("用车账单");waitFor("本期已记金额");Thread.sleep(1800)
        screenshot("ledger-dark")
        click("返回");click("查看完整车况");waitFor("门锁与哨兵")
        screenshot("status-dark")
        click("查看车况历史与时间线");waitFor("车辆指标轨迹");Thread.sleep(1500)
        screenshot("history-dark")
        // Leave a normal saved connection for manual acceptance on this dedicated emulator.
    }
}
