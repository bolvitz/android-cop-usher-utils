package com.eventmonitor.app.presentation.navigation

import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild

/**
 * Height of the floating tab bar (with its margin and the system bar inset), so tab screens
 * can pad their scroll content and let it pass beneath the glass. Zero on pushed screens.
 */
val LocalFloatingBarInset = staticCompositionLocalOf { 0.dp }

internal data class GlassTabItem(val label: String, val icon: ImageVector, val badge: Int = 0)

/**
 * Floating frosted tab bar with a lens that slides (and briefly stretches) to the selected tab.
 * [hazeState] must belong to a `haze` sibling drawn beneath the bar, never an ancestor.
 * Below Android 12 the blur falls back to a near-opaque tint.
 */
@Composable
internal fun GlassTabs(
    tabs: List<GlassTabItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    val glass = rememberGlassColors()
    val shape = RoundedCornerShape(GlassCorner)
    Box(modifier.glassPane(hazeState, shape, glass).padding(GlassInset)) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(TabHeight)) {
            val tabWidth = maxWidth / tabs.size
            Lens(
                offset = tabWidth * selectedIndex.coerceAtLeast(0),
                visible = selectedIndex >= 0,
                glass = glass,
                modifier = Modifier.width(tabWidth).fillMaxHeight()
            )
            Row(Modifier.selectableGroup()) {
                tabs.forEachIndexed { index, tab ->
                    GlassTab(
                        tab = tab,
                        selected = index == selectedIndex,
                        onClick = { onSelect(index) },
                        glass = glass,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
private fun Lens(offset: Dp, visible: Boolean, glass: GlassColors, modifier: Modifier = Modifier) {
    val position by animateDpAsState(
        targetValue = offset,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 400f),
        label = "lensOffset"
    )
    // The lens stretches along its travel while it moves, then settles back.
    val stretch = remember { Animatable(0f) }
    val reduceMotion = isReducedMotionEnabled()
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(offset) {
        if (first) {
            first = false
            return@LaunchedEffect
        }
        if (reduceMotion) return@LaunchedEffect
        stretch.snapTo(0f)
        stretch.animateTo(0f, keyframes {
            durationMillis = LENS_STRETCH_MILLIS
            1f at LENS_STRETCH_MILLIS / 2
        })
    }
    val alpha by animateFloatAsState(if (visible) 1f else 0f, label = "lensAlpha")
    val shape = RoundedCornerShape(GlassCorner - GlassInset)
    Box(
        modifier
            .offset { IntOffset(position.roundToPx(), 0) }
            .graphicsLayer {
                this.alpha = alpha
                scaleX = 1f + (LENS_STRETCH - 1f) * stretch.value
            }
            .clip(shape)
            .background(glass.lens)
            .border(1.dp, glass.lensRim, shape)
    )
}

@Composable
private fun GlassTab(
    tab: GlassTabItem,
    selected: Boolean,
    onClick: () -> Unit,
    glass: GlassColors,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "tabPress"
    )
    val tint by animateColorAsState(if (selected) glass.selected else glass.unselected, label = "tabTint")
    val a11y = if (tab.badge > 0) "${tab.label}, ${tab.badge}" else tab.label

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(GlassCorner - GlassInset))
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = interaction,
                indication = ripple(color = glass.selected)
            )
            .semantics(mergeDescendants = true) { contentDescription = a11y }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BadgedBox(badge = { if (tab.badge > 0) Badge { Text(tab.badge.toString()) } }) {
            Icon(tab.icon, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
        }
        Text(
            tab.label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}

private fun Modifier.glassPane(hazeState: HazeState, shape: Shape, glass: GlassColors): Modifier =
    this
        .shadow(elevation = 18.dp, shape = shape, ambientColor = glass.shadow, spotColor = glass.shadow)
        .clip(shape)
        .hazeChild(state = hazeState, style = glass.style)
        .border(1.dp, glass.rim, shape)

@Stable
private class GlassColors(
    val style: HazeStyle,
    val rim: Brush,
    val lens: Color,
    val lensRim: Brush,
    val shadow: Color,
    val selected: Color,
    val unselected: Color
)

@Composable
private fun rememberGlassColors(): GlassColors {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.surface.luminance() < 0.5f
    return remember(scheme, dark) {
        GlassColors(
            style = HazeStyle(
                backgroundColor = scheme.surface,
                tints = listOf(HazeTint(scheme.surface.copy(alpha = if (dark) 0.5f else 0.45f))),
                blurRadius = 24.dp,
                noiseFactor = 0.05f,
                fallbackTint = HazeTint(scheme.surface.copy(alpha = 0.94f))
            ),
            rim = Brush.verticalGradient(
                if (dark) listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.04f))
                else listOf(Color.White.copy(alpha = 0.9f), scheme.outlineVariant.copy(alpha = 0.7f))
            ),
            lens = scheme.primary.copy(alpha = if (dark) 0.18f else 0.09f),
            lensRim = Brush.verticalGradient(
                if (dark) listOf(Color.White.copy(alpha = 0.18f), Color.Transparent)
                else listOf(Color.White.copy(alpha = 0.95f), scheme.primary.copy(alpha = 0.08f))
            ),
            shadow = if (dark) Color.Black.copy(alpha = 0.6f) else scheme.primary.copy(alpha = 0.35f),
            selected = scheme.primary,
            unselected = scheme.onSurfaceVariant
        )
    }
}

/** True when the system animator scale is off ("Remove animations"). */
@Composable
private fun isReducedMotionEnabled(): Boolean {
    val context = LocalContext.current
    return remember {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE)
        }.getOrDefault(1f) == 0f
    }
}

private const val LENS_STRETCH = 1.15f
private const val LENS_STRETCH_MILLIS = 260

private val GlassCorner = 30.dp
private val GlassInset = 5.dp
private val TabHeight = 58.dp
