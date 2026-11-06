package app.meridian.mobile
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import org.json.*

internal val LocalAppDestination=staticCompositionLocalOf<((String)->Unit)?>{null}
internal fun assistantCitationTripIds(metadata:JSONObject):List<String>{val result=mutableListOf<String>();for(key in listOf("sources","references","citations","evidence")){metadata.optJSONArray(key)?.objects()?.forEach{row->val value=row.optString("tripId").ifBlank{if(row.optString("type").equals("trip",true))row.optString("id")else""};if(value.isNotBlank()&&value.length<=80)result.add(value)}};return result.distinct().take(8)}
@Composable internal fun AssistantEvidence(api:Api,metadata:JSONObject){
 var open by remember{mutableStateOf(false)};var detail by remember{mutableStateOf<JSONObject?>(null)};val action=rememberServerAction();val navigate=LocalAppDestination.current;val ids=assistantCitationTripIds(metadata)
 CinemaTextButton({open=true}){Icon(Icons.Outlined.FactCheck,null,Modifier.size(15.dp));Spacer(Modifier.width(5.dp));Text(if(ids.isEmpty())"核对用车数据"else"查看关联行程",fontSize=12.sp)}
 if(open)FlowDetailPanel({open=false},title={Text("核对数据")},body={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
 Text(if(ids.isEmpty())"服务端未返回可验证的行程引用。以下入口用于自行核对，不代表这些数据已被助手引用。"else"以下行程编号由助手服务端返回，可打开原始记录核对。",fontSize=12.sp,color=muted)
 ids.forEachIndexed{i,id->CinemaTonalButton({action.run{detail=api.call("/car/trip/getDetail",JSONObject().put("tripId",id)).optJSONObject("data")?:throw Exception("未返回行程详情")}},Modifier.fillMaxWidth(),enabled=!action.busy){Text("关联行程 "+(i+1));Spacer(Modifier.weight(1f));Icon(Icons.Outlined.ChevronRight,null,Modifier.size(16.dp))}}
 if(navigate!=null){CinemaTonalButton({open=false;navigate("records")},Modifier.fillMaxWidth()){Text("查看行程记录")};CinemaTonalButton({open=false;navigate("month")},Modifier.fillMaxWidth()){Text("查看本月用车统计")}}
 ServerNotice(action);detail?.let{TripDetailPage(api,it){detail=null}}
 }},confirm={CinemaTextButton({open=false}){Text("关闭")}})
}
