package com.eventmonitor.core.common.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Small icon-bordered button used in the app bar corners. */
@Composable
fun FieldAppBarIcon(
    glyph: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    hot: Boolean = false,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val paper = MaterialTheme.colorScheme.background
    val hotColor = MaterialTheme.colorScheme.error

    val bg = when {
        !enabled -> paper
        hot -> hotColor
        else -> paper
    }
    val fg = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant
        hot -> paper
        else -> ink
    }
    Box(
        modifier = modifier
            .size(FieldTokens.AppBarIconSize)
            .border(FieldTokens.Hair, if (hot) hotColor else ink)
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = glyph,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
        )
    }
}

/**
 * The FIELD app bar. Two optional corner icons, a centered mono eyebrow + serif title,
 * and a hairline rule below.
 */
@Composable
fun FieldAppBar(
    title: String,
    eyebrow: String? = null,
    modifier: Modifier = Modifier,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.width(FieldTokens.AppBarIconSize)) {
                leading?.invoke()
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(
                modifier = Modifier
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (!eyebrow.isNullOrBlank()) {
                    Text(
                        text = eyebrow.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Box(modifier = Modifier.width(FieldTokens.AppBarIconSize)) {
                trailing?.invoke()
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(FieldTokens.Hair)
                .background(MaterialTheme.colorScheme.onBackground),
        )
    }
}

@Composable
fun FieldAppBarIconSpacer() {
    Box(modifier = Modifier.size(FieldTokens.AppBarIconSize)) {}
}

@Suppress("UnusedReceiverParameter")
@Composable
fun Color.unused() = Unit
