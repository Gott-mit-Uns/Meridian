package app.meridian.mobile
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import dev.chrisbanes.haze.HazeState
import app.meridian.mobile.glass.*
internal val LocalControlBackdrop=staticCompositionLocalOf<HazeState?>{null}
internal val LocalControlSettings=staticCompositionLocalOf{false}
internal val LocalFooterAction=staticCompositionLocalOf{false}
internal val LocalFooterPrimary=staticCompositionLocalOf{false}
internal object UiSpace{val cardRadius=22.dp;val controlRadius=16.dp;val touch=48.dp}
private val ControlShape=RoundedCornerShape(16.dp)
internal object UiType{val label=TextStyle(fontSize=14.sp,lineHeight=20.sp);val metadata=TextStyle(fontSize=12.sp,lineHeight=18.sp)}
@Composable internal fun Modifier.motionPress(source:MutableInteractionSource):Modifier{val pressed by source.collectIsPressedAsState();val motion=LocalAppMotion.current;val scale=animateFloatAsState(if(pressed).972f else 1f,if(motion.full)spring(dampingRatio=if(pressed)1f else .82f,stiffness=if(pressed)1100f else 430f)else snap(),label="flow-control-pressure");return graphicsLayer{scaleX=scale.value;scaleY=scale.value}}

@Composable internal fun Modifier.cinemaGlass(shape: Shape = RoundedCornerShape(UiSpace.cardRadius), tint: Color = Color.Transparent): Modifier {
    val dark = LocalLiusuDark.current
    val recipe=surfaceRecipe(SurfaceRole.Glass,dark)
    val highlight=normalizedGlassHighlight(LocalAppPreferences.current.glassHighlight)
    val base = if (dark) Color(0xFF1C1C1F) else Color.White
    return shadow(recipe.shadowDp.dp, shape, ambientColor = Color.Black.copy(alpha = .07f), spotColor = Color.Black.copy(alpha = .10f))
        .clip(shape).background(base.copy(alpha=recipe.opacity))
        .background(tint.copy(alpha = tint.alpha * .14f))
        .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = if (dark) .045f else .42f), Color.Transparent)))
        .border(.55.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = ((if (dark) .29f else .76f)*highlight).coerceIn(0f,1f)), Color.Black.copy(alpha = if(dark).12f else .10f), Color.White.copy(alpha = ((if (dark) .18f else .42f)*highlight).coerceIn(0f,1f)))), shape)
        .glassBoundaryEdge(shape,dark).subtleGlassHighlight(shape,dark)
}


@Composable private fun CinemaAction(onClick: () -> Unit, modifier: Modifier, enabled: Boolean, fill: Color, ink: Color,
    shape: Shape = ControlShape, outlined: Boolean = false, minWidth: Dp = 48.dp, primary:Boolean=false, content: @Composable RowScope.() -> Unit) {
    val compact = LocalPlayerMenu.current
    val settings=LocalControlSettings.current
    val buttonShape=if(settings)RoundedCornerShape(14.dp) else shape
    val interaction = remember { MutableInteractionSource() }; val pressed by interaction.collectIsPressedAsState()
    val filled=true
    val backdrop=if(!settings && !compact && !LocalInsideGlassChrome.current && LocalAppearance.current.glass && androidx.compose.ui.platform.LocalView.current.parent !is androidx.compose.ui.window.DialogWindowProvider)LocalControlBackdrop.current else null
    Box(modifier.then(if(LocalFooterAction.current)Modifier.fillMaxWidth()else Modifier).defaultMinSize(minWidth=minWidth,minHeight=if(compact)34.dp else UiSpace.touch)
        .motionPress(interaction).alpha(if(enabled)1f else .4f).clip(buttonShape)
        .clickable(interactionSource=interaction,indication=null,enabled=enabled,role=Role.Button,onClick=onClick),contentAlignment=Alignment.Center) {
        SharedGlassControl(buttonShape,Modifier.matchParentSize(),if(primary&&LocalAppearance.current.glass)Color.Transparent else fill,pressed)
        if(primary&&LocalAppearance.current.glass)Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(fill.copy(alpha=if(LocalLiusuDark.current).34f else .25f),fill.copy(alpha=if(LocalLiusuDark.current).22f else .16f)))).border(.8.dp,fill.copy(alpha=.44f),buttonShape))
        if(outlined && backdrop==null)Box(Modifier.matchParentSize().border(.7.dp,glassEdge,buttonShape))
        Row(Modifier.background(if(pressed)ink.copy(alpha=.04f) else Color.Transparent)
            .padding(horizontal=if(compact)11.dp else 15.dp,vertical=if(compact)6.dp else 9.dp),
            horizontalArrangement=Arrangement.Center,verticalAlignment=Alignment.CenterVertically) {
            CompositionLocalProvider(LocalContentColor provides ink) {ProvideTextStyle((if(compact)UiType.metadata else UiType.label).copy(fontWeight=if(primary)FontWeight.SemiBold else FontWeight.Normal)){content()}}
        }
    }
}

@Composable fun CinemaButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, shape: Shape = ControlShape,
    colors: ButtonColors? = null, elevation: ButtonElevation? = null, content: @Composable RowScope.() -> Unit) {
    val fill = colors?.containerColor ?: MaterialTheme.colorScheme.primary
    val ink = if(LocalAppearance.current.glass){if(colors?.containerColor==MaterialTheme.colorScheme.error)MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface} else colors?.contentColor ?: themeOnAccent(fill)
    CinemaAction(onClick, modifier, enabled, fill, ink, shape, primary=true, content = content)
}
@Composable fun CinemaTonalButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, colors: ButtonColors? = null, content: @Composable RowScope.() -> Unit) {
    CinemaAction(onClick, modifier, enabled, colors?.containerColor ?: MaterialTheme.colorScheme.primary.copy(alpha = if (LocalLiusuDark.current) .18f else .09f), colors?.contentColor ?: MaterialTheme.colorScheme.onSurface, content = content)
}
@Composable fun CinemaOutlineButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    CinemaAction(onClick, modifier, enabled, Color.Transparent, MaterialTheme.colorScheme.onSurface, ControlShape, outlined = true, content = content)
}
@Composable fun CinemaTextButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    if(LocalFooterPrimary.current)CinemaButton(onClick,modifier,enabled,content=content)
    else CinemaAction(onClick, modifier, enabled, Color.Transparent, MaterialTheme.colorScheme.onSurface, minWidth = 40.dp, content = content)
}
@Composable fun CinemaIconButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable () -> Unit) {
 val interaction=remember{MutableInteractionSource()};val pressed by interaction.collectIsPressedAsState()
 Box(modifier.sizeIn(minWidth=44.dp,minHeight=44.dp).motionPress(interaction).alpha(if(enabled)1f else .4f).clip(CircleShape).clickable(interactionSource=interaction,indication=null,enabled=enabled,role=Role.Button,onClick=onClick),contentAlignment=Alignment.Center){SharedGlassControl(CircleShape,Modifier.matchParentSize(),pressed=pressed);content()}
}
@Composable internal fun SharedGlassControl(shape:Shape,modifier:Modifier=Modifier,tint:Color=Color.Transparent,pressed:Boolean=false){
 val backdrop=LocalControlBackdrop.current;val glass=LocalAppearance.current.glass
 if(LocalInsideGlassChrome.current)Box(modifier.background(tint.copy(alpha=tint.alpha*.12f)))
 else if(glass&&backdrop!=null)GlassChrome(backdrop,modifier,shape,elevated=false,pressed=pressed){Box(Modifier.matchParentSize().background(tint.copy(alpha=tint.alpha*.12f)))}
 else if(glass)Box(modifier.cinemaGlass(shape,tint))
 else Box(modifier.background(if(tint.alpha==0f)softWash else tint,shape).border(.6.dp,glassEdge,shape))
}

@Composable fun CinemaChoice(selected: Boolean, onClick: () -> Unit, label: @Composable () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val ink=MaterialTheme.colorScheme.onSurface
    val compact=LocalPlayerMenu.current
    val settings=LocalControlSettings.current
    val choiceShape=if(settings)RoundedCornerShape(12.dp) else ControlShape
    val interaction=remember{MutableInteractionSource()}
    val pressed by interaction.collectIsPressedAsState()
    val backdrop=if(!settings && !compact && !LocalInsideGlassChrome.current && LocalAppearance.current.glass && androidx.compose.ui.platform.LocalView.current.parent !is androidx.compose.ui.window.DialogWindowProvider)LocalControlBackdrop.current else null
    val fill by androidx.compose.animation.animateColorAsState(ink.copy(alpha=if(selected).085f else .018f),tween(LocalAppMotion.current.duration(120)),label="choiceTint")
    val edge by androidx.compose.animation.animateColorAsState(ink.copy(alpha=if(selected).23f else .07f),tween(LocalAppMotion.current.duration(140,60)),label="choiceEdge")
    val selection=animateFloatAsState(if(selected)1f else 0f,tween(LocalAppMotion.current.duration(140,60)),label="choiceSelection")
    val fullMotion=LocalAppMotion.current.full
    val plate=Modifier.background(fill).border(.7.dp,edge,choiceShape)
    Box(modifier.heightIn(min=if(compact)34.dp else UiSpace.touch).motionPress(interaction).alpha(if(enabled)1f else GlassSpec.disabled).clip(choiceShape)
        .selectable(selected=selected,enabled=enabled,role=Role.RadioButton,interactionSource=interaction,indication=null,onClick=onClick),contentAlignment=Alignment.Center) {
        SharedGlassControl(choiceShape,Modifier.matchParentSize(),fill,pressed)
        Box(Modifier.matchParentSize().border(.7.dp,edge,choiceShape))
        Row(Modifier.padding(horizontal=12.dp,vertical=if(compact)6.dp else 10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(7.dp,Alignment.CenterHorizontally)) {
            if(selected || settings)Icon(Icons.Outlined.Check,null,Modifier.size(15.dp).graphicsLayer {
                alpha=selection.value
                scaleX=if(fullMotion).86f+.14f*selection.value else 1f;scaleY=scaleX
            },tint=ink)
            CompositionLocalProvider(LocalContentColor provides ink){ProvideTextStyle(UiType.label){label()}}
        }
    }
}
