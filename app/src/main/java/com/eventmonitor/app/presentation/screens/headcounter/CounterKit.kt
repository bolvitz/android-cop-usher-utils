package com.eventmonitor.app.presentation.screens.headcounter

import android.app.Activity
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.text.format.DateUtils
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntOffset
import androidx.core.view.WindowCompat
import com.eventmonitor.core.common.theme.CounterDigits
import com.eventmonitor.core.common.utils.AnimationSpecs
import com.eventmonitor.shared.design.DesignTokens
import kotlin.math.roundToInt

// Counter colours, read from the theme the counter forces to dark. Names match iOS CounterPalette.
internal object CounterPalette {
    val ground: Color @Composable get() = MaterialTheme.colorScheme.background
    val plate: Color @Composable get() = MaterialTheme.colorScheme.surfaceContainer
    val plateHigh: Color @Composable get() = MaterialTheme.colorScheme.surfaceContainerHigh
    val brass: Color @Composable get() = MaterialTheme.colorScheme.primary
    val brassInk: Color @Composable get() = MaterialTheme.colorScheme.onPrimary
    val chalk: Color @Composable get() = MaterialTheme.colorScheme.onBackground
    val pew: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
    val signal: Color @Composable get() = MaterialTheme.colorScheme.error
    val line: Color @Composable get() = chalk.copy(alpha = DesignTokens.LINE_ALPHA.toFloat())

    /** Bar and status colour for a fill percentage; the status text carries the same meaning. */
    @Composable
    fun level(percent: Int): Color = when {
        percent >= 100 -> signal
        percent >= 90 -> brass
        else -> chalk
    }
}

/** Condensed figures at a fixed size: counts never grow with the font scale, like iOS `fixedSize`. */
@Composable
internal fun digitStyle(size: Dp, bold: Boolean = false): TextStyle {
    val density = LocalDensity.current
    return remember(size, bold, density) {
        val sp: TextUnit = with(density) { size.toSp() }
        TextStyle(
            fontFamily = CounterDigits,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = sp,
            lineHeight = sp,
            lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
        )
    }
}

/** Percent with no capacity treated as 0, so empty zones never divide by zero. */
internal fun percent(count: Int, capacity: Int): Int =
    if (capacity > 0) (count * 100.0 / capacity).roundToInt() else 0

/** True when a fill level crosses 90% or 100% on the way up. */
internal fun crossedWarning(old: Int, new: Int): Boolean =
    (old < 90 && new >= 90) || (old < 100 && new >= 100)

/** Hold-to-repeat timing: first repeat after [HOLD_DELAY_MS], then each gap shrinks to a floor. */
internal const val HOLD_DELAY_MS = 420L
internal fun nextHoldInterval(current: Double): Double = maxOf(70.0, current * 0.86)
internal const val FIRST_HOLD_INTERVAL_MS = 220.0

/** True when the system animator scale is off, the Android equivalent of iOS Reduce Motion. */
@Composable
internal fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/** Count start time as shown on the intake screen and in History, e.g. "Fri, Nov 6, 10:58 PM". */
internal fun formatCountDate(context: Context, millis: Long): String = DateUtils.formatDateTime(
    context, millis,
    DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_WEEKDAY or DateUtils.FORMAT_SHOW_DATE or
        DateUtils.FORMAT_ABBREV_MONTH or DateUtils.FORMAT_NO_YEAR or DateUtils.FORMAT_SHOW_TIME,
)

/**
 * The counter's dark theme tints the activity's system bar icons; this puts back the app's own
 * appearance when the calling screen or dialog leaves. Read before the dark theme composes.
 */
@Composable
internal fun RestoreSystemBarsOnExit() {
    val view = LocalView.current
    val bars = remember(view) {
        (view.context as? Activity)?.window?.let { WindowCompat.getInsetsController(it, view) }
    }
    val light = remember(bars) { bars?.isAppearanceLightStatusBars to bars?.isAppearanceLightNavigationBars }
    DisposableEffect(bars) {
        onDispose {
            light.first?.let { bars?.isAppearanceLightStatusBars = it }
            light.second?.let { bars?.isAppearanceLightNavigationBars = it }
        }
    }
}

@Composable
internal fun EmptyState(icon: ImageVector, title: String, detail: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = CounterPalette.pew, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = CounterPalette.chalk)
        Text(detail, style = MaterialTheme.typography.bodyMedium, color = CounterPalette.pew, textAlign = TextAlign.Center)
    }
}

// MARK: Haptics

/** Same named set as iOS CounterHaptics, mapped to the closest Android constants. */
internal class CounterHaptics(private val view: View) {
    private fun play(constant: Int) { view.performHapticFeedback(constant) }
    fun tick() = play(HapticFeedbackConstants.CLOCK_TICK)
    fun soft() = play(
        if (Build.VERSION.SDK_INT >= 27) HapticFeedbackConstants.KEYBOARD_RELEASE
        else HapticFeedbackConstants.KEYBOARD_TAP
    )
    fun select() = play(
        if (Build.VERSION.SDK_INT >= 34) HapticFeedbackConstants.SEGMENT_TICK
        else HapticFeedbackConstants.CLOCK_TICK
    )
    fun warning() = play(
        if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.REJECT
        else HapticFeedbackConstants.LONG_PRESS
    )
    fun success() = play(
        if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM
        else HapticFeedbackConstants.VIRTUAL_KEY
    )
    fun heavy() = play(HapticFeedbackConstants.LONG_PRESS)
}

@Composable
internal fun rememberCounterHaptics(): CounterHaptics {
    val view = LocalView.current
    return remember(view) { CounterHaptics(view) }
}

// MARK: Odometer

/** Mechanical counter: each digit wheel rolls to its new value. No leading zeros. */
@Composable
internal fun Odometer(
    value: Int,
    size: Dp,
    modifier: Modifier = Modifier,
    color: Color = CounterPalette.chalk,
    reduceMotion: Boolean = false,
) {
    val v = maxOf(0, value)
    val digits = v.toString().length
    val style = digitStyle(size)
    Row(
        modifier.clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(-size * 0.02f),
    ) {
        // Keyed from the units up, so the units wheel stays put when a digit is added.
        for (place in digits - 1 downTo 0) {
            key(place) {
                var pow = 1
                repeat(place) { pow *= 10 }
                DigitWheel((v / pow) % 10, size, style, color, reduceMotion)
            }
        }
    }
}

@Composable
private fun DigitWheel(digit: Int, size: Dp, style: TextStyle, color: Color, reduceMotion: Boolean) {
    val position by animateFloatAsState(
        targetValue = digit.toFloat(),
        animationSpec = if (reduceMotion) snap() else AnimationSpecs.roll,
        label = "digitWheel",
    )
    val density = LocalDensity.current
    Box(
        Modifier
            .width(size * 0.5f)
            .height(size)
            .clipToBounds(),
    ) {
        Column(
            Modifier.offset { IntOffset(0, -(position * with(density) { size.toPx() }).roundToInt()) }
                .wrapContentHeight(unbounded = true, align = Alignment.Top),
        ) {
            for (n in 0..9) {
                Box(Modifier.width(size * 0.5f).height(size), contentAlignment = Alignment.Center) {
                    // Unbounded so tall glyph metrics overflow evenly instead of clipping at the bottom.
                    Text("$n", style = style, color = color, softWrap = false, maxLines = 1, modifier = Modifier.wrapContentHeight(unbounded = true))
                }
            }
        }
    }
}

// MARK: Level bar

/** Capacity bar with a tick at 90%. */
@Composable
internal fun LevelBar(percent: Int, height: Dp, color: Color, modifier: Modifier = Modifier) {
    val fill by animateFloatAsState(
        targetValue = minOf(percent, 100) / 100f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 195f),
        label = "levelBar",
    )
    val tick = CounterPalette.pew.copy(alpha = 0.7f)
    BoxWithConstraints(modifier.height(height)) {
        Box(Modifier.fillMaxSize().background(CounterPalette.plate, CircleShape))
        Box(Modifier.fillMaxHeight().fillMaxWidth(fill).background(color, CircleShape))
        Box(
            Modifier
                .offset(x = maxWidth * 0.9f - 1.dp, y = (-3).dp)
                .width(2.dp)
                .height(height + 6.dp)
                .background(tick),
        )
    }
}

// MARK: Pill

/** Capsule button for secondary actions: the Android stand-in for the iOS glass pill. */
@Composable
internal fun CounterPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = CounterPalette.plate,
    content: @Composable RowScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, AnimationSpecs.press, label = "pill")
    Row(
        modifier
            .graphicsLayer {
                scaleX = scale; scaleY = scale
                alpha = if (enabled) 1f else 0.38f
            }
            .heightIn(min = DesignTokens.MIN_TOUCH.dp)
            .background(tint, RoundedCornerShape(50))
            .clickable(interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides CounterPalette.chalk) {
            content()
        }
    }
}
