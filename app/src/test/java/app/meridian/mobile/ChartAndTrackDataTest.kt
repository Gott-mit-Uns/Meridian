package app.meridian.mobile

import org.json.*
import org.junit.Assert.*
import org.junit.Test

class ChartAndTrackDataTest {
 @Test fun trendKeepsZerosAndConvertsMetersToKilometers(){
  val rows=JSONArray("""[{"date":"2026-11-01","mileage":0},{"date":"2026-11-02","mileage":12345}]""")
  val points=chartSamples(rows,"mileage",.001)
  assertEquals(2,points.size);assertEquals(0.0,points[0].value,0.0);assertEquals(12.345,points[1].value,.0001)
 }
 @Test fun missingOrInvalidValuesAreNotRenderedAsZero(){
  val rows=JSONArray("""[{"date":"a","value":null},{"date":"b","value":"—"},{"date":"c","value":8}]""")
  val points=chartSamples(rows,"value");assertEquals(1,points.size);assertEquals("c",points.single().label)
 }
 @Test fun posterTrackAcceptsServerJsonStringAndRejectsInvalidCoordinates(){
  val rows=JSONArray("""[{"latitude":30.2,"longitude":120.1},{"latitude":999,"longitude":120},{"latitude":30.3,"longitude":120.2}]""")
  val data=JSONObject().put("track",JSONObject().put("locationTrack",rows.toString()))
  val points=trackPositions(data);assertEquals(2,points.size);assertEquals(30.3,points.last().latitude,.0001)
 }
 @Test fun noCoordinatesMeansNoInventedShareRoute(){
  assertTrue(trackPositions(JSONObject()).isEmpty());assertTrue(endpointPositions(JSONObject()).isEmpty())
 }
}
