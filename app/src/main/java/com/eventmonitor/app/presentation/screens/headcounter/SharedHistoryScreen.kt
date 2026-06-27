package com.eventmonitor.app.presentation.screens.headcounter

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eventmonitor.shared.data.models.EventWithDetails
import com.eventmonitor.shared.presentation.headcounter.HistoryViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedHistoryScreen(
    venueId: String?,
    onBack: () -> Unit,
    onOpenTrends: () -> Unit,
    viewModel: HistoryViewModel = koinViewModel { parametersOf(venueId) }
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val dateFormat = remember { SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenTrends) {
                        Icon(Icons.Filled.ShowChart, contentDescription = "Trends")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.isEmpty -> Text(
                    "No events recorded yet.",
                    Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.events, key = { it.event.id }) { event ->
                        HistoryRow(
                            event = event,
                            dateText = dateFormat.format(Date(event.event.date)),
                            onUnlock = { viewModel.unlockEvent(event.event.id) },
                            onDelete = { viewModel.deleteEvent(event.event.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    event: EventWithDetails,
    dateText: String,
    onUnlock: () -> Unit,
    onDelete: () -> Unit
) {
    val title = event.event.eventName.ifEmpty { event.eventType?.name ?: "Event" }
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text(
                    "${event.event.totalAttendance}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(event.venue.name, style = MaterialTheme.typography.bodyMedium)
            Text(dateText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (event.event.isLocked) {
                    AssistChip(onClick = onUnlock, label = { Text("Unlock") })
                } else {
                    Text("Open", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDelete) { Text("Delete") }
            }
        }
    }
}
