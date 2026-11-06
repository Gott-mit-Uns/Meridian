package app.meridian.mobile

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.*

/** Measure text at its real column width before sizing backgrounds. No intrinsic
 * height / fillMaxHeight feedback loop can compress multi-line metric text. */
@Composable internal fun EqualCardRow(count:Int,modifier:Modifier=Modifier,gap:Dp=10.dp,
 surface:@Composable (Int,Modifier)->Unit,content:@Composable (Int)->Unit){
 Layout(modifier=modifier.fillMaxWidth(),content={
  repeat(count){surface(it,Modifier)}
  repeat(count){Box(Modifier.padding(14.dp)){content(it)}}
 }){children,constraints->
  val spacing=gap.roundToPx();val width=((constraints.maxWidth-spacing)/2).coerceAtLeast(0)
  val contents=(0 until count).map{children[count+it].measure(Constraints.fixedWidth(width))}
  val height=contents.maxOfOrNull{it.height}?:0
  val surfaces=(0 until count).map{children[it].measure(Constraints.fixed(width,height))}
  layout(constraints.maxWidth,height){repeat(count){i->val x=i*(width+spacing);surfaces[i].placeRelative(x,0);contents[i].placeRelative(x,0)}}
 }
}
