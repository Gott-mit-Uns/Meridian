// Adapted from user-owned 流影 source; optical implementation retained.
package app.meridian.mobile.glass

import android.os.Build
import android.graphics.RuntimeShader
import androidx.compose.animation.core.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import dev.chrisbanes.haze.*

/** Only the captured backdrop is refracted; controls are drawn above it. */
@Composable internal fun GlassChrome(state:HazeState, modifier:Modifier=Modifier,
    shape:Shape=RoundedCornerShape(30.dp), optical:Boolean=true, elevated:Boolean=true, showRim:Boolean=true,lens:Boolean=false,
    refractionStrength:Float=if(lens)1f else .8f,edgeWidthDp:Float=0f,cover:Float?=null,captureAll:Boolean=false,pressed:Boolean?=null,navigationFrame:Boolean=false,quietLens:Boolean=false,droplet:Boolean=false,pressureOverride:State<Float>?=null,content:@Composable BoxScope.()->Unit) {
    // First-entry Haze/geometry must settle before applying a refracting render effect.
    val pageVisible=rememberPageVisible()
    val captureDensity=androidx.compose.ui.platform.LocalDensity.current
    var captureReady by remember(state,captureDensity){mutableStateOf(false)}
    LaunchedEffect(state,pageVisible,captureDensity){if(pageVisible && !captureReady){withFrameNanos{};withFrameNanos{};captureReady=true}}
    val dark=LocalLiusuDark.current
    val options=LocalNavigationOptions.current
    val liquidBlur=LocalAppPreferences.current.liquidBlur
    val glass=options.glass&&!options.reduceTransparency
    val liquid=liquidGlassAvailable(if(options.liquid)"liquid" else "blur",glass,Build.VERSION.SDK_INT,LocalPlayerMenu.current)
    val motion=LocalAppMotion.current
    var touching by remember {mutableStateOf(false)}
    val touchPoint=remember {mutableStateOf(Offset.Zero)}
    val ownPressure=animateFloatAsState(if((pressed?:touching) && liquid)1f else 0f,
        if(motion.full)spring(dampingRatio=if(pressed?:touching)1f else .9f,stiffness=if(pressed?:touching)1000f else 300f) else snap(),label="glassPressure")
    val pressure=pressureOverride ?: ownPressure
    val observation=if(liquid && optical && pressed==null)Modifier.pointerInput(Unit){
        awaitPointerEventScope {
            var dragging=false
            try {
                while(true){
                    val event=awaitPointerEvent(PointerEventPass.Final)
                    val active=event.changes.firstOrNull{it.pressed}
                    if(active==null)dragging=false
                    else if(event.changes.any{it.isConsumed && it.position!=it.previousPosition})dragging=true
                    touching=active!=null && !dragging
                    if(active!=null && !dragging)touchPoint.value=active.position
                }
            } finally {touching=false}
        }
    } else Modifier
    // A clear lens transmits the scene. Opacity/blur preferences belong to frosted glass.
    val surfaceTint=if(navigationFrame)(if(dark)Color(0xFF181A1F)else Color.White).copy(alpha=if(dark).20f else .16f) else if(liquid) (if(dark)Color(0xFF111820) else Color.White).copy(alpha=if(dark).045f else .035f)
        else if(lens)Color.White.copy(alpha=if(dark).065f else .18f) else if(dark)Color(0xFF141C27).copy(alpha=maxOf(options.darkOpacity,.24f))
        else LiusuVisual.surface.copy(alpha=cover?.coerceIn(0f,1f)?:GlassSpec.chromeCover(options.opacity))
    Box(modifier.then(observation).shadow(if(!elevated)0.dp else if(navigationFrame)6.dp else if(liquid)6.dp else if(glass)GlassSpec.floatingElevationDp.dp else 4.dp,shape,
        ambientColor=Color.Black.copy(alpha=if(dark).18f else if(navigationFrame).075f else .13f),spotColor=Color.Black.copy(alpha=if(navigationFrame).10f else if(liquid).20f else .14f)).clip(shape)) {
        Box(Modifier.matchParentSize().glassOptics(glass&&options.optics&&optical&&captureReady,shape,refractionStrength,edgeWidthDp,pressureSource=pressure,touchSource=touchPoint,rimStrength=if(quietLens)0f else if(navigationFrame)0f else 1f,droplet=droplet).clip(shape)
            .then(if(glass)Modifier.hazeEffect(state,style=HazeStyle(backgroundColor=if(navigationFrame)Color.Transparent else if(captureAll || cover==null)LiusuVisual.canvas else Color.Transparent,
                tints=listOf(HazeTint(surfaceTint)),blurRadius=(if(liquid && droplet)liquidBlur*.45f else if(liquid)liquidBlur else if(lens)GlassSpec.backdropBlur(options.blur)*.65f else GlassSpec.backdropBlur(options.blur)).dp,noiseFactor=if(liquid)0f else GlassSpec.chromeNoise,
                fallbackTint=HazeTint(if(dark)Color(0xFF242426).copy(alpha=(if(cover!=null).16f else GlassSpec.fallbackAlpha)) else Color.White.copy(alpha=(if(cover!=null).16f else GlassSpec.fallbackAlpha))))) {blurEnabled=Build.VERSION.SDK_INT>=31;if(liquid || captureAll){inputScale=HazeInputScale.Fixed(.75f)}}
                else Modifier.background(LiusuVisual.surface)))
        if(glass&&!dark&&!liquid) Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(
            Color.White.copy(alpha=if(cover!=null).025f else GlassSpec.chromeSheenDay),Color.Transparent,Color.Black.copy(alpha=.008f)))))

        // No full-plate white gradient: reflections belong to the bevel, not the clear centre.
        CompositionLocalProvider(LocalInsideGlassChrome provides true) { content() }
        if(showRim&&glass)Box(Modifier.matchParentSize().then(if(navigationFrame)Modifier.navigationGlassBevel(dark).subtleGlassHighlight(shape,dark).glassBoundaryEdge(shape,dark) else Modifier.glassSurfaceEdge(shape,dark,if(liquid).88f else .65f,pressureSource=if(liquid)pressure else null)))
    }
}

@Composable internal fun Modifier.glassOptics(enabled:Boolean,shape:Shape,strength:Float=1f,edgeWidthDp:Float=0f,pressure:Float=0f,touchPoint:Offset=Offset.Zero,rimStrength:Float=1f,pressureSource:State<Float>?=null,touchSource:State<Offset>?=null,droplet:Boolean=false,pressedOnly:Boolean=false):Modifier {
    if(!enabled||!LocalNavigationOptions.current.liquid||LocalPlayerMenu.current||Build.VERSION.SDK_INT<33)return this
    if(LocalAppPreferences.current.liquidRefraction<=0f)return this
    return api33GlassOptics(shape,strength,edgeWidthDp,pressure,touchPoint,LocalAppPreferences.current.liquidRefraction,rimStrength*normalizedGlassHighlight(LocalAppPreferences.current.glassHighlight),pressureSource ?: rememberUpdatedState(pressure),touchSource ?: rememberUpdatedState(touchPoint),droplet,pressedOnly)
}

/** Directional reflected rim and a narrow inner shadow, never a solid outline. */
@Composable
internal fun Modifier.glassSurfaceEdge(shape:Shape,dark:Boolean,amount:Float=1f,pressureSource:State<Float>?=null):Modifier {
    val gain=normalizedGlassHighlight(LocalAppPreferences.current.glassHighlight)
    return drawWithCache {
    val outline=shape.createOutline(size,layoutDirection,this)
    fun reflectedLight(day:Float,night:Float)=Color.White.copy(alpha=((if(dark)night else day)*gain).coerceIn(0f,1f))
    val reflected=Brush.linearGradient(0f to reflectedLight(.96f,.66f),.32f to reflectedLight(.26f,.19f),
        .58f to reflectedLight(.13f,.10f),.82f to reflectedLight(.44f,.28f),1f to reflectedLight(.64f,.40f))
    val inner=Brush.verticalGradient(listOf(Color.White.copy(alpha=((if(dark).08f else .24f)*gain).coerceIn(0f,1f)),
        Color.Transparent,Color.Black.copy(alpha=if(dark).10f else .09f)))
    onDrawBehind {
        val opacity=amount+(pressureSource?.value?.coerceIn(0f,1f)?.times(.12f) ?: 0f)
        drawOutline(outline,inner,alpha=opacity*.65f,style=Stroke(1.4.dp.toPx()))
        drawOutline(outline,reflected,alpha=opacity,style=Stroke(1.1.dp.toPx()))
    }
}.glassBoundaryEdge(shape,dark)
}

@RequiresApi(33)
@Composable private fun Modifier.api33GlassOptics(shape:Shape,strength:Float,edgeWidthDp:Float,pressure:Float,touchPoint:Offset,refraction:Float,rimStrength:Float,pressureSource:State<Float>,touchSource:State<Offset>,droplet:Boolean,pressedOnly:Boolean):Modifier {
    val shader=remember {runCatching {RuntimeShader(GLASS_EDGE_SHADER)}.onFailure {android.util.Log.w("LiuyingGlass","Lens shader unavailable",it)}.getOrNull()}
    if(shader==null)return this
    var dimensions by remember {mutableStateOf(IntSize.Zero)}
    val density=androidx.compose.ui.platform.LocalDensity.current
    val direction=androidx.compose.ui.platform.LocalLayoutDirection.current
    // Shader compilation is cached. Rebuild its effect only for geometry or a short touch transition.
    val profile=glassRefractionProfile(refraction)
    val geometryReady=remember(shader,dimensions,shape,density,direction,strength,edgeWidthDp,profile,rimStrength,droplet) {
        if(dimensions.width<=0||dimensions.height<=0)false else runCatching {
            shader.setFloatUniform("resolution",dimensions.width.toFloat(),dimensions.height.toFloat())
            val outline=shape.createOutline(androidx.compose.ui.geometry.Size(dimensions.width.toFloat(),dimensions.height.toFloat()),direction,density)
            val corners=(outline as? Outline.Rounded)?.roundRect
            shader.setFloatUniform("cornerRadii",corners?.topLeftCornerRadius?.x?:(if(droplet)dimensions.height*.5f else 0f),corners?.topRightCornerRadius?.x?:(if(droplet)dimensions.height*.5f else 0f),corners?.bottomRightCornerRadius?.x?:(if(droplet)dimensions.height*.5f else 0f),corners?.bottomLeftCornerRadius?.x?:(if(droplet)dimensions.height*.5f else 0f))
            shader.setFloatUniform("waterDrop",if(droplet)1f else 0f)
            shader.setFloatUniform("lensStrength",strength)
            shader.setFloatUniform("rimStrength",rimStrength)
            shader.setFloatUniform("edgeWidth",with(density){(if(edgeWidthDp>0f)edgeWidthDp else profile.bandDp).dp.toPx()})
            shader.setFloatUniform("refraction",with(density){profile.displacementDp.dp.toPx()})
            shader.setFloatUniform("magnification",profile.magnification)
            shader.setFloatUniform("pixel",density.density)
            true
        }.getOrDefault(false)
    }
    return this.onSizeChanged {dimensions=it}
        .graphicsLayer {
            // Observe motion in the layer phase; do not recompose the Haze hierarchy per touch frame.
            val currentPressure=pressureSource.value.takeIf{it.isFinite()}?.coerceIn(0f,1f) ?: 0f
            val effectActive=glassEffectActive(geometryReady && size.width.toInt()==dimensions.width && size.height.toInt()==dimensions.height,pressedOnly,currentPressure)
            renderEffect=if(!effectActive)null else runCatching {
                shader.setFloatUniform("pressure",currentPressure)
                val drop=dropletOptics(if(droplet)currentPressure else 0f)
                shader.setFloatUniform("dropProfile",drop.bandScale,drop.oppositeLip,drop.dispersionDp,drop.shoulderLight)
                shader.setFloatUniform("pressZoom",drop.centerMagnification)
                val point=touchSource.value
                shader.setFloatUniform("touch",if(point==Offset.Zero)dimensions.width*.5f else point.x,if(point==Offset.Zero)dimensions.height*.5f else point.y)
                // Runtime effects capture uniform values, so refresh this small filter, not the geometry or hierarchy.
                android.graphics.RenderEffect.createRuntimeShaderEffect(shader,"backdrop").asComposeRenderEffect()
            }.getOrNull()
            compositingStrategy=if(effectActive)CompositingStrategy.Offscreen else CompositingStrategy.Auto
        }
}

// Original convex bevel. Centre is an untouched sample; edge normals bend scene details.
// All optical dimensions are density-correct dp, never capped to a few physical pixels.
private const val GLASS_EDGE_SHADER="""
uniform shader backdrop;
uniform float2 resolution;
uniform float4 cornerRadii;
uniform float lensStrength;
uniform float rimStrength;
uniform float edgeWidth;
uniform float refraction;
uniform float magnification;
uniform float pixel;
uniform float pressure;
uniform float2 touch;
uniform float waterDrop;
uniform float4 dropProfile;
uniform float pressZoom;
half4 main(float2 xy) {
    float2 h=resolution*0.5;
    float2 p=xy-h;
    float r=p.x<0.0 ? (p.y<0.0?cornerRadii.x:cornerRadii.w) : (p.y<0.0?cornerRadii.y:cornerRadii.z);
    r=min(r,min(h.x,h.y));
    float2 q=abs(p)-h+float2(r);
    float2 outside=max(q,float2(0.0));
    float sdf=length(outside)+min(max(q.x,q.y),0.0)-r;
    float wet=clamp(pressure*waterDrop,0.0,1.0);
    // A thin meniscus around a stable capsule, not a broad spherical bevel.
    float depth=max(-sdf,0.0);
    float band=min(edgeWidth*mix(1.0,0.56,waterDrop)*dropProfile.x,min(h.x,h.y)*0.60);
    float edge=1.0-clamp(depth/max(band,1.0),0.0,1.0);
    float2 normal=length(outside)>0.001?normalize(outside)*sign(p):(q.x>q.y?float2(sign(p.x),0.0):float2(0.0,sign(p.y)));
    normal=normal/max(length(normal),0.00001);
    float proximity=1.0-clamp(length(xy-touch)/max(length(resolution),1.0),0.0,1.0);
    // Curved, continuous index profile: a narrow rolled edge and a clear flat interior.
    float curvature=edge*edge*(3.0-2.0*edge);
    float t=clamp(depth/max(band,1.0),0.0,1.0);
    // Opposite displacement at the outer lip and inner shoulder creates a convex meniscus.
    float rolled=sin(t*3.14159265)*0.85*wet-exp(-t*15.0)*dropProfile.y;
    float lensCurve=curvature*(1.0-wet*0.88)+rolled*0.90;
    float bend=lensCurve*min(refraction*mix(1.0,mix(0.65,1.35,wet),waterDrop),min(h.x,h.y)*0.48)*clamp(lensStrength,0.0,1.3)*(1.0+pressure*(0.06+proximity*0.06));
    // Press zoom is independent of the subdued rest bevel; a clear central sample stays readable.
    float zoom=mix(magnification,1.0+(magnification-1.0)*0.35,waterDrop)+(pressZoom-1.0)*waterDrop;
    float2 glassPoint=h+p/max(zoom,1.0);
    float2 uv=clamp(glassPoint-normal*bend,float2(0.5),resolution-float2(0.5));
    half4 color=backdrop.eval(uv);
    if(edge>0.001) {
        float2 dispersion=normal*curvature*pixel*(dropProfile.z+pressure*0.05);
        color.r=backdrop.eval(clamp(uv+dispersion,float2(0.5),resolution-float2(0.5))).r;
        color.b=backdrop.eval(clamp(uv-dispersion,float2(0.5),resolution-float2(0.5))).b;
    }
    // A narrow optical rim stays visible on white without whitening the entire lens.
    float rim=1.0-smoothstep(0.0,pixel*2.0,depth);
    float light=dot(normal,normalize(float2(-0.55,-0.83)));
    float highlight=(max(light,0.0)*0.38+max(-light,0.0)*0.18+0.025)*rim*rimStrength;
    float luminance=dot(float3(color.rgb)/max(float(color.a),0.001),float3(0.2126,0.7152,0.0722));
    float contrast=smoothstep(0.45,0.95,luminance);
    float shade=(0.035+max(-light,0.0)*0.13+contrast*0.055)*rim*rimStrength;
    color.rgb=color.rgb*(1.0-half(shade));
    color.rgb=mix(color.rgb,half3(color.a),half(highlight*(1.0-contrast*0.28)));
    float shoulder=exp(-pow((t-0.24)/0.17,2.0))*dropProfile.w;
    float lipShadow=exp(-depth/max(pixel*1.1,0.5))*wet*(0.015+contrast*0.025);
    color.rgb*=half(1.0-lipShadow);
    color.rgb=mix(color.rgb,half3(color.a),half(shoulder*(0.35+max(light,0.0)*0.65)));
    return color;
}
"""
