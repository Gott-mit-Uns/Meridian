package app.meridian.mobile

import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Opt-in contract check; private responses remain in memory and are never printed. */
class TeslaMateLiveTest {
    @Test fun configuredReadOnlyServiceSupportsTheCoreScreens() = runBlocking {
        val address = System.getenv("TESLAMATE_INTEGRATION_BASE")
        assumeTrue("No live service configured", !address.isNullOrBlank())
        val client = TeslaMateClient(connectionAddress(address!!, VehicleBackend.TESLAMATE))
        val cars = client.call("/car/get", null, "").getJSONArray("data")
        assertTrue(cars.length() > 0)
        val id = cars.getJSONObject(0).getLong("id").toString()
        val query = JSONObject().put("carId", id)
        val home = client.call("/dashboard/get", query, id).getJSONObject("data")
        assertTrue(home.getBoolean("_teslamate")); assertTrue(home.has("overview"))
        val drives = client.call("/car/trip/get", query, id).getJSONObject("data")
        assertTrue(drives.getInt("total") > 0)
        val trip = drives.getJSONArray("list").getJSONObject(0)
        val detail = client.call("/car/trip/getDetail", JSONObject().put("tripId", trip.getLong("id")), id).getJSONObject("data")
        assertEquals(trip.getLong("id"), detail.getLong("id"))
        val points = detail.getJSONObject("track").getJSONArray("locationTrack")
        assertTrue(points.length() in 2..1201)
        val charges = client.call("/car/charge/history", query, id).getJSONObject("data")
        assertTrue(charges.getInt("total") > 0)
        val charge = charges.getJSONArray("list").getJSONObject(0)
        val charging = client.call("/car/charge/detail", JSONObject().put("recordId", charge.getLong("id")), id).getJSONObject("data")
        assertEquals(charge.getLong("id"), charging.getLong("id"))
        assertTrue(charging.getJSONArray("samples").length() > 0)
        assertFalse(charging.has("cost"))
    }
    @Test fun configuredEnhancedServiceSupportsReportsFootprintsAndHistory() = runBlocking {
        val address=System.getenv("TESLAMATE_INTEGRATION_BASE");assumeTrue(!address.isNullOrBlank())
        val client=TeslaMateClient(connectionAddress(address!!,VehicleBackend.TESLAMATE))
        val id=client.call("/car/get",null,"").getJSONArray("data").getJSONObject(0).getLong("id").toString()
        val today=java.time.LocalDate.now(teslaMateZone)
        val query=JSONObject().put("carId",id).put("startDate",today.minusDays(29).toString()).put("endDate",today.toString())
        val stats=client.call("/statistics/energy-overview",query,id).getJSONObject("data")
        assertTrue(stats.getJSONObject("periodDashboard").getJSONArray("metricGroups").length()>=4)
        val monthly=client.call("/trip-month-stats/get",JSONObject().put("carId",id).put("month",today.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"))),id).getJSONObject("data")
        assertTrue(monthly.has("monthMileage"));assertTrue(monthly.getJSONArray("dayMileages").length()>=28)
        val footprint=client.call("/car/trip/cityFootprint/get",query,id).getJSONObject("data")
        assertTrue(footprint.getJSONObject("summary").getInt("cityCount")>0)
        val tracks=client.call("/car/trip/trackMap",query,id).getJSONObject("data")
        assertTrue(tracks.getJSONArray("tracks").length() in 1..100)
        val historical=client.call("/car/status/health",query,id).getJSONObject("data")
        assertTrue(historical.getJSONArray("metricTrail").length()>0)
        val current=client.call("/car/status/current",query,id).getJSONObject("data")
        assertTrue(current.has("status"));assertTrue(current.has("batteryHealth"))
        val charge=client.call("/car/charge/history",query,id).getJSONObject("data").getJSONArray("list").getJSONObject(0)
        assertTrue(charge.getBoolean("_costProvenance"))
        assertTrue(client.call("/car/charge/current",query,id).getJSONObject("data").has("active"))
    }

}
