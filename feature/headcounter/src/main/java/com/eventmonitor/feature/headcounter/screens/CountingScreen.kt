package com.eventmonitor.feature.headcounter.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
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
import org.koin.androidx.compose.koinViewModel
import com.eventmonitor.core.common.theme.Amber
import com.eventmonitor.core.common.theme.BodySans
import com.eventmonitor.core.common.theme.DataMono
import com.eventmonitor.core.common.theme.MonoTiny
import com.eventmonitor.core.common.theme.Sage
import com.eventmonitor.core.common.theme.Signal
import com.eventmonitor.core.common.ui.ArcadeBackground
import com.eventmonitor.core.common.ui.BrandBlue
import com.eventmonitor.core.common.ui.BrandBlueDeep
import com.eventmonitor.core.common.ui.BrandBlueGlow
import com.eventmonitor.core.common.ui.BrandRed
import com.eventmonitor.core.common.ui.DigitRoll
import com.eventmonitor.core.common.ui.FieldAppBarIcon
import com.eventmonitor.core.common.ui.FieldTokens
import com.eventmonitor.core.common.ui.Hairline
import com.eventmonitor.core.common.ui.HairlineSoft
import com.eventmonitor.core.common.ui.KeyTone
import com.eventmonitor.core.common.ui.SoftAppBar
import com.eventmonitor.core.common.ui.SoftBottomDock
import com.eventmonitor.core.common.ui.SoftCapacityBar
import com.eventmonitor.core.common.ui.SoftCard
import com.eventmonitor.core.common.ui.SoftKey
import com.eventmonitor.core.common.ui.SoftSection
import com.eventmonitor.core.common.ui.softHeadline
import com.eventmonitor.core.common.ui.SoftSnackbar
import com.eventmonitor.core.common.ui.SoftToolButton
import com.eventmonitor.core.common.ui.SoftZoneChip
import com.eventmonitor.core.common.ui.StatStrip
import com.eventmonitor.core.common.utils.rememberHapticFeedback
import com.eventmonitor.core.data.local.entities.EventTypeEntity
import com.eventmonitor.feature.headcounter.seatmap.components.SeatMapView
import com.eventmonitor.feature.headcounter.seatmap.models.Seat
import com.eventmonitor.feature.headcounter.seatmap.models.SeatStatus
import com.eventmonitor.feature.headcounter.seatmap.models.SeatType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun CountingScreen(
    viewModel: CountingViewModel = koinViewModel(),
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
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SoftAppBar(
                title = uiState.branchName.ifBlank { "Headcount" },
                subtitle = uiState.eventName.ifBlank { "Live count" },
                onBack = { haptic.light(); onNavigateBack() },
                onUndo = { haptic.medium(); viewModel.undo() },
                undoEnabled = canUndo,
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
                        viewModel = viewModel,
                        uiState = uiState,
                        activeArea = activeArea,
                        areas = areas,
                        canUndo = canUndo,
                        canRedo = canRedo,
                        onSelectArea = { selectedAreaId = it.id },
                        onBump = { delta ->
                            if (delta >= 0) viewModel.incrementCount(activeArea.id, delta)
                            else viewModel.decrementCount(activeArea.id, -delta)
                        },
                        onToggleInclusion = {
                            haptic.selection()
                            selectedAreaId = activeArea.id
                            viewModel.toggleAreaInclusion(activeArea.id)
                            snackMessage = if (activeArea.isIncluded) {
                                "${activeArea.template.name} excluded from total."
                            } else {
                                "${activeArea.template.name} included in total."
                            }
                        },
                        onUndo = {
                            viewModel.undo()
                            snackMessage = "Undone."
                        },
                        onRedo = { viewModel.redo() },
                        onShare = { viewModel.generateReport() },
                    )
                }
            }

            uiState.shareableReport?.let { report ->
                ServiceReportDialog(
                    report = report,
                    onDismiss = {
                        haptic.light()
                        viewModel.clearReport()
                    },
                )
            }

            // Snackbar dock — float above the gesture bar.
            SoftSnackbar(
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
    viewModel: CountingViewModel,
    uiState: CountingUiState,
    activeArea: AreaCountState,
    areas: List<AreaCountState>,
    canUndo: Boolean,
    canRedo: Boolean,
    onSelectArea: (AreaCountState) -> Unit,
    onBump: (Int) -> Unit,
    onToggleInclusion: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onShare: () -> Unit,
) {
    val locked = uiState.isLocked
    val seatMapped = activeArea.template.hasSeatMap
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

    // Floating "+1" / "−1" score tokens that pop out of the pressed key.
    val tokens = remember { mutableStateListOf<FloatToken>() }
    var tokenSeq by remember { mutableLongStateOf(0L) }

    val bump: (Int) -> Unit = { delta ->
        onBump(delta)
        tokenSeq += 1
        tokens.add(FloatToken(id = tokenSeq, value = delta, fromAdd = delta >= 0))
    }

    val totalPct = if (uiState.totalCapacity > 0) {
        (uiState.totalAttendance.toFloat() / uiState.totalCapacity).coerceIn(0f, 1f)
    } else 0f

    Box(modifier = Modifier.fillMaxSize()) {
        ArcadeBackground(modifier = Modifier.matchParentSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
        ) {
            // ── Top region: stats + zone selector pinned above the hero ─────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(Modifier.height(8.dp))

                // Compact stat strip — date + total/capacity/% in a single mono
                // line, freeing the visual centre for the active-zone tally.
                StatStrip(
                    dateLine = dateFormatter.format(uiState.serviceDate),
                    total = uiState.totalAttendance,
                    capacity = uiState.totalCapacity,
                    pct = totalPct,
                )

                Spacer(Modifier.height(14.dp))

                // Slim total power meter — context glance, not the focal point.
                SoftCapacityBar(
                    pct = totalPct,
                    knownCapacity = uiState.totalCapacity > 0,
                    modifier = Modifier.fillMaxWidth(),
                    segments = 18,
                )

                Spacer(Modifier.height(24.dp))

                // ── Selector rail ────────────────────────────────────────────
                LazyRow(
                    state = chipListState,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(areas, key = { it.id }) { area ->
                        Box(
                            modifier = Modifier.graphicsLayer {
                                alpha = if (area.isIncluded) 1f else 0.42f
                            },
                        ) {
                            SoftZoneChip(
                                label = area.template.name,
                                count = area.count,
                                selected = area.id == activeArea.id,
                                onClick = { onSelectArea(area) },
                            )
                        }
                    }
                }
            }

            // ── Hero: active-zone tally slab, centred between rail and dock ──
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (seatMapped) {
                    SeatTallyPanel(
                        viewModel = viewModel,
                        area = activeArea,
                        onToggleInclusion = onToggleInclusion,
                    )
                } else {
                    TallyPanel(
                        area = activeArea,
                        areas = areas,
                        onSelectArea = onSelectArea,
                        onToggleInclusion = onToggleInclusion,
                        dragOffset = dragOffset,
                    )
                }
            }

            // ── Bottom dock: tools + keypad, pinned for thumb reach ──────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.28f to MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
                            1f to MaterialTheme.colorScheme.background,
                        ),
                    )
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                if (locked) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Signal.copy(alpha = 0.12f))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Signal),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Count locked. Unlock from the event menu to resume.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                if (seatMapped) {
                    // Seat map drives count via taps in the hero panel; the +/− keys
                    // would be misleading here, so show a status hint instead.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    ) {
                        Text(
                            text = "Tap a seat to cycle: AVAILABLE → OCCUPIED → RESERVED → BLOCKED.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                } else {
                    // Keypad — arcade keys with floating tokens emerging upward.
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            SoftKey(
                                label = "−1",
                                caption = "Subtract",
                                tone = KeyTone.Subtract,
                                enabled = !locked && activeArea.count > 0,
                                onTap = { bump(-1) },
                                modifier = Modifier.weight(1f),
                            )
                            SoftKey(
                                label = "+1",
                                caption = "Tap or hold",
                                tone = KeyTone.Add,
                                enabled = !locked,
                                onTap = { bump(1) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        FloatingTokensLayer(
                            tokens = tokens,
                            onComplete = { tokens.remove(it) },
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth(),
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Tool row — small text actions; secondary to the keys above.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SoftToolButton(
                        label = "Undo",
                        glyph = "↶",
                        enabled = canUndo && !locked,
                        modifier = Modifier.weight(1f),
                        onClick = onUndo,
                    )
                    SoftToolButton(
                        label = "Redo",
                        glyph = "↷",
                        enabled = canRedo && !locked,
                        modifier = Modifier.weight(1f),
                        onClick = onRedo,
                    )
                    SoftToolButton(
                        label = "Report",
                        glyph = "≡",
                        enabled = uiState.eventId != null,
                        modifier = Modifier.weight(1f),
                        onClick = onShare,
                    )
                }
            }
        }
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
    onToggleInclusion: () -> Unit,
    dragOffset: Animatable<Float, *>,
) {
    val pct = if (area.capacity > 0) {
        (area.count.toFloat() / area.capacity).coerceIn(0f, 1f)
    } else 0f

    var prev by remember(area.id) { mutableIntStateOf(area.count) }
    LaunchedEffect(area.count) { prev = area.count }

    // Pulse the digit briefly whenever the count changes — tactile feedback.
    val pulse = remember { Animatable(1f) }
    val pulseScope = rememberCoroutineScope()
    LaunchedEffect(area.count, area.id) {
        if (area.count != prev) {
            pulseScope.launch {
                pulse.snapTo(1.06f)
                pulse.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = 0.55f,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                )
            }
        }
    }

    val haptic = rememberHapticFeedback()
    val density = LocalDensity.current
    val commitThreshold = with(density) { 96.dp.toPx() }
    val travelLimit = with(density) { 260.dp.toPx() }
    val flingOffset = with(density) { 360.dp.toPx() }

    val currentIndex = areas.indexOfFirst { it.id == area.id }
    val prevArea = if (currentIndex > 0) areas[currentIndex - 1] else null
    val nextArea = if (currentIndex in 0 until areas.lastIndex) areas[currentIndex + 1] else null

    val scope = rememberCoroutineScope()

    val progressColor by animateColorAsState(
        targetValue = when {
            pct >= 0.95f -> Signal
            pct >= 0.80f -> Amber
            else -> Sage
        },
        animationSpec = tween(durationMillis = 320),
        label = "progressColor",
    )
    val animatedPct by animateFloatAsState(
        targetValue = pct,
        animationSpec = tween(durationMillis = 520, easing = LinearOutSlowInEasing),
        label = "pct",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(BrandBlueDeep, Color(0xFF071A55)),
                ),
            )
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
            .padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Card is always rendered on the deep-blue gradient, so the on-card
            // foreground is pinned to a fixed light color rather than the theme
            // background — otherwise dark mode produces dark-on-dark text.
            val onCard = Color.White
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = area.template.name,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp,
                        letterSpacing = (-0.2).sp,
                    ),
                    color = onCard,
                    modifier = Modifier.weight(1f, fill = false),
                )
                InclusionPill(
                    included = area.isIncluded,
                    onToggle = onToggleInclusion,
                    onCard = onCard,
                )
            }
            Spacer(Modifier.height(14.dp))

            // Soft sans digit — light weight, generous size.
            val digitStyle = MaterialTheme.typography.displayLarge.copy(
                fontFamily = BodySans,
                fontWeight = FontWeight.Light,
                fontSize = 112.sp,
                lineHeight = 112.sp,
                letterSpacing = (-6).sp,
            )

            fun captionFor(a: AreaCountState): String {
                return if (a.capacity > 0) {
                    val p = (a.count.toFloat() / a.capacity).coerceIn(0f, 1f) * 100f
                    String.format(Locale.US, "%d of %d · %.0f%%", a.count, a.capacity, p)
                } else {
                    "${a.count} counted"
                }
            }

            val prevCaption = prevArea?.let { captionFor(it) }
            val nextCaption = nextArea?.let { captionFor(it) }

            // Count row — prev ghost (left), next ghost (right), active (center).
            // All reads of dragOffset happen in graphicsLayer lambdas so the swap
            // + snapTo are applied in the same draw frame (no flicker).
            Box(modifier = Modifier.fillMaxWidth()) {
                if (prevArea != null) {
                    Text(
                        text = prevArea.count.toString(),
                        style = digitStyle,
                        color = onCard.copy(alpha = 0.7f),
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
                        color = onCard.copy(alpha = 0.7f),
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
                    color = onCard,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .graphicsLayer {
                            val p = dragOffset.value
                            translationX = p
                            alpha = 1f - (abs(p) / commitThreshold).coerceIn(0f, 1f) * 0.55f
                            scaleX = pulse.value
                            scaleY = pulse.value
                        },
                )
            }
            Spacer(Modifier.height(14.dp))

            // Rounded capacity track sits directly under the digit so the meter
            // reads as part of the headline, not a footnote.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(onCard.copy(alpha = 0.18f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedPct)
                        .clip(RoundedCornerShape(4.dp))
                        .background(progressColor),
                )
            }

            Spacer(Modifier.height(10.dp))

            // Caption row — soft sans, parallax with the digit on swipe. Now
            // serves as the meter's legend rather than the digit's subtitle.
            val captionStyle = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = DataMono,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                letterSpacing = 0.4.sp,
                fontFeatureSettings = "tnum",
            )
            val captionColor = onCard.copy(alpha = 0.85f)
            Box(modifier = Modifier.fillMaxWidth()) {
                if (prevArea != null && prevCaption != null) {
                    Text(
                        text = prevCaption,
                        style = captionStyle,
                        color = captionColor,
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
                        style = captionStyle,
                        color = captionColor,
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
                    text = captionFor(area),
                    style = captionStyle,
                    color = captionColor,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .graphicsLayer {
                            val p = dragOffset.value
                            translationX = p * 0.55f
                            alpha = 1f - (abs(p) / commitThreshold).coerceIn(0f, 1f) * 0.55f
                        },
                )
            }
        }
    }
}


@Composable
private fun InclusionPill(
    included: Boolean,
    onToggle: () -> Unit,
    onCard: Color,
) {
    val bg by animateColorAsState(
        targetValue = if (included) onCard.copy(alpha = 0.16f) else onCard.copy(alpha = 0.06f),
        animationSpec = tween(220),
        label = "inclusionBg",
    )
    val borderAlpha by animateFloatAsState(
        targetValue = if (included) 0.45f else 0.22f,
        animationSpec = tween(220),
        label = "inclusionBorder",
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(1.dp, onCard.copy(alpha = borderAlpha), RoundedCornerShape(999.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (included) Sage else onCard.copy(alpha = 0.45f)),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (included) "IN TOTAL" else "EXCLUDED",
            style = MonoTiny.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
                fontSize = 11.sp,
            ),
            color = onCard.copy(alpha = if (included) 0.95f else 0.65f),
        )
    }
}

private data class FloatToken(
    val id: Long,
    val value: Int,
    val fromAdd: Boolean,
)

@Composable
private fun FloatingTokensLayer(
    tokens: List<FloatToken>,
    onComplete: (FloatToken) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.height(160.dp)) {
        tokens.forEach { token ->
            key(token.id) {
                FloatingToken(token = token, onComplete = { onComplete(token) })
            }
        }
    }
}

@Composable
private fun FloatingToken(
    token: FloatToken,
    onComplete: () -> Unit,
) {
    // Tokens spawn just above the pressed key (offset toward its half of the row)
    // and float upward toward the score readout, fading on the way.
    val rise = remember { Animatable(0f) }
    val fade = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(token.id) {
        scope.launch {
            rise.animateTo(
                targetValue = -180f,
                animationSpec = tween(720, easing = EaseOutCubic),
            )
        }
        scope.launch {
            delay(220)
            fade.animateTo(0f, tween(480))
        }
        delay(720)
        onComplete()
    }

    val alignment = if (token.fromAdd) Alignment.TopEnd else Alignment.TopStart
    val color = if (token.fromAdd) BrandBlueGlow else BrandRed
    val sign = if (token.value >= 0) "+" else ""

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 36.dp),
    ) {
        Text(
            text = "$sign${token.value}",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = DataMono,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                letterSpacing = (-1).sp,
            ),
            color = color.copy(alpha = fade.value),
            modifier = Modifier
                .align(alignment)
                .graphicsLayer {
                    translationY = rise.value
                    // Tokens drift slightly inward as they rise — feels like
                    // the score is pulling them in.
                    translationX = if (token.fromAdd) -rise.value * 0.08f else rise.value * 0.08f
                    val s = 0.85f + 0.25f * fade.value
                    scaleX = s
                    scaleY = s
                },
        )
    }
}

@Composable
private fun LoadingPanel() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Loading…",
            style = MaterialTheme.typography.bodyMedium,
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(paper)
                .imePadding(),
        ) {
            ArcadeBackground(modifier = Modifier.matchParentSize())
            Column(modifier = Modifier.fillMaxSize()) {
            // ── Masthead ───────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Filing № $filingNo",
                        style = MonoTiny,
                        color = muted,
                    )
                    Text(
                        text = "New count",
                        style = softHeadline(22),
                        color = ink,
                    )
                }
                Spacer(Modifier.width(12.dp))
                SoftToolButton(
                    label = "Cancel",
                    glyph = "×",
                    enabled = true,
                    onClick = { haptic.light(); onDismiss() },
                )
            }

            // ── Body (scrollable) ─────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(Modifier.height(20.dp))

                // Field 01 — TYPE
                SoftSection(title = "Type", eyebrow = "01 · TYPE", hint = "What's on the card")
                Spacer(Modifier.height(14.dp))

                if (eventTypes.isEmpty()) {
                    SoftCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
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
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Field 02 — STAMP
                SoftSection(title = "Timestamp", eyebrow = "02 · STAMP", hint = "Filed at (auto)")
                Spacer(Modifier.height(14.dp))

                SoftCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(2.dp))
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
                }

                Spacer(Modifier.height(20.dp))

                // Field 03 — COUNTER
                SoftSection(
                    title = "Counter",
                    eyebrow = "03 · COUNTER",
                    hint = "Who's on the clicker"
                )
                Spacer(Modifier.height(14.dp))

                CounterInput(
                    value = countedBy,
                    onValueChange = { countedBy = it },
                    focusRequester = focusRequester,
                )

                Spacer(Modifier.height(48.dp))
            }

                // ── Footer CTA ────────────────────────────────────────────────
            val canStart =
                countedBy.isNotBlank() && selectedType != null && eventTypes.isNotEmpty()
                SoftBottomDock(modifier = Modifier.navigationBarsPadding()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                ) {
                        SoftToolButton(
                            label = "Cancel",
                            glyph = "×",
                            enabled = true,
                            modifier = Modifier.weight(1f),
                            onClick = { haptic.light(); onDismiss() },
                    )
                        SoftKey(
                            label = "START COUNT",
                            caption = "Begin",
                            tone = KeyTone.Add,
                            enabled = canStart,
                            onTap = {
                            haptic.success()
                            selectedType?.let { t ->
                                onCreate(t.id, t.name, System.currentTimeMillis(), countedBy)
                            }
                        },
                            modifier = Modifier.weight(2f),
                            repeatable = false,
                        )
                    }
                }
            }
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

// ---------------------------------------------------------------------------
// Seat-mapped tally panel — replaces TallyPanel for areas with hasSeatMap=true.
// ---------------------------------------------------------------------------

@Composable
private fun SeatTallyPanel(
    viewModel: CountingViewModel,
    area: AreaCountState,
    onToggleInclusion: () -> Unit,
) {
    val rows by viewModel.observeSeatLayout(area.template.id)
        .collectAsState(initial = emptyList())
    // observeSeatStatuses returns null if no eventId; treat as empty list.
    val statusFlow = remember(area.template.id) {
        viewModel.observeSeatStatuses(area.template.id)
    }
    val statuses by (statusFlow ?: kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())
    val statusByseat = remember(statuses) { statuses.associateBy { it.seatId } }

    val seats = remember(rows, statusByseat) {
        rows.flatMap { rowWithSeats ->
            rowWithSeats.seats.sortedBy { it.number }.map { seatEntity ->
                val statusName = statusByseat[seatEntity.id]?.status ?: "AVAILABLE"
                Seat(
                    id = seatEntity.id,
                    row = rowWithSeats.row.label,
                    number = seatEntity.number,
                    status = runCatching { SeatStatus.valueOf(statusName) }
                        .getOrDefault(SeatStatus.AVAILABLE),
                    seatType = runCatching { SeatType.valueOf(seatEntity.seatType) }
                        .getOrDefault(SeatType.STANDARD),
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(BrandBlueDeep, Color(0xFF071A55)),
                ),
            )
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            val onCard = Color.White
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = area.template.name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 22.sp,
                            letterSpacing = (-0.2).sp,
                        ),
                        color = onCard,
                    )
                    val occupied = seats.count { it.status == SeatStatus.OCCUPIED }
                    val totalUsable = seats.count { it.status != SeatStatus.BLOCKED }
                    Text(
                        text = "$occupied of $totalUsable seated",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = DataMono,
                            fontFeatureSettings = "tnum",
                        ),
                        color = onCard.copy(alpha = 0.85f),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                InclusionPill(
                    included = area.isIncluded,
                    onToggle = onToggleInclusion,
                    onCard = onCard,
                )
            }
            Spacer(Modifier.height(12.dp))

            if (seats.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No rows configured. Add rows in this zone's editor.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = onCard.copy(alpha = 0.75f),
                    )
                }
            } else {
                Box(modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()) {
                    SeatMapView(
                        seats = seats,
                        onSeatClick = { tapped ->
                            viewModel.cycleSeatStatus(tapped.id, tapped.status.name)
                        },
                    )
                }
            }
        }
    }
}

