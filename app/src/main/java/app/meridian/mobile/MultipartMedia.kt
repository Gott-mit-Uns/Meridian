package app.meridian.mobile

import java.io.InputStream

internal fun InputStream.readCapped(limit:Int):ByteArray {
 val output=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
 while(true){val count=read(buffer);if(count<0)break;require(output.size()+count<=limit){"文件超过允许大小"};output.write(buffer,0,count)}
 return output.toByteArray()
}

/** Binary-safe byte mapping: never decode image payload as UTF-8. */
internal fun parseMultipartImages(contentType:String,bytes:ByteArray):Map<String,ByteArray>{
 val boundary=Regex("boundary\\s*=\\s*(?:\"([^\"]+)\"|([^;\\s]+))",RegexOption.IGNORE_CASE).find(contentType)?.let{it.groupValues[1].ifBlank{it.groupValues[2]}}?:throw IllegalArgumentException("图片响应缺少分隔符")
 require(boundary.length in 1..200)
 val raw=bytes.toString(Charsets.ISO_8859_1);val marker="--$boundary";val result=linkedMapOf<String,ByteArray>()
 fun nextMarker(from:Int):Int{var found=raw.indexOf("\r\n$marker",from);while(found>=0){val tail=found+2+marker.length;if(raw.startsWith("\r\n",tail)||raw.startsWith("--",tail))return found;found=raw.indexOf("\r\n$marker",found+2)};return -1}
 var cursor=if(raw.startsWith(marker))0 else nextMarker(0).let{if(it<0)-1 else it+2}
 while(cursor>=0){
  val start=cursor+marker.length;if(raw.startsWith("--",start))break
  val headerEnd=raw.indexOf("\r\n\r\n",start);if(headerEnd<0)break
  val next=nextMarker(headerEnd+4);require(next>=0){"图片响应不完整"}
  val headers=raw.substring(start,headerEnd)
  val name=Regex("(?:^|[;\\s])name\\s*=\\s*\"([^\"]+)\"",RegexOption.IGNORE_CASE).find(headers)?.groupValues?.get(1)
  if(name!=null&&Regex("media-\\d+").matches(name))result[name]=bytes.copyOfRange(headerEnd+4,next)
  cursor=next+2
 }
 return result
}
