package com.eventmonitor.core.common.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eventmonitor.core.common.theme.LocalThemeMode
import com.eventmonitor.core.common.theme.MonoTiny
import com.eventmonitor.core.common.utils.rememberHapticFeedback
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Newspaper masthead — volume/issue marque on top, giant serif brand underneath
 * with a live clock on the right, then tagline + hairline.
 *
 * Designed to own the top 28% of the home screen.
 */
@Composable
fun FieldMasthead(
    title: String = "Field",
    tagline: String = "The Operations Console",
    volume: String = "VOL. 01",
    issue: String = "NO. 007",
    modifier: Modifier = Modifier,
) {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(30_000L)
        }
    }
    val dateFmt = remember { SimpleDateFormat("EEE d MMM · yyyy", Locale.getDefault()) }
    val clockFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 4.dp),
    ) {
        // Top meta row — vol/issue, dateline
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$volume · $issue".uppercase(),
                style = MonoTiny,
                color = muted,
            )
            Text(
                text = dateFmt.format(now).uppercase(),
                style = MonoTiny,
                color = muted,
            )
        }
        Spacer(Modifier.height(6.dp))

        // Brand + clock — kerned huge serif next to tabular mono time.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = title.substring(0, 1),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 72.sp,
                        lineHeight = 70.sp
                    ),
                    color = ink,
                )
                if (title.length > 2) {
                    Text(
                        text = title.substring(1, 2),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 72.sp,
                            lineHeight = 70.sp,
                            fontStyle = FontStyle.Italic,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = title.substring(2),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 72.sp,
                            lineHeight = 70.sp
                        ),
                        color = ink,
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(bottom = 6.dp),
            ) {
                ThemeToggle()
                Spacer(Modifier.height(6.dp))
                Text(
                    text = clockFmt.format(now),
                    style = MaterialTheme.typography.headlineMedium,
                    color = ink,
                )
            }
        }
        Spacer(Modifier.height(2.dp))

        // Tagline
        Text(
            text = tagline,
            style = MaterialTheme.typography.labelMedium,
            color = muted,
        )
        Spacer(Modifier.height(10.dp))
        // Hairline rule
        Box(
            Modifier
                .fillMaxWidth()
                .height(FieldTokens.Hair)
                .background(ink),
        )
    }
}

/**
 * Compact light/dark switch — a stamp-bordered button with a sun↔moon glyph
 * that flips on press. Reads/writes the [LocalThemeMode] composition local.
 */
@Composable
private fun ThemeToggle(modifier: Modifier = Modifier) {
    val theme = LocalThemeMode.current
    val haptic = rememberHapticFeedback()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "toggleScale",
    )

    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val isDark = theme.isDark

    Row(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(2.dp))
            .border(FieldTokens.Hair, ink, RoundedCornerShape(2.dp))
            .background(if (isDark) ink else MaterialTheme.colorScheme.background)
            .clickable(
                interactionSource = interaction,
                indication = null,
            ) {
                haptic.selection()
                theme.isDark = !isDark
            }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedContent(
            targetState = isDark,
            label = "toggleGlyph",
            transitionSpec = {
                (scaleIn(tween(180), initialScale = 0.6f) + fadeIn(tween(180)))
                    .togetherWith(scaleOut(tween(140), targetScale = 0.6f) + fadeOut(tween(140)))
            },
        ) { dark ->
            Text(
                text = if (dark) "☾" else "☀",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = if (dark) MaterialTheme.colorScheme.background else ink,
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = if (isDark) "DARK" else "LIGHT",
            style = MonoTiny.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
            ),
            color = if (isDark) MaterialTheme.colorScheme.background else muted,
        )
    }
}
