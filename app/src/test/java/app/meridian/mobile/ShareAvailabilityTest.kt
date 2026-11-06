package app.meridian.mobile
import org.json.*
import org.junit.Assert.*
import org.junit.Test
class ShareAvailabilityTest{
 @Test fun missingValuesAreOmittedButMeasuredZeroSurvives(){listOf("","—","null","NaN","暂无数据","— km").forEach{assertFalse(shareMetricAvailable(HomeMetricValue("里程",it,"km")))};assertTrue(shareMetricAvailable(HomeMetricValue("里程","0","km")));assertTrue(shareMetricAvailable(HomeMetricValue("驾驶","1时30分","")))}
 @Test fun absentChargingHistoryOmitsEntireZeroBlock(){val items=JSONArray("""[{"label":"充电次数","value":0},{"label":"累计充电量","value":"0.0 kWh"},{"label":"充电时长","value":"0分钟"}]""");assertTrue(emptyChargeGroup(StatisticsMetricGroup("充电",items,"charge","lifetime")));assertFalse(emptyChargeGroup(StatisticsMetricGroup("行程",items,"trip","lifetime")))}
 @Test fun inconsistentZeroCountDoesNotDiscardKnownPositiveEnergy(){val items=JSONArray("""[{"label":"充电次数","value":0},{"label":"累计充电量","value":12}]""");assertFalse(emptyChargeGroup(StatisticsMetricGroup("充电",items,"charge")))}
}
