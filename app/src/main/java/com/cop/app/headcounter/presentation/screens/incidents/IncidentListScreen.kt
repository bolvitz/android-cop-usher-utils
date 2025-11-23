package com.cop.app.headcounter.presentation.screens.incidents

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cop.app.headcounter.data.local.entities.IncidentEntity
import com.cop.app.headcounter.domain.models.IncidentSeverity
import com.cop.app.headcounter.domain.models.IncidentStatus
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddIncident: (String) -> Unit,
    onNavigateToIncidentDetail: (String) -> Unit,
    branchId: String? = null,
    viewModel: IncidentListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedStatus by viewModel.selectedStatus.collectAsState()
    val selectedSeverity by viewModel.selectedSeverity.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var showFilterMenu by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Incident Reports") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterMenu = true }) {
                        Icon(Icons.Default.FilterList, "Filter")
                        if (selectedStatus != null || selectedSeverity != null) {
                            Badge()
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (branchId != null) {
                FloatingActionButton(
                    onClick = { onNavigateToAddIncident(branchId) }
                ) {
                    Icon(Icons.Default.Add, "Report Incident")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchText,
                onValueChange = {
                    searchText = it
                    viewModel.searchIncidents(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search incidents...") },
                leadingIcon = { Icon(Icons.Default.Search, "Search") },
                trailingIcon = {
                    if (searchText.isNotEmpty()) {
                        IconButton(onClick = {
                            searchText = ""
                            viewModel.searchIncidents("")
                        }) {
                            Icon(Icons.Default.Clear, "Clear")
                        }
                    }
                },
                singleLine = true
            )

            // Active filters
            if (selectedStatus != null || selectedSeverity != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    selectedStatus?.let { status ->
                        FilterChip(
                            selected = true,
                            onClick = { viewModel.filterByStatus(null) },
                            label = { Text(IncidentStatus.fromString(status).displayName) },
                            trailingIcon = { Icon(Icons.Default.Close, "Remove") }
                        )
                    }
                    selectedSeverity?.let { severity ->
                        FilterChip(
                            selected = true,
                            onClick = { viewModel.filterBySeverity(null) },
                            label = { Text(IncidentSeverity.fromString(severity).displayName) },
                            trailingIcon = { Icon(Icons.Default.Close, "Remove") }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Content
            when (val state = uiState) {
                is IncidentListUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is IncidentListUiState.Empty -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No incidents found",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
                is IncidentListUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.incidents) { incident ->
                            IncidentCard(
                                incident = incident,
                                onClick = { onNavigateToIncidentDetail(incident.id) }
                            )
                        }
                    }
                }
                is IncidentListUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            state.message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Filter menu
        if (showFilterMenu) {
            FilterDialog(
                selectedStatus = selectedStatus,
                selectedSeverity = selectedSeverity,
                onDismiss = { showFilterMenu = false },
                onFilterByStatus = {
                    viewModel.filterByStatus(it)
                    showFilterMenu = false
                },
                onFilterBySeverity = {
                    viewModel.filterBySeverity(it)
                    showFilterMenu = false
                }
            )
        }

        // Error snackbar
        errorMessage?.let { error ->
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("Dismiss")
                    }
                }
            ) {
                Text(error)
            }
        }
    }
}

@Composable
fun IncidentCard(
    incident: IncidentEntity,
    onClick: () -> Unit
) {
    val severity = IncidentSeverity.fromString(incident.severity)
    val status = IncidentStatus.fromString(incident.status)
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Title and severity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = incident.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                SeverityBadge(severity)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Description
            Text(
                text = incident.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Status and date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(status)

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Reported: ${dateFormat.format(Date(incident.reportedAt))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    incident.resolvedAt?.let { resolvedAt ->
                        Text(
                            text = "Resolved: ${dateFormat.format(Date(resolvedAt))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Category and location
            if (incident.category.isNotEmpty() || incident.location.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (incident.category.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Category,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = incident.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    if (incident.location.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = incident.location,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SeverityBadge(severity: IncidentSeverity) {
    val color = Color(android.graphics.Color.parseColor(severity.color))

    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.border(1.dp, color, RoundedCornerShape(4.dp))
    ) {
        Text(
            text = severity.displayName,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StatusBadge(status: IncidentStatus) {
    val color = Color(android.graphics.Color.parseColor(status.color))

    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.border(1.dp, color, RoundedCornerShape(4.dp))
    ) {
        Text(
            text = status.displayName,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterDialog(
    selectedStatus: String?,
    selectedSeverity: String?,
    onDismiss: () -> Unit,
    onFilterByStatus: (String?) -> Unit,
    onFilterBySeverity: (String?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Incidents") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("By Status:", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedStatus == null,
                        onClick = { onFilterByStatus(null) },
                        label = { Text("All") }
                    )
                    IncidentStatus.entries.forEach { status ->
                        FilterChip(
                            selected = selectedStatus == status.name,
                            onClick = { onFilterByStatus(status.name) },
                            label = { Text(status.displayName) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text("By Severity:", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedSeverity == null,
                        onClick = { onFilterBySeverity(null) },
                        label = { Text("All") }
                    )
                    IncidentSeverity.entries.forEach { severity ->
                        FilterChip(
                            selected = selectedSeverity == severity.name,
                            onClick = { onFilterBySeverity(severity.name) },
                            label = { Text(severity.displayName) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
