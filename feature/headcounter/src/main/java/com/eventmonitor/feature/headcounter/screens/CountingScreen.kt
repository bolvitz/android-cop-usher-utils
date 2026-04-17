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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.eventmonitor.core.common.theme.Amber
import com.eventmonitor.core.common.theme.MonoTiny
import com.eventmonitor.core.common.theme.Signal
import com.eventmonitor.core.common.ui.DigitRoll
import com.eventmonitor.core.common.ui.FieldAppBar
import com.eventmonitor.core.common.ui.FieldAppBarIcon
import com.eventmonitor.core.common.ui.FieldKeyButton
import com.eventmonitor.core.common.ui.FieldTokens
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
// Create Event dialog — FIELD-styled.
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventDialog(
    eventTypes: List<EventTypeEntity>,
    onDismiss: () -> Unit,
    onCreate: (String, String, Long, String) -> Unit,
) {
    val haptic = rememberHapticFeedback()
    var selectedType by remember { mutableStateOf(eventTypes.firstOrNull()) }
    var countedBy by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(eventTypes) {
        if (selectedType == null) selectedType = eventTypes.firstOrNull()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        title = {
            Column {
                Text(
                    "NEW · EVENT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text("Start counting.", style = MaterialTheme.typography.headlineSmall)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (eventTypes.isEmpty()) {
                    Text(
                        "No event types configured. Configure types before starting a count.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                    ) {
                        OutlinedTextField(
                            value = selectedType?.let { "${it.name} — ${it.dayType} ${it.time}" }
                                ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = {
                                Text(
                                    "EVENT TYPE",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                        ) {
                            eventTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                type.name,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                "${type.dayType} · ${type.time}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    },
                                    onClick = {
                                        haptic.selection()
                                        selectedType = type
                                        expanded = false
                                    },
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = countedBy,
                        onValueChange = { countedBy = it },
                        label = { Text("COUNTED BY", style = MaterialTheme.typography.labelSmall) },
                        placeholder = {
                            Text(
                                "Your name",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    haptic.success()
                    selectedType?.let { type ->
                        onCreate(type.id, type.name, System.currentTimeMillis(), countedBy)
                    }
                },
                enabled = countedBy.isNotBlank() && selectedType != null && eventTypes.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.background,
                ),
            ) {
                Text("START →", style = MaterialTheme.typography.labelMedium)
            }
        },
        dismissButton = {
            TextButton(onClick = { haptic.light(); onDismiss() }) {
                Text("CANCEL", style = MaterialTheme.typography.labelMedium)
            }
        },
    )
}

