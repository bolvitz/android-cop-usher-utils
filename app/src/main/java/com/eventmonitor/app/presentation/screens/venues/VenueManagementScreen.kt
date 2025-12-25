package com.eventmonitor.app.presentation.screens.venues

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.eventmonitor.core.data.local.entities.VenueEntity
import com.eventmonitor.core.common.utils.rememberHapticFeedback

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VenueManagementScreen(
    viewModel: VenueManagementViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onAddVenue: () -> Unit,
    onEditVenue: (String) -> Unit
) {
    val haptic = rememberHapticFeedback()
    val venues by viewModel.venues.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedVenue by remember { mutableStateOf<VenueEntity?>(null) }

    LaunchedEffect(uiState.message, uiState.error) {
        if (uiState.message != null || uiState.error != null) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Branches") },
                navigationIcon = {
                    IconButton(onClick = {
                        haptic.light()
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                haptic.medium()
                onAddVenue()
            }) {
                Icon(Icons.Default.Add, "Add Branch")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            uiState.message?.let { message ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            uiState.error?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (venues.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "No branches configured",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Tap + to add your first branch",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(venues) { venue ->
                        VenueCard(
                            venue = venue,
                            onEdit = {
                                onEditVenue(venue.id)
                            },
                            onDelete = {
                                selectedVenue = venue
                                showDeleteDialog = true
                            },
                            onToggleStatus = { isActive ->
                                haptic.medium()
                                viewModel.toggleVenueStatus(venue.id, isActive)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog && selectedVenue != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                selectedVenue = null
            },
            title = { Text("Delete Branch") },
            text = {
                Text("Are you sure you want to delete \"${selectedVenue?.name}\"?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteVenue(selectedVenue!!.id)
                        showDeleteDialog = false
                        selectedVenue = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        selectedVenue = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun VenueCard(
    venue: VenueEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleStatus: (Boolean) -> Unit
) {
    val haptic = rememberHapticFeedback()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (venue.isActive) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (venue.isActive) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = venue.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (venue.isActive) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Surface(
                        color = if (venue.isActive) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        },
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = if (venue.isActive) "Active" else "Inactive",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (venue.isActive) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                    }
                }
                Switch(
                    checked = venue.isActive,
                    onCheckedChange = {
                        onToggleStatus(it)
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = venue.location,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tag,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Code: ${venue.code}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (venue.contactPerson.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = venue.contactPerson,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = {
                    haptic.light()
                    onEdit()
                }) {
                    Icon(Icons.Default.Edit, "Edit")
                }
                IconButton(onClick = {
                    haptic.medium()
                    onDelete()
                }) {
                    Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
