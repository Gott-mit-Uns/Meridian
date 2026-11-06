package app.meridian.mobile

import org.json.JSONObject

internal fun syncJobId(data:Any?):String{
 val candidate=if(data is JSONObject)data.opt("jobId")else data
 if(candidate==null||candidate==JSONObject.NULL)return ""
 if(candidate is Number){val value=candidate.toDouble();return if(value.isFinite()&&value>0&&value==kotlin.math.floor(value)&&candidate.toLong()>0)candidate.toLong().toString()else ""}
 if(candidate !is String)return ""
 val id=candidate.toString().trim()
 if(id.isBlank()||id=="null"||id.toLongOrNull()?.let{it<=0}==true)return ""
 return id
}
