package app.meridian.mobile
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
/** Flow controls own their pressure treatment; cards have no platform ink ripple. */
internal object FlowQuietIndication:IndicationNodeFactory {
 override fun create(interactionSource:InteractionSource):DelegatableNode=object:Modifier.Node(),DrawModifierNode {override fun ContentDrawScope.draw(){drawContent()}}
 override fun hashCode()=1984
 override fun equals(other:Any?)=other===this
}
