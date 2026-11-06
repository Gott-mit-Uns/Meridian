// Adapted from user-owned 流影 source; optical implementation retained.
package app.meridian.mobile.glass

internal fun normalizedGlassHighlight(value: Float): Float = if (value.isFinite()) value.coerceIn(0f,2f) else 1.15f
