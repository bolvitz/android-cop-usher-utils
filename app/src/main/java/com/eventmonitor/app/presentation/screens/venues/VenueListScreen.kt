package com.eventmonitor.app.presentation.screens.venues

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.eventmonitor.core.common.utils.rememberHapticFeedback

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun VenueListScreen(
    viewModel: VenueListViewModel = hiltViewModel(),
    onVenueClick: (String) -> Unit,
    onManageAreas: (String) -> Unit = {},
    onEditVenue: (String) -> Unit = {},
    onVenueHistory: (String) -> Unit = {},
    onVenueIncidents: (String) -> Unit = {},
    onVenueLostAndFound: (String) -> Unit = {},
    onNavigateToReports: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val haptic = rememberHapticFeedback()
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Event Monitor") },
                actions = {
                    IconButton(onClick = {
                        haptic.light()
                        onNavigateToReports()
                    }) {
                        Icon(Icons.Default.Analytics, "Reports")
                    }
                    IconButton(onClick = {
                        haptic.light()
                        onNavigateToSettings()
                    }) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is VenueListUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is VenueListUiState.Empty -> {
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseScale"
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No venues yet")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tap + to add your first branch", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            is VenueListUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = state.venues,
                        key = { it.venue.id }
                    ) { branchWithAreas ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItemPlacement(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (branchWithAreas.venue.isActive) {
                                    MaterialTheme.colorScheme.surface
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = if (branchWithAreas.venue.isActive) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = branchWithAreas.venue.name,
                                                style = MaterialTheme.typography.titleLarge,
                                                color = if (branchWithAreas.venue.isActive) {
                                                    MaterialTheme.colorScheme.onSurface
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                            Surface(
                                                color = if (branchWithAreas.venue.isActive) {
                                                    MaterialTheme.colorScheme.primaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.errorContainer
                                                },
                                                shape = MaterialTheme.shapes.small
                                            ) {
                                                Text(
                                                    text = if (branchWithAreas.venue.isActive) "Active" else "Inactive",
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (branchWithAreas.venue.isActive) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        MaterialTheme.colorScheme.error
                                                    }
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = branchWithAreas.venue.location,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "${branchWithAreas.areas.size} areas configured",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (branchWithAreas.venue.isActive) {
                                                MaterialTheme.colorScheme.secondary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
                                    Row {
                                        IconButton(onClick = {
                                            haptic.light()
                                            onManageAreas(branchWithAreas.venue.id)
                                        }) {
                                            Icon(
                                                Icons.Default.Settings,
                                                contentDescription = "Manage Areas",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        var expanded by remember { mutableStateOf(false) }
                                        Box {
                                            IconButton(onClick = {
                                                haptic.light()
                                                expanded = true
                                            }) {
                                                Icon(Icons.Default.MoreVert, "More options")
                                            }
                                            DropdownMenu(
                                                expanded = expanded,
                                                onDismissRequest = { expanded = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("Edit") },
                                                    onClick = {
                                                        haptic.light()
                                                        expanded = false
                                                        onEditVenue(branchWithAreas.venue.id)
                                                    },
                                                    leadingIcon = {
                                                        Icon(Icons.Default.Edit, null)
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Delete") },
                                                    onClick = {
                                                        haptic.medium()
                                                        expanded = false
                                                        showDeleteDialog = branchWithAreas.venue.id
                                                    },
                                                    leadingIcon = {
                                                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Feature buttons row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Head Count button
                                    if (branchWithAreas.venue.isHeadCountEnabled) {
                                        Button(
                                            onClick = {
                                                haptic.medium()
                                                onVenueHistory(branchWithAreas.venue.id)
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Head Count", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }

                                    // Lost and Found button
                                    if (branchWithAreas.venue.isLostAndFoundEnabled) {
                                        Button(
                                            onClick = {
                                                haptic.medium()
                                                onVenueLostAndFound(branchWithAreas.venue.id)
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Lost & Found", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }

                                    // Incident Reporting button
                                    if (branchWithAreas.venue.isIncidentReportingEnabled) {
                                        Button(
                                            onClick = {
                                                haptic.medium()
                                                onVenueIncidents(branchWithAreas.venue.id)
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Incidents", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            is VenueListUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Error: ${state.message}")
                }
            }
        }

        // Delete confirmation dialog with animation
        AnimatedVisibility(
            visible = showDeleteDialog != null,
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + scaleOut(targetScale = 0.8f)
        ) {
            showDeleteDialog?.let { branchId ->
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = null },
                    title = { Text("Delete Branch") },
                    text = { Text("Are you sure you want to delete this branch? This action cannot be undone.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                haptic.strong()
                                viewModel.deleteVenue(branchId) { error ->
                                    errorMessage = error
                                }
                                showDeleteDialog = null
                            }
                        ) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            haptic.light()
                            showDeleteDialog = null
                        }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }

        // Error snackbar with animation
        AnimatedVisibility(
            visible = errorMessage != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            errorMessage?.let { error ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = {
                            haptic.light()
                            errorMessage = null
                        }) {
                            Text("OK")
                        }
                    }
                ) {
                    Text(error)
                }
            }
        }
    }
}
