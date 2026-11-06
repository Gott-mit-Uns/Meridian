package app.meridian.mobile
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.unit.*
import org.json.JSONObject

@Composable internal fun PageSkeleton(){Column(verticalArrangement=Arrangement.spacedBy(12.dp)){repeat(3){Column(Modifier.fillMaxWidth().background(glassWhite,RoundedCornerShape(18.dp)).padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Box(Modifier.fillMaxWidth(.38f).height(14.dp).background(softWash,CircleShape));Box(Modifier.fillMaxWidth(.7f).height(28.dp).background(softWash,CircleShape));Box(Modifier.fillMaxWidth().height(10.dp).background(softWash,CircleShape))}}}}
internal fun responseSyncTime(response:JSONObject?):Long=response?.optLong("_meridianUpdatedAt",0)?.takeIf{it>0}?:response?.optJSONObject("data")?.optLong("_meridianUpdatedAt",0)?:0L
@Composable internal fun CachedReadStatus(response:JSONObject?,onRetry:(()->Unit)?=null){
 if(response?.optBoolean("_meridianOffline")==true||response?.optJSONObject("data")?.optBoolean("_meridianOffline")==true)Text("离线缓存 · 当前未连接服务",fontSize=11.sp,color=muted)
}
@Composable internal fun SolidActionButton(onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,content:@Composable RowScope.()->Unit){
 val source=remember{androidx.compose.foundation.interaction.MutableInteractionSource()};val shape=RoundedCornerShape(14.dp)
 Row(modifier.heightIn(min=48.dp).motionPress(source).clip(shape).background(accent.copy(alpha=if(enabled)1f else .3f)).clickable(interactionSource=source,indication=null,enabled=enabled,onClick=onClick).padding(horizontal=14.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center){CompositionLocalProvider(LocalContentColor provides themeOnAccent(accent)){ProvideTextStyle(UiType.label.copy(fontWeight=androidx.compose.ui.text.font.FontWeight.SemiBold)){content()}}}
}
@Composable internal fun AssistantSendButton(onClick:()->Unit,enabled:Boolean=true,content:@Composable ()->Unit){Box(Modifier.size(44.dp).cardClick(enabled,onClick).shadow(if(enabled)2.dp else 0.dp,CircleShape).clip(CircleShape).background(accent.copy(alpha=if(enabled)1f else .30f)),contentAlignment=Alignment.Center){CompositionLocalProvider(LocalContentColor provides themeOnAccent(accent),content=content)}}
