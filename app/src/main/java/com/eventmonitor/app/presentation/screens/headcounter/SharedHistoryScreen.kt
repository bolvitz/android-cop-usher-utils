package com.eventmonitor.app.presentation.screens.headcounter

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eventmonitor.shared.data.models.EventWithDetails
import com.eventmonitor.shared.presentation.headcounter.HistoryViewModel
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/** Past counts, newest first. Tap a count to resume it; its menu has the report and more. Matches iOS HistoryView. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedHistoryScreen(
    venueId: String?,
    onBack: () -> Unit,
    onOpenTrends: () -> Unit,
    onOpenCount: (venueId: String, eventId: String) -> Unit,
    viewModel: HistoryViewModel = koinViewModel { parametersOf(venueId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val report by viewModel.report.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<EventWithDetails?>(null) }
    val haptics = rememberCounterHaptics()
    // Best turnout per venue, so each bar compares like with like.
    val bestByVenue = remember(state.events) {
        state.events.groupBy { it.event.venueId }.mapValues { (_, list) -> list.maxOf { it.event.totalAttendance } }
    }

    Scaffold(
        containerColor = CounterPalette.ground,
        topBar = {
            TopAppBar(
                title = { Text("History", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = onOpenTrends) {
                        Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = "Trends", tint = CounterPalette.brass)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CounterPalette.ground),
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = CounterPalette.chalk)
                state.isEmpty -> EmptyState(
                    Icons.Filled.History,
                    "No counts yet",
                    "Counts you take appear here, open or locked.",
                    Modifier.align(Alignment.Center),
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        Text(
                            "Bars compare each count with its venue's best turnout. Tap a count to resume it, or open its menu for the report.",
                            style = MaterialTheme.typography.bodySmall,
                            color = CounterPalette.pew,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                        )
                    }
                    itemsIndexed(state.events, key = { _, item -> item.event.id }) { index, item ->
                        HistoryRow(
                            item = item,
                            best = bestByVenue[item.event.venueId] ?: 0,
                            index = index,
                            onOpen = { onOpenCount(item.event.venueId, item.event.id) },
                            onReport = { viewModel.showReport(item.event.id) },
                            onUnlock = { haptics.soft(); viewModel.unlockEvent(item.event.id) },
                            onDelete = { pendingDelete = item },
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete this count?") },
            text = { Text("Its zone counts are removed for good.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteEvent(item.event.id); pendingDelete = null }) {
                    Text("Delete", color = CounterPalette.signal, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
        )
    }

    report?.let { HeadCountReceiptDialog(report = it, onDismiss = viewModel::clearReport) }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryRow(
    item: EventWithDetails,
    best: Int,
    index: Int,
    onOpen: () -> Unit,
    onReport: () -> Unit,
    onUnlock: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val title = item.event.eventName.ifEmpty { item.eventType?.name ?: "Count" }
    val total = item.event.totalAttendance
    val isBest = best > 0 && total == best
    val locked = item.event.isLocked
    var menuOpen by remember { mutableStateOf(false) }
    val grow = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(minOf(index, 10) * 50L)
        grow.animateTo(1f, spring(dampingRatio = 0.85f, stiffness = 80f))
    }
    val date = formatCountDate(context, item.event.date)

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CounterPalette.plate.copy(alpha = 0.6f))
            .combinedClickable(onClick = onOpen, onLongClick = { menuOpen = true }, onClickLabel = "Resume count")
            .padding(start = 16.dp, top = 12.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = CounterPalette.chalk)
                Text(item.venue.name, style = MaterialTheme.typography.bodyMedium, color = CounterPalette.chalk)
                Text(date, style = MaterialTheme.typography.bodySmall, color = CounterPalette.pew)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("$total", style = digitStyle(34.dp), color = CounterPalette.chalk)
                Text(
                    if (locked) "Locked" else "Open",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (locked) CounterPalette.chalk else CounterPalette.brassInk,
                    modifier = Modifier
                        .background(if (locked) CounterPalette.plateHigh else CounterPalette.brass, CircleShape)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More for $title", tint = CounterPalette.pew)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("View report") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, null) },
                        onClick = { menuOpen = false; onReport() },
                    )
                    if (locked) {
                        DropdownMenuItem(
                            text = { Text("Unlock") },
                            leadingIcon = { Icon(Icons.Filled.LockOpen, null) },
                            onClick = { menuOpen = false; onUnlock() },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Delete", color = CounterPalette.signal) },
                        leadingIcon = { Icon(Icons.Filled.Delete, null, tint = CounterPalette.signal) },
                        onClick = { menuOpen = false; onDelete() },
                    )
                }
            }
        }
        Row(
            Modifier.padding(end = 16.dp).clearAndSetSemantics { },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val fraction = if (best > 0) total.toFloat() / best else 0f
            Box(Modifier.weight(1f).height(4.dp).background(CounterPalette.plateHigh, CircleShape)) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction * grow.value)
                        .background(if (isBest) CounterPalette.brass else CounterPalette.pew, CircleShape),
                )
            }
            Text(
                when {
                    isBest -> "Best turnout"
                    best > 0 -> "${percent(total, best)}% of best"
                    else -> ""
                },
                style = MaterialTheme.typography.bodySmall,
                color = CounterPalette.pew,
            )
        }
    }
}
