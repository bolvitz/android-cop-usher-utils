package com.eventmonitor.app.presentation.screens.lostandfound

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eventmonitor.shared.data.models.LostItemDto
import com.eventmonitor.shared.domain.models.ItemCategory
import com.eventmonitor.shared.domain.models.ItemStatus
import com.eventmonitor.shared.presentation.lostandfound.LostItemDetailViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedLostItemDetailScreen(
    itemId: String,
    onBack: () -> Unit,
    viewModel: LostItemDetailViewModel = koinViewModel { parametersOf(itemId) }
) {
    val item by viewModel.item.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lost Item") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.delete(); onBack() }) { Text("Delete") }
                }
            )
        }
    ) { padding ->
        val current = item
        if (current == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LostItemForm(
                modifier = Modifier.padding(padding),
                item = current,
                onSave = viewModel::save,
                onClaim = { by, contact, notes -> viewModel.claim(by, contact, notes) }
            )
        }
    }
}

@Composable
private fun LostItemForm(
    modifier: Modifier,
    item: LostItemDto,
    onSave: (LostItemDto) -> Unit,
    onClaim: (String, String, String) -> Unit
) {
    var description by remember(item.id) { mutableStateOf(item.description) }
    var category by remember(item.id) { mutableStateOf(ItemCategory.fromString(item.category)) }
    var foundZone by remember(item.id) { mutableStateOf(item.foundZone) }
    var color by remember(item.id) { mutableStateOf(item.color) }
    var brand by remember(item.id) { mutableStateOf(item.brand) }
    var showClaim by remember { mutableStateOf(false) }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AssistChip(onClick = {}, label = { Text(ItemStatus.fromString(item.status).displayName) })
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = foundZone, onValueChange = { foundZone = it }, label = { Text("Found zone") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = color, onValueChange = { color = it }, label = { Text("Colour") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Brand") }, modifier = Modifier.fillMaxWidth())
        if (item.claimedBy.isNotBlank()) {
            Text("Claimed by ${item.claimedBy}", style = MaterialTheme.typography.bodyMedium)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    onSave(item.copy(description = description, category = category.name, foundZone = foundZone, color = color, brand = brand))
                },
                modifier = Modifier.weight(1f)
            ) { Text("Save") }
            if (item.status == ItemStatus.PENDING.name) {
                OutlinedButton(onClick = { showClaim = true }, modifier = Modifier.weight(1f)) { Text("Claim") }
            }
        }
    }

    if (showClaim) {
        var by by remember { mutableStateOf("") }
        var contact by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showClaim = false },
            confirmButton = {
                TextButton(onClick = { onClaim(by, contact, ""); showClaim = false }, enabled = by.isNotBlank()) { Text("Claim") }
            },
            dismissButton = { TextButton(onClick = { showClaim = false }) { Text("Cancel") } },
            title = { Text("Claim Item") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = by, onValueChange = { by = it }, label = { Text("Claimed by") })
                    OutlinedTextField(value = contact, onValueChange = { contact = it }, label = { Text("Contact") })
                }
            }
        )
    }
}
