package app.meridian.mobile

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import org.json.JSONObject
import java.util.Locale

fun JSONObject.safeText(key:String,fallback:String=""):String {
 val value=opt(key)
 val text=if(value==null||value==JSONObject.NULL||value.toString().trim().lowercase(Locale.ROOT) in listOf("null","undefined","nan","infinity","-infinity"))fallback else value.toString()
 return if(addressField(key))privateAddress(text)else if(key in setOf("storySummary","summary","shareCopy"))privateProse(text)else text
}

fun metricIcon(label:String):ImageVector=when {
 label.contains("电量")||label.contains("电池")->Icons.Outlined.BatteryChargingFull
 label.contains("能耗")||label.contains("耗电")->Icons.Outlined.Bolt
 label.contains("充电")||label.contains("补能")->Icons.Outlined.EvStation
 label.contains("里程")||label.contains("续航")->Icons.Outlined.Route
 label.contains("时长")||label.contains("时间")||label.contains("驾驶")->Icons.Outlined.Schedule
 label.contains("速度")||label.contains("车速")->Icons.Outlined.Speed
 label.contains("天数")||label.contains("天")||label.contains("活跃")->Icons.Outlined.CalendarMonth
 label.contains("温度")->Icons.Outlined.Thermostat
 label.contains("辅助")||label.contains("智驾")->Icons.Outlined.AutoMode
 label.contains("行程")||label.contains("次数")->Icons.Outlined.DirectionsCar
 label.contains("城市")||label.contains("足迹")->Icons.Outlined.LocationOn
 label.contains("安全")||label.contains("车况")->Icons.Outlined.VerifiedUser
 else->Icons.Outlined.Insights
}
