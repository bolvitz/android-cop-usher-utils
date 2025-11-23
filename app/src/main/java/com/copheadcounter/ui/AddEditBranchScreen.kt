package com.copheadcounter.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.copheadcounter.model.Branch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBranchScreen(
    branch: Branch?,
    onSave: (Branch) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf(branch?.name ?: "") }
    var location by remember { mutableStateOf(branch?.location ?: "") }
    var description by remember { mutableStateOf(branch?.description ?: "") }
    var isCounterEnabled by remember { mutableStateOf(branch?.isCounterEnabled ?: true) }
    var isLostFoundEnabled by remember { mutableStateOf(branch?.isLostFoundEnabled ?: true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (branch == null) "Add Branch" else "Edit Branch") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
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
            // Branch Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Branch Name *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Location
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("e.g., 123 Main St") }
            )

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                placeholder = { Text("Brief description of the branch...") }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Features Section
            Text(
                text = "Features",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Enable or disable features for this branch",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Counter Feature Toggle
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCounterEnabled) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Head Counter",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Enable counting feature",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = isCounterEnabled,
                        onCheckedChange = { isCounterEnabled = it }
                    )
                }
            }

            // Lost & Found Feature Toggle
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isLostFoundEnabled) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Lost & Found Inventory",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Enable lost & found tracking",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = isLostFoundEnabled,
                        onCheckedChange = { isLostFoundEnabled = it }
                    )
                }
            }

            // Warning if both disabled
            if (!isCounterEnabled && !isLostFoundEnabled) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "Warning: All features are disabled for this branch. Please enable at least one feature.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val newBranch = if (branch == null) {
                                Branch(
                                    name = name,
                                    location = location,
                                    description = description,
                                    isCounterEnabled = isCounterEnabled,
                                    isLostFoundEnabled = isLostFoundEnabled
                                )
                            } else {
                                branch.copy(
                                    name = name,
                                    location = location,
                                    description = description,
                                    isCounterEnabled = isCounterEnabled,
                                    isLostFoundEnabled = isLostFoundEnabled
                                )
                            }
                            onSave(newBranch)
                        }
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (branch == null) "Add Branch" else "Save Changes")
                }
            }
        }
    }
}
