package app.meridian.mobile

internal fun shareMetricAvailable(metric:HomeMetricValue):Boolean{
 val value=metric.value.trim().lowercase()
 return Regex("[0-9]").containsMatchIn(value)&&value !in setOf("—","-","--","null","undefined","nan","infinity","暂无数据","暂无","无数据")
}
internal fun emptyChargeGroup(group:StatisticsMetricGroup):Boolean{
 if(group.key!="charge")return false
 val items=group.items.objects();val count=items.firstOrNull{it.safeText("label").contains("次数")}?.number("value")
 return count==0.0&&items.none{(it.number("value")?:0.0)>0}
}
