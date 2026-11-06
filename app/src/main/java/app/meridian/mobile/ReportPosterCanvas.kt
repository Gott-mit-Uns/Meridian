package app.meridian.mobile

import android.graphics.*
import android.text.*
import kotlin.math.*

/** Standalone report artwork: editorial header, hero, multiple data visualisations,
 * a quiet ledger and optional narrative. No screen capture or account fields. */
internal fun usagePoster(title:String,period:String,inputMetrics:List<HomeMetricValue>,charts:List<Pair<String,List<ChartSample>>> = emptyList(),story:String="",visualCharts:List<ReportChart> = emptyList(),colors:PosterTheme=PosterTheme(),coverStyle:String="classic",nickname:String=""):Bitmap{
 val metrics=inputMetrics.filter(::shareMetricAvailable)
 val ink=colors.text;val secondary=colors.muted;val blue=colors.accent
 val tones=colors.chartColors
 fun pen(size:Float,color:Int=ink,bold:Boolean=false)=TextPaint(Paint.ANTI_ALIAS_FLAG).apply{textSize=size;this.color=color;typeface=Typeface.create("sans-serif",if(bold)Typeface.BOLD else Typeface.NORMAL)}
 fun layout(s:String,size:Float,width:Int,color:Int=ink,bold:Boolean=false)=StaticLayout.Builder.obtain(s,0,s.length,pen(size,color,bold),width).setIncludePad(false).setLineSpacing(5f,1f).build()
 val plots=(visualCharts+charts.map{ReportChart(it.first,"",it.second)}).map{it.copy(samples=it.samples.filter{v->v.value.isFinite()&&v.value>=0})}.filter{it.samples.isNotEmpty()}.take(8)
 fun chartHeight(chart:ReportChart)=when(chart.kind){"bars"->150+chart.samples.take(12).sumOf{max(63,layout(it.label,24f,235).height+20)};"donut"->max(420,145+chart.samples.take(10).sumOf{max(48,layout(it.label,23f,210).height+16)});else->430}
 val hero=metrics.take(2);val ledger=metrics.drop(2).chunked(2)
 val rowHeights=ledger.map{row->row.maxOf{layout(it.label,25f,410,secondary).height+layout(it.value,36f,410,ink,true).height+layout(listOf(it.unit,it.trend).filter{v->v.isNotBlank()}.joinToString(" · "),21f,410,secondary).height+90}}
 val heroHeight=if(hero.isEmpty())0 else hero.maxOf{layout(it.value,53f,410,Color.WHITE,true).height+layout(it.label,25f,410,Color.LTGRAY).height+layout(listOf(it.unit,it.trend).filter{v->v.isNotBlank()}.joinToString(" · "),22f,410,Color.LTGRAY).height+94}
 val storyLayout=story.takeIf{it.isNotBlank()}?.let{layout(it.take(900),28f,900)}
 val height=300+heroHeight+(if(hero.isEmpty())0 else 26)+plots.sumOf{chartHeight(it)+24}+(if(ledger.isEmpty())0 else 86+rowHeights.sum())+(storyLayout?.height?.plus(140)?:0)+130
 val bitmap=Bitmap.createBitmap(1080,height,Bitmap.Config.ARGB_8888);val c=Canvas(bitmap);c.drawColor(colors.background.first());if(colors.background.size>1)c.drawRect(0f,0f,1080f,height.toFloat(),pen(1f).apply{shader=LinearGradient(0f,0f,1080f,height.toFloat(),colors.background.toIntArray(),null,Shader.TileMode.CLAMP)})
 fun text(s:String,x:Float,y:Float,size:Float=27f,color:Int=ink,bold:Boolean=false){c.drawText(s,x,y,pen(size,color,bold))}
 fun block(l:StaticLayout,x:Float,y:Float){c.save();c.translate(x,y);l.draw(c);c.restore()}
 fun box(y:Float,h:Float){c.drawRoundRect(60f,y,1020f,y+h,30f,30f,pen(1f,colors.card))}
 if(coverStyle=="gallery"){c.drawRoundRect(32f,24f,1048f,274f,32f,32f,pen(1f,colors.card));c.drawRoundRect(44f,105f,50f,226f,3f,3f,pen(1f,blue))}
 text(if(coverStyle=="minimal")"M E R I D I A N"else"MERIDIAN   /   DRIVE JOURNAL",60f,76f,24f,blue,true);text(title,60f,155f,if(coverStyle=="minimal")60f else 52f,ink,true);text(period,60f,208f,27f,secondary);text(nickname.trim().take(22).takeIf{it.isNotBlank()}?.let{privateProse(it)+" · 用车记录"}?:"每一次出发，留下清晰的用车印记",60f,252f,23f,secondary)
 var y=290f
 if(hero.isNotEmpty()){
  c.drawRoundRect(60f,y,1020f,y+heroHeight,30f,30f,pen(1f).apply{shader=LinearGradient(60f,y,1020f,y+heroHeight,intArrayOf(colors.heroStart,colors.heroEnd),null,Shader.TileMode.CLAMP)})
  hero.forEachIndexed{i,m->val x=90f+i*480;var top=y+30;val label=layout(m.label,25f,410,Color.rgb(191,210,230));block(label,x,top);top+=label.height+20;val value=layout(m.value,53f,410,Color.WHITE,true);block(value,x,top);top+=value.height+14;block(layout(listOf(m.unit,m.trend).filter{it.isNotBlank()}.joinToString(" · "),22f,410,Color.rgb(191,210,230)),x,top)}
  if(hero.size==2)c.drawLine(540f,y+32,540f,y+heroHeight-32,pen(1f,Color.rgb(96,118,143)).apply{strokeWidth=1f})
  y+=heroHeight+26
 }
 plots.forEachIndexed{index,chart->
  val h=chartHeight(chart).toFloat();box(y,h);text("0${index+1}",90f,y+48,23f,blue,true);text(chart.title.take(25),143f,y+48,29f,ink,true);if(chart.unit.isNotBlank())text(chart.unit,90f,y+84,21f,secondary)
  val values=chart.samples;val peak=(values.maxOfOrNull{it.value}?:0.0).coerceAtLeast(1.0)
  when(chart.kind){
   "bars"->{var top=y+112;values.take(12).forEachIndexed{i,v->val label=layout(v.label,24f,235,secondary);val rh=max(63,label.height+20);block(label,90f,top);val cy=top+14;c.drawRoundRect(340f,cy,820f,cy+16,8f,8f,pen(1f,colors.wash));val width=(v.value/peak*480).toFloat();if(width>0)c.drawRoundRect(340f,cy,340f+width,cy+16,8f,8f,pen(1f,tones[i%tones.size]));text(numberText(v.value),852f,top+27,25f,ink,true);top+=rh}}
   "donut"->{val total=values.sumOf{it.value};val cx=290f;val cy=y+h/2+25;val bounds=RectF(cx-122,cy-122,cx+122,cy+122);val stroke=pen(1f,colors.wash).apply{style=Paint.Style.STROKE;strokeWidth=37f;strokeCap=Paint.Cap.BUTT};c.drawOval(bounds,stroke);var angle=-90f;if(total>0)values.forEachIndexed{i,v->val sweep=(v.value/total*360).toFloat();stroke.color=tones[i%tones.size];c.drawArc(bounds,angle,sweep,false,stroke);angle+=sweep};text(if(chart.unit=="%")"能耗组成"else"结构分布",cx-65,cy+10,27f,ink,true);var top=y+115;values.take(10).forEachIndexed{i,v->c.drawCircle(505f,top+8,6f,pen(1f,tones[i%tones.size]));val label=layout(v.label,23f,210,secondary);block(label,526f,top-9);text(numberText(v.value)+if(chart.unit=="%")"%"else"",800f,top+15,25f,ink,true);top+=max(48,label.height+16)}}
   else->{val left=110f;val right=970f;val bottom=y+330;val top=y+126;repeat(4){n->val gy=bottom-(bottom-top)*n/3;c.drawLine(left,gy,right,gy,pen(1f,colors.border).apply{strokeWidth=1f});text(numberText(peak*n/3,0),90f,gy-7,19f,secondary)}
    if(chart.kind=="bar"){val slot=(right-left)/values.size;values.forEachIndexed{i,v->val bh=(v.value/peak*(bottom-top)).toFloat();val x=left+i*slot+slot*.20f;val w=(slot*.6f).coerceAtMost(35f);if(bh>0)c.drawRoundRect(x,bottom-bh,x+w,bottom,6f,6f,pen(1f).apply{shader=LinearGradient(0f,bottom-bh,0f,bottom,tones[0],colors.wash,Shader.TileMode.CLAMP)})}}
    else{val path=Path();values.forEachIndexed{i,v->val x=left+i*(right-left)/(values.size-1).coerceAtLeast(1);val py=bottom-(v.value/peak*(bottom-top)).toFloat();if(i==0)path.moveTo(x,py)else path.lineTo(x,py)};val area=Path(path).apply{lineTo(right,bottom);lineTo(left,bottom);close()};if(values.size>1)c.drawPath(area,pen(1f).apply{shader=LinearGradient(0f,top,0f,bottom,androidx.core.graphics.ColorUtils.setAlphaComponent(blue,75),androidx.core.graphics.ColorUtils.setAlphaComponent(blue,0),Shader.TileMode.CLAMP)});c.drawPath(path,pen(1f,blue).apply{style=Paint.Style.STROKE;strokeWidth=4f;strokeJoin=Paint.Join.ROUND});if(values.size==1)c.drawCircle(left,bottom-(values.first().value/peak*(bottom-top)).toFloat(),6f,pen(1f,blue))}
    text(values.first().label.take(10),110f,y+371,21f,secondary);text(values.last().label.take(10),820f,y+371,21f,secondary);text("峰值 "+numberText(values.maxOf{it.value})+" "+chart.unit,110f,y+407,22f,blue)
   }
  }
  y+=h+24
 }
 if(ledger.isNotEmpty()){
  text("数据档案",60f,y+40,31f,ink,true);y+=70
  ledger.forEachIndexed{i,row->
   row.forEachIndexed{j,m->val x=60f+j*490;val h=rowHeights[i].toFloat()-16f
    c.drawRoundRect(x,y,x+470,y+h,24f,24f,pen(1f,colors.card));c.drawRoundRect(x,y,x+470,y+h,24f,24f,pen(1f,colors.border).apply{style=Paint.Style.STROKE;strokeWidth=1f})
    var top=y+24;val label=layout(m.label,25f,410,secondary);block(label,x+25,top);top+=label.height+12;val value=layout(m.value,36f,410,ink,true);block(value,x+25,top);top+=value.height+9;block(layout(listOf(m.unit,m.trend).filter{it.isNotBlank()}.joinToString(" · "),21f,410,secondary),x+25,top)
   };y+=rowHeights[i]
  }
 }
 storyLayout?.let{text("本月回顾",60f,y+36,29f,ink,true);block(it,90f,y+72);y+=it.height+130}
 text("MERIDIAN · 连接每一次出发",60f,height-48f,23f,secondary);text("数据来自车辆服务",790f,height-48f,21f,secondary)
 return bitmap
}
