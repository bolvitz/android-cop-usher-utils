package com.eventmonitor.core.common.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class Severity { CRITICAL, HIGH, LOW, NEUTRAL }

private data class SevStyle(val bg: Color, val fg: Color, val outlineColor: Color?)

@Composable
private fun styleFor(sev: Severity): SevStyle {
    val ink = MaterialTheme.colorScheme.onBackground
    val paper = MaterialTheme.colorScheme.background
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    return when (sev) {
        Severity.CRITICAL -> SevStyle(MaterialTheme.colorScheme.error, paper, null)
        Severity.HIGH -> SevStyle(ink, paper, null)
        Severity.LOW -> SevStyle(Color.Transparent, ink, ink)
        Severity.NEUTRAL -> SevStyle(Color.Transparent, muted, muted)
    }
}

/** RETRO severity chip — rounded pill, all-caps mono, B&W + red only. */
@Composable
fun SevPill(
    severity: Severity,
    label: String,
    modifier: Modifier = Modifier,
) {
    val s = styleFor(severity)
    val base = modifier
        .clip(RoundedCornerShape(8.dp))
        .background(s.bg)
    val withBorder = if (s.outlineColor != null) {
        base.border(1.5.dp, s.outlineColor, RoundedCornerShape(8.dp))
    } else base
    Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.labelLarge.copy(color = s.fg),
        modifier = withBorder.padding(horizontal = 10.dp, vertical = 5.dp),
    )
}
