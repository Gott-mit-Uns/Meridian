package app.meridian.mobile
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import app.meridian.mobile.glass.*

@Composable fun CinemaCheck(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val tint = MaterialTheme.colorScheme.primary; val edge = MaterialTheme.colorScheme.onSurface.copy(alpha = .2f)
    Box(modifier.size(48.dp).then(if (onCheckedChange != null) Modifier.toggleable(checked, enabled, role = Role.Checkbox, onValueChange = onCheckedChange) else Modifier), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(22.dp).alpha(if (enabled) 1f else .4f)) {
            drawRoundRect(if (checked) tint else Color.Transparent, cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()))
            if (!checked) drawRoundRect(edge, cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()), style = Stroke(1.5.dp.toPx()))
            else { val path = Path().apply { moveTo(size.width * .24f, size.height * .51f); lineTo(size.width * .44f, size.height * .7f); lineTo(size.width * .79f, size.height * .31f) }; drawPath(path, Color.White, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)) }
        }
    }
}

@Composable fun CinemaSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val fraction by animateFloatAsState(if (checked) 1f else 0f, if (!LocalAppMotion.current.full) snap() else spring(dampingRatio=1f,stiffness=1000f), label = "cinemaSwitch")
    val tint = MaterialTheme.colorScheme.primary; val off = MaterialTheme.colorScheme.onSurface.copy(alpha = .15f)
    Box(modifier.width(56.dp).height(48.dp).then(if (onCheckedChange != null) Modifier.toggleable(checked, enabled, Role.Switch, onValueChange = onCheckedChange) else Modifier), contentAlignment = Alignment.Center) {
        Canvas(Modifier.width(48.dp).height(29.dp).alpha(if (enabled) 1f else .4f)) {
            drawRoundRect(lerp(off, tint, fraction), cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2))
            drawCircle(Color.Black.copy(alpha = .08f), 11.dp.toPx(), Offset(14.dp.toPx() + fraction * 20.dp.toPx(), size.height / 2 + 1.dp.toPx()))
            drawCircle(Color.White, 10.5.dp.toPx(), Offset(14.dp.toPx() + fraction * 20.dp.toPx(), size.height / 2))
        }
    }
}

@Composable fun CinemaSlider(value: Float, onValueChange: (Float) -> Unit, modifier: Modifier = Modifier, valueRange: ClosedFloatingPointRange<Float> = 0f..1f, onValueChangeFinished: (() -> Unit)? = null, onValueChangeCancelled: (() -> Unit)? = null, bufferedValue: Float? = null) {
    val tint = MaterialTheme.colorScheme.primary; val track = LocalContentColor.current.copy(alpha = .15f)
    val current by rememberUpdatedState(onValueChange); val finished by rememberUpdatedState(onValueChangeFinished)
    val cancelled by rememberUpdatedState(onValueChangeCancelled)
    val thumbInset=with(androidx.compose.ui.platform.LocalDensity.current){8.dp.toPx()}
    var dragging by remember {mutableStateOf(false)}
    val emphasis=animateFloatAsState(if(dragging)1f else 0f,
        if(LocalAppMotion.current.full)spring(dampingRatio=1f,stiffness=700f) else snap(),label="sliderTouch")
    var width by remember { mutableIntStateOf(1) }
    val usable=sliderRangeUsable(valueRange)
    val displayRange=if(usable)valueRange else 0f..1f
    val displayValue=if(usable)sliderDisplayValue(value,displayRange) else 0f
    fun update(x: Float) { if(usable)current(valueRange.start + sliderTouchFraction(x,width.toFloat(),thumbInset) * (valueRange.endInclusive - valueRange.start)) }
    val fraction=if(usable)(displayValue-valueRange.start)/(valueRange.endInclusive-valueRange.start) else 0f
    Canvas(modifier.fillMaxWidth().height(48.dp).alpha(if(usable)1f else GlassSpec.disabled).onSizeChanged { width = it.width }.semantics {
        if(!usable)disabled()
        progressBarRangeInfo=ProgressBarRangeInfo(displayValue,displayRange)
        setProgress { if(!usable || !it.isFinite())false else {current(it.coerceIn(valueRange));finished?.invoke();true} }
    }.pointerInput(valueRange,thumbInset,usable) { if(!usable)return@pointerInput;detectTapGestures { update(it.x); finished?.invoke() } }.pointerInput(valueRange,thumbInset,usable) {
        if(!usable)return@pointerInput
        detectHorizontalDragGestures(onDragStart = { dragging=true;update(it.x) }, onDragEnd = { dragging=false;finished?.invoke() }, onDragCancel = { dragging=false;cancelled?.invoke() }) { change, _ -> change.consume(); update(change.position.x) }
    }) {
        val y = size.height / 2
        val touch=emphasis.value.coerceIn(0f,1f)
        val trackWidth=(4f+touch).dp.toPx()
        val start=minOf(thumbInset,size.width/2f)
        val end=maxOf(start,size.width-start)
        val span=end-start
        drawLine(track, Offset(start, y), Offset(end, y), trackWidth, StrokeCap.Round)
        bufferedValue?.takeIf{it.isFinite() && it>0f}?.let { cached ->
            drawLine(Color.White.copy(alpha=.66f),Offset(start,y),Offset(start+span*cached.coerceIn(0f,1f),y),trackWidth,StrokeCap.Round)
        }
        drawLine(tint, Offset(start, y), Offset(start+span*fraction, y), trackWidth, StrokeCap.Round)
        val thumb=Offset(start+span*fraction,y)
        if(touch>0f)drawCircle(tint.copy(alpha=.10f*touch),13.dp.toPx(),thumb)
        drawCircle(tint,(6f+2f*touch).dp.toPx(),thumb);drawCircle(Color.White,2.dp.toPx(),thumb)
    }
}

internal fun sliderTouchFraction(x:Float,width:Float,inset:Float):Float {
    if(!x.isFinite() || !width.isFinite() || width<=0f)return 0f
    val edge=inset.takeIf{it.isFinite()}?.coerceIn(0f,width/2f) ?: 0f
    val span=width-2f*edge
    return if(span>0f)((x-edge)/span).coerceIn(0f,1f) else 0f
}

internal fun sliderRangeUsable(range:ClosedFloatingPointRange<Float>)=
    range.start.isFinite() && range.endInclusive.isFinite() && (range.endInclusive-range.start).isFinite() && range.endInclusive>range.start
internal fun sliderDisplayValue(value:Float,range:ClosedFloatingPointRange<Float>):Float =
    if(!sliderRangeUsable(range))0f else (value.takeIf{it.isFinite()} ?: range.start).coerceIn(range)
