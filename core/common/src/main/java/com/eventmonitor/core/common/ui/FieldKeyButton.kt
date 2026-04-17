package com.eventmonitor.core.common.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eventmonitor.core.common.utils.HapticFeedbackHelper
import com.eventmonitor.core.common.utils.rememberHapticFeedback
import kotlinx.coroutines.delay

enum class KeyVariant { Plus, Minus }

/**
 * A sharp-edged counter key. Supports tap + accelerating long-press rapid fire.
 * Ink-filled for Plus, paper for Minus — matches FIELD keypad grammar.
 */
@Composable
fun FieldKeyButton(
    label: String,
    caption: String,
    variant: KeyVariant,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    repeatable: Boolean = true,
    haptic: HapticFeedbackHelper = rememberHapticFeedback(),
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val paper = MaterialTheme.colorScheme.background
    val paperSoft = MaterialTheme.colorScheme.surfaceVariant

    val bg: Color
    val fg: Color
    val captionColor: Color
    when (variant) {
        KeyVariant.Plus -> {
            bg = if (enabled) ink else paperSoft
            fg = if (enabled) paper else MaterialTheme.colorScheme.onSurfaceVariant
            captionColor = fg.copy(alpha = 0.65f)
        }

        KeyVariant.Minus -> {
            bg = if (enabled) paperSoft else paperSoft
            fg = if (enabled) ink else MaterialTheme.colorScheme.onSurfaceVariant
            captionColor = MaterialTheme.colorScheme.onSurfaceVariant
        }
    }

    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(90),
        label = "keyScale",
    )

    // Accelerating rapid-fire while held.
    LaunchedEffect(pressed, enabled, repeatable) {
        if (pressed && enabled && repeatable) {
            delay(380)
            var interval = 180L
            while (pressed) {
                onTap()
                haptic.counter()
                delay(interval)
                if (interval > 50L) interval = (interval * 0.85).toLong().coerceAtLeast(50L)
            }
        }
    }

    Box(
        modifier = modifier
            .scale(scale)
            .heightIn(min = FieldTokens.KeypadHeight)
            .fillMaxWidth()
            .border(FieldTokens.HairStrong, ink)
            .background(bg)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        pressed = true
                        val released = tryAwaitRelease()
                        pressed = false
                        if (released) { /* rapid-fire coroutine handles the fire */
                        }
                    },
                    onTap = {
                        haptic.counter()
                        onTap()
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.headlineMedium,
                color = fg,
                textAlign = TextAlign.Center,
            )
            Text(
                text = caption.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = captionColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
