package com.eventmonitor.core.common.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// RETRO shapes — soft, generous corners.
// The reference (Voice Recognition) leans on big-radius pills and rounded
// rectangles to make tap targets feel inviting and the type feel like signage.
val FieldShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)
