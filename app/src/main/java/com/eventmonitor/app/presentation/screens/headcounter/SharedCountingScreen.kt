package com.eventmonitor.app.presentation.screens.headcounter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eventmonitor.shared.data.local.entities.SeatEntity
import com.eventmonitor.shared.data.local.entities.SeatRowWithSeats
import com.eventmonitor.shared.data.local.entities.SeatStatusEntity
import com.eventmonitor.shared.presentation.headcounter.AreaCountState
import com.eventmonitor.shared.presentation.headcounter.CountingViewModel
import kotlinx.coroutines.flow.flowOf
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Head counter screen rendered from the shared KMP [CountingViewModel].
 * Seat-mapped areas open a tap-to-cycle seat grid; other areas use +/- counters.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedCountingScreen(
    venueId: String,
    onBack: () -> Unit,
    viewModel: CountingViewModel = koinViewModel { parametersOf(venueId, null) }
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()
    var seatMapAreaId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.venueName.ifEmpty { "Head Count" }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.eventId != null) {
                        TextButton(onClick = viewModel::undo, enabled = canUndo) { Text("Undo") }
                        TextButton(onClick = viewModel::redo, enabled = canRedo) { Text("Redo") }
                        IconButton(onClick = {
                            if (state.isLocked) viewModel.unlockEvent() else viewModel.lockEvent()
                        }) {
                            Icon(
                                if (state.isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                                contentDescription = "Lock"
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier
            .fillMaxSize()
            .padding(padding)) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.eventId == null -> StartPrompt(
                    Modifier.align(Alignment.Center),
                    onStart = {
                        viewModel.createNewEvent(
                            eventTypeId = null,
                            eventTypeName = "Head Count",
                            date = System.currentTimeMillis(),
                            countedBy = "Android"
                        )
                    }
                )
                else -> Column(Modifier.fillMaxSize()) {
                    TotalBanner(state.totalAttendance, state.totalCapacity)
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.areaCounts, key = { it.id }) { area ->
                            AreaCounterCard(
                                area = area,
                                locked = state.isLocked,
                                onIncrement = { viewModel.incrementCount(area.id, 1) },
                                onDecrement = { viewModel.decrementCount(area.id, 1) },
                                onToggle = { viewModel.toggleAreaInclusion(area.id) },
                                onOpenSeats = { seatMapAreaId = area.template.id }
                            )
                        }
                    }
                }
            }
        }
    }

    seatMapAreaId?.let { areaId ->
        SeatMapDialog(
            viewModel = viewModel,
            areaTemplateId = areaId,
            locked = state.isLocked,
            onClose = { seatMapAreaId = null }
        )
    }
}

@Composable
private fun StartPrompt(modifier: Modifier = Modifier, onStart: () -> Unit) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("No active count for this venue.")
        Spacer(Modifier.height(12.dp))
        Button(onClick = onStart) { Text("Start Head Count") }
    }
}

@Composable
private fun TotalBanner(total: Int, capacity: Int) {
    Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Total Attendance", style = MaterialTheme.typography.labelMedium)
                Text("$total", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Capacity", style = MaterialTheme.typography.labelMedium)
                Text("$capacity", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Composable
private fun AreaCounterCard(
    area: AreaCountState,
    locked: Boolean,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onToggle: () -> Unit,
    onOpenSeats: () -> Unit
) {
    Card {
        Column(Modifier
            .fillMaxWidth()
            .padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(area.template.name, style = MaterialTheme.typography.titleMedium)
                FilterChip(
                    selected = area.isIncluded,
                    onClick = onToggle,
                    label = { Text(if (area.isIncluded) "Included" else "Excluded") }
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${area.count} / ${area.capacity}  (${area.percentage}%)",
                    style = MaterialTheme.typography.titleLarge
                )
                if (area.template.hasSeatMap) {
                    OutlinedButton(onClick = onOpenSeats) { Text("View Seats") }
                } else {
                    Row {
                        FilledTonalIconButton(onClick = onDecrement, enabled = !locked) {
                            Icon(Icons.Filled.Remove, contentDescription = "Decrement")
                        }
                        Spacer(Modifier.width(8.dp))
                        FilledTonalIconButton(onClick = onIncrement, enabled = !locked) {
                            Icon(Icons.Filled.Add, contentDescription = "Increment")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeatMapDialog(
    viewModel: CountingViewModel,
    areaTemplateId: String,
    locked: Boolean,
    onClose: () -> Unit
) {
    val layoutFlow = remember(areaTemplateId) { viewModel.observeSeatLayout(areaTemplateId) }
    val rows by layoutFlow.collectAsStateWithLifecycle(emptyList())
    val statusFlow = remember(areaTemplateId) { viewModel.observeSeatStatuses(areaTemplateId) }
    val statuses by (statusFlow ?: flowOf(emptyList<SeatStatusEntity>()))
        .collectAsStateWithLifecycle(emptyList())
    val statusBySeat = remember(statuses) { statuses.associate { it.seatId to it.status } }

    Dialog(onDismissRequest = onClose) {
        Surface(shape = MaterialTheme.shapes.large, tonalElevation = 4.dp) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Seat Map", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onClose) { Text("Done") }
                }
                Spacer(Modifier.height(8.dp))
                if (rows.isEmpty()) {
                    Text("No seats defined for this area.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    // Cinema layout: the screen and every row share one center line; wide maps scroll sideways
                    BoxWithConstraints(Modifier.weight(1f, fill = false)) {
                        Column(
                            Modifier
                                .verticalScroll(rememberScrollState())
                                .horizontalScroll(rememberScrollState())
                                .widthIn(min = maxWidth),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Column(
                                Modifier.width(IntrinsicSize.Max),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CinemaScreen()
                                rows.forEach { rowWithSeats ->
                                    key(rowWithSeats.row.id) {
                                        SeatRow(
                                            rowWithSeats = rowWithSeats,
                                            statusBySeat = statusBySeat,
                                            locked = locked,
                                            onTapSeat = { seat ->
                                                viewModel.cycleSeatStatus(
                                                    seat.id,
                                                    statusBySeat[seat.id] ?: "AVAILABLE"
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    SeatLegend()
                }
            }
        }
    }
}

@Composable
private fun SeatRow(
    rowWithSeats: SeatRowWithSeats,
    statusBySeat: Map<String, String>,
    locked: Boolean,
    onTapSeat: (SeatEntity) -> Unit
) {
    // Labels on both sides keep the seats centered under the screen
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RowLabel(rowWithSeats.row.label)
        rowWithSeats.seats.sortedBy { it.number }.forEach { seat ->
            SeatBox(
                number = seat.number,
                status = statusBySeat[seat.id] ?: "AVAILABLE",
                enabled = !locked,
                onClick = { onTapSeat(seat) }
            )
        }
        RowLabel(rowWithSeats.row.label)
    }
}

@Composable
private fun RowLabel(label: String) {
    Text(
        label,
        Modifier.width(24.dp),
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.labelMedium
    )
}

@Composable
private fun CinemaScreen() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp)
            .padding(bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(bottomStart = 50.dp, bottomEnd = 50.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "SCREEN",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 4.sp
        )
    }
}

@Composable
private fun SeatBox(number: Int, status: String, enabled: Boolean, onClick: () -> Unit) {
    val color = seatColor(status)
    Box(
        Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(color)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "$number",
            style = MaterialTheme.typography.labelSmall,
            color = if (status == "AVAILABLE") MaterialTheme.colorScheme.onSurfaceVariant else Color.White
        )
    }
}

@Composable
private fun SeatLegend() {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
    ) {
        listOf("AVAILABLE", "OCCUPIED", "RESERVED", "BLOCKED").forEach { s ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(seatColor(s)))
                Spacer(Modifier.width(4.dp))
                Text(s.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun seatColor(status: String): Color = when (status) {
    "OCCUPIED" -> MaterialTheme.colorScheme.primary
    "RESERVED" -> MaterialTheme.colorScheme.tertiary
    "BLOCKED" -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.surfaceVariant
}
