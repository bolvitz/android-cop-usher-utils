package com.cop.app.headcounter.presentation.utils

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Common animation specifications for consistent motion design
 */
object AnimationSpecs {
    val fast = tween<Float>(150, easing = FastOutSlowInEasing)
    val medium = tween<Float>(300, easing = FastOutSlowInEasing)
    val slow = tween<Float>(500, easing = FastOutSlowInEasing)

    val spring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    val fastSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
}

/**
 * Animated visibility for list items with slide and fade
 */
@Composable
fun AnimatedListItem(
    visible: Boolean,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            animationSpec = tween(300, easing = FastOutSlowInEasing),
            initialOffsetY = { it / 3 }
        ) + fadeIn(animationSpec = tween(300)),
        exit = slideOutVertically(
            animationSpec = tween(200, easing = FastOutLinearInEasing),
            targetOffsetY = { -it / 3 }
        ) + fadeOut(animationSpec = tween(200)),
        content = content
    )
}

/**
 * Scale animation modifier for button press effects
 */
fun Modifier.pressAnimation(pressed: Boolean): Modifier = graphicsLayer {
    val scale = if (pressed) 0.95f else 1f
    scaleX = scale
    scaleY = scale
}

/**
 * Pulse animation for attention-grabbing elements
 */
@Composable
fun rememberPulseAnimation(durationMillis: Int = 1000): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    return infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis / 2, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    ).value
}

/**
 * Shake animation for errors
 */
@Composable
fun rememberShakeAnimation(trigger: Boolean): Float {
    val shakeOffset = remember { Animatable(0f) }

    if (trigger) {
        androidx.compose.runtime.LaunchedEffect(trigger) {
            repeat(3) {
                shakeOffset.animateTo(10f, tween(50))
                shakeOffset.animateTo(-10f, tween(50))
            }
            shakeOffset.animateTo(0f, tween(50))
        }
    }

    return shakeOffset.value
}

/**
 * Fade in animation for content
 */
fun fadeInAnimation() = fadeIn(
    animationSpec = tween(400, easing = LinearOutSlowInEasing)
)

/**
 * Fade out animation for content
 */
fun fadeOutAnimation() = fadeOut(
    animationSpec = tween(200, easing = FastOutLinearInEasing)
)

/**
 * Slide up animation for dialogs
 */
fun slideUpAnimation() = slideInVertically(
    initialOffsetY = { it / 2 },
    animationSpec = tween(300, easing = FastOutSlowInEasing)
) + fadeIn(animationSpec = tween(300))

/**
 * Slide down animation for dialogs
 */
fun slideDownAnimation() = slideOutVertically(
    targetOffsetY = { it / 2 },
    animationSpec = tween(200, easing = FastOutLinearInEasing)
) + fadeOut(animationSpec = tween(200))
