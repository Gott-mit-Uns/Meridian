package app.meridian.mobile
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class TeslaMateCurrentChargeTest {
    private fun client(active:Boolean)=TeslaMateClient("https://test.invalid",read={path->
        when(path){
            "globalsettings"->JSONObject("""{"data":{"settings":{"teslamate_units":{"unit_of_length":"km","unit_of_temperature":"C"}}}}""")
            "cars/1/status"->JSONObject("""{"data":{"car":{"car_id":1},"status":{"state":"charging","charging_details":{"charging_state":"Charging"}},"units":{"unit_of_length":"km","unit_of_temperature":"C"}}}""")
            "cars/1/charges/current"->JSONObject("""{"data":{"car":{"car_id":1},"charge":{"charge_id":7,"is_charging":$active,"start_date":"2026-01-01T00:00:00Z","charge_energy_added":2.5,"battery_details":{"start_battery_level":20,"current_battery_level":30},"charge_details":[]},"units":{"unit_of_length":"km","unit_of_temperature":"C"}}}""")
            else->error("unexpected route")
        }
    })
    @Test fun databaseEndedChargeOverridesStaleChargingStatus()=runBlocking{
        val data=client(false).call("/car/charge/current",JSONObject().put("carId",1),"1").getJSONObject("data")
        assertFalse(data.getBoolean("active"));assertFalse(data.has("_ongoing"))
    }
    @Test fun onlyConfirmedCurrentChargeIsMarkedOngoing()=runBlocking{
        val data=client(true).call("/car/charge/current",JSONObject().put("carId",1),"1").getJSONObject("data")
        assertTrue(data.getBoolean("active"));assertTrue(data.getBoolean("_ongoing"));assertEquals(30.0,data.getDouble("endSoc"),0.0)
    }
}
