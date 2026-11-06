package app.meridian.mobile

import android.util.TypedValue
import android.widget.TextView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.*
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.ext.tables.*
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/** CommonMark rendering, including GFM tables; parse off the UI thread during streaming. */
@Composable fun MarkdownMessage(text:String){
 val context=LocalContext.current;val density=LocalDensity.current;val foreground=LocalContentColor.current.toArgb();val link=accent.toArgb();val wash=softWash.toArgb();val edge=glassEdge.toArgb()
 val textSize=with(density){14.sp.toPx()};val gap=with(density){5.dp.toPx()}
 val renderer=remember(context,foreground,link,wash,edge,density){
  Markwon.builder(context)
   .usePlugin(TablePlugin.create(TableTheme.buildWithDefaults(context).tableBorderColor(edge).tableBorderWidth(1).tableCellPadding(with(density){7.dp.roundToPx()}).tableHeaderRowBackgroundColor(wash).build()))
   .usePlugin(StrikethroughPlugin.create()).usePlugin(TaskListPlugin.create(context))
   .usePlugin(object:AbstractMarkwonPlugin(){override fun configureTheme(builder:MarkwonTheme.Builder){builder.linkColor(link).codeTextColor(foreground).codeBackgroundColor(wash).blockQuoteColor(link).headingBreakHeight(0)}})
   .build()
 }
 var rendered by remember(renderer){mutableStateOf<CharSequence>("")};val latest by rememberUpdatedState(privateProse(text))
 LaunchedEffect(renderer){snapshotFlow{latest}.conflate().collect{source->rendered=withContext(Dispatchers.Default){renderer.toMarkdown(source)}}}
 AndroidView(factory={ctx->TextView(ctx).apply{setPadding(0,0,0,0);includeFontPadding=false;setTextIsSelectable(true);setBackgroundColor(android.graphics.Color.TRANSPARENT)}},modifier=Modifier.fillMaxWidth(),update={view->
  view.setTextColor(foreground);view.setLinkTextColor(link);view.setTextSize(TypedValue.COMPLEX_UNIT_PX,textSize);view.setLineSpacing(gap,1f)
  renderer.setParsedMarkdown(view,rendered as? android.text.Spanned?:android.text.SpannedString(rendered));view.movementMethod=TableAwareMovementMethod.create()
 })
}
