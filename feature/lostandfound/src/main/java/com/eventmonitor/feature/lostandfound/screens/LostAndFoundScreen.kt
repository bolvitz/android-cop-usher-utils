package com.eventmonitor.feature.lostandfound.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.eventmonitor.core.data.local.entities.LostItemEntity
import com.eventmonitor.core.domain.models.ItemStatus
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LostAndFoundScreen(
    locationId: String?,
    onNavigateToAddItem: (String) -> Unit,
    onNavigateToItemDetail: (String) -> Unit = {},
    onNavigateBack: () -> Unit,
    viewModel: LostAndFoundViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedStatus by viewModel.selectedStatus.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showFilterDialog by remember { mutableStateOf(false) }
    var showClaimDialog by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lost & Found") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(Icons.Default.FilterList, "Filter")
                    }
                }
            )
        },
        floatingActionButton = {
            if (locationId != null) {
                FloatingActionButton(onClick = { onNavigateToAddItem(locationId) }) {
                    Icon(Icons.Default.Add, "Add Item")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is LostAndFoundUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is LostAndFoundUiState.Empty -> {
                    EmptyState(modifier = Modifier.align(Alignment.Center))
                }
                is LostAndFoundUiState.Success -> {
                    ItemList(
                        items = state.items,
                        onItemClick = onNavigateToItemDetail,
                        onClaimItem = { showClaimDialog = it },
                        onUpdateStatus = viewModel::updateItemStatus,
                        onDeleteItem = viewModel::deleteItem
                    )
                }
                is LostAndFoundUiState.Error -> {
                    ErrorState(
                        message = state.message,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }

    if (showFilterDialog) {
        FilterDialog(
            currentStatus = selectedStatus,
            onDismiss = { showFilterDialog = false },
            onSelectStatus = {
                viewModel.filterByStatus(it)
                showFilterDialog = false
            }
        )
    }

    showClaimDialog?.let { itemId ->
        ClaimItemDialog(
            onDismiss = { showClaimDialog = null },
            onClaim = { name, contact, notes ->
                viewModel.claimItem(itemId, name, contact, notes)
                showClaimDialog = null
            }
        )
    }
}

@Composable
fun ItemList(
    items: List<LostItemEntity>,
    onItemClick: (String) -> Unit,
    onClaimItem: (String) -> Unit,
    onUpdateStatus: (String, String) -> Unit,
    onDeleteItem: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items, key = { it.id }) { item ->
            LostItemCard(
                item = item,
                onClick = { onItemClick(item.id) },
                onClaim = { onClaimItem(item.id) },
                onUpdateStatus = { status -> onUpdateStatus(item.id, status) },
                onDelete = { onDeleteItem(item.id) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LostItemCard(
    item: LostItemEntity,
    onClick: () -> Unit,
    onClaim: () -> Unit,
    onUpdateStatus: (String) -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val status = ItemStatus.fromString(item.status)

    // Calculate if 6 months have passed since item was found
    val currentTime = System.currentTimeMillis()
    val sixMonthsInMillis = 6L * 30L * 24L * 60L * 60L * 1000L // Approximately 6 months (180 days)
    val timeSinceFound = currentTime - item.foundDate
    val canDonate = timeSinceFound >= sixMonthsInMillis

    // Calculate days remaining and progress
    val totalDays = 180
    val daysElapsed = (timeSinceFound / (24L * 60L * 60L * 1000L)).toInt()
    val daysRemaining = (totalDays - daysElapsed).coerceAtLeast(0)
    val donationProgress = (daysElapsed.toFloat() / totalDays).coerceIn(0f, 1f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with description and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Text(
                        text = "Found ${dateFormat.format(Date(item.foundDate))} at ${timeFormat.format(Date(item.foundDate))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AssistChip(
                    onClick = {},
                    label = { Text(status.displayName) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Color(android.graphics.Color.parseColor(status.color)).copy(alpha = 0.2f),
                        labelColor = Color(android.graphics.Color.parseColor(status.color))
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Item details section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Location
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = item.foundZone,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                        )
                    }

                    // Color and Brand if available
                    if (item.color.isNotBlank() || item.brand.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (item.color.isNotBlank()) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Palette,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        text = item.color,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            if (item.brand.isNotBlank()) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Business,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        text = item.brand,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }

                    // Identifying marks if available
                    if (item.identifyingMarks.isNotBlank()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = item.identifyingMarks,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Donation countdown section (only for pending items)
            if (item.status == ItemStatus.PENDING.name) {
                Spacer(modifier = Modifier.height(12.dp))

                // Donation Progress Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (canDonate)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (canDonate) Icons.Default.CheckCircle else Icons.Default.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (canDonate)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = if (canDonate) "Ready for Donation" else "Holding Period",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    color = if (canDonate)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Text(
                                text = if (canDonate) "180/180 days" else "$daysElapsed/180 days",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (canDonate)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Progress bar
                        LinearProgressIndicator(
                            progress = { donationProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = if (canDonate)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )

                        if (!canDonate) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$daysRemaining days remaining until donation eligibility",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onClaim,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Claim")
                    }
                    OutlinedButton(
                        onClick = { onUpdateStatus(ItemStatus.DONATED.name) },
                        enabled = canDonate,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Donate")
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("No items found", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun ErrorState(message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun FilterDialog(
    currentStatus: String?,
    onDismiss: () -> Unit,
    onSelectStatus: (String?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter by Status") },
        text = {
            Column {
                TextButton(onClick = { onSelectStatus(null) }) {
                    Text("All Items")
                }
                ItemStatus.entries.forEach { status ->
                    TextButton(onClick = { onSelectStatus(status.name) }) {
                        Text(status.displayName)
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

@Composable
fun ClaimItemDialog(
    onDismiss: () -> Unit,
    onClaim: (String, String, String) -> Unit
) {
    var claimerName by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Claim Item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = claimerName,
                    onValueChange = { claimerName = it },
                    label = { Text("Claimer Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text("Contact (Phone/Email)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Verification Notes") },
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onClaim(claimerName, contact, notes) },
                enabled = claimerName.isNotBlank()
            ) {
                Text("Claim")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
