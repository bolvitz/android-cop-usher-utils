package com.eventmonitor.core.common.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.eventmonitor.core.common.theme.Amber
import com.eventmonitor.core.common.theme.Sage
import com.eventmonitor.core.common.theme.Signal

enum class Severity { CRITICAL, HIGH, LOW, NEUTRAL }

private data class SevStyle(val bg: Color, val fg: Color)

@Composable
private fun styleFor(sev: Severity): SevStyle = when (sev) {
    Severity.CRITICAL -> SevStyle(Signal, MaterialTheme.colorScheme.onError)
    Severity.HIGH -> SevStyle(Amber, MaterialTheme.colorScheme.onError)
    Severity.LOW -> SevStyle(Sage, MaterialTheme.colorScheme.onError)
    Severity.NEUTRAL -> SevStyle(
        MaterialTheme.colorScheme.background,
        MaterialTheme.colorScheme.onBackground
    )
}

/** Monospace all-caps severity chip. One of four tones, no rounded corners. */
@Composable
fun SevPill(
    severity: Severity,
    label: String,
    modifier: Modifier = Modifier,
) {
    val s = styleFor(severity)
    Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(color = s.fg),
        modifier = modifier
            .border(
                FieldTokens.Hair,
                if (severity == Severity.NEUTRAL) MaterialTheme.colorScheme.onBackground else s.bg
            )
            .background(if (severity == Severity.NEUTRAL) Color.Transparent else s.bg)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}
