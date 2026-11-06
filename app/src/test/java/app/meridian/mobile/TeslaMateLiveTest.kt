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
}
