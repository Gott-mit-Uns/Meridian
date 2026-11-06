package app.meridian.mobile
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
class DailyBriefMetricTest{
 @Test fun stringMinutesHaveAVisibleDurationUnit(){val m=dailyBriefMetric(JSONObject().put("title","今日驾驶").put("value","90"),true);assertEquals("1时30分",m.value);assertEquals("",m.unit)}
 @Test fun numericHoursRespectDeclaredUnit(){val m=dailyBriefMetric(JSONObject().put("title","今日驾驶").put("value",1.5).put("unit","小时"),true);assertEquals("1时30分",m.value)}
 @Test fun formattedDurationIsNotGivenDuplicateUnits(){val m=dailyBriefMetric(JSONObject().put("title","今日驾驶").put("value","1小时30分钟"),true);assertEquals("1小时30分钟",m.value);assertEquals("",m.unit)}
 @Test fun missingTimeAndOtherMetricsRetainTheirUnits(){assertEquals("分钟",dailyBriefMetric(JSONObject().put("title","今日驾驶"),false).unit);assertEquals("当日驾驶",dailyBriefMetric(JSONObject().put("title","今日驾驶"),false).label);assertEquals("km",dailyBriefMetric(JSONObject().put("title","今日里程").put("value",12),true).unit)}
}
