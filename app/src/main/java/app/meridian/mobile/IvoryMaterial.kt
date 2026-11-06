package app.meridian.mobile
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
@Composable internal fun Modifier.themeCardShadow(shape:Shape):Modifier=if(LocalGlobalTheme.current.id in setOf("ivory","vivid")&&!LocalPalette.current.dark)shadow(2.dp,shape,clip=false,ambientColor=Color(0xFF685D48).copy(alpha=.08f),spotColor=Color(0xFF685D48).copy(alpha=.08f))else this
