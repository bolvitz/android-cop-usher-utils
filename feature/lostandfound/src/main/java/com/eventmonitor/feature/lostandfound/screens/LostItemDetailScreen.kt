package com.eventmonitor.feature.lostandfound.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.eventmonitor.core.data.local.entities.LostItemEntity
import com.eventmonitor.core.domain.models.ItemStatus
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LostItemDetailScreen(
    itemId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String, String) -> Unit,
    viewModel: LostItemDetailViewModel = hiltViewModel()
) {
    val item by viewModel.item.collectAsState(initial = null)
    var showClaimDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lost Item Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    item?.let { itm ->
                        IconButton(onClick = { onNavigateToEdit(itm.locationId, itm.id) }) {
                            Icon(Icons.Default.Edit, "Edit")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, "Delete")
                        }
                    }
                }
            )
        }
    ) { padding ->
        item?.let { itm ->
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val status = ItemStatus.fromString(itm.status)

            // Calculate donation eligibility
            val currentTime = System.currentTimeMillis()
            val sixMonthsInMillis = 6L * 30L * 24L * 60L * 60L * 1000L
            val timeSinceFound = currentTime - itm.foundDate
            val canDonate = timeSinceFound >= sixMonthsInMillis
            val totalDays = 180
            val daysElapsed = (timeSinceFound / (24L * 60L * 60L * 1000L)).toInt()
            val daysRemaining = (totalDays - daysElapsed).coerceAtLeast(0)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Photo
                if (itm.photoUri.isNotBlank()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                    ) {
                        AsyncImage(
                            model = itm.photoUri,
                            contentDescription = "Item Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // Description and Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = itm.description,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    AssistChip(
                        onClick = {},
                        label = { Text(status.displayName) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color(android.graphics.Color.parseColor(status.color)).copy(alpha = 0.2f),
                            labelColor = Color(android.graphics.Color.parseColor(status.color))
                        )
                    )
                }

                HorizontalDivider()

                // Found Info
                DetailSection(
                    title = "Found",
                    content = buildString {
                        append(dateFormat.format(Date(itm.foundDate)))
                        append(" at ")
                        append(timeFormat.format(Date(itm.foundDate)))
                        append("\nLocation: ${itm.foundZone}")
                    }
                )

                // Category
                DetailSection(
                    title = "Category",
                    content = com.eventmonitor.core.domain.models.ItemCategory.fromString(itm.category).displayName
                )

                // Item Details
                if (itm.color.isNotBlank() || itm.brand.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (itm.color.isNotBlank()) {
                            Column(modifier = Modifier.weight(1f)) {
                                DetailLabel("Color")
                                DetailValue(itm.color)
                            }
                        }
                        if (itm.brand.isNotBlank()) {
                            Column(modifier = Modifier.weight(1f)) {
                                DetailLabel("Brand")
                                DetailValue(itm.brand)
                            }
                        }
                    }
                }

                // Identifying Marks
                if (itm.identifyingMarks.isNotBlank()) {
                    DetailSection(
                        title = "Identifying Marks",
                        content = itm.identifyingMarks
                    )
                }

                // Reported By
                if (itm.reportedBy.isNotBlank()) {
                    DetailSection(
                        title = "Reported By",
                        content = itm.reportedBy
                    )
                }

                // Notes
                if (itm.notes.isNotBlank()) {
                    DetailSection(
                        title = "Additional Notes",
                        content = itm.notes
                    )
                }

                // Claimed Info
                if (itm.claimedDate > 0) {
                    DetailSection(
                        title = "Claimed",
                        content = buildString {
                            append(dateFormat.format(Date(itm.claimedDate)))
                            if (itm.claimedBy.isNotBlank()) {
                                append("\nBy: ${itm.claimedBy}")
                            }
                            if (itm.claimerContact.isNotBlank()) {
                                append("\nContact: ${itm.claimerContact}")
                            }
                        }
                    )
                }

                // Donation Progress (only for pending items)
                if (itm.status == ItemStatus.PENDING.name) {
                    HorizontalDivider()

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (canDonate)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
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
                                        tint = if (canDonate)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = if (canDonate) "Ready for Donation" else "Holding Period",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (canDonate)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                Text(
                                    text = if (canDonate) "180/180 days" else "$daysElapsed/180 days",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (canDonate)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            LinearProgressIndicator(
                                progress = { (daysElapsed.toFloat() / totalDays).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                color = if (canDonate)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.secondary,
                            )

                            if (!canDonate) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "$daysRemaining days remaining until donation eligibility",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    // Actions
                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { showClaimDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Claim Item")
                        }

                        OutlinedButton(
                            onClick = { viewModel.updateItemStatus(ItemStatus.DONATED.name) },
                            enabled = canDonate,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Donate")
                        }
                    }
                }
            }
        } ?: Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }

    // Claim Dialog
    if (showClaimDialog) {
        var claimerName by remember { mutableStateOf("") }
        var contact by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showClaimDialog = false },
            title = { Text("Claim Item") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = claimerName,
                        onValueChange = { claimerName = it },
                        label = { Text("Claimer Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = contact,
                        onValueChange = { contact = it },
                        label = { Text("Contact (Phone/Email)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Verification Notes") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.claimItem(claimerName, contact, notes)
                        showClaimDialog = false
                    },
                    enabled = claimerName.isNotBlank()
                ) {
                    Text("Claim")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClaimDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Item") },
            text = { Text("Are you sure you want to delete this item? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteItem()
                        showDeleteDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DetailSection(title: String, content: String) {
    Column {
        DetailLabel(title)
        DetailValue(content)
    }
}

@Composable
fun DetailLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.outline,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun DetailValue(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge
    )
}
