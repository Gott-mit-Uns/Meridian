package app.meridian.mobile

import org.json.JSONObject

data class SharePrivacy(val hideAddresses:Boolean=true,val hideRoute:Boolean=true,val hideTime:Boolean=true){val protected get()=hideAddresses||hideRoute||hideTime}
data class ShareSnapshot(val data:JSONObject,val insight:JSONObject?)

/** Prepare a separate export snapshot; never alter vehicle data or mask by drawing over text. */
fun prepareShareSnapshot(original:JSONObject,insight:JSONObject?,privacy:SharePrivacy):ShareSnapshot{
 val data=JSONObject(original.toString())
 val allowed=setOf("tripStartTime","tripEndTime","startLocation","endLocation","startAddress","endAddress","startLocationName","endLocationName","track","locationTrack","points","tripStartLatitude","tripStartLongitude","tripEndLatitude","tripEndLongitude","mileageKm","drivingMileage","durationMinutes","drivingDuration","avgSpeed","maxSpeed","tripEnergyConsumption","totalPowerConsumption","startRemainingRange","endRemainingRange","consumptionAchievementRate")
 data.keys().asSequence().toList().filterNot{it in allowed}.forEach{data.remove(it)}
 if(privacy.hideAddresses){for(key in listOf("startLocation","startAddress","startLocationName"))data.put(key,"起点地址已隐藏");for(key in listOf("endLocation","endAddress","endLocationName"))data.put(key,"终点地址已隐藏")}
 if(privacy.hideRoute){for(key in listOf("track","locationTrack","points","tripStartLatitude","tripStartLongitude","tripEndLatitude","tripEndLongitude"))data.remove(key)}
 if(privacy.hideTime){data.put("tripStartTime",data.safeText("tripStartTime").take(10));data.put("tripEndTime","");data.remove("startTime");data.remove("endTime")}
 // Free-form analysis can repeat addresses, coordinates and times; omit it in protected exports.
 return ShareSnapshot(data,if(privacy.protected)null else insight?.let{JSONObject(it.toString())})
}
