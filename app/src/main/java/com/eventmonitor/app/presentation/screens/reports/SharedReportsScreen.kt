package com.eventmonitor.app.presentation.screens.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eventmonitor.shared.presentation.reports.AreaStatistics
import com.eventmonitor.shared.presentation.reports.ReportPeriod
import com.eventmonitor.shared.presentation.reports.ReportsViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedReportsScreen(
    onBack: () -> Unit,
    viewModel: ReportsViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.exportMessage) {
        state.exportMessage?.let {
            scope.launch { snackbarHost.showSnackbar(it) }
            viewModel.clearExportMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::exportCsv) {
                        Icon(Icons.Filled.FileDownload, contentDescription = "Export CSV")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReportPeriod.entries.forEach { p ->
                        FilterChip(
                            selected = state.period == p,
                            onClick = { viewModel.selectPeriod(p) },
                            label = { Text(p.displayName) }
                        )
                    }
                }
            }
            item {
                FilterRow(
                    venueLabel = state.venues.firstOrNull { it.id == state.selectedVenueId }?.name ?: "All venues",
                    venues = listOf(null to "All venues") + state.venues.map { it.id to it.name },
                    onVenue = viewModel::selectVenue,
                    typeLabel = state.eventTypes.firstOrNull { it.id == state.selectedEventTypeId }?.name ?: "All types",
                    types = listOf(null to "All types") + state.eventTypes.map { it.id to it.name },
                    onType = viewModel::selectEventType
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("Events", "${state.data.totalEvents}", Modifier.weight(1f))
                    StatCard("Total", "${state.data.totalAttendance}", Modifier.weight(1f))
                    StatCard("Average", "${state.data.averageAttendance}", Modifier.weight(1f))
                }
            }
            item { Text("By area", style = MaterialTheme.typography.titleMedium) }
            if (state.data.areaStatistics.isEmpty()) {
                item {
                    Text("No data for the selected filters.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(state.data.areaStatistics) { stat -> AreaStatCard(stat) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterRow(
    venueLabel: String,
    venues: List<Pair<String?, String>>,
    onVenue: (String?) -> Unit,
    typeLabel: String,
    types: List<Pair<String?, String>>,
    onType: (String?) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterDropdown(venueLabel, venues, onVenue, Modifier.weight(1f))
        FilterDropdown(typeLabel, types, onType, Modifier.weight(1f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterDropdown(
    label: String,
    options: List<Pair<String?, String>>,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Filter") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (id, name) ->
                DropdownMenuItem(text = { Text(name) }, onClick = { onSelect(id); expanded = false })
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AreaStatCard(stat: AreaStatistics) {
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(stat.areaName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                "avg ${stat.averageCount} • min ${stat.minCount} • max ${stat.maxCount} • cap ${stat.capacity}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
