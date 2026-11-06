package app.meridian.mobile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.*

/** The raised blue-violet glass sparkle in the approved dock reference. */
@Composable fun AssistantOrb(modifier:Modifier=Modifier,size:Dp=48.dp,quiet:Boolean=false) {
 val dark=LocalPalette.current.dark
 val colors=if(dark)listOf(Color(0xFF52627D),Color(0xFF28354D),Color(0xFF151D30))else listOf(Color(0xFFF1F4FF),Color(0xFFB5C3FF),Color(0xFF9395EB))
 Box(modifier.size(size).shadow(if(quiet)2.dp else 9.dp,CircleShape,ambientColor=Color(0xFF7772DF).copy(alpha=.18f),spotColor=Color(0xFF6866D6).copy(alpha=.3f)).clip(CircleShape)
  .background(Brush.verticalGradient(colors))
  .background(Brush.radialGradient(listOf(Color.White.copy(alpha=if(dark).08f else .28f),Color.Transparent),center=Offset(25f,8f),radius=110f))
  .border(1.dp,Brush.verticalGradient(listOf(Color.White.copy(alpha=if(dark).4f else .95f),Color.White.copy(alpha=if(dark).08f else .3f))),CircleShape)) {
  Canvas(Modifier.fillMaxSize().padding(size*.22f)) {
   fun star(x:Float,y:Float,r:Float){val p=Path().apply{moveTo(x,y-r);cubicTo(x+r*.2f,y-r*.25f,x+r*.25f,y-r*.2f,x+r,y);cubicTo(x+r*.25f,y+r*.2f,x+r*.2f,y+r*.25f,x,y+r);cubicTo(x-r*.2f,y+r*.25f,x-r*.25f,y+r*.2f,x-r,y);cubicTo(x-r*.25f,y-r*.2f,x-r*.2f,y-r*.25f,x,y-r);close()};drawPath(p,if(dark)Color(0xFFD8E9FF)else Color.White)}
   star(this.size.width*.45f,this.size.height*.52f,this.size.width*.4f)
   star(this.size.width*.87f,this.size.height*.13f,this.size.width*.115f)
  }
 }
}
