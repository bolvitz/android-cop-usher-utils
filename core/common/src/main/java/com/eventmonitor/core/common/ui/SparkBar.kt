package com.eventmonitor.core.common.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** RETRO inline progress bar — ink fill, red flash above 95%, rounded ends. */
@Composable
fun SparkBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
    trackColor: Color = MaterialTheme.colorScheme.outlineVariant,
) {
    val clamped = progress.coerceIn(0f, 1f)
    val fill = when {
        clamped >= 0.95f -> MaterialTheme.colorScheme.error
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
            .clip(RoundedCornerShape(height / 2))
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

/** Capacity tone — collapses to ink under capacity, red at danger. */
@Composable
fun capacityToneFor(progress: Float): Color = when {
    progress >= 0.95f -> MaterialTheme.colorScheme.error
    progress > 0f -> MaterialTheme.colorScheme.onBackground
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
