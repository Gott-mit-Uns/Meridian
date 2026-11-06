package app.meridian.mobile
import app.meridian.mobile.glass.*

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Artwork stays unobscured. Content is quiet; only floating controls carry the optical material. */
internal enum class SurfaceRole { Content,Form,Glass,Artwork,Overlay }
internal data class SurfaceRecipe(val opacity:Float,val edge:Float,val shadowDp:Float)
internal fun surfaceRecipe(role:SurfaceRole,dark:Boolean)=when(role){
    SurfaceRole.Content->SurfaceRecipe(if(dark).90f else .95f,if(dark).065f else .035f,0f)
    SurfaceRole.Form->SurfaceRecipe(if(dark).96f else .98f,if(dark).10f else .075f,0f)
    SurfaceRole.Glass->SurfaceRecipe(if(dark).52f else .64f,if(dark).12f else .10f,3f)
    SurfaceRole.Artwork->SurfaceRecipe(0f,0f,0f)
    SurfaceRole.Overlay->SurfaceRecipe(.97f,.12f,6f)
}
@Composable internal fun Modifier.contentSurface(shape:Shape=RoundedCornerShape(UiSpace.cardRadius),tint:Color=Color.Transparent):Modifier =
    quietSurface(SurfaceRole.Content,shape,tint)
@Composable internal fun Modifier.formSurface(shape:Shape=RoundedCornerShape(UiSpace.controlRadius)):Modifier =
    quietSurface(SurfaceRole.Form,shape)
@Composable internal fun Modifier.quietSurface(role:SurfaceRole,shape:Shape,tint:Color=Color.Transparent):Modifier {
    val recipe=surfaceRecipe(role,LocalLiusuDark.current)
    if(role==SurfaceRole.Artwork)return clip(shape)
    return then(if(recipe.shadowDp>0f)Modifier.shadow(recipe.shadowDp.dp,shape,ambientColor=Color.Black.copy(alpha=.08f),spotColor=Color.Black.copy(alpha=.12f)) else Modifier).clip(shape).background(MaterialTheme.colorScheme.surface.copy(alpha=recipe.opacity))
        .background(tint.copy(alpha=tint.alpha*.08f))
        .border(.6.dp,MaterialTheme.colorScheme.onSurface.copy(alpha=recipe.edge),shape)
}
