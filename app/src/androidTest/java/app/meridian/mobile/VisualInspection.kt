package app.meridian.mobile

import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import android.graphics.Bitmap
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import kotlinx.coroutines.*
import org.json.*
import java.io.File

/** Device-only visual and lifecycle checks with clearly synthetic service data. */
class VisualInspection:Instrumentation(){
 override fun onCreate(arguments:Bundle?){super.onCreate(arguments);start()}
 private fun screenshot(name:String){Thread.sleep(1000);val bitmap=uiAutomation.takeScreenshot()?:error("No screenshot");val dir=File(targetContext.filesDir,"visual-qa").apply{mkdirs()};File(dir,"$name.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}}
 private fun find(node:AccessibilityNodeInfo?,label:String):AccessibilityNodeInfo?{if(node==null)return null;if(node.text?.toString()==label||node.contentDescription?.toString()==label)return node;for(i in 0 until node.childCount){val n=find(node.getChild(i),label);if(n!=null)return n};return null}
 private fun click(label:String){var n=find(uiAutomation.rootInActiveWindow,label)?:error("Missing node: $label");while(!n.isClickable){n=n.parent?:error("Node is not clickable: $label")};check(n.performAction(AccessibilityNodeInfo.ACTION_CLICK));Thread.sleep(1400)}
 override fun onStart(){val result=Bundle();try{
  val activity=startActivitySync(Intent(targetContext,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))as MainActivity
  val api=FixtureApi();val vault=Vault(activity);vault.save(JSONObject().put("address","https://fixture.invalid").put("user","界面验收示例").put("password","fixture-only").put("keep",true).put("auto",true).toString())
  val mode=mutableStateOf("light")
  runOnMainSync{activity.setContent{MeridianTheme(mode.value){Meridian(vault,mode.value,{mode.value=it},api)}}};Thread.sleep(2000)
  screenshot("home-light");click("剩余电量");screenshot("detail-light");sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);Thread.sleep(500)
  runOnMainSync{mode.value="dark"};screenshot("home-dark");click("剩余电量");screenshot("detail-dark");sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);Thread.sleep(600)
  click("足迹");screenshot("footprint-dark");click("全国足迹");screenshot("nation-dark");click("返回");click("哨兵模式");screenshot("sentry-dark");click("返回")
  // Closing while the start response is in flight must still stop the acquired session.
  runBlocking{
   val slow=FixtureApi().apply{slowStart=true};lateinit var controller:SentryController
   withContext(Dispatchers.Main){controller=SentryController(activity,slow,1);controller.start()};delay(50);val closed=CompletableDeferred<Unit>();withContext(Dispatchers.Main){controller.stop(false){closed.complete(Unit)}};closed.await();delay(600);check(slow.stops==1){"Cancelled start leaked session: ${slow.stops}"};withContext(Dispatchers.Main){controller.dispose()}
   val unavailable=FixtureApi();withContext(Dispatchers.Main){controller=SentryController(activity,unavailable,1);controller.start()};delay(500);check(unavailable.stops==1){"Unavailable session was not stopped"};withContext(Dispatchers.Main){check(controller.session==null);controller.dispose()}
  }
  vault.clear();result.putString("result","PASS: light/dark home, detail sheets, native footprints, native sentry idle, start cancellation cleanup, unavailable cleanup");finish(0,result)
 }catch(t:Throwable){result.putString("error",android.util.Log.getStackTraceString(t));finish(1,result)}}
}

class FixtureApi:Api(){
 var slowStart=false;var stops=0
 override suspend fun call(path:String,body:JSONObject?):JSONObject{
  val payload:Any=when(path){
   "/user/login"->JSONObject()
   "/car/get"->JSONArray("""[{"id":1,"carName":"雅灰 · 示例车辆","carModel":"Xiaomi SU7"}]""")
   "/dashboard/get"->JSONObject("""{"overview":{"batteryLevel":79,"remainingRange":446,"carLocation":"杭州 · 示例位置","carMode":"正常模式","speed":0,"chargeState":0,"sentrySwitch":"未开启","insideTemp":25,"recent30Mileage":1286.4,"recent30DrivingDurationMs":115200000},"dailyBrief":[{"title":"今日行程","value":3,"unit":"次","trend":"较昨日 +1 次"},{"title":"今日里程","value":49.3,"unit":"km","trend":"较昨日 +12.5 km"},{"title":"今日驾驶","value":68,"trend":"出行有序，平稳抵达"},{"title":"平均电耗","value":14.2,"unit":"kWh/100km","trend":"较昨日 -4.8%"}],"comparisons":[{"label":"行程次数","currentValue":105,"previousValue":93,"unit":"次","changePercent":12.9},{"label":"活跃天数","currentValue":25,"previousValue":22,"unit":"天","changePercent":13.6}],"analysisStats":{"adMileage":342,"humanMileage":944}}""")
   "/car/trip/trackMap"->JSONObject("""{"summary":{"totalMileageKm":49.3,"trackTrips":3,"avgSpeedKmh":38,"totalDurationMinutes":68},"tracks":[{"tripId":1,"startLocation":"家 · 示例","endLocation":"公司 · 示例","tripStartTime":"2026-11-04T08:26:00","drivingMileage":23600,"points":[{"latitude":30.25,"longitude":120.12},{"latitude":30.26,"longitude":120.13},{"latitude":30.28,"longitude":120.16}]}]}""")
   "/car/trip/cityFootprint/get"->JSONObject("""{"summary":{"provinceCount":3,"cityCount":8,"districtCount":16},"regions":[{"name":"浙江省","visitTripCount":30,"lastVisitTime":"2026-11-04T08:26:00"},{"name":"上海市","visitTripCount":12,"lastVisitTime":"2026-10-30T08:26:00"}]}""")
   "/car/sentry/start"->{if(slowStart)delay(250);JSONObject().put("sessionRef","fixture-session").put("status",if(slowStart)"PENDING"else"UNAVAILABLE").put("message","测试不可用状态")}
   "/car/sentry/stop"->{stops++;JSONObject()}
   else->JSONObject()
  }
  return JSONObject().put("code",200).put("data",payload)
 }
}
