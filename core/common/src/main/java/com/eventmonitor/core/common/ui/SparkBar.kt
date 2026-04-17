package com.eventmonitor.core.common.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eventmonitor.core.common.theme.Amber
import com.eventmonitor.core.common.theme.Sage
import com.eventmonitor.core.common.theme.Signal

/** Thin inline progress bar. Recolours as capacity approaches full. */
@Composable
fun SparkBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 3.dp,
    trackColor: Color = MaterialTheme.colorScheme.outlineVariant,
) {
    val clamped = progress.coerceIn(0f, 1f)
    val fill = when {
        clamped >= 0.95f -> Signal
        clamped >= 0.80f -> Amber
        clamped >= 0.05f -> MaterialTheme.colorScheme.onBackground
        else -> MaterialTheme.colorScheme.outline
    }
    val animated by animateFloatAsState(
        targetValue = clamped,
        animationSpec = tween(durationMillis = 640),
        label = "sparkBarFill",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(trackColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated)
                .background(fill),
        )
    }
}

/** Accents the capacity colour for inline use in labels etc. */
@Composable
fun capacityToneFor(progress: Float): Color = when {
    progress >= 0.95f -> Signal
    progress >= 0.80f -> Amber
    progress > 0f -> Sage
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
