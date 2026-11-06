package app.meridian.mobile.glass

/** No texture or index buffers: coordinates are already expanded into triangles. */
internal data class GlassBevelBuffers(val coordinates:FloatArray,val colors:IntArray)
internal fun buildGlassBevelBuffers(points:List<Pair<Float,Float>>,colors:List<Int>):GlassBevelBuffers {
    require(points.size==colors.size && points.size%3==0){"Invalid glass triangle buffers"}
    require(points.all{it.first.isFinite() && it.second.isFinite()}){"Non-finite glass geometry"}
    return GlassBevelBuffers(FloatArray(points.size*2){i->if(i%2==0)points[i/2].first else points[i/2].second},colors.toIntArray())
}
