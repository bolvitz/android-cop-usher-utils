package com.eventmonitor.feature.headcounter.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.eventmonitor.core.common.theme.Amber
import com.eventmonitor.core.common.theme.MonoTiny
import com.eventmonitor.core.common.theme.Sage
import com.eventmonitor.core.common.theme.Signal
import com.eventmonitor.core.common.ui.DigitRoll
import com.eventmonitor.core.common.ui.FieldAppBar
import com.eventmonitor.core.common.ui.FieldAppBarIcon
import com.eventmonitor.core.common.ui.FieldKeyButton
import com.eventmonitor.core.common.ui.FieldTokens
import com.eventmonitor.core.common.ui.Hairline
import com.eventmonitor.core.common.ui.HairlineSoft
import com.eventmonitor.core.common.ui.KeyVariant
import com.eventmonitor.core.common.ui.SevPill
import com.eventmonitor.core.common.ui.Severity
import com.eventmonitor.core.common.ui.ZoneChip
import com.eventmonitor.core.common.utils.rememberHapticFeedback
import com.eventmonitor.core.data.local.entities.EventTypeEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun CountingScreen(
    viewModel: CountingViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
) {
    val haptic = rememberHapticFeedback()
    val uiState by viewModel.uiState.collectAsState()
    val eventTypes by viewModel.eventTypes.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()

    var showCreateDialog by remember { mutableStateOf(uiState.eventId == null) }
    var selectedAreaId by remember { mutableStateOf<String?>(null) }
    var snackMessage by remember { mutableStateOf<String?>(null) }

    // Choose the active zone: last-tapped, else first included, else first.
    val areas = uiState.areaCounts
    val activeArea = remember(areas, selectedAreaId) {
        areas.firstOrNull { it.id == selectedAreaId }
            ?: areas.firstOrNull { it.isIncluded }
            ?: areas.firstOrNull()
    }

    // Auto-dismiss snackbar.
    LaunchedEffect(snackMessage) {
        if (snackMessage != null) {
            delay(2400)
            snackMessage = null
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        // FieldAppBar pads itself against the status bar; body handles bottom.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            FieldAppBar(
                title = uiState.branchName.ifBlank { "Headcount" },
                eyebrow = uiState.eventName.ifBlank { "zone · live" },
                leading = {
                    FieldAppBarIcon("‹", onClick = {
                        haptic.light(); onNavigateBack()
                    })
                },
                trailing = {
                    FieldAppBarIcon("↺", enabled = canUndo, onClick = {
                        haptic.medium(); viewModel.undo()
                    })
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                showCreateDialog -> {
                    CreateEventDialog(
                        eventTypes = eventTypes,
                        onDismiss = {
                            showCreateDialog = false
                            if (uiState.eventId == null) onNavigateBack()
                        },
                        onCreate = { typeId, typeName, date, counter ->
                            viewModel.createNewService(typeId, typeName, date, counter)
                            showCreateDialog = false
                        },
                    )
                }

                uiState.isLoading -> {
                    LoadingPanel()
                }

                activeArea == null -> {
                    EmptyPanel()
                }

                else -> {
                    CountBody(
                        uiState = uiState,
                        activeArea = activeArea,
                        areas = areas,
                        canUndo = canUndo,
                        canRedo = canRedo,
                        onSelectArea = { selectedAreaId = it.id },
                        onBump = { delta ->
                            if (delta >= 0) viewModel.incrementCount(activeArea.id, delta)
                            else viewModel.decrementCount(activeArea.id, -delta)
                            snackMessage =
                                "${if (delta >= 0) "+" else ""}$delta on ${activeArea.template.name}"
                        },
                        onUndo = {
                            viewModel.undo()
                            snackMessage = "Undone."
                        },
                        onRedo = { viewModel.redo() },
                        onShare = { viewModel.shareReport() },
                    )
                }
            }

            // Snackbar dock — float above the gesture bar.
            FieldSnackbar(
                message = snackMessage,
                onUndo = {
                    viewModel.undo()
                    snackMessage = "Undone."
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(16.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Body composition
// ---------------------------------------------------------------------------

@Composable
private fun CountBody(
    uiState: CountingUiState,
    activeArea: AreaCountState,
    areas: List<AreaCountState>,
    canUndo: Boolean,
    canRedo: Boolean,
    onSelectArea: (AreaCountState) -> Unit,
    onBump: (Int) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onShare: () -> Unit,
) {
    val locked = uiState.isLocked
    val scroll = rememberScrollState()
    val dateFormatter = remember { SimpleDateFormat("EEE d MMM · HH:mm", Locale.getDefault()) }

    // Shared horizontal-swipe state. The tally panel drives it via drag gestures,
    // while the zone chip rail mirrors it with parallax so both tracks move as one.
    val dragOffset = remember { Animatable(0f) }
    val chipListState = rememberLazyListState()

    // Auto-center the active chip when it changes (after a swipe or explicit tap).
    LaunchedEffect(activeArea.id, areas) {
        val idx = areas.indexOfFirst { it.id == activeArea.id }
        if (idx >= 0) chipListState.animateScrollToItem(idx)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(12.dp))

        // Crumbs + editorial header
        Text(
            text = "VENUES › ${uiState.branchName.uppercase().ifBlank { "—" }} › HEADCOUNT",
            style = MonoTiny,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = uiState.eventName.ifBlank { "Live count" },
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = dateFormatter.format(uiState.serviceDate).uppercase(),
                    style = MonoTiny,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = "TOTAL",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = uiState.totalAttendance.toString(),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (uiState.totalCapacity > 0) {
                    Text(
                        text = "/ ${uiState.totalCapacity}",
                        style = MonoTiny,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        HairlineSoft()
        Spacer(Modifier.height(12.dp))

        // Zone chip rail — stationary during tally drags; auto-scrolls to centre
        // the active chip once a swipe commits (or an explicit tap changes zones).
        LazyRow(
            state = chipListState,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(areas, key = { it.id }) { area ->
                ZoneChip(
                    label = area.template.name,
                    selected = area.id == activeArea.id,
                    onClick = { onSelectArea(area) },
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Zone head
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activeArea.template.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "ZONE · ${activeArea.template.name.uppercase()}",
                    style = MonoTiny,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                val capText = if (activeArea.capacity > 0) activeArea.capacity.toString() else "—"
                Text(
                    "CAP $capText",
                    style = MonoTiny,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${activeArea.count} / $capText",
                    style = MonoTiny,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        TallyPanel(
            area = activeArea,
            areas = areas,
            onSelectArea = onSelectArea,
            dragOffset = dragOffset,
        )

        Spacer(Modifier.height(12.dp))

        // Keypad (2x2)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FieldKeyButton(
                label = "−1",
                caption = "Subtract",
                variant = KeyVariant.Minus,
                enabled = !locked && activeArea.count > 0,
                onTap = { onBump(-1) },
                modifier = Modifier.weight(1f),
            )
            FieldKeyButton(
                label = "+1",
                caption = "Tap · Hold = rapid",
                variant = KeyVariant.Plus,
                enabled = !locked,
                onTap = { onBump(1) },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FieldKeyButton(
                label = "−5",
                caption = "Bulk out",
                variant = KeyVariant.Minus,
                enabled = !locked && activeArea.count > 0,
                repeatable = false,
                onTap = { onBump(-5) },
                modifier = Modifier.weight(1f),
            )
            FieldKeyButton(
                label = "+5",
                caption = "Group in",
                variant = KeyVariant.Plus,
                enabled = !locked,
                repeatable = false,
                onTap = { onBump(5) },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(10.dp))

        // Tool row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ToolButton(
                label = "↶ UNDO",
                enabled = canUndo && !locked,
                modifier = Modifier.weight(1f),
                onClick = onUndo,
            )
            ToolButton(
                label = "↷ REDO",
                enabled = canRedo && !locked,
                modifier = Modifier.weight(1f),
                onClick = onRedo,
            )
            ToolButton(
                label = "SHARE",
                enabled = true,
                modifier = Modifier.weight(1f),
                onClick = onShare,
            )
        }

        if (locked) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SevPill(Severity.CRITICAL, "LOCKED")
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "This count is sealed. Unlock from the event menu to resume.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(28.dp))
    }
}

// ---------------------------------------------------------------------------
// Tally panel — black slab, big Fraunces-slot count, progress bar.
// ---------------------------------------------------------------------------

@Composable
private fun TallyPanel(
    area: AreaCountState,
    areas: List<AreaCountState>,
    onSelectArea: (AreaCountState) -> Unit,
    dragOffset: Animatable<Float, *>,
) {
    val pct = if (area.capacity > 0) {
        (area.count.toFloat() / area.capacity).coerceIn(0f, 1f)
    } else 0f
    val progressColor = when {
        pct >= 0.95f -> Signal
        pct >= 0.80f -> Amber
        else -> Signal
    }
    val animatedPct by animateFloatAsState(
        targetValue = pct,
        animationSpec = tween(durationMillis = 500),
        label = "pct",
    )

    var prev by remember(area.id) { mutableIntStateOf(area.count) }
    LaunchedEffect(area.count) { prev = area.count }

    val haptic = rememberHapticFeedback()
    val density = LocalDensity.current
    val commitThreshold = with(density) { 96.dp.toPx() }
    val travelLimit = with(density) { 260.dp.toPx() }
    val flingOffset = with(density) { 360.dp.toPx() }

    val currentIndex = areas.indexOfFirst { it.id == area.id }
    val prevArea = if (currentIndex > 0) areas[currentIndex - 1] else null
    val nextArea = if (currentIndex in 0 until areas.lastIndex) areas[currentIndex + 1] else null

    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.onBackground)
            .pointerInput(area.id, areas) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        scope.launch { dragOffset.stop() }
                    },
                    onDragEnd = {
                        val off = dragOffset.value
                        scope.launch {
                            when {
                                off <= -commitThreshold && nextArea != null -> {
                                    haptic.selection()
                                    // Continuity: new active starts where the ghost was.
                                    val continuePos = flingOffset + off
                                    onSelectArea(nextArea)
                                    dragOffset.snapTo(continuePos)
                                    dragOffset.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = 0.82f,
                                            stiffness = Spring.StiffnessMediumLow,
                                        ),
                                    )
                                }

                                off >= commitThreshold && prevArea != null -> {
                                    haptic.selection()
                                    val continuePos = -flingOffset + off
                                    onSelectArea(prevArea)
                                    dragOffset.snapTo(continuePos)
                                    dragOffset.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = 0.82f,
                                            stiffness = Spring.StiffnessMediumLow,
                                        ),
                                    )
                                }

                                else -> {
                                    dragOffset.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = 0.62f,
                                            stiffness = Spring.StiffnessMedium,
                                        ),
                                    )
                                }
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            dragOffset.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = 0.62f,
                                    stiffness = Spring.StiffnessMedium,
                                ),
                            )
                        }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        val target = dragOffset.value + dragAmount
                        val resisted = when {
                            target < 0f && nextArea == null -> target * 0.28f
                            target > 0f && prevArea == null -> target * 0.28f
                            else -> target
                        }.coerceIn(-travelLimit, travelLimit)
                        scope.launch { dragOffset.snapTo(resisted) }
                    },
                )
            }
            .padding(horizontal = 16.dp, vertical = 20.dp),
    ) {
        // Corner crosshairs
        Text(
            "+",
            style = MonoTiny,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.align(Alignment.TopStart),
        )
        Text(
            "+",
            style = MonoTiny,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.align(Alignment.TopEnd),
        )
        Text(
            "+",
            style = MonoTiny,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.align(Alignment.BottomStart),
        )
        Text(
            "+",
            style = MonoTiny,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.align(Alignment.BottomEnd),
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val digitStyle = MaterialTheme.typography.displayLarge.copy(
                fontSize = FieldTokens.TallyFontSize,
                lineHeight = FieldTokens.TallyLineHeight,
            )
            val prevCaption = prevArea?.let {
                if (it.capacity > 0) {
                    "${
                        ((it.count.toFloat() / it.capacity).coerceIn(
                            0f,
                            1f
                        ) * 100).toInt()
                    }% OF CAPACITY"
                } else "NO CAPACITY SET"
            }
            val nextCaption = nextArea?.let {
                if (it.capacity > 0) {
                    "${
                        ((it.count.toFloat() / it.capacity).coerceIn(
                            0f,
                            1f
                        ) * 100).toInt()
                    }% OF CAPACITY"
                } else "NO CAPACITY SET"
            }

            // Count row — prev ghost (left), next ghost (right), active (center).
            // All reads of dragOffset happen in graphicsLayer lambdas so the swap
            // + snapTo are applied in the same draw frame (no flicker).
            Box(modifier = Modifier.fillMaxWidth()) {
                if (prevArea != null) {
                    Text(
                        text = prevArea.count.toString(),
                        style = digitStyle,
                        color = MaterialTheme.colorScheme.background,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .graphicsLayer {
                                val p = dragOffset.value
                                translationX = -flingOffset + p
                                alpha = (p / commitThreshold).coerceIn(0f, 1f) * 0.45f
                            },
                    )
                }
                if (nextArea != null) {
                    Text(
                        text = nextArea.count.toString(),
                        style = digitStyle,
                        color = MaterialTheme.colorScheme.background,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .graphicsLayer {
                                val p = dragOffset.value
                                translationX = flingOffset + p
                                alpha = (-p / commitThreshold).coerceIn(0f, 1f) * 0.45f
                            },
                    )
                }
                DigitRoll(
                    value = area.count,
                    previous = prev,
                    style = digitStyle,
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .graphicsLayer {
                            val p = dragOffset.value
                            translationX = p
                            alpha = 1f - (abs(p) / commitThreshold).coerceIn(0f, 1f) * 0.55f
                        },
                )
            }
            Spacer(Modifier.height(6.dp))

            // Caption row — same tri-layer pattern, with parallax (0.55×).
            Box(modifier = Modifier.fillMaxWidth()) {
                if (prevArea != null && prevCaption != null) {
                    Text(
                        text = prevCaption,
                        style = MonoTiny,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .graphicsLayer {
                                val p = dragOffset.value
                                translationX = (-flingOffset + p) * 0.55f
                                alpha = (p / commitThreshold).coerceIn(0f, 1f) * 0.45f
                            },
                    )
                }
                if (nextArea != null && nextCaption != null) {
                    Text(
                        text = nextCaption,
                        style = MonoTiny,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .graphicsLayer {
                                val p = dragOffset.value
                                translationX = (flingOffset + p) * 0.55f
                                alpha = (-p / commitThreshold).coerceIn(0f, 1f) * 0.45f
                            },
                    )
                }
                Text(
                    text = if (area.capacity > 0) {
                        "${(pct * 100).toInt()}% OF CAPACITY"
                    } else {
                        "NO CAPACITY SET"
                    },
                    style = MonoTiny,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .graphicsLayer {
                            val p = dragOffset.value
                            translationX = p * 0.55f
                            alpha = 1f - (abs(p) / commitThreshold).coerceIn(0f, 1f) * 0.55f
                        },
                )
            }

            Spacer(Modifier.height(14.dp))

            // Progress track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedPct)
                        .background(progressColor),
                )
            }

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("0", style = MonoTiny, color = MaterialTheme.colorScheme.outline)
                Text(
                    text = area.capacity.takeIf { it > 0 }?.toString() ?: "—",
                    style = MonoTiny,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Small bits
// ---------------------------------------------------------------------------

@Composable
private fun ToolButton(
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val haptic = rememberHapticFeedback()
    val ink = MaterialTheme.colorScheme.onBackground
    val alpha = if (enabled) 1f else 0.4f
    Box(
        modifier = modifier
            .height(FieldTokens.ToolHeight)
            .border(FieldTokens.Hair, ink.copy(alpha = alpha))
            .clickable(enabled = enabled) {
                haptic.light(); onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = ink.copy(alpha = alpha),
        )
    }
}

@Composable
private fun FieldSnackbar(
    message: String?,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = message != null,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.onBackground)
                .border(FieldTokens.Hair, MaterialTheme.colorScheme.outline)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = message.orEmpty(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "UNDO",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier
                    .border(FieldTokens.Hair, MaterialTheme.colorScheme.outline)
                    .clickable { onUndo() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun LoadingPanel() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "LOADING · PLEASE STAND BY",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyPanel() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "No zones configured.",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Add a zone template to this venue to begin counting.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---------------------------------------------------------------------------
// Create Event dialog — FIELD intake sheet (full-bleed, editorial).
// ---------------------------------------------------------------------------

@Composable
fun CreateEventDialog(
    eventTypes: List<EventTypeEntity>,
    onDismiss: () -> Unit,
    onCreate: (String, String, Long, String) -> Unit,
) {
    val haptic = rememberHapticFeedback()
    var selectedType by remember { mutableStateOf(eventTypes.firstOrNull()) }
    var countedBy by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val openedAt = remember { System.currentTimeMillis() }
    val filingNo = remember {
        val base = (openedAt / 1000L) % 1000L
        base.toString().padStart(3, '0')
    }
    val stampFmt = remember { SimpleDateFormat("EEE d MMM · HH:mm", Locale.getDefault()) }

    LaunchedEffect(eventTypes) {
        if (selectedType == null) selectedType = eventTypes.firstOrNull()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val ink = MaterialTheme.colorScheme.onBackground
        val paper = MaterialTheme.colorScheme.background
        val muted = MaterialTheme.colorScheme.onSurfaceVariant
        val outline = MaterialTheme.colorScheme.outline

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(paper)
                .imePadding(),
        ) {
            // ── Masthead ───────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FieldAppBarIcon("×", onClick = { haptic.light(); onDismiss() })
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "INTAKE · FILING № $filingNo",
                        style = MonoTiny,
                        color = muted,
                    )
                    Text(
                        text = "New count",
                        style = MaterialTheme.typography.headlineSmall,
                        color = ink,
                    )
                }
                Box(
                    modifier = Modifier
                        .border(FieldTokens.Hair, outline)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                ) {
                    Text("DRAFT", style = MonoTiny, color = muted)
                }
            }
            Hairline()

            // ── Body (scrollable) ─────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(Modifier.height(28.dp))

                // Editorial lede
                Text(
                    text = "Open a",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 44.sp,
                        lineHeight = 44.sp,
                        fontWeight = FontWeight.Light,
                    ),
                    color = ink,
                )
                Text(
                    text = "ledger.",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 44.sp,
                        lineHeight = 44.sp,
                        fontStyle = FontStyle.Italic,
                    ),
                    color = ink,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Pick a type, stamp the counter, start the tally.".uppercase(),
                    style = MonoTiny,
                    color = muted,
                )

                Spacer(Modifier.height(26.dp))
                HairlineSoft()
                Spacer(Modifier.height(22.dp))

                // Field 01 — TYPE
                IntakeFieldHead("01", "TYPE", "What's on the card")
                Spacer(Modifier.height(14.dp))

                if (eventTypes.isEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(FieldTokens.Hair, Signal)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Signal),
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "NO TYPES ON FILE",
                                style = MaterialTheme.typography.labelSmall,
                                color = Signal,
                            )
                            Text(
                                "Configure event types before opening a count.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ink,
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(FieldTokens.Hair, ink),
                    ) {
                        eventTypes.forEachIndexed { idx, type ->
                            TypePickRow(
                                index = idx + 1,
                                name = type.name,
                                day = type.dayType,
                                time = type.time,
                                selected = selectedType?.id == type.id,
                                onClick = {
                                    haptic.selection()
                                    selectedType = type
                                },
                            )
                            if (idx < eventTypes.lastIndex) HairlineSoft()
                        }
                    }
                }

                Spacer(Modifier.height(26.dp))
                HairlineSoft()
                Spacer(Modifier.height(22.dp))

                // Field 02 — STAMP
                IntakeFieldHead("02", "STAMP", "Filed at (auto)")
                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(FieldTokens.Hair, outline)
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(Sage),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stampFmt.format(Date(openedAt)).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = ink,
                        )
                        Text(
                            text = "LIVE · CAPTURED AT OPEN",
                            style = MonoTiny,
                            color = muted,
                        )
                    }
                    Text("NOW", style = MonoTiny, color = ink)
                }

                Spacer(Modifier.height(26.dp))
                HairlineSoft()
                Spacer(Modifier.height(22.dp))

                // Field 03 — COUNTER
                IntakeFieldHead("03", "COUNTER", "Who's on the clicker")
                Spacer(Modifier.height(14.dp))

                CounterInput(
                    value = countedBy,
                    onValueChange = { countedBy = it },
                    focusRequester = focusRequester,
                )

                Spacer(Modifier.height(48.dp))
            }

            // ── Footer CTA slab ───────────────────────────────────────────
            Hairline()
            val canStart =
                countedBy.isNotBlank() && selectedType != null && eventTypes.isNotEmpty()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .clickable { haptic.light(); onDismiss() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "CANCEL",
                        style = MaterialTheme.typography.labelLarge,
                        color = ink,
                    )
                }
                Box(
                    modifier = Modifier
                        .width(FieldTokens.Hair)
                        .height(64.dp)
                        .background(ink),
                )
                Box(
                    modifier = Modifier
                        .weight(2f)
                        .height(64.dp)
                        .background(if (canStart) ink else outline.copy(alpha = 0.25f))
                        .clickable(enabled = canStart) {
                            haptic.success()
                            selectedType?.let { t ->
                                onCreate(t.id, t.name, System.currentTimeMillis(), countedBy)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "START COUNT",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (canStart) paper else muted,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "→",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (canStart) paper else muted,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IntakeFieldHead(index: String, label: String, hint: String) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = index,
            style = MaterialTheme.typography.displaySmall.copy(
                fontSize = 30.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Light,
            ),
            color = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.padding(bottom = 2.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = hint.uppercase(),
                style = MonoTiny,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TypePickRow(
    index: Int,
    name: String,
    day: String,
    time: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val paper = MaterialTheme.colorScheme.background
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val bg = if (selected) ink else paper
    val fg = if (selected) paper else ink
    val subFg = if (selected) paper.copy(alpha = 0.6f) else muted

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = index.toString().padStart(2, '0'),
            style = MonoTiny,
            color = subFg,
            modifier = Modifier.width(28.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = fg,
            )
            Text(
                text = "${day.uppercase()} · ${time.uppercase()}",
                style = MonoTiny,
                color = subFg,
            )
        }
        Text(
            text = if (selected) "●" else "○",
            style = MaterialTheme.typography.titleMedium,
            color = fg,
        )
    }
}

@Composable
private fun CounterInput(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Column {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineSmall.copy(color = ink),
            cursorBrush = SolidColor(ink),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .padding(vertical = 10.dp),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = "Your name",
                            style = MaterialTheme.typography.headlineSmall,
                            color = muted.copy(alpha = 0.5f),
                        )
                    }
                    inner()
                }
            },
        )
        Hairline()
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("SIGN OFF · REQUIRED", style = MonoTiny, color = muted)
            Text(
                text = if (value.isBlank()) "—" else "${value.length} CHAR",
                style = MonoTiny,
                color = muted,
            )
        }
    }
}

