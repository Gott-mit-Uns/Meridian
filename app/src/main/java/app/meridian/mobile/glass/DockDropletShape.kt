// Adapted from user-owned 流影 source; optical implementation retained.
package app.meridian.mobile.glass

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.*

/** Retain a short flat shoulder and rounded ends; inflation belongs to the centred layer.
 * An oval outline made the rim read as a polished stone rather than a clear lens.
 */
internal object DockDropletShape:Shape {
    override fun createOutline(size:Size,layoutDirection:LayoutDirection,density:Density):Outline =
        CircleShape.createOutline(size,layoutDirection,density)
}
