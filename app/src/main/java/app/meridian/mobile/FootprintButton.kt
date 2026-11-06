package app.meridian.mobile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.runtime.Composable
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.*
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.*

@Composable fun FootprintButton(enabled:Boolean=true,onClick:()->Unit){
 val dark=LocalPalette.current.dark
 val shape=CircleShape;val tint=accent;val surface=glassWhite;val wash=softWash
 HomeHeaderButton("我的足迹",onClick,enabled){
  Canvas(Modifier.size(26.dp)){
   val c=center;val radius=size.minDimension*.40f;val line=tint;val stroke=Stroke(1.1.dp.toPx())
   drawCircle(Brush.radialGradient(listOf(surface,wash)),radius,c)
   drawCircle(line.copy(alpha=.85f),radius,c,style=stroke)
   drawOval(line.copy(alpha=.6f),Offset(c.x-radius*.43f,c.y-radius),Size(radius*.86f,radius*2),style=stroke)
   drawOval(line.copy(alpha=.55f),Offset(c.x-radius,c.y-radius*.32f),Size(radius*2,radius*.64f),style=stroke)
  }
 }
}

@Composable internal fun HomeHeaderButton(label:String,onClick:()->Unit,enabled:Boolean=true,content:@Composable ()->Unit){Box(Modifier.size(44.dp).alpha(if(enabled)1f else .4f).clip(CircleShape).clickable(enabled=enabled,role=Role.Button,onClickLabel=label,onClick=onClick),contentAlignment=Alignment.Center){SharedGlassControl(CircleShape,Modifier.matchParentSize());content()}}
@Composable internal fun HomeEditButton(editing:Boolean,enabled:Boolean=true,onClick:()->Unit){HomeHeaderButton(if(editing)"完成编辑"else"编辑首页卡片",onClick,enabled){Icon(if(editing)Icons.Outlined.Check else Icons.Outlined.Edit,null,Modifier.size(22.dp),tint=accent)}}
