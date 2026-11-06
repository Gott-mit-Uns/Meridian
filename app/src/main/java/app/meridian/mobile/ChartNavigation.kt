package app.meridian.mobile
import androidx.compose.runtime.staticCompositionLocalOf
import java.time.LocalDate
internal val LocalChartDateAction=staticCompositionLocalOf<((LocalDate)->Unit)?>{null}
internal fun chartDate(label:String):LocalDate?=runCatching{LocalDate.parse(label.take(10))}.getOrNull()
