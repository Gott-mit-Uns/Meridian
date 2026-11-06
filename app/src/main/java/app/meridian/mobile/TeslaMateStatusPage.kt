package app.meridian.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject

@Composable internal fun TeslaMateStatusPage(dashboard: JSONObject?, onClose: () -> Unit) {
    val overview = dashboard?.optJSONObject("overview") ?: JSONObject()
    FullscreenDataPage("车辆状态", onClose) {
        Panel {
            Text(overview.safeText("carMode", "状态待更新"), fontSize = 23.sp)
            Text(dashboard?.safeText("_teslamateStatusNote") ?: "连接车辆服务后读取状态", fontSize = 12.sp, color = muted)
        }
        Panel {
            Column(Modifier.padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("batteryLevel" to "剩余电量 · %", "remainingRange" to "额定续航 · km", "odometer" to "累计里程 · km",
                    "insideTemp" to "车内温度 · °C", "outsideTemp" to "车外温度 · °C").forEach { (key, label) ->
                    Text(label + "    " + numberText(overview.number(key)), fontSize = 15.sp)
                }
                Text("哨兵模式    " + overview.safeText("sentrySwitch", "暂无数据"), fontSize = 15.sp)
            }
        }
    }
}
