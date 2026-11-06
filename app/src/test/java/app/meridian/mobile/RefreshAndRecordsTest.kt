package app.meridian.mobile
import org.junit.Assert.*
import org.junit.Test
import org.json.*
import java.time.LocalDate
class RefreshAndRecordsTest{
 @Test fun pullFollowsFingerWithIncreasingResistance(){val values=(0..400).map{elasticPullOffset(it.toFloat(),112f)};assertEquals(0f,values.first(),0f);assertTrue(values.zipWithNext().all{it.first<=it.second});assertTrue(values.last()<112f);assertTrue(values[100]-values[50]>values[350]-values[300]);assertEquals(0f,elasticPullOffset(-10f,112f),0f)}
 @Test fun widgetAlwaysHasFourDistinctValidSlots(){assertEquals(4,normalizedWidgetFields(emptyList()).size);val fields=replaceWidgetField(listOf("battery","range","today","temperature"),0,"range");assertEquals(listOf("range","battery","today","temperature"),fields);assertEquals(4,fields.distinct().size);assertEquals(4,replaceWidgetField(fields,2,"recent30").size)}
 @Test fun recordsDoNotInterpretDashboardAsEmptyList(){assertNull(recordRows(JSONObject().put("data",JSONObject().put("overview",JSONObject()))));assertEquals(0,recordRows(JSONObject().put("data",JSONObject().put("list",JSONArray())))!!.length())}
 @Test fun chargingAndTripQueriesAreIsolated(){val date=LocalDate.of(2026,11,5);val trip=recordReadSpec(7,false,1,"desc","home","all","杭州",7,null,date);val charge=recordReadSpec(7,true,1,"desc","home","all","杭州",7,null,date);assertNotEquals(trip.path,charge.path);assertEquals("2026-10-30",trip.body.getString("startDate"));assertTrue(trip.body.has("keyword"));assertFalse(charge.body.has("keyword"));assertFalse(charge.body.has("cityName"))}
}
