package com.eventmonitor.core.common.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.Animatable
import com.eventmonitor.core.common.theme.Amber
import com.eventmonitor.core.common.theme.DataMono
import com.eventmonitor.core.common.theme.MonoTiny
import com.eventmonitor.core.common.utils.HapticFeedbackHelper
import com.eventmonitor.core.common.utils.rememberHapticFeedback
import kotlinx.coroutines.delay
import java.util.Locale

// ---------------------------------------------------------------------------
// Brand palette — pulled from the app icon (shield + figures on electric blue
// with a medical-cross red accent). Used across the Soft kit to keep every
// screen on the same hue family.
// ---------------------------------------------------------------------------
val BrandBlue = Color(0xFF1E54D6)
val BrandBlueDeep = Color(0xFF0B2A82)
val BrandBlueGlow = Color(0xFF4A91E8)
val BrandRed = Color(0xFFE63946)

// ---------------------------------------------------------------------------
// SoftAppBar — the primary top bar for the Soft aesthetic.
//
// Title (semi-bold, 22sp, tight tracking) + muted subtitle, with a circular
// back glyph leading and an optional trailing slot (RowScope) for screen
// actions. Status bar inset is handled internally.
// ---------------------------------------------------------------------------

@Composable
fun SoftAppBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    backGlyph: String = "‹",
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                SoftIconButton(glyph = backGlyph, onClick = onBack)
                Spacer(Modifier.width(8.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp,
                        letterSpacing = (-0.2).sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

/** Convenience overload preserving the headcount undo-button signature. */
@Composable
fun SoftAppBar(
    title: String,
    subtitle: String?,
    onBack: () -> Unit,
    onUndo: () -> Unit,
    undoEnabled: Boolean,
) {
    SoftAppBar(
        title = title,
        subtitle = subtitle,
        onBack = onBack,
        trailing = {
            SoftIconButton(glyph = "↺", onClick = onUndo, enabled = undoEnabled)
        },
    )
}

// ---------------------------------------------------------------------------
// SoftIconButton — 40dp circular glyph button with scale-on-press + soft
// pressed background.
// ---------------------------------------------------------------------------

@Composable
fun SoftIconButton(
    glyph: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "iconScale",
    )
    val bg by animateColorAsState(
        targetValue = if (pressed) {
            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.10f)
        } else {
            Color.Transparent
        },
        label = "iconBg",
    )
    val fg = MaterialTheme.colorScheme.onBackground.copy(alpha = if (enabled) 1f else 0.32f)
    Box(
        modifier = Modifier
            .size(40.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(bg)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = glyph,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal),
            color = fg,
        )
    }
}

// ---------------------------------------------------------------------------
// SoftZoneChip — rounded-rect tab with corner-bracket selector and an
// optional animated count pill. Use without `count` for plain filter chips.
// ---------------------------------------------------------------------------

@Composable
fun SoftZoneChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    count: Int? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val ink = MaterialTheme.colorScheme.onBackground
    val paper = MaterialTheme.colorScheme.background

    val bg by animateColorAsState(
        targetValue = if (selected) BrandBlue else ink.copy(alpha = 0.06f),
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "chipBg",
    )
    val fg by animateColorAsState(
        targetValue = if (selected) paper else ink,
        animationSpec = tween(220),
        label = "chipFg",
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "chipScale",
    )
    val animatedCount by animateIntAsState(
        targetValue = count ?: 0,
        animationSpec = tween(durationMillis = 280, easing = EaseOutCubic),
        label = "chipCount",
    )
    val countAlpha by animateFloatAsState(
        targetValue = if ((count ?: 1) == 0) 0.32f else 1f,
        animationSpec = tween(220),
        label = "chipCountAlpha",
    )
    val labelAlpha = if (selected) 1f else 0.78f

    Box(
        modifier = Modifier
            .scale(scale)
            .heightIn(min = if (count != null) 68.dp else 44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val len = 9.dp.toPx()
                val w = 1.8.dp.toPx()
                val inset = 5.dp.toPx()
                val c = paper
                val x0 = inset
                val y0 = inset
                val x1 = size.width - inset
                val y1 = size.height - inset
                drawLine(c, Offset(x0, y0), Offset(x0 + len, y0), w)
                drawLine(c, Offset(x0, y0), Offset(x0, y0 + len), w)
                drawLine(c, Offset(x1, y0), Offset(x1 - len, y0), w)
                drawLine(c, Offset(x1, y0), Offset(x1, y0 + len), w)
                drawLine(c, Offset(x0, y1), Offset(x0 + len, y1), w)
                drawLine(c, Offset(x0, y1), Offset(x0, y1 - len), w)
                drawLine(c, Offset(x1, y1), Offset(x1 - len, y1), w)
                drawLine(c, Offset(x1, y1), Offset(x1, y1 - len), w)
            }
        }
        Row(
            modifier = Modifier.padding(
                horizontal = if (count != null) 20.dp else 16.dp,
                vertical = if (count != null) 14.dp else 10.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label.uppercase(),
                style = MonoTiny.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.6.sp,
                    fontSize = 14.sp,
                    lineHeight = 16.sp,
                ),
                color = fg.copy(alpha = labelAlpha),
            )
            if (count != null) {
                Spacer(Modifier.width(12.dp))
                Text(
                    text = animatedCount.toString().padStart(2, '0'),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = DataMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        lineHeight = 26.sp,
                        letterSpacing = 0.sp,
                        fontFeatureSettings = "tnum",
                    ),
                    color = fg.copy(alpha = countAlpha),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// SoftKey — the arcade depth-key. Tap or hold (rapid-fire). Tone resolves to
// brand accents and resting face colours.
// ---------------------------------------------------------------------------

enum class KeyTone { Add, Subtract, AddGhost, SubtractGhost }

@Composable
fun SoftKey(
    label: String,
    caption: String,
    tone: KeyTone,
    enabled: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    repeatable: Boolean = true,
    haptic: HapticFeedbackHelper = rememberHapticFeedback(),
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val paper = MaterialTheme.colorScheme.background

    val accent = when (tone) {
        KeyTone.Add -> BrandBlue
        KeyTone.Subtract -> BrandRed
        KeyTone.AddGhost -> BrandBlue
        KeyTone.SubtractGhost -> BrandRed
    }
    val (restBg, restFg, pressedBg) = when (tone) {
        KeyTone.Add -> Triple(BrandBlue, paper, BrandBlueDeep)
        KeyTone.Subtract -> Triple(ink.copy(alpha = 0.06f), ink, BrandRed.copy(alpha = 0.18f))
        KeyTone.AddGhost -> Triple(ink.copy(alpha = 0.06f), ink, ink.copy(alpha = 0.14f))
        KeyTone.SubtractGhost -> Triple(ink.copy(alpha = 0.04f), ink, ink.copy(alpha = 0.12f))
    }

    val currentOnTap by rememberUpdatedState(onTap)
    var pressed by remember { mutableStateOf(false) }

    var ringSeed by remember { mutableIntStateOf(0) }
    val ringProgress = remember { Animatable(1f) }
    LaunchedEffect(ringSeed) {
        if (ringSeed > 0) {
            ringProgress.snapTo(0f)
            ringProgress.animateTo(1f, tween(560, easing = EaseOutCubic))
        }
    }

    val density = LocalDensity.current
    val depthPx = with(density) { 6.dp.toPx() }
    val depthOffset by animateFloatAsState(
        targetValue = if (pressed && enabled) depthPx else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
        label = "depth",
    )
    val faceBg by animateColorAsState(
        targetValue = when {
            !enabled -> ink.copy(alpha = 0.05f)
            pressed -> pressedBg
            else -> restBg
        },
        animationSpec = tween(140),
        label = "keyBg",
    )
    val plateAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.18f,
        animationSpec = tween(180),
        label = "platePct",
    )
    val fg = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
        else -> restFg
    }

    LaunchedEffect(pressed, enabled, repeatable) {
        if (pressed && enabled && repeatable) {
            delay(380)
            var interval = 180L
            while (pressed) {
                currentOnTap()
                ringSeed += 1
                haptic.counter()
                delay(interval)
                if (interval > 50L) interval = (interval * 0.85).toLong().coerceAtLeast(50L)
            }
        }
    }

    LaunchedEffect(enabled) {
        if (!enabled) pressed = false
    }

    val ringStrokePx = with(density) { 2.dp.toPx() }
    val cornerPx = with(density) { 22.dp.toPx() }
    val plateGapPx = with(density) { 8.dp.toPx() }

    Box(
        modifier = modifier.height(112.dp),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(top = with(density) { plateGapPx.toDp() })
                .clip(RoundedCornerShape(22.dp))
                .background(accent.copy(alpha = 0.55f * plateAlpha)),
        )

        if (enabled) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val p = ringProgress.value
                if (p < 1f) {
                    val grow = p * 14f
                    val alpha = (1f - p) * 0.7f
                    drawRoundRect(
                        color = accent.copy(alpha = alpha),
                        topLeft = Offset(-grow, -grow),
                        size = Size(size.width + grow * 2f, size.height + grow * 2f),
                        cornerRadius = CornerRadius(cornerPx + grow, cornerPx + grow),
                        style = Stroke(width = ringStrokePx * (1f - p * 0.4f)),
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(bottom = with(density) { plateGapPx.toDp() })
                .graphicsLayer { translationY = depthOffset }
                .clip(RoundedCornerShape(22.dp))
                .background(faceBg)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectTapGestures(
                        onPress = {
                            pressed = true
                            try {
                                tryAwaitRelease()
                            } finally {
                                pressed = false
                            }
                        },
                        onTap = {
                            haptic.counter()
                            currentOnTap()
                            ringSeed += 1
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val stripeColor = if (tone == KeyTone.Add) {
                    paper.copy(alpha = 0.04f)
                } else {
                    ink.copy(alpha = 0.04f)
                }
                val stride = 6.dp.toPx()
                var y = 0f
                while (y < size.height) {
                    drawRect(
                        color = stripeColor,
                        topLeft = Offset(0f, y),
                        size = Size(size.width, 1f),
                    )
                    y += stride
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontFamily = DataMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 40.sp,
                        letterSpacing = (-1).sp,
                    ),
                    color = fg,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = caption.uppercase(),
                    style = MonoTiny.copy(letterSpacing = 1.6.sp),
                    color = fg.copy(alpha = 0.7f),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// SoftPrimaryButton — clean filled rounded primary CTA used everywhere
// OUTSIDE the counting screen. No arcade depth plate, no expanding ring, no
// scanlines, no rapid-fire-on-hold — those tactile flourishes belong to
// SoftKey on the keypad. This is the calm sibling: 56dp height, 14dp
// corners, filled brand colour, single press-scale.
//
// Tones: Primary = BrandBlue · Destructive = BrandRed · Ghost = transparent
// ink-tinted (used on dialog footers when the action is opt-in).
// ---------------------------------------------------------------------------

enum class SoftButtonTone { Primary, Destructive, Ghost }

@Composable
fun SoftPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tone: SoftButtonTone = SoftButtonTone.Primary,
    trailingGlyph: String? = "→",
) {
    val haptic = rememberHapticFeedback()
    val ink = MaterialTheme.colorScheme.onBackground
    val paper = MaterialTheme.colorScheme.background

    val (bgRest, bgPressed, fg) = when (tone) {
        SoftButtonTone.Primary -> Triple(BrandBlue, BrandBlueDeep, paper)
        SoftButtonTone.Destructive -> Triple(BrandRed, BrandRed.copy(alpha = 0.78f), paper)
        SoftButtonTone.Ghost -> Triple(ink.copy(alpha = 0.06f), ink.copy(alpha = 0.12f), ink)
    }

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "primaryScale",
    )
    val bg by animateColorAsState(
        targetValue = when {
            !enabled -> ink.copy(alpha = 0.08f)
            pressed -> bgPressed
            else -> bgRest
        },
        animationSpec = tween(160),
        label = "primaryBg",
    )
    val labelColor = if (!enabled) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    } else fg

    Box(
        modifier = modifier
            .scale(scale)
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
            ) {
                haptic.medium()
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    letterSpacing = 0.4.sp,
                ),
                color = labelColor,
            )
            if (!trailingGlyph.isNullOrBlank()) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = trailingGlyph,
                    style = MaterialTheme.typography.titleMedium,
                    color = labelColor,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// SoftToolButton — secondary 14dp rounded text-with-glyph button. Used in
// tool rows beneath the keypad / on dialog footers for non-primary actions.
// ---------------------------------------------------------------------------

@Composable
fun SoftToolButton(
    label: String,
    glyph: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val haptic = rememberHapticFeedback()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val ink = MaterialTheme.colorScheme.onBackground

    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "toolScale",
    )
    val bg by animateColorAsState(
        targetValue = if (pressed && enabled) ink.copy(alpha = 0.10f) else ink.copy(alpha = 0.04f),
        animationSpec = tween(140),
        label = "toolBg",
    )
    val fg = ink.copy(alpha = if (enabled) 1f else 0.35f)

    Box(
        modifier = modifier
            .scale(scale)
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
            ) {
                haptic.light(); onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = glyph,
                style = MaterialTheme.typography.titleMedium,
                color = fg,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                ),
                color = fg,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// SoftCapacityBar — segmented power meter. Top segments pulse only when fill
// crosses the danger line.
// ---------------------------------------------------------------------------

@Composable
fun SoftCapacityBar(
    pct: Float,
    knownCapacity: Boolean,
    modifier: Modifier = Modifier,
    segments: Int = 14,
) {
    val ink = MaterialTheme.colorScheme.onBackground

    val infinite = rememberInfiniteTransition(label = "powerBar")
    val dangerPulse by infinite.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(720, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dangerPulse",
    )
    val animatedPct by animateFloatAsState(
        targetValue = pct,
        animationSpec = tween(durationMillis = 480, easing = LinearOutSlowInEasing),
        label = "capPct",
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(segments) { i ->
            val segHigh = (i + 1) / segments.toFloat()
            val active = knownCapacity && animatedPct >= segHigh - (1f / segments)
            val baseColor = when {
                segHigh >= 0.93f -> BrandRed
                segHigh >= 0.78f -> Amber
                else -> BrandBlue
            }
            val target = when {
                !active -> ink.copy(alpha = 0.10f)
                pct >= 0.95f && segHigh > 0.78f -> baseColor.copy(alpha = dangerPulse)
                else -> baseColor
            }
            val color by animateColorAsState(
                targetValue = target,
                animationSpec = tween(220),
                label = "seg",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// SoftSnackbar — inverted-card 16dp rounded toast. Optional Undo CTA.
// ---------------------------------------------------------------------------

@Composable
fun SoftSnackbar(
    message: String?,
    onUndo: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = message != null,
        enter = slideInVertically(
            initialOffsetY = { it / 2 },
            animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
        ) + fadeIn(tween(180)),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(140)),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.onBackground)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = message.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.weight(1f),
            )
            if (onUndo != null) {
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Undo",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.14f))
                        .clickable { onUndo() }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// StatStrip — slim mono-typed live-dot row. Total + capacity ratio.
// Use `dotColor` to retune the heartbeat per screen (red = live count,
// amber = warning, sage = safe, etc.).
// ---------------------------------------------------------------------------

@Composable
fun StatStrip(
    dateLine: String,
    total: Int,
    capacity: Int,
    pct: Float,
    label: String = "TOTAL",
    dotColor: Color = BrandRed,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val ink = MaterialTheme.colorScheme.onBackground
    val animatedTotal by animateIntAsState(
        targetValue = total,
        animationSpec = tween(durationMillis = 380, easing = EaseOutCubic),
        label = "stripTotal",
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val infinite = rememberInfiniteTransition(label = "live")
        val liveAlpha by infinite.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "liveAlpha",
        )
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(dotColor.copy(alpha = liveAlpha)),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = dateLine.uppercase(),
            style = MonoTiny.copy(
                fontSize = 14.sp,
                lineHeight = 16.sp,
                letterSpacing = 1.6.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = muted,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = animatedTotal.toString().padStart(3, '0'),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = DataMono,
                fontWeight = FontWeight.Bold,
                fontSize = 56.sp,
                lineHeight = 60.sp,
                letterSpacing = (-1.2).sp,
                fontFeatureSettings = "tnum",
            ),
            color = ink,
        )
        Spacer(Modifier.width(14.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = label,
                style = MonoTiny.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    lineHeight = 15.sp,
                    letterSpacing = 2.0.sp,
                ),
                color = muted,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (capacity > 0) {
                    "/${capacity} · ${String.format(Locale.US, "%.0f%%", pct * 100f)}"
                } else "/ —",
                style = MonoTiny.copy(
                    fontSize = 16.sp,
                    lineHeight = 18.sp,
                    letterSpacing = 0.8.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = muted,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// ArcadeBackground — radial dot grid + concentric ring sweep canvas. Apply
// behind any screen content with `Box { ArcadeBackground(); content }`.
// ---------------------------------------------------------------------------

@Composable
fun ArcadeBackground(modifier: Modifier = Modifier) {
    val accent = BrandBlue

    val infinite = rememberInfiniteTransition(label = "arcadeBg")
    val ring by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ringPulse",
    )
    val drift by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(11000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "drift",
    )

    Canvas(modifier = modifier) {
        val step = 26.dp.toPx()
        val dotR = 1.1.dp.toPx()
        val cx = size.width / 2f
        val cy = size.height * 0.32f
        val maxR = size.maxDimension * 1.1f

        val cols = (size.width / step).toInt() + 2
        val rows = (size.height / step).toInt() + 2
        val driftPx = drift * step
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val x = c * step - driftPx * 0.5f
                val y = r * step + driftPx * 0.3f
                val dx = x - cx
                val dy = y - cy
                val d = kotlin.math.sqrt(dx * dx + dy * dy)
                val falloff = (1f - (d / maxR).coerceIn(0f, 1f)) * 0.32f + 0.08f
                drawCircle(
                    color = accent.copy(alpha = falloff),
                    radius = dotR,
                    center = Offset(x, y),
                )
            }
        }

        val ringR = maxR * ring
        drawCircle(
            color = BrandBlueGlow.copy(alpha = (1f - ring) * 0.30f),
            radius = ringR,
            center = Offset(cx, cy),
            style = Stroke(width = 1.dp.toPx()),
        )
        drawCircle(
            color = accent.copy(alpha = (1f - ring) * 0.18f),
            radius = ringR * 0.62f,
            center = Offset(cx, cy),
            style = Stroke(width = 1.dp.toPx()),
        )
    }
}

// ---------------------------------------------------------------------------
// SoftBottomDock — convenience container that fades from transparent to the
// background colour, used to anchor a CTA / tool row at the bottom of a
// screen (matches the dock pattern in CountingScreen).
// ---------------------------------------------------------------------------

@Composable
fun SoftBottomDock(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.28f to MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
                    1f to MaterialTheme.colorScheme.background,
                ),
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        content = content,
    )
}

// ---------------------------------------------------------------------------
// SoftLivePulseDot — heartbeat indicator. Default colour is BrandRed (live
// counts, recording, alarms); pass `BrandBlue` for "ready/paired" states or
// `Sage` for "safe/closed". Matches the dot used in StatStrip so every
// "active" indicator across the app shares the same heartbeat.
// ---------------------------------------------------------------------------

@Composable
fun SoftLivePulseDot(
    modifier: Modifier = Modifier,
    color: Color = BrandRed,
    size: Int = 8,
) {
    val infinite = rememberInfiniteTransition(label = "livePulse")
    val alpha by infinite.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "liveAlpha",
    )
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha)),
    )
}

// ---------------------------------------------------------------------------
// Body type — sans-serif semi-bold tight-tracked headline used in screen
// bodies in place of the editorial Fraunces serif. Matches SoftAppBar's
// title weight so screen content stays consistent with the chrome.
// ---------------------------------------------------------------------------

@Composable
fun softHeadline(size: Int = 22): androidx.compose.ui.text.TextStyle =
    MaterialTheme.typography.titleLarge.copy(
        fontWeight = FontWeight.SemiBold,
        fontSize = size.sp,
        lineHeight = (size + 4).sp,
        letterSpacing = (-0.2).sp,
    )

@Composable
fun softLabelStyle(): androidx.compose.ui.text.TextStyle =
    MonoTiny.copy(
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.6.sp,
    )

// ---------------------------------------------------------------------------
// SoftCard — replacement for the FIELD `border(Hair, ink) + background(paper)`
// rectangle. Soft rounded fill (ink 4-8% alpha), optional press scale and
// BrandBlue selected treatment. Use anywhere a card or list row was
// previously framed by hairlines.
// ---------------------------------------------------------------------------

@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    cornerRadius: Int = 16,
    contentPadding: androidx.compose.foundation.layout.PaddingValues =
        androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val ink = MaterialTheme.colorScheme.onBackground

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && onClick != null) 0.985f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
        label = "cardScale",
    )
    val bg by animateColorAsState(
        targetValue = when {
            selected -> BrandBlue.copy(alpha = 0.92f)
            else -> ink.copy(alpha = 0.04f)
        },
        animationSpec = tween(220),
        label = "cardBg",
    )

    // Expanding-ring "thock" overlay — same arcade response as SoftKey but
    // tuned softer (smaller grow, shorter ring, lower alpha) so list rows
    // don't shout when tapped.
    var ringSeed by remember { mutableIntStateOf(0) }
    val ringProgress = remember { Animatable(1f) }
    LaunchedEffect(ringSeed) {
        if (ringSeed > 0) {
            ringProgress.snapTo(0f)
            ringProgress.animateTo(1f, tween(440, easing = EaseOutCubic))
        }
    }
    val density = LocalDensity.current
    val ringStrokePx = with(density) { 1.5.dp.toPx() }
    val cornerPx = with(density) { cornerRadius.dp.toPx() }
    val accent = if (selected) BrandBlueGlow else BrandBlue

    val base = modifier
        .scale(scale)
        .clip(RoundedCornerShape(cornerRadius.dp))
        .background(bg)

    val withClick = if (onClick != null) {
        base.clickable(
            interactionSource = interaction,
            indication = null,
            onClick = {
                ringSeed += 1
                onClick()
            },
        )
    } else base

    Box(modifier = withClick) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content,
        )
        if (onClick != null) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val p = ringProgress.value
                if (p < 1f) {
                    val grow = p * 8f
                    val alpha = (1f - p) * 0.35f
                    drawRoundRect(
                        color = accent.copy(alpha = alpha),
                        topLeft = Offset(-grow, -grow),
                        size = Size(size.width + grow * 2f, size.height + grow * 2f),
                        cornerRadius = CornerRadius(cornerPx + grow, cornerPx + grow),
                        style = Stroke(width = ringStrokePx * (1f - p * 0.4f)),
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// SoftSection — replacement for IntakeFieldHead and similar editorial
// section headers. Mono uppercase eyebrow + sans-serif semi-bold title +
// optional muted hint underneath.
// ---------------------------------------------------------------------------

@Composable
fun SoftSection(
    title: String,
    eyebrow: String? = null,
    hint: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (!eyebrow.isNullOrBlank()) {
            Text(
                text = eyebrow.uppercase(),
                style = softLabelStyle(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
        }
        Text(
            text = title,
            style = softHeadline(20),
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (!hint.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = hint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// SoftAlertDialog — replacement for Material AlertDialog. Uses ArcadeBackground
// behind the body, softHeadline title, BrandBlue/BrandRed primary CTA via
// SoftKey, and SoftToolButton for dismiss. The body scrolls if needed so it
// works for both short confirmations and longer forms (pass content slot).
// ---------------------------------------------------------------------------

@Composable
fun SoftAlertDialog(
    onDismiss: () -> Unit,
    title: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    eyebrow: String? = null,
    message: String? = null,
    dismissLabel: String = "Cancel",
    confirmTone: SoftButtonTone = SoftButtonTone.Primary,
    confirmEnabled: Boolean = true,
    body: (@Composable ColumnScope.() -> Unit)? = null,
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .padding(24.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.background),
        ) {
            ArcadeBackground(modifier = Modifier.matchParentSize())
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            ) {
                if (!eyebrow.isNullOrBlank()) {
                    Text(
                        text = eyebrow.uppercase(),
                        style = softLabelStyle(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                }
                Text(
                    text = title,
                    style = softHeadline(22),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (!message.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (body != null) {
                    Spacer(Modifier.height(14.dp))
                    body()
                }
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SoftToolButton(
                        label = dismissLabel,
                        glyph = "×",
                        enabled = true,
                        modifier = Modifier.weight(1f),
                        onClick = onDismiss,
                    )
                    SoftPrimaryButton(
                        label = confirmLabel,
                        onClick = onConfirm,
                        enabled = confirmEnabled,
                        tone = confirmTone,
                        modifier = Modifier.weight(2f),
                        trailingGlyph = null,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// softEnter — staggered fade + slide-up entrance, applied as a modifier so
// callers can compose it with their own layout modifiers. Pass an `index`
// (0-based) for stagger; first item is delay 0, each subsequent +60ms.
// ---------------------------------------------------------------------------

@Composable
fun Modifier.softEnter(index: Int = 0, baseDelayMs: Int = 60): Modifier {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay((index * baseDelayMs).toLong())
        visible = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 360, easing = EaseOutCubic),
        label = "enterAlpha",
    )
    val translate by animateFloatAsState(
        targetValue = if (visible) 0f else 20f,
        animationSpec = tween(durationMillis = 360, easing = EaseOutCubic),
        label = "enterTranslate",
    )
    return this.graphicsLayer {
        this.alpha = alpha
        this.translationY = translate * density
    }
}

// ---------------------------------------------------------------------------
// SoftScreen — convenience wrapper that paints ArcadeBackground beneath the
// content and clips to the screen bounds. Use as the top-level child of a
// Scaffold's body.
// ---------------------------------------------------------------------------

@Composable
fun SoftScreen(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        ArcadeBackground(modifier = Modifier.matchParentSize())
        content()
    }
}
