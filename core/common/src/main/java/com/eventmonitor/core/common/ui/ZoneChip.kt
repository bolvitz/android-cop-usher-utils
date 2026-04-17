package com.eventmonitor.core.common.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Selectable mono chip — used for zone switching, category filters, etc. */
@Composable
fun ZoneChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val paper = MaterialTheme.colorScheme.background
    val bg by animateColorAsState(if (selected) ink else paper, label = "chipBg")
    val fg by animateColorAsState(if (selected) paper else ink, label = "chipFg")
    val interaction = remember { MutableInteractionSource() }

    Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(color = fg),
        modifier = modifier
            .border(FieldTokens.Hair, ink)
            .background(bg)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}
