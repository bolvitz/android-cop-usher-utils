package com.eventmonitor.core.common.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

/**
 * DigitRoll — renders an integer with a per-digit slide animation.
 * When a digit changes, it rolls up (for increases) or down (for decreases).
 */
@Composable
fun DigitRoll(
    value: Int,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    previous: Int = value,
) {
    val text = value.toString()
    val direction = if (value >= previous) 1 else -1

    Row(modifier = modifier) {
        // Index each digit so Compose can animate positionally.
        text.forEachIndexed { index, ch ->
            key(index, ch) {
                AnimatedContent(
                    targetState = ch,
                    transitionSpec = {
                        val enter = slideInVertically(
                            animationSpec = tween(durationMillis = 280),
                            initialOffsetY = { h -> direction * h },
                        ) + fadeIn(tween(180))
                        val exit = slideOutVertically(
                            animationSpec = tween(durationMillis = 280),
                            targetOffsetY = { h -> -direction * h },
                        ) + fadeOut(tween(180))
                        enter togetherWith exit
                    },
                    label = "digit-$index",
                ) { digit ->
                    Text(text = digit.toString(), style = style, color = color)
                }
            }
        }
    }
}
