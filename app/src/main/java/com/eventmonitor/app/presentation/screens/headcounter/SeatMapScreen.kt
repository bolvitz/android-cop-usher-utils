package com.eventmonitor.app.presentation.screens.headcounter

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eventmonitor.shared.data.local.entities.SeatEntity
import com.eventmonitor.shared.data.local.entities.SeatStatusEntity
import com.eventmonitor.shared.presentation.headcounter.CountingViewModel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

private enum class SeatStatus(val key: String, val title: String) {
    AVAILABLE("AVAILABLE", "Available"),
    OCCUPIED("OCCUPIED", "Taken"),
    RESERVED("RESERVED", "Reserved"),
    BLOCKED("BLOCKED", "Blocked");

    companion object {
        fun of(key: String?) = entries.firstOrNull { it.key == key } ?: AVAILABLE
    }
}

private val SeatShape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomEnd = 3.dp, bottomStart = 3.dp)

/**
 * Seat-by-seat counter for a seat-mapped zone: tap a seat to cycle its status.
 * Shown full screen inside the counter so it shares the counter's view model. Matches iOS SeatMapView.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SeatMapScreen(
    viewModel: CountingViewModel,
    areaTemplateId: String,
    zoneName: String,
    locked: Boolean,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val layoutFlow = remember(areaTemplateId) { viewModel.observeSeatLayout(areaTemplateId) }
    val rows by layoutFlow.collectAsStateWithLifecycle(emptyList())
    val statusFlow = remember(areaTemplateId) { viewModel.observeSeatStatuses(areaTemplateId) ?: flowOf(emptyList<SeatStatusEntity>()) }
    val statuses by statusFlow.collectAsStateWithLifecycle(emptyList())
    val statusBySeat = remember(statuses) { statuses.associate { it.seatId to it.status } }
    val haptics = rememberCounterHaptics()

    val seats = rows.flatMap { it.seats }
    fun count(status: SeatStatus) = seats.count { SeatStatus.of(statusBySeat[it.id]) == status }
    val taken = count(SeatStatus.OCCUPIED)
    val usable = seats.size - count(SeatStatus.BLOCKED)
    val pct = percent(taken, usable)

    Column(Modifier.fillMaxSize().background(CounterPalette.ground)) {
        CenterAlignedTopAppBar(
            title = { Text(zoneName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1) },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
        )

        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 8.dp), verticalAlignment = Alignment.Bottom) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Seated", style = MaterialTheme.typography.bodySmall, color = CounterPalette.pew)
                Odometer(
                    taken, 72.dp, color = CounterPalette.brass,
                    modifier = Modifier.semantics { contentDescription = "Seated"; stateDescription = "$taken" },
                )
            }
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(bottom = 10.dp)) {
                Text("$pct% full", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = CounterPalette.level(pct))
                Text("of $usable usable seats", style = MaterialTheme.typography.bodySmall, color = CounterPalette.pew)
            }
        }

        if (rows.isEmpty()) {
            EmptyState(
                Icons.Filled.GridOn,
                "No seats yet",
                "Add rows in this zone's seat map editor, then come back to count.",
                Modifier.weight(1f).fillMaxWidth().wrapContentHeight(),
            )
        } else {
            // Stage and rows share one centre line; wide maps scroll sideways.
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                        .widthIn(min = maxWidth)
                        .padding(horizontal = 12.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(
                        Modifier.width(IntrinsicSize.Max),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Stage()
                        rows.forEach { row ->
                            key(row.row.id) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    RowLabel(row.row.label)
                                    row.seats.sortedBy { it.number }.forEach { seat ->
                                        key(seat.id) {
                                            SeatButton(seat, SeatStatus.of(statusBySeat[seat.id]), enabled = !locked) { status ->
                                                haptics.tick()
                                                viewModel.cycleSeatStatus(seat.id, status.key)
                                            }
                                        }
                                    }
                                    RowLabel(row.row.label)
                                }
                            }
                        }
                    }
                }
            }
            Legend(::count, Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun Stage() {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 14.dp).clearAndSetSemantics { },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
                .background(CounterPalette.chalk.copy(alpha = 0.5f)),
        )
        Text("Stage", style = MaterialTheme.typography.labelSmall, color = CounterPalette.pew)
    }
}

@Composable
private fun RowLabel(label: String) {
    Text(
        label,
        Modifier.width(18.dp).clearAndSetSemantics { },
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = CounterPalette.pew,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun SeatButton(seat: SeatEntity, status: SeatStatus, enabled: Boolean, onCycle: (SeatStatus) -> Unit) {
    val scope = rememberCoroutineScope()
    val pop = remember { Animatable(1f) }
    Box(
        Modifier
            .scale(pop.value)
            .size(24.dp, 26.dp)
            .seatFace(status)
            .clickable(enabled = enabled, role = Role.Button) {
                onCycle(status)
                scope.launch {
                    pop.animateTo(0.7f, tween(60))
                    pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 503f))
                }
            }
            .semantics { contentDescription = "Seat ${seat.label}"; stateDescription = status.title },
        contentAlignment = Alignment.Center,
    ) {
        if (status != SeatStatus.BLOCKED) {
            Text(
                "${seat.number}",
                style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                color = when (status) {
                    SeatStatus.OCCUPIED -> CounterPalette.brassInk
                    SeatStatus.RESERVED -> CounterPalette.brass
                    else -> CounterPalette.pew
                },
            )
        }
    }
}

/** Seat face per status; shape and fill match the iOS seat map, so status never relies on colour alone. */
@Composable
private fun Modifier.seatFace(status: SeatStatus): Modifier {
    val plate = CounterPalette.plate
    val plateHigh = CounterPalette.plateHigh
    val brass = CounterPalette.brass
    return when (status) {
        SeatStatus.AVAILABLE -> background(plateHigh, SeatShape)
        SeatStatus.OCCUPIED -> background(brass, SeatShape)
        SeatStatus.RESERVED -> border(1.5.dp, brass, SeatShape)
        SeatStatus.BLOCKED -> clip(SeatShape).background(plate).drawBehind {
            // Diagonal hatching.
            val step = 6.dp.toPx()
            var x = -size.height
            while (x < size.width) {
                drawLine(plateHigh, Offset(x, size.height), Offset(x + size.height, 0f), 2.dp.toPx())
                x += step
            }
        }
    }
}

@Composable
private fun Legend(count: (SeatStatus) -> Int, modifier: Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .background(CounterPalette.plate)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Tap a seat to change it: available, taken, reserved, blocked.",
            style = MaterialTheme.typography.bodySmall,
            color = CounterPalette.pew,
        )
        listOf(SeatStatus.AVAILABLE to SeatStatus.OCCUPIED, SeatStatus.RESERVED to SeatStatus.BLOCKED).forEach { (a, b) ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                LegendItem(a, count(a), Modifier.weight(1f))
                LegendItem(b, count(b), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LegendItem(status: SeatStatus, value: Int, modifier: Modifier) {
    Row(
        modifier.semantics(mergeDescendants = true) { },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(14.dp, 16.dp).seatFace(status))
        Text(status.title, style = MaterialTheme.typography.bodyMedium, color = CounterPalette.chalk, modifier = Modifier.weight(1f))
        Text("$value", style = digitStyle(19.dp), color = CounterPalette.chalk)
    }
}
