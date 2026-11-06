package app.meridian.mobile
import androidx.compose.runtime.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable fun Button(onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,content:@Composable RowScope.()->Unit)=CinemaButton(onClick,modifier,enabled,content=content)
@Composable fun OutlinedButton(onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,content:@Composable RowScope.()->Unit)=CinemaOutlineButton(onClick,modifier,enabled,content)
@Composable fun TextButton(onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,content:@Composable RowScope.()->Unit)=CinemaTextButton(onClick,modifier,enabled,content)
@Composable fun FilterChip(selected:Boolean,onClick:()->Unit,modifier:Modifier=Modifier,label:@Composable ()->Unit)=CinemaChoice(selected,onClick,label,modifier)
@Composable fun Checkbox(checked:Boolean,onCheckedChange:(Boolean)->Unit,modifier:Modifier=Modifier)=CinemaCheck(checked,onCheckedChange,modifier)
@Composable fun OutlinedTextField(value:String,onValueChange:(String)->Unit,modifier:Modifier=Modifier,label:(@Composable ()->Unit)?=null,placeholder:(@Composable ()->Unit)?=null,singleLine:Boolean=false,maxLines:Int=Int.MAX_VALUE,enabled:Boolean=true,visualTransformation:VisualTransformation=VisualTransformation.None,bare:Boolean=false,keyboardOptions:androidx.compose.foundation.text.KeyboardOptions=androidx.compose.foundation.text.KeyboardOptions.Default,keyboardActions:androidx.compose.foundation.text.KeyboardActions=androidx.compose.foundation.text.KeyboardActions.Default){
 var focused by remember{mutableStateOf(false)}
 var reveal by remember{mutableStateOf(false)}
 val password=visualTransformation is PasswordVisualTransformation
 val shape=RoundedCornerShape(16.dp)
 Column(modifier,verticalArrangement=Arrangement.spacedBy(8.dp)){
  if(label!=null)CompositionLocalProvider(LocalContentColor provides muted){ProvideTextStyle(TextStyle(fontSize=12.sp)){label()}}
  Row(Modifier.fillMaxWidth().then(if(bare)Modifier else Modifier.formSurface(shape).border(if(focused)1.dp else .6.dp,if(focused)ink.copy(alpha=.35f)else glassEdge,shape)).padding(horizontal=if(bare)10.dp else 13.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically){
   BasicTextField(value,onValueChange,modifier=Modifier.weight(1f).onFocusChanged{focused=it.isFocused}.padding(vertical=5.dp),keyboardOptions=keyboardOptions,keyboardActions=keyboardActions,enabled=enabled,singleLine=singleLine,maxLines=if(singleLine)1 else maxLines,visualTransformation=if(password&&reveal)VisualTransformation.None else visualTransformation,textStyle=TextStyle(color=ink,fontSize=15.sp,lineHeight=22.sp),cursorBrush=SolidColor(ink),decorationBox={inner->Box{if(value.isEmpty()&&placeholder!=null)CompositionLocalProvider(LocalContentColor provides muted){placeholder()};inner()}})
   if(password)CinemaIconButton({reveal=!reveal}){Icon(if(reveal)Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,if(reveal)"隐藏密码"else"显示密码",Modifier.size(18.dp),tint=muted)}
  }
 }
}

@Composable fun Switch(checked:Boolean,onCheckedChange:(Boolean)->Unit)=CinemaSwitch(checked,onCheckedChange)
@Composable fun AlertDialog(onDismissRequest:()->Unit,confirmButton:@Composable ()->Unit,title:(@Composable ()->Unit)?=null,text:(@Composable ()->Unit)?=null,dismissButton:(@Composable ()->Unit)?=null)=FlowDetailPanel(onDismissRequest,confirmButton,title,text,dismissButton)

@Composable fun SendButton(onClick:()->Unit,enabled:Boolean=true,content:@Composable RowScope.()->Unit){CinemaIconButton(onClick,Modifier.size(44.dp),enabled){CompositionLocalProvider(LocalContentColor provides accent){Row(content=content)}}}
