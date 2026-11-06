package app.meridian.mobile
internal data class ShareReportContent(val metrics:List<HomeMetricValue>,val charts:List<ReportChart>)
internal fun shareReportContent(metrics:List<HomeMetricValue>,charts:List<ReportChart>,full:Boolean):ShareReportContent{
 val valid=metrics.filter(::shareMetricAvailable)
 if(full)return ShareReportContent(valid,charts)
 val priorities=listOf("里程","能耗","耗电","时长","天数","行程","次数")
 val ordered=valid.withIndex().sortedWith(compareBy<IndexedValue<HomeMetricValue>>{if(it.value.trend.contains("累计")||it.value.label.startsWith("累计"))1 else 0}.thenBy{it.index}).map{it.value}
 val selected=mutableListOf<HomeMetricValue>()
 // One representative per metric family first; several mileage variants must not crowd out energy/time.
 val period=ordered.filter{!it.trend.contains("累计")&&!it.label.startsWith("累计")}
 val pool=period.ifEmpty{ordered}
 priorities.forEach{word->if(selected.size<6)pool.firstOrNull{it !in selected&&it.label.contains(word)}?.let{selected.add(it)}}
 ordered.forEach{if(selected.size<6&&it !in selected)selected.add(it)}

 return ShareReportContent(selected,charts.take(3))
}
