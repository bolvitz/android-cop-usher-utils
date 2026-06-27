package com.eventmonitor.app.presentation.screens.lostandfound

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eventmonitor.shared.data.models.LostItemDto
import com.eventmonitor.shared.domain.models.ItemCategory
import com.eventmonitor.shared.domain.models.ItemStatus
import com.eventmonitor.shared.presentation.lostandfound.LostAndFoundViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedLostAndFoundScreen(
    locationId: String?,
    onBack: () -> Unit,
    viewModel: LostAndFoundViewModel = koinViewModel { parametersOf(locationId) }
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lost & Found") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (locationId != null) {
                FloatingActionButton(onClick = { showAdd = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add found item")
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            StatusFilterRow(state.filters.status, viewModel::filterByStatus)
            Box(Modifier.fillMaxSize()) {
                when {
                    state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    state.isEmpty -> Text(
                        "No items recorded.",
                        Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    else -> LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.items, key = { it.id }) { item ->
                            LostItemCard(
                                item = item,
                                onClaim = { viewModel.claim(item.id, "Claimant", "") },
                                onStatus = { status -> viewModel.updateStatus(item.id, status) },
                                onDelete = { viewModel.delete(item.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddItemDialog(
            onDismiss = { showAdd = false },
            onConfirm = { desc, category, zone ->
                viewModel.createItem(description = desc, category = category, foundZone = zone)
                showAdd = false
            }
        )
    }
}

@Composable
private fun StatusFilterRow(selected: String?, onSelect: (String?) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text("All") })
        ItemStatus.entries.forEach { status ->
            FilterChip(
                selected = selected == status.name,
                onClick = { onSelect(status.name) },
                label = { Text(status.displayName) }
            )
        }
    }
}

@Composable
private fun LostItemCard(
    item: LostItemDto,
    onClaim: () -> Unit,
    onStatus: (String) -> Unit,
    onDelete: () -> Unit
) {
    val category = ItemCategory.fromString(item.category)
    val status = ItemStatus.fromString(item.status)
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.description, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Surface(color = Color(parseHex(status.color)), shape = MaterialTheme.shapes.small) {
                    Text(
                        status.displayName,
                        Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "${category.displayName} • ${item.foundZone}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (status == ItemStatus.PENDING) {
                    TextButton(onClick = onClaim) { Text("Claim") }
                    TextButton(onClick = { onStatus(ItemStatus.DONATED.name) }) { Text("Donate") }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDelete) { Text("Delete") }
            }
        }
    }
}

@Composable
private fun AddItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (description: String, category: String, foundZone: String) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var zone by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ItemCategory.OTHER) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onConfirm(description, category.name, zone) },
                enabled = description.isNotBlank()
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Found Item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
                OutlinedTextField(value = zone, onValueChange = { zone = it }, label = { Text("Found zone") })
                Text("Category", style = MaterialTheme.typography.labelMedium)
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ItemCategory.entries.take(6).forEach { c ->
                        FilterChip(
                            selected = category == c,
                            onClick = { category = c },
                            label = { Text(c.displayName) }
                        )
                    }
                }
            }
        }
    )
}

private fun parseHex(hex: String): Long {
    val clean = hex.removePrefix("#")
    return 0xFF000000 or clean.toLong(16)
}
