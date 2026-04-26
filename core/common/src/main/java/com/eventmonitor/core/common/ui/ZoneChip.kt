package com.eventmonitor.core.common.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eventmonitor.core.common.theme.MonoTiny

/**
 * Selectable mono chip — used for filters, category pickers, and status
 * pills throughout the app. Body matches the Soft kit (rounded BrandBlue
 * with corner-bracket reticle when selected, scale-on-press); label is
 * passed through verbatim so callers can keep "ALL · 12"-style strings.
 */
@Composable
fun ZoneChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val paper = MaterialTheme.colorScheme.background
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val bg by animateColorAsState(
        targetValue = if (selected) BrandBlue else ink.copy(alpha = 0.06f),
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "chipBg",
    )
    val fg by animateColorAsState(
        targetValue = if (selected) paper else ink,
        animationSpec = tween(220),
        label = "chipFg",
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "chipScale",
    )
    val labelAlpha = if (selected) 1f else 0.78f

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val len = 7.dp.toPx()
                val w = 1.5.dp.toPx()
                val inset = 4.dp.toPx()
                val c = paper
                val x0 = inset
                val y0 = inset
                val x1 = size.width - inset
                val y1 = size.height - inset
                drawLine(c, Offset(x0, y0), Offset(x0 + len, y0), w)
                drawLine(c, Offset(x0, y0), Offset(x0, y0 + len), w)
                drawLine(c, Offset(x1, y0), Offset(x1 - len, y0), w)
                drawLine(c, Offset(x1, y0), Offset(x1, y0 + len), w)
                drawLine(c, Offset(x0, y1), Offset(x0 + len, y1), w)
                drawLine(c, Offset(x0, y1), Offset(x0, y1 - len), w)
                drawLine(c, Offset(x1, y1), Offset(x1 - len, y1), w)
                drawLine(c, Offset(x1, y1), Offset(x1, y1 - len), w)
            }
        }
        Text(
            text = label.uppercase(),
            style = MonoTiny.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                color = fg.copy(alpha = labelAlpha),
            ),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}
