package app.meridian.mobile
import org.junit.Assert.*
import org.junit.Test
class ReportShareSizeTest{
 private val metrics=(1..12).map{HomeMetricValue("里程$it","$it","km")}
 private val charts=(1..5).map{ReportChart("图表$it","km",listOf(ChartSample("一天",1.0)))}
 @Test fun compactCapsContentWithoutScalingText(){val result=shareReportContent(metrics,charts,false);assertEquals(6,result.metrics.size);assertEquals(3,result.charts.size);assertEquals(metrics.take(6),result.metrics);assertEquals(charts.take(3),result.charts)}
 @Test fun fullPreservesEveryValidMetricAndChart(){val result=shareReportContent(metrics,charts,true);assertEquals(metrics,result.metrics);assertEquals(charts,result.charts)}
 @Test fun compactPrioritizesPeriodMileageOverLifetime(){val values=listOf(HomeMetricValue("累计总里程","999","km","累计指标"),HomeMetricValue("本期里程","12","km","周期"));assertEquals("本期里程",shareReportContent(values,emptyList(),false).metrics.first().label)}
 @Test fun unavailableValuesAreOmitted(){assertTrue(shareReportContent(listOf(HomeMetricValue("里程","—","km")),emptyList(),true).metrics.isEmpty())}
 @Test fun compactIncludesDifferentFamiliesBeforeMileageVariants(){val values=(1..8).map{HomeMetricValue("里程$it","$it","km")}+listOf(HomeMetricValue("平均能耗","12","kWh/100km"),HomeMetricValue("驾驶时长","2","小时"));val labels=shareReportContent(values,emptyList(),false).metrics.map{it.label};assertTrue(labels.contains("平均能耗"));assertTrue(labels.contains("驾驶时长"));assertEquals(6,labels.size)}
}
