package com.eventmonitor.core.common.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/** A 1dp solid rule. The workhorse of the layout. */
@Composable
fun Hairline(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onBackground,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(FieldTokens.Hair)
            .background(color),
    )
}

/** Softer rule used inside sections. */
@Composable
fun HairlineSoft(modifier: Modifier = Modifier) {
    Hairline(
        modifier = modifier,
        color = MaterialTheme.colorScheme.outline,
    )
}
