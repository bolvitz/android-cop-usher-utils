package com.eventmonitor.app.presentation.screens.headcounter

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eventmonitor.core.common.theme.HeadCounterTheme
import com.eventmonitor.shared.presentation.headcounter.AreaCountState
import com.eventmonitor.shared.presentation.headcounter.CountingUiState
import com.eventmonitor.shared.presentation.headcounter.CountingViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.util.Date
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Head counter, the usher's clicker: total and zone rail on top, a big odometer per zone,
 * and a thumb pad at the bottom (tap, hold to repeat, or swipe up/down). Matches the iOS
 * HeadCounterView. Dark always, for dim halls.
 */
@Composable
fun SharedCountingScreen(
    venueId: String,
    onBack: () -> Unit,
    eventId: String? = null,
    viewModel: CountingViewModel = koinViewModel { parametersOf(venueId, eventId) },
) {
    val view = LocalView.current
    RestoreSystemBarsOnExit()
    DisposableEffect(view) {
        // Counting can take a while; keep the screen on so ushers don't lose their place.
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    HeadCounterTheme(darkTheme = true) {
        // Surface supplies chalk as the content colour for every Text without its own.
        Surface(color = CounterPalette.ground) {
            CountingContent(viewModel, onBack)
        }
    }
}

private data class SnackMessage(val id: Int, val text: String, val undoInclusionOf: String?)

private data class FloatToken(val id: Int, val value: Int, val x: Float)

/** A zone's number travelling between the zone and the total when it is included or left out. */
private data class FlyToken(val id: Int, val text: String, val joining: Boolean)

/** Where the total and zone odometers sit, in root coordinates; Zero until laid out. */
private class FlyAnchors {
    var origin = Offset.Zero
    var total = Offset.Zero
    var zone = Offset.Zero
    fun relative(point: Offset) = if (point == Offset.Zero) Offset.Zero else point - origin
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountingContent(viewModel: CountingViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val eventTypes by viewModel.eventTypes.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()
    val haptics = rememberCounterHaptics()
    val context = LocalContext.current
    val reduceMotion = rememberReduceMotion()

    var selectedAreaId by rememberSaveable { mutableStateOf<String?>(null) }
    var forward by remember { mutableStateOf(true) }
    var snack by remember { mutableStateOf<SnackMessage?>(null) }
    val tokens = remember { mutableStateListOf<FloatToken>() }
    var seq by remember { mutableIntStateOf(0) }
    var lockedAt by remember { mutableStateOf<Long?>(null) }
    var fly by remember { mutableStateOf<FlyToken?>(null) }
    var showSeatMap by remember { mutableStateOf(false) }
    // Plain fields, not state: they change every layout pass while a zone slides, and only the fly reads them.
    val anchors = remember { FlyAnchors() }
    val lastPercents = remember { mutableMapOf<String, Int>() }

    // Last-picked zone, else the first included one, else the first. Reads state at call time, so
    // gesture callbacks created on an earlier composition still see the current zone.
    fun activeNow(): AreaCountState? {
        val list = state.areaCounts
        return list.firstOrNull { it.id == selectedAreaId } ?: list.firstOrNull { it.isIncluded } ?: list.firstOrNull()
    }

    val areas = state.areaCounts
    val active = activeNow()
    val title = state.venueName.ifEmpty { "Head count" }

    // References to local functions compare equal across compositions, so Compose may keep an old one;
    // select and step therefore read live state instead of the captured areas and active.
    fun select(area: AreaCountState) {
        val list = state.areaCounts
        val current = activeNow()
        if (area.id == current?.id) return
        forward = list.indexOfFirst { it.id == area.id } > list.indexOfFirst { it.id == current?.id }
        haptics.select()
        selectedAreaId = area.id
    }

    fun step(delta: Int) {
        val list = state.areaCounts
        val index = list.indexOfFirst { it.id == activeNow()?.id }
        list.getOrNull(index + delta)?.let(::select)
    }

    fun bump(area: AreaCountState, delta: Int) {
        if (state.isLocked || (delta < 0 && area.count <= 0)) return
        haptics.tick()
        if (delta >= 0) viewModel.incrementCount(area.id, delta) else viewModel.decrementCount(area.id, -delta)
        seq += 1
        val x = if (delta > 0) Random.nextDouble(0.5, 0.85) else Random.nextDouble(0.1, 0.22)
        tokens += FloatToken(seq, delta, x.toFloat())
    }

    fun toggleInclusion(area: AreaCountState) {
        // The snackbar's Undo can outlive a lock; a locked count must not change.
        if (state.isLocked) return
        haptics.select()
        selectedAreaId = area.id
        val joining = !area.isIncluded
        viewModel.toggleAreaInclusion(area.id)
        seq += 1
        if (area.count > 0 && !reduceMotion) {
            fly = FlyToken(seq, "${if (joining) "+" else "−"}${area.count}", joining)
        }
        val name = area.template.name
        snack = SnackMessage(
            seq,
            if (joining) "$name counted in the total." else "$name left out of the total.",
            area.id,
        )
    }

    fun toggleLock() {
        if (state.isLocked) {
            haptics.soft()
            viewModel.unlockEvent()
        } else {
            lockedAt = System.currentTimeMillis()
            haptics.heavy()
            viewModel.lockEvent()
        }
    }

    // One warning buzz when a zone or the total crosses 90% or 100% on the way up.
    LaunchedEffect(areas.map { Triple(it.id, it.count, it.isIncluded) }, state.totalAttendance) {
        var warn = false
        val next = areas.associate { it.id to percent(it.count, it.capacity) } +
            ("total" to percent(state.totalAttendance, state.totalCapacity))
        next.forEach { (id, pct) -> lastPercents[id]?.let { if (crossedWarning(it, pct)) warn = true } }
        if (warn) haptics.warning()
        lastPercents.clear()
        lastPercents.putAll(next)
    }

    LaunchedEffect(snack?.id) {
        if (snack == null) return@LaunchedEffect
        delay(3200)
        snack = null
    }

    state.error?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::clearError,
            confirmButton = { TextButton(onClick = viewModel::clearError) { Text("OK") } },
            title = { Text("Something went wrong") },
            text = { Text(message) },
        )
    }

    if (!state.isLoading && state.eventId == null) {
        NewCountScreen(
            venueName = title,
            eventTypes = eventTypes,
            onCancel = onBack,
            onStart = { type, countedBy ->
                viewModel.createNewEvent(type?.id, type?.name ?: "Head Count", System.currentTimeMillis(), countedBy)
            },
        )
        return
    }

    if (showSeatMap && active != null && active.template.hasSeatMap) {
        SeatMapScreen(viewModel, active.template.id, active.template.name, state.isLocked) { showSeatMap = false }
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(CounterPalette.ground),
    ) {
        CenterAlignedTopAppBar(
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(subtitle(state, context), style = MaterialTheme.typography.bodySmall, color = CounterPalette.pew, maxLines = 1)
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            },
            actions = {
                if (state.eventId != null) {
                    IconButton(onClick = ::toggleLock) {
                        Crossfade(state.isLocked, label = "lockIcon") { locked ->
                            Icon(
                                if (locked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                                contentDescription = if (locked) "Unlock count" else "Lock count",
                                tint = if (locked) CounterPalette.brass else CounterPalette.chalk,
                            )
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
        )

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .onGloballyPositioned { anchors.origin = it.positionInRoot() },
        ) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = CounterPalette.chalk)
                active == null -> EmptyState(
                    Icons.Filled.GridOn,
                    "No zones yet",
                    "Add a zone to this venue from Manage Areas, then come back to count.",
                    Modifier.align(Alignment.Center),
                )
                else -> {
                    RoomLevel(percent(active.count, active.capacity), active.isIncluded, Modifier.matchParentSize())
                    Column(Modifier.fillMaxSize().navigationBarsPadding()) {
                        TotalHeader(
                            state = state,
                            reduceMotion = reduceMotion,
                            modifier = Modifier.padding(horizontal = 20.dp).padding(top = 4.dp),
                            onPlaced = { anchors.total = it },
                        )
                        ZoneRail(areas, active.id, ::select, Modifier.padding(top = 10.dp))
                        ZoneStage(
                            areas = areas,
                            active = active,
                            forward = forward,
                            locked = state.isLocked,
                            reduceMotion = reduceMotion,
                            onStep = ::step,
                            onToggle = ::toggleInclusion,
                            onPlaced = { anchors.zone = it },
                            modifier = Modifier.weight(1f),
                        )
                        Pad(
                            area = active,
                            locked = state.isLocked,
                            lockedAt = lockedAt,
                            tokens = tokens,
                            reduceMotion = reduceMotion,
                            onBump = { bump(active, it) },
                            onOpenSeats = { showSeatMap = true },
                            onUnlock = ::toggleLock,
                            onTokenDone = { id -> tokens.removeAll { it.id == id } },
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                        if (!active.template.hasSeatMap) {
                            Text(
                                "Swipe up or down on the pad to count without looking.",
                                style = MaterialTheme.typography.bodySmall,
                                color = CounterPalette.pew,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(top = 10.dp).padding(horizontal = 24.dp),
                            )
                        }
                        Dock(
                            canUndo = canUndo && !state.isLocked,
                            canRedo = canRedo && !state.isLocked,
                            onUndo = viewModel::undo,
                            onRedo = viewModel::redo,
                            onReport = viewModel::generateReport,
                            modifier = Modifier.padding(horizontal = 16.dp).padding(top = 12.dp, bottom = 8.dp),
                        )
                    }
                    fly?.let { token ->
                        FlyOverlay(
                            token,
                            anchors.relative(anchors.total),
                            anchors.relative(anchors.zone),
                        ) { if (fly?.id == token.id) fly = null }
                    }
                    CountSnackbar(snack, Modifier.align(Alignment.TopCenter)) { areaId ->
                        areas.firstOrNull { it.id == areaId }?.let(::toggleInclusion)
                    }
                }
            }
        }
    }

    state.report?.let { report ->
        HeadCountReceiptDialog(
            report = report,
            onDismiss = viewModel::clearReport,
            notes = state.notes,
            onNotesChange = viewModel::updateNotes,
        )
    }

}

private fun subtitle(state: CountingUiState, context: android.content.Context): String {
    val name = state.eventName.ifEmpty { "Live count" }
    if (state.eventDate <= 0) return name
    return "$name, started ${DateFormat.getTimeFormat(context).format(Date(state.eventDate))}"
}

// MARK: Total

@Composable
private fun TotalHeader(state: CountingUiState, reduceMotion: Boolean, modifier: Modifier, onPlaced: (Offset) -> Unit) {
    val total = state.totalAttendance
    val capacity = state.totalCapacity
    val pct = percent(total, capacity)
    val status = if (capacity > 0 && total > capacity) "${total - capacity} over" else "$pct% full"

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("In the room", style = MaterialTheme.typography.bodySmall, color = CounterPalette.pew)
        Row(verticalAlignment = Alignment.Bottom) {
            Odometer(
                total, 64.dp, reduceMotion = reduceMotion,
                modifier = Modifier
                    .onGloballyPositioned { onPlaced(it.boundsInRoot().center) }
                    .semantics { contentDescription = "$total"; testTag = "totalAttendance" },
            )
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(bottom = 8.dp)) {
                Text(
                    if (capacity > 0) status else "No capacity set",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = CounterPalette.level(pct),
                )
                if (capacity > 0) {
                    Text("of $capacity capacity", style = MaterialTheme.typography.bodySmall, color = CounterPalette.pew)
                }
            }
        }
        LevelBar(
            pct, 8.dp, CounterPalette.level(pct),
            Modifier.fillMaxWidth().semantics {
                contentDescription = "Capacity"
                stateDescription = if (capacity > 0) "$pct percent" else "Unknown"
            },
        )
    }
}

// MARK: Zone rail

@Composable
private fun ZoneRail(areas: List<AreaCountState>, activeId: String, onSelect: (AreaCountState) -> Unit, modifier: Modifier) {
    val list = rememberLazyListState()
    LaunchedEffect(activeId, areas.size) {
        val index = areas.indexOfFirst { it.id == activeId }.takeIf { it >= 0 } ?: return@LaunchedEffect
        list.animateScrollToItem(index)
        val info = list.layoutInfo
        val item = info.visibleItemsInfo.firstOrNull { it.index == index } ?: return@LaunchedEffect
        val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
        list.animateScrollBy((item.offset + item.size / 2 - center).toFloat())
    }
    LazyRow(
        modifier,
        state = list,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(areas, key = { it.id }) { area -> ZoneChip(area, area.id == activeId) { onSelect(area) } }
    }
}

@Composable
private fun ZoneChip(area: AreaCountState, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) CounterPalette.chalk else CounterPalette.plate, tween(220), label = "chipBg")
    val fg = when {
        selected -> CounterPalette.ground
        area.isIncluded -> CounterPalette.chalk
        else -> CounterPalette.pew
    }
    Row(
        Modifier
            .height(40.dp)
            .clip(CircleShape)
            .background(bg)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = "${area.template.name}, ${area.count}" +
                    if (area.isIncluded) "" else ", left out of total"
            }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Name and count take different tones so the number reads first, like the stage odometer.
        Text(
            area.template.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (selected) fg.copy(alpha = 0.7f) else fg,
        )
        Text(
            "${area.count}",
            style = digitStyle(19.dp),
            color = if (!selected && area.isIncluded) CounterPalette.brass else fg,
            textDecoration = if (area.isIncluded) null else TextDecoration.LineThrough,
        )
    }
}

// MARK: Zone stage

@Composable
private fun ZoneStage(
    areas: List<AreaCountState>,
    active: AreaCountState,
    forward: Boolean,
    locked: Boolean,
    reduceMotion: Boolean,
    onStep: (Int) -> Unit,
    onToggle: (AreaCountState) -> Unit,
    onPlaced: (Offset) -> Unit,
    modifier: Modifier,
) {
    val index = areas.indexOfFirst { it.id == active.id }
    val density = LocalDensity.current
    val threshold = with(density) { 80.dp.toPx() }
    val scope = rememberCoroutineScope()
    val drag = remember { Animatable(0f) }
    val latestIndex by rememberUpdatedState(index)
    val count by rememberUpdatedState(areas.size)

    AnimatedContent(
        targetState = active.id,
        transitionSpec = {
            if (reduceMotion) {
                fadeIn(tween(150)) togetherWith fadeOut(tween(150))
            } else {
                val spec = spring<IntOffset>(dampingRatio = 0.86f, stiffness = 273f)
                val sign = if (forward) 1 else -1
                slideInHorizontally(spec) { sign * it } togetherWith slideOutHorizontally(spec) { -sign * it }
            }
        },
        label = "zoneStage",
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { translationX = drag.value }
            .pointerInput(Unit) {
                var total = 0f
                detectHorizontalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = {
                        val i = latestIndex
                        when {
                            total < -threshold && i < count - 1 -> { scope.launch { drag.snapTo(0f) }; onStep(1) }
                            total > threshold && i > 0 -> { scope.launch { drag.snapTo(0f) }; onStep(-1) }
                            else -> scope.launch { drag.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 322f)) }
                        }
                    },
                    onDragCancel = { scope.launch { drag.animateTo(0f) } },
                ) { change, amount ->
                    change.consume()
                    total += amount
                    val i = latestIndex
                    val atEdge = (total > 0 && i == 0) || (total < 0 && i >= count - 1)
                    scope.launch { drag.snapTo(total * if (atEdge) 0.2f else 0.6f) }
                }
            }
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction("Next zone") { onStep(1); true },
                    CustomAccessibilityAction("Previous zone") { onStep(-1); true },
                )
            },
    ) { id ->
        val area = areas.firstOrNull { it.id == id } ?: return@AnimatedContent
        val i = areas.indexOfFirst { it.id == id }
        ZoneStageBody(area, i > 0, i < areas.size - 1, locked, reduceMotion, onStep, { onToggle(area) }, onPlaced)
    }
}

@Composable
private fun ZoneStageBody(
    area: AreaCountState,
    hasPrevious: Boolean,
    hasNext: Boolean,
    locked: Boolean,
    reduceMotion: Boolean,
    onStep: (Int) -> Unit,
    onToggle: () -> Unit,
    onPlaced: (Offset) -> Unit,
) {
    val count = area.count
    val capacity = area.capacity
    val pct = percent(count, capacity)
    val status: Pair<String, Color>? = when {
        capacity > 0 && count > capacity -> "${count - capacity} over capacity" to CounterPalette.signal
        capacity > 0 && pct >= 100 -> "Full" to CounterPalette.signal
        pct >= 90 -> "Nearly full" to CounterPalette.brass
        else -> null
    }
    val caption = when {
        area.template.hasSeatMap -> "$count of $capacity seats taken"
        capacity > 0 -> "$pct% of $capacity"
        else -> "$count counted"
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onStep(-1) }, enabled = hasPrevious) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous zone")
            }
            Text(
                area.template.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { onStep(1) }, enabled = hasNext) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "Next zone")
            }
        }

        Odometer(
            count, 150.dp,
            color = if (area.isIncluded) CounterPalette.brass else CounterPalette.pew,
            reduceMotion = reduceMotion,
            modifier = Modifier
                .onGloballyPositioned { onPlaced(it.boundsInRoot().center) }
                .semantics { contentDescription = "${area.template.name} count"; stateDescription = "$count" },
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(caption, style = MaterialTheme.typography.bodyMedium, color = CounterPalette.pew)
            status?.let { (text, color) ->
                Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = color)
            }
        }

        LevelBar(
            pct, 6.dp,
            if (area.isIncluded) CounterPalette.level(pct) else CounterPalette.pew,
            Modifier.width(220.dp).padding(top = 6.dp).clearAndSetSemantics { },
        )

        InclusionToggle(area.isIncluded, enabled = !locked, onToggle = onToggle, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun InclusionToggle(included: Boolean, enabled: Boolean, onToggle: () -> Unit, modifier: Modifier) {
    val thumb by animateDpAsState(if (included) 16.dp else 0.dp, spring(dampingRatio = 0.7f, stiffness = 439f), label = "thumb")
    val label = if (included) "Counted in total" else "Left out of total"
    Row(
        modifier
            .heightIn(min = 48.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(CircleShape)
            .toggleable(value = included, enabled = enabled, role = Role.Switch) { onToggle() }
            .semantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(40.dp, 24.dp)
                .background(if (included) CounterPalette.brass else CounterPalette.plateHigh, CircleShape)
                .padding(3.dp),
        ) {
            Box(
                Modifier
                    .offset { IntOffset(thumb.roundToPx(), 0) }
                    .size(18.dp)
                    .background(if (included) CounterPalette.brassInk else CounterPalette.chalk, CircleShape),
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (included) CounterPalette.chalk else CounterPalette.pew,
            modifier = Modifier.padding(end = 12.dp),
        )
    }
}

// MARK: Pad

@Composable
private fun Pad(
    area: AreaCountState,
    locked: Boolean,
    lockedAt: Long?,
    tokens: List<FloatToken>,
    reduceMotion: Boolean,
    onBump: (Int) -> Unit,
    onOpenSeats: () -> Unit,
    onUnlock: () -> Unit,
    onTokenDone: (Int) -> Unit,
    modifier: Modifier,
) {
    val name = area.template.name
    val shape = RoundedCornerShape(24.dp)
    BoxWithConstraints(modifier.fillMaxWidth().height(184.dp)) {
        Box(Modifier.fillMaxSize().clip(shape)) {
            if (area.template.hasSeatMap) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(CounterPalette.plate, shape)
                        .clickable(role = Role.Button, onClick = onOpenSeats),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
                ) {
                    Icon(Icons.Filled.GridOn, contentDescription = null, tint = CounterPalette.brass, modifier = Modifier.size(36.dp))
                    Text("Open seat map", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("$name is counted seat by seat", style = MaterialTheme.typography.bodySmall, color = CounterPalette.pew)
                }
            } else {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PadKey(-1, "−1", null, "Subtract one from $name", !locked && area.count > 0, onBump, Modifier.weight(1f))
                    PadKey(1, "+1", "Tap or hold", "Add one to $name", !locked, onBump, Modifier.weight(2f))
                }
            }
            Shutter(locked, lockedAt, reduceMotion, onUnlock)
        }
        // Outside the clip, so the tokens float up over the zone.
        tokens.forEach { token ->
            key(token.id) {
                FloatingToken(token, reduceMotion, Modifier.offset(x = maxWidth * token.x, y = 44.dp)) { onTokenDone(token.id) }
            }
        }
    }
}

/** Count key: tap for one, hold to repeat faster, or swipe up/down on it to add or subtract. */
@Composable
private fun PadKey(
    sign: Int,
    title: String,
    caption: String?,
    accessibilityName: String,
    enabled: Boolean,
    onBump: (Int) -> Unit,
    modifier: Modifier,
) {
    val bump by rememberUpdatedState(onBump)
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, spring(dampingRatio = 0.7f, stiffness = 815f), label = "key")
    val swipeStep = with(LocalDensity.current) { 28.dp.toPx() }
    val isAdd = sign > 0
    val shape = RoundedCornerShape(22.dp)

    Column(
        modifier
            .fillMaxHeight()
            .graphicsLayer {
                scaleX = scale; scaleY = scale
                alpha = if (enabled) 1f else 0.35f
            }
            .background(if (isAdd) CounterPalette.brass else CounterPalette.plate.copy(alpha = 0.7f), shape)
            .then(
                if (!enabled) Modifier else Modifier.pointerInput(sign) {
                    coroutineScope {
                        val scope = this
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            pressed = true
                            // Set when a hold or swipe already counted, so the release doesn't add one more.
                            var consumed = false
                            var released = false
                            var lastY = down.position.y
                            val hold = scope.launch {
                                delay(HOLD_DELAY_MS)
                                var interval = FIRST_HOLD_INTERVAL_MS
                                while (isActive) {
                                    consumed = true
                                    bump(sign)
                                    delay(interval.toLong())
                                    interval = nextHoldInterval(interval)
                                }
                            }
                            try {
                                while (true) {
                                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                                    if (!change.pressed) { released = true; break }
                                    val dy = lastY - change.position.y
                                    if (abs(dy) >= swipeStep) {
                                        hold.cancel()
                                        consumed = true
                                        lastY = change.position.y
                                        bump(if (dy > 0) 1 else -1)
                                    }
                                    change.consume()
                                }
                            } finally {
                                hold.cancel()
                                pressed = false
                            }
                            if (released && !consumed) bump(sign)
                        }
                    }
                },
            )
            .semantics {
                role = Role.Button
                contentDescription = accessibilityName
                if (enabled) onClick { bump(sign); true } else disabled()
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
    ) {
        val fg = if (isAdd) CounterPalette.brassInk else CounterPalette.chalk
        Text(title, style = digitStyle(if (isAdd) 72.dp else 44.dp), color = fg)
        caption?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = fg.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun FloatingToken(token: FloatToken, reduceMotion: Boolean, modifier: Modifier, onComplete: () -> Unit) {
    val adding = token.value > 0
    val travel = remember { Animatable(0f) }
    val fade = remember { Animatable(1f) }
    val done by rememberUpdatedState(onComplete)
    LaunchedEffect(token.id) {
        val distance = if (reduceMotion) 0f else if (adding) -170f else 90f
        launch { travel.animateTo(distance, tween(720, easing = EaseOut)) }
        launch { delay(220); fade.animateTo(0f, tween(500, easing = EaseIn)) }
        delay(740)
        done()
    }
    Text(
        if (adding) "+1" else "−1",
        style = digitStyle(34.dp, bold = true),
        color = if (adding) CounterPalette.brass else CounterPalette.pew,
        modifier = modifier
            .centeredAt()
            .graphicsLayer {
                translationY = travel.value * density
                alpha = fade.value
                val s = if (adding) 0.85f + 0.35f * (1 - fade.value) else 1f
                scaleX = s; scaleY = s
            }
            .clearAndSetSemantics { },
    )
}

/** Places the content centred on its layout origin, like SwiftUI `.position`. */
private fun Modifier.centeredAt(at: () -> Offset = { Offset.Zero }) = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
    layout(0, 0) {
        val p = at()
        placeable.place((p.x - placeable.width / 2f).roundToInt(), (p.y - placeable.height / 2f).roundToInt())
    }
}

// MARK: Lock shutter

/** Roller shutter that drops over the pad when the count is locked, with a stamped time. */
@Composable
private fun Shutter(locked: Boolean, lockedAt: Long?, reduceMotion: Boolean, onUnlock: () -> Unit) {
    val context = LocalContext.current
    val slat = CounterPalette.plateHigh
    val gap = CounterPalette.plate
    var stamped by remember { mutableStateOf(false) }
    LaunchedEffect(locked) {
        stamped = false
        if (locked) {
            if (!reduceMotion) delay(400)
            stamped = true
        }
    }
    val stampSpring = spring<Float>(dampingRatio = 0.55f, stiffness = 247f)
    val stampScale by animateFloatAsState(if (stamped) 1f else 1.7f, if (stamped) stampSpring else snap(), label = "stampScale")
    val stampAlpha by animateFloatAsState(if (stamped) 1f else 0f, if (stamped) stampSpring else snap(), label = "stampAlpha")
    val stampText = lockedAt?.let { "Locked at ${DateFormat.getTimeFormat(context).format(Date(it))}" } ?: "Locked"

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val y by animateDpAsState(
            if (locked) 0.dp else -(maxHeight + 8.dp),
            if (reduceMotion) tween(150) else tween(520, easing = CubicBezierEasing(0.65f, 0f, 0.25f, 1f)),
            label = "shutter",
        )
        Column(
            Modifier
                .offset { IntOffset(0, y.roundToPx()) }
                .fillMaxSize()
                .drawBehind {
                    val slatH = 22.dp.toPx()
                    val gapH = 2.dp.toPx()
                    var top = 0f
                    while (top < size.height) {
                        drawRect(slat, Offset(0f, top), Size(size.width, slatH))
                        drawRect(gap, Offset(0f, top + slatH), Size(size.width, gapH))
                        top += slatH + gapH
                    }
                }
                .then(
                    if (locked) {
                        // Swallow touches so nothing under the shutter reacts.
                        Modifier.pointerInput(Unit) { awaitEachGesture { awaitFirstDown().consume() } }
                    } else {
                        Modifier.clearAndSetSemantics { }
                    },
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
        ) {
            Text(
                stampText,
                style = digitStyle(26.dp, bold = true),
                color = CounterPalette.brass,
                modifier = Modifier
                    .graphicsLayer { rotationZ = -4f; scaleX = stampScale; scaleY = stampScale; alpha = stampAlpha }
                    .background(CounterPalette.ground, RoundedCornerShape(8.dp))
                    .border(2.dp, CounterPalette.brass, RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
            CounterPill(onClick = onUnlock, enabled = locked) {
                Text("Unlock to keep counting", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// MARK: Dock and overlays

@Composable
private fun Dock(
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onReport: () -> Unit,
    modifier: Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CounterPill(onUndo, Modifier.weight(1f), enabled = canUndo) {
            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null)
            Text("Undo", fontWeight = FontWeight.SemiBold)
        }
        CounterPill(onRedo, Modifier.weight(1f), enabled = canRedo) {
            Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = null)
            Text("Redo", fontWeight = FontWeight.SemiBold)
        }
        CounterPill(onReport, Modifier.weight(1f)) {
            Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null)
            Text("Report", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun FlyOverlay(token: FlyToken, total: Offset, zone: Offset, onDone: () -> Unit) {
    if (total == Offset.Zero || zone == Offset.Zero) {
        LaunchedEffect(token.id) { onDone() }
        return
    }
    val progress = remember { Animatable(0f) }
    val done by rememberUpdatedState(onDone)
    LaunchedEffect(token.id) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(760, easing = CubicBezierEasing(0.5f, 0f, 0.3f, 1f)))
        done()
    }
    val from = if (token.joining) zone else total
    val to = if (token.joining) total else zone
    Text(
        token.text,
        style = digitStyle(56.dp, bold = true),
        color = if (token.joining) CounterPalette.brass else CounterPalette.pew,
        modifier = Modifier
            .centeredAt { from + (to - from) * progress.value }
            .graphicsLayer {
                val p = progress.value
                val s = if (token.joining) 1f - 0.58f * p else 0.42f + 0.58f * p
                scaleX = s; scaleY = s
                alpha = 1f - p
            }
            .clearAndSetSemantics { },
    )
}

@Composable
private fun CountSnackbar(snack: SnackMessage?, modifier: Modifier, onUndo: (String) -> Unit) {
    AnimatedContent(
        targetState = snack,
        transitionSpec = {
            (slideInVertically { -it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
        },
        contentKey = { it?.id },
        label = "snack",
        modifier = modifier.padding(horizontal = 16.dp).padding(top = 4.dp),
    ) { message ->
        if (message == null) return@AnimatedContent
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .background(CounterPalette.plateHigh, CircleShape)
                .padding(horizontal = 18.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(message.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            message.undoInclusionOf?.let { id ->
                TextButton(onClick = { onUndo(id) }) {
                    Text("Undo", fontWeight = FontWeight.SemiBold, color = CounterPalette.brass)
                }
            }
        }
    }
}

// MARK: Room level

/** Brass "water line" behind the counter showing how full the selected zone is. */
@Composable
private fun RoomLevel(percent: Int, included: Boolean, modifier: Modifier) {
    val target = when {
        !included -> CounterPalette.pew
        percent >= 100 -> CounterPalette.signal
        else -> CounterPalette.brass
    }
    val color by animateColorAsState(target, tween(300), label = "roomColor")
    val level by animateFloatAsState(minOf(percent, 100) / 100f, spring(dampingRatio = 0.8f, stiffness = 110f), label = "roomLevel")
    Box(
        modifier.drawBehind {
            val h = size.height * level
            drawRect(color.copy(alpha = 0.10f), Offset(0f, size.height - h), Size(size.width, h))
            drawRect(color.copy(alpha = 0.45f), Offset(0f, size.height - h - 1.5.dp.toPx()), Size(size.width, 1.5.dp.toPx()))
        },
    )
}
