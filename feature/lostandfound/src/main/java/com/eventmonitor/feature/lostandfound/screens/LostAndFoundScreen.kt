package com.eventmonitor.feature.lostandfound.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
    onClaim: () -> Unit,
    onUpdateStatus: (String) -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    val status = ItemStatus.fromString(item.status)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(android.graphics.Color.parseColor(status.color)).copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.titleMedium
                )
                AssistChip(
                    onClick = {},
                    label = { Text(status.displayName, style = MaterialTheme.typography.labelSmall) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text("Found: ${item.foundZone}", style = MaterialTheme.typography.bodySmall)
            Text("Date: ${dateFormat.format(Date(item.foundDate))}", style = MaterialTheme.typography.bodySmall)
            if (item.color.isNotBlank()) {
                Text("Color: ${item.color}", style = MaterialTheme.typography.bodySmall)
            }
            if (item.brand.isNotBlank()) {
                Text("Brand: ${item.brand}", style = MaterialTheme.typography.bodySmall)
            }

            if (item.status == ItemStatus.PENDING.name) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onClaim) {
                        Text("Claim")
                    }
                    OutlinedButton(onClick = { onUpdateStatus(ItemStatus.DONATED.name) }) {
                        Text("Mark as Donated")
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
