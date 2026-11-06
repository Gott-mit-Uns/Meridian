package app.meridian.mobile
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.*

@Composable internal fun SettingsDirectory(hasCar:Boolean,teslaMate:Boolean=false,onOpen:(String)->Unit){
 var search by remember{mutableStateOf("")};val context=LocalContext.current
 OutlinedTextField(search,{search=it},placeholder={Text("搜索设置、账本、收藏…",fontSize=13.sp)},singleLine=true,modifier=Modifier.fillMaxWidth())
 val groups=listOf(listOf(Triple("appearance","外观与动效",Icons.Outlined.Palette),Triple("widget","桌面卡片与内容",Icons.Outlined.Widgets)),listOf(Triple("ledger","用车账单",Icons.Outlined.AccountBalanceWallet),Triple("library","行程收藏与手记",Icons.Outlined.Bookmarks),Triple("routes","常用路线对比",Icons.Outlined.AltRoute),Triple("inbox","消息中心",Icons.Outlined.NotificationsNone)),listOf(Triple("vehicles","车辆管理与数据同步",Icons.Outlined.DirectionsCar),Triple("account","账户与服务",Icons.Outlined.ManageAccounts),Triple("notifications","通知渠道设置",Icons.Outlined.Notifications)),listOf(Triple("backup","NAS备份与恢复",Icons.Outlined.Backup),Triple("localArchive","本机资料备份",Icons.Outlined.EnhancedEncryption),Triple("diagnostics","连接诊断与本机缓存",Icons.Outlined.NetworkCheck),Triple("about","关于 Meridian",Icons.Outlined.Info)))
 val query=search.trim();var found=false
 groups.forEach{group->val items=group.filter{(!teslaMate||it.first !in setOf("ledger","vehicles","account","notifications","backup","inbox"))&&(hasCar||it.first !in listOf("ledger","library","routes","inbox","localArchive"))&&(query.isBlank()||it.second.contains(query,true))};if(items.isNotEmpty()){found=true;FlowSettingsGroup{items.forEachIndexed{i,item->FlowSettingsRow(item.third,item.second,i<items.lastIndex){if(hasCar||item.first !in listOf("ledger","library","routes","inbox","localArchive"))onOpen(item.first)}}}}}
 if(!found&&!"隐私模式".contains(query))Text("没有找到相关设置",fontSize=13.sp,color=muted)
 if(query.isBlank()||"隐私模式".contains(query))FlowSettingsGroup{Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){Icon(Icons.Outlined.PrivacyTip,null,Modifier.size(24.dp));Column(Modifier.weight(1f)){Text("隐私模式",fontSize=15.sp);Text("地址只显示到市，后台预览隐藏内容",fontSize=12.sp,color=muted)};Switch(PrivacyMode.enabled,{PrivacyMode.set(context,it)})}}
}
