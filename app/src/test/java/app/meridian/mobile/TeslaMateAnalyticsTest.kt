package app.meridian.mobile

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class TeslaMateAnalyticsTest {
    private fun trip(id:Long,km:Double,energy:Double?=null,province:String="上海市",city:String="上海市",district:String="嘉定区")=JSONObject()
        .put("id",id).put("tripId",id).put("tripStartTime","2026-10-31T23:55:00+08:00").put("tripEndTime","2026-11-01T00:05:00+08:00")
        .put("mileageKm",km).put("durationMinutes",10).put("maxSpeed",80).put("startLocation","甲").put("endLocation","乙")
        .put("startProvince",province).put("endProvince",province).put("startCity",city).put("endCity",city).put("startDistrict",district).put("endDistrict",district)
        .apply{if(energy!=null)put("totalPowerConsumption",energy).put("tripEnergyConsumption",energy/km*100)}
    private fun charge(id:Long)=JSONObject().put("id",id).put("recordId",id).put("startTime","2026-10-31T23:55:00+08:00").put("endTime","2026-11-01T00:20:00+08:00").put("chargeEnergy",10).put("durationMinutes",25)
    @Test fun energyAverageUsesPairedDistanceNotAverageOfTripRates(){assertEquals(200.0,tmAverageEnergy(listOf(trip(1,10.0,1.0),trip(2,90.0,19.0),trip(3,500.0)))!!,.00001)}
    @Test fun missingEnergyDoesNotBecomeZero(){assertNull(tmKnownSum(listOf(trip(1,10.0)),"totalPowerConsumption"));assertNull(tmAverageEnergy(listOf(trip(1,10.0))))}
    @Test fun dailyTrendKeepsCrossMonthTripsOnDepartureDay(){val r=tmTrend(listOf(trip(1,10.0,1.0)),emptyList(),TmPeriod(LocalDate.of(2026,10,31),LocalDate.of(2026,11,1))).getJSONObject("drivingTrend").getJSONArray("mileageStats");assertEquals(10000.0,r.getJSONObject(0).getDouble("mileage"),0.0);assertEquals(0.0,r.getJSONObject(1).getDouble("mileage"),0.0)}
    @Test fun monthlyReportKeepsUnitsAndDoesNotInventAssistance(){val p=tmPeriod(JSONObject().put("month","202610"));val r=tmMonthly(listOf(trip(1,10.0,1.0)),listOf(charge(2)),p);assertEquals(10.0,r.getDouble("monthMileage"),0.0);assertEquals(10.0,r.getDouble("avgEnergyConsumption"),0.0);assertFalse(r.has("hnoaMileage"));assertEquals(31,r.getJSONArray("dayMileages").length())}
    @Test fun invalidPeriodIsRejected(){assertThrows(IllegalArgumentException::class.java){TmPeriod(LocalDate.of(2026,11,2),LocalDate.of(2026,11,1))}}
    @Test fun zeroCostAndMissingCostRemainDifferent(){assertNull(tmCost(charge(1)));val row=charge(2).put("costRecorded","0.00");assertEquals("原始记录",tmCost(row)!!.second);assertEquals(0,tmCost(row)!!.first.signum())}
    @Test fun originalAndManualAmountsAreBothPreserved(){val row=tmEnrich(charge(1),JSONObject().put("cost_recorded",JSONObject.NULL).put("cost_manual","1.10").put("manual_rate","0.80").put("energy_used_raw","1.2"),true,true);assertFalse(row.has("costRecorded"));assertEquals("1.10",row.getString("costManual"));assertEquals("人工估算",tmCost(row)!!.second);assertEquals(1.2,row.getDouble("supplyEnergy"),0.0)}
    @Test fun recordedAmountWinsWithoutErasingTimeOfUseEstimate(){val row=charge(1).put("costRecorded","12.50").put("costTou","10.25");assertEquals("原始记录",tmCost(row)!!.second);val result=tmMergeLedger(emptyList(),listOf(row),emptySet()).single();assertEquals(1250,result.getLong("cents"));assertEquals("12.50",result.getString("costRecorded"))}
    @Test fun ledgerImportIsIdempotentAndProtectsManualEdits(){val source=charge(1).put("costRecorded","2.30");val once=tmMergeLedger(emptyList(),listOf(source),emptySet());assertEquals(1,tmMergeLedger(once,listOf(source),emptySet()).size);val edited=JSONObject(once.single().toString()).put("source","manual").put("cents",500);assertEquals(500,tmMergeLedger(listOf(edited),listOf(source),emptySet()).single().getLong("cents"))}
    @Test fun ignoredAndUnknownCostsAreNeverImported(){assertTrue(tmMergeLedger(emptyList(),listOf(charge(1)),emptySet()).isEmpty());assertTrue(tmMergeLedger(emptyList(),listOf(charge(1).put("costRecorded","2")),setOf("auto-charge:1")).isEmpty())}
    @Test fun negativeAdjustmentIsPreserved(){assertEquals(-125,tmMergeLedger(emptyList(),listOf(charge(1).put("costRecorded","-1.25")),emptySet()).single().getLong("cents"))}
    @Test fun regionFiltersMustMatchTheSameEndpoint(){val r=trip(1,10.0).put("endProvince","江苏省").put("endCity","苏州市");assertFalse(tmFilterRegion(r,JSONObject().put("provinceName","上海市").put("cityName","苏州市")));assertTrue(tmFilterRegion(r,JSONObject().put("provinceName","江苏省").put("cityName","苏州市")))}
    @Test fun sameTripVisitsARegionOnceAndKeepsParents(){val r=tmFootprint(listOf(trip(1,10.0),trip(2,20.0)),JSONObject().put("level","PROVINCE"));assertEquals(2,r.getJSONArray("regions").getJSONObject(0).getInt("visitTripCount"));assertEquals("上海市",r.getJSONArray("regions").getJSONObject(0).getString("parentName"))}
    @Test fun sameDistrictNamesInDifferentCitiesDoNotCollapse(){val r=tmFootprint(listOf(trip(1,10.0,district="新区"),trip(2,20.0,province="江苏省",city="苏州市",district="新区")),JSONObject());assertEquals(2,r.getJSONObject("summary").getInt("districtCount"))}
    @Test fun provincialDrillDoesNotIncludeTheOtherEndpointCity(){val r=trip(1,10.0).put("endProvince","江苏省").put("endCity","苏州市");val cities=tmFootprint(listOf(r),JSONObject().put("level","PROVINCE").put("provinceName","上海市")).getJSONArray("regions");assertEquals(1,cities.length());assertEquals("上海市",cities.getJSONObject(0).getString("name"))}
    @Test fun validNumericStringsDoNotUseLooseTextExtraction(){assertEquals(1.25,tmDbNumber(JSONObject().put("v","1.25"),"v")!!,0.0);assertNull(tmDbNumber(JSONObject().put("v","金额1.25"),"v"))}
    @Test fun manualOverrideWinsAndRecordedZeroWinsOverTimeOfUse(){
        val row=charge(1).put("costRecorded","0").put("costTou","10")
        assertEquals(0,tmCost(row)!!.first.signum())
        row.put("costManual","3.25");assertEquals("3.25",tmCost(row)!!.first.toPlainString())
    }
    @Test fun monthlyUnknownEnergyIsNotPresentedAsACompleteTotal(){
        val p=tmPeriod(JSONObject().put("month","202610"));val data=tmMonthly(listOf(trip(1,10.0,1.0),trip(2,20.0)),emptyList(),p)
        assertFalse(data.has("totalEnergyConsumption"));assertEquals(1.0,data.getDouble("knownEnergyConsumption"),0.0)
        assertFalse(data.getJSONObject("periodComparison").has("currentTotalEnergy"))
        assertTrue(monthlyShareValues(data,false).has("energyCoverage"))
    }
    @Test fun municipalityUsesProvinceAsCityAndRetainsDistrict(){
        val raw=trip(1,10.0)
        val extra=JSONObject().put("start_province","上海市").put("start_city","浦东新区").put("start_district","")
        val result=tmEnrich(raw,extra,false,true)
        assertEquals("上海市",result.getString("startCity"));assertEquals("浦东新区",result.getString("startDistrict"))
        assertTrue(tmFilterRegion(result,JSONObject().put("provinceName","上海市").put("cityName","上海市")))
    }
    @Test fun unavailableProvenanceDoesNotDeleteImportedLedgerHistory(){
        val known=charge(1).put("costRecorded","12")
        val old=tmMergeLedger(emptyList(),listOf(known),emptySet())
        assertEquals(1200,tmMergeLedger(old,listOf(charge(1)),emptySet()).single().getLong("cents"))
        assertTrue(tmMergeLedger(old,listOf(charge(1).put("_costProvenance",true)),emptySet()).isEmpty())
    }
    @Test fun historicalOverrideIsNotLabeledAsManual(){
        val row=tmEnrich(charge(1),JSONObject().put("cost_manual",JSONObject.NULL).put("override_source","default").put("override_original_cost","15.0").put("cost_effective","8.5").put("cost_source","默认估算").put("energy_used_raw","10"),true,true)
        assertFalse(row.has("costManual"));assertEquals("default",row.getString("overrideSource"));assertEquals("默认估算",tmCost(row)!!.second)
    }
    @Test fun defaultZeroEstimateWithoutEnergyIsNotAConfirmedZeroCost(){
        val row=tmEnrich(charge(1).put("chargeEnergy",0),JSONObject().put("cost_effective","0").put("cost_source","默认估算").put("energy_used_raw","0"),true,true)
        assertNull(tmCost(row))
    }
    @Test fun defaultEstimateUsesPositiveAddedEnergyWhenSupplyIsZero(){
        val row=charge(1).put("_serverEffective",true).put("supplyEnergy",0).put("costEffective","8.0").put("effectiveCostSource","默认估算")
        assertEquals("8.0",tmCost(row)!!.first.toPlainString())
        assertEquals(800,tmMergeLedger(emptyList(),listOf(row),emptySet()).single().getLong("cents"))
    }

}
