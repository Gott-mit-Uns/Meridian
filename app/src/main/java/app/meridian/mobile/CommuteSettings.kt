package app.meridian.mobile

import org.json.JSONObject
import java.time.LocalTime

internal fun validateCommuteOverrides(overrides:JSONObject?){
 overrides?:return
 val weekdays=setOf("MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY","SUNDAY")
 for(day in overrides.keys()){
  require(day in weekdays){"通勤设置包含无效星期"}
  val plan=overrides.optJSONObject(day)?:throw IllegalArgumentException("通勤设置格式无效")
  for((enabled,key)in listOf("enabled" to "reminderTime","eveningEnabled" to "eveningReminderTime")){
   val value=plan.optString(key,"")
   if(plan.optBoolean(enabled)||value.isNotBlank())require(Regex("\\d{2}:\\d{2}").matches(value)&&runCatching{LocalTime.parse(value)}.isSuccess){"提醒时间请填写有效的 HH:mm"}
  }
 }
}
