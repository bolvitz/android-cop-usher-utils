package com.copheadcounter.ui.lostfound

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.copheadcounter.model.ItemStatus
import com.copheadcounter.model.LostFoundItem
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailsScreen(
    item: LostFoundItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClaim: (String, String) -> Unit,
    onUpdateStatus: (ItemStatus) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showClaimDialog by remember { mutableStateOf(false) }
    var showStatusMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    val dateFormatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy")
    val dateTimeFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Item Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showMoreMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                showMoreMenu = false
                                showDeleteDialog = true
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssistChip(
                            onClick = { },
                            label = { Text(item.category.displayName) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Category,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )

                        StatusChip(status = item.status)
                    }
                }
            }

            // Description
            DetailSection(title = "Description") {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            // Location & Date
            DetailSection(title = "Found Information") {
                DetailRow(
                    icon = Icons.Default.Place,
                    label = "Location",
                    value = item.location
                )
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                DetailRow(
                    icon = Icons.Default.CalendarToday,
                    label = "Date Found",
                    value = item.dateFound.format(dateFormatter)
                )

                // Deadline Info
                if (item.status == ItemStatus.ACTIVE) {
                    val daysUntilDeadline = item.getDaysUntilDeadline()
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (daysUntilDeadline < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "6-Month Deadline",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (daysUntilDeadline < 0) {
                                    "Overdue by ${-daysUntilDeadline} days"
                                } else {
                                    "$daysUntilDeadline days remaining"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (daysUntilDeadline < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Claim Information (if claimed)
            if (item.status == ItemStatus.CLAIMED && item.claimantName != null) {
                DetailSection(title = "Claim Information") {
                    DetailRow(
                        icon = Icons.Default.Person,
                        label = "Claimant Name",
                        value = item.claimantName
                    )
                    if (item.claimantContact != null) {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        DetailRow(
                            icon = Icons.Default.Phone,
                            label = "Contact",
                            value = item.claimantContact
                        )
                    }
                    if (item.claimDate != null) {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        DetailRow(
                            icon = Icons.Default.CalendarToday,
                            label = "Claim Date",
                            value = item.claimDate.format(dateFormatter)
                        )
                    }
                }
            }

            // Notes
            if (item.notes.isNotBlank()) {
                DetailSection(title = "Additional Notes") {
                    Text(
                        text = item.notes,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            // Metadata
            DetailSection(title = "Record Information") {
                DetailRow(
                    icon = Icons.Default.Schedule,
                    label = "Created",
                    value = item.createdAt.format(dateTimeFormatter)
                )
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                DetailRow(
                    icon = Icons.Default.Update,
                    label = "Last Updated",
                    value = item.updatedAt.format(dateTimeFormatter)
                )
            }

            // Action Buttons
            if (item.status == ItemStatus.ACTIVE) {
                Button(
                    onClick = { showClaimDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mark as Claimed")
                }
            }

            OutlinedButton(
                onClick = { showStatusMenu = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.ChangeCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Change Status")
            }

            // Status Change Menu
            DropdownMenu(
                expanded = showStatusMenu,
                onDismissRequest = { showStatusMenu = false }
            ) {
                ItemStatus.values().forEach { status ->
                    if (status != item.status) {
                        DropdownMenuItem(
                            text = { Text(status.name.replace("_", " ")) },
                            onClick = {
                                onUpdateStatus(status)
                                showStatusMenu = false
                            }
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            title = { Text("Delete Item") },
            text = { Text("Are you sure you want to delete this item? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
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

    // Claim Dialog
    if (showClaimDialog) {
        ClaimItemDialog(
            onDismiss = { showClaimDialog = false },
            onConfirm = { name, contact ->
                onClaim(name, contact)
                showClaimDialog = false
            }
        )
    }
}

@Composable
fun DetailSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ClaimItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var claimantName by remember { mutableStateOf("") }
    var claimantContact by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
        title = { Text("Mark as Claimed") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Enter the claimant's information:")

                OutlinedTextField(
                    value = claimantName,
                    onValueChange = { claimantName = it },
                    label = { Text("Claimant Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = claimantContact,
                    onValueChange = { claimantContact = it },
                    label = { Text("Contact (Phone/Email)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (claimantName.isNotBlank() && claimantContact.isNotBlank()) {
                        onConfirm(claimantName, claimantContact)
                    }
                },
                enabled = claimantName.isNotBlank() && claimantContact.isNotBlank()
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
