package app.meridian.mobile

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import kotlinx.coroutines.runBlocking

class TeslaMateAdapterTest {
    private val km = TeslaMateUnits("km", "C")
    private fun drive(id: Int = 1, start: String = "2026-10-31T15:55:00Z", end: String = "2026-10-31T16:05:00Z") =
        JSONObject().put("drive_id", id).put("start_date", start).put("end_date", end)
            .put("start_address", "起点").put("end_address", "终点")
            .put("odometer_details", JSONObject().put("odometer_distance", 10.0))
            .put("duration_min", 10).put("speed_avg", 60).put("speed_max", 80)
            .put("energy_consumed_net", 1.5).put("consumption_net", 150.0)
    private fun settings() = JSONObject("""{"data":{"settings":{"teslamate_units":{"unit_of_length":"km","unit_of_temperature":"C"},"teslamate_webgui":{"preferred_range":"rated"}}}}""")
    private fun response(kind: String, rows: JSONArray, car: Int = 1) = JSONObject().put("data", JSONObject().put("car", JSONObject().put("car_id", car)).put(kind, rows))

    @Test fun mapsEnergyAndShanghaiCrossMidnightWithoutLosingUnits() {
        val mapped = tmDrive(drive(), km)
        assertEquals(15.0, mapped.getDouble("tripEnergyConsumption"), 0.00001)
        assertEquals(1.5, mapped.getDouble("totalPowerConsumption"), 0.00001)
        assertEquals("2026-10-31T23:55:00+08:00", mapped.getString("tripStartTime"))
        assertEquals("2026-11-01T00:05:00+08:00", mapped.getString("tripEndTime"))
        val october = tmRecordPage(listOf(mapped), false, JSONObject().put("startDate", "2026-10-01").put("endDate", "2026-10-31"))
        val november = tmRecordPage(listOf(mapped), false, JSONObject().put("startDate", "2026-11-01"))
        assertEquals(1, october.getInt("total")); assertEquals(0, november.getInt("total"))
    }
    @Test fun missingEnergyStaysMissing() {
        val row = drive().put("energy_consumed_net", JSONObject.NULL).put("consumption_net", JSONObject.NULL)
        val mapped = tmDrive(row, km)
        assertFalse(mapped.has("tripEnergyConsumption")); assertFalse(mapped.has("totalPowerConsumption"))
    }
    @Test fun imperialDistanceAndConsumptionConvertTogether() {
        val units = TeslaMateUnits("mi", "F")
        val mapped = tmDrive(drive(), units)
        assertEquals(16.09344, mapped.getDouble("mileageKm"), 0.000001)
        assertEquals(15 / 1.609344, mapped.getDouble("tripEnergyConsumption"), 0.000001)
        assertEquals(20.0, units.celsius(68.0)!!, 0.000001)
    }
    @Test fun chargeDoesNotInventCostFromAnUpstreamZero() {
        val row = JSONObject().put("charge_id", 3).put("start_date", "2026-11-01T00:00:00Z")
            .put("end_date", "2026-11-01T01:00:00Z").put("cost", 0).put("charge_energy_added", 12)
        val mapped = tmCharge(row, km)
        assertEquals(12.0, mapped.getDouble("chargeEnergy"), 0.0)
        assertFalse(mapped.has("cost")); assertFalse(mapped.has("startSoc"))
    }
    @Test fun recordSortingIsStableAndPagingHappensAfterSearch() {
        val rows = (1..41).map { tmDrive(drive(it).put("start_address", if (it % 2 == 0) "公司" else "家"), km) }
        val page = tmRecordPage(rows, false, JSONObject().put("keyword", "公司").put("sortOrder", "asc").put("pageSize", 5).put("pageNum", 2))
        assertEquals(20, page.getInt("total")); assertEquals(12, page.getJSONArray("list").getJSONObject(0).getInt("id"))
        assertEquals(200000.0, page.getJSONObject("summary").getDouble("totalMileage"), 0.0001)
    }
    @Test fun mapCorrectionHasKnownReferenceAndCanBeDisabled() {
        val (lat, lon) = tmMapCoordinate(39.916527, 116.397128, true)
        assertEquals(39.91793075, lat, 0.000001); assertEquals(116.40337249, lon, 0.000001)
        assertEquals(39.916527 to 116.397128, tmMapCoordinate(39.916527, 116.397128, false))
        assertEquals(51.5 to -0.1, tmMapCoordinate(51.5, -0.1, true))
    }
    @Test fun tracksAreTimeSortedAndBoundedWithEndpointsKept() {
        val points = JSONArray()
        for (i in 0..4999) points.put(JSONObject().put("detail_id", i + 1).put("date", if(i==0)"2026-11-01T00:01:00Z" else "2026-11-01T00:00:00Z")
            .put("latitude", 31.0 + i * .00001).put("longitude", 121.0).put("speed", 20))
        val result = tmDrive(drive().put("drive_details", points), km, false).getJSONObject("track").getJSONArray("locationTrack")
        assertTrue(result.length() <= 1201)
        assertEquals(31.00001, result.getJSONObject(0).getDouble("latitude"), 0.0000001)
        assertEquals(31.0, result.getJSONObject(result.length()-1).getDouble("latitude"), 0.0000001)
    }
    @Test fun onlyExplicitLocalHttpIsAccepted() {
        assertEquals("http://192.168.1.2:8081", connectionAddress("http://192.168.1.2:8081/api/v1/", VehicleBackend.TESLAMATE))
        for (input in listOf("http://example.com", "https://user:pass@example.com", "https://example.com?token=x", "http://192.bad.168.1.1")) {
            assertThrows(IllegalArgumentException::class.java) { connectionAddress(input, VehicleBackend.TESLAMATE) }
        }
        assertThrows(IllegalArgumentException::class.java) { connectionAddress("http://192.168.1.2", VehicleBackend.XIAOMI) }
    }
    @Test fun snapshotReadsTerminalPageAndReusesVerifiedResult() = runBlocking {
        val paths = mutableListOf<String>()
        val client = TeslaMateClient("https://test.invalid", read = { path ->
            paths.add(path)
            when { path=="globalsettings" -> settings()
                "page=1&" in path -> response("drives", JSONArray().put(drive()))
                "page=2&" in path -> response("drives", JSONArray())
                else -> error("unexpected request") }
        })
        repeat(2) { assertEquals(1, client.call("/car/trip/get", JSONObject().put("carId", 1), "1").getJSONObject("data").getInt("total")) }
        assertEquals(1, paths.count { "page=2&" in it })
    }
    @Test fun repeatedIdsNeverProduceAPartialTotal() = runBlocking {
        val client = TeslaMateClient("https://test.invalid", read = { path -> if(path=="globalsettings")settings() else response("drives", JSONArray().put(drive())) })
        var rejected = false
        try { client.call("/car/trip/get", JSONObject().put("carId", 1), "1") } catch (_: IllegalArgumentException) { rejected = true }
        assertTrue(rejected)
    }
    @Test fun wrongVehicleEnvelopeIsRejected() = runBlocking {
        val client = TeslaMateClient("https://test.invalid", read = { path -> if(path=="globalsettings")settings() else response("drives", JSONArray(), 2) })
        var rejected = false
        try { client.call("/car/trip/get", JSONObject().put("carId", 1), "1") } catch (_: IllegalArgumentException) { rejected = true }
        assertTrue(rejected)
    }
    @Test fun dateFilterIsPassedToTheServiceAndDoesNotShareAnAllHistoryCache() = runBlocking {
        val paths = mutableListOf<String>()
        val client = TeslaMateClient("https://test.invalid",read={ path ->
            paths.add(path)
            if(path=="globalsettings")settings() else response("drives",JSONArray())
        })
        client.call("/car/trip/get",JSONObject().put("carId",1).put("startDate","2026-11-01"),"1")
        client.call("/car/trip/get",JSONObject().put("carId",1),"1")
        assertEquals(2,paths.count{it.startsWith("cars/")})
        assertTrue(paths.any{it.contains("startDate=2026-10-31T16%3A00%3A00Z")})
    }
    @Test fun aggregateDoesNotReplaceMissingMeasurementsWithZero() {
        val row=tmDrive(drive().apply{remove("odometer_details")},km)
        val page=tmRecordPage(listOf(row),false,JSONObject())
        assertFalse(page.getJSONObject("summary").has("totalMileage"))
        assertEquals(0.0,tmSum(emptyList(),"mileageKm")!!,0.0)
    }

    @Test fun coordinatePreferencePartitionsReadsWithoutHidingLocalNotes() {
        val api=Api().apply{base="https://test.invalid";accountScope="teslamate:test";useTeslaMate("","",true)}
        val notes=api.localScope(1);val correctedReads=api.cacheScope
        api.useTeslaMate("","",false)
        assertEquals(notes,api.localScope(1))
        assertNotEquals(correctedReads,api.cacheScope)
    }

    @Test fun browsingManyDatesDoesNotKeepEveryHistoricalSnapshot() = runBlocking {
        var requests=0
        val client=TeslaMateClient("https://test.invalid",read={ path ->
            if(path=="globalsettings")settings() else {requests++;response("drives",JSONArray())}
        })
        val first=java.time.LocalDate.of(2026,1,1)
        for(day in 0..30)client.call("/car/trip/get",JSONObject().put("carId",1).put("startDate",first.plusDays(day.toLong()).toString()),"1")
        val before=requests
        client.call("/car/trip/get",JSONObject().put("carId",1).put("startDate",first.toString()),"1")
        assertEquals(before+1,requests)
    }

    @Test fun responseUnitsWinOverSettingsAndCachedRowsAreNotConvertedTwice() = runBlocking {
        val client=TeslaMateClient("https://test.invalid",read={path->
            when {
                path=="globalsettings" -> settings()
                "page=2&" in path -> response("drives",JSONArray())
                else -> response("drives",JSONArray().put(drive())).apply{
                    getJSONObject("data").put("units",JSONObject().put("unit_of_length","mi").put("unit_of_temperature","F"))
                }
            }
        })
        repeat(2){
            val page=client.call("/car/trip/get",JSONObject().put("carId",1),"1").getJSONObject("data")
            assertEquals(16.09344,page.getJSONArray("list").getJSONObject(0).getDouble("mileageKm"),0.000001)
        }
        assertEquals(20.0,tmResponseUnits(JSONObject().put("units",JSONObject().put("unit_of_length","mi").put("unit_of_temperature","F")),km).celsius(68.0)!!,0.000001)
    }

}
