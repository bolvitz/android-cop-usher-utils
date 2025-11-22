package com.cop.app.headcounter.presentation.screens.branches

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BranchSetupScreen(
    viewModel: BranchSetupViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onManageAreas: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Branch") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::updateName,
                label = { Text("Branch Name") },
                placeholder = { Text("e.g., Main Campus") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.location,
                onValueChange = viewModel::updateLocation,
                label = { Text("Location") },
                placeholder = { Text("Address or location description") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.code,
                onValueChange = viewModel::updateCode,
                label = { Text("Branch Code") },
                placeholder = { Text("e.g., MC") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.contactPerson,
                onValueChange = viewModel::updateContactPerson,
                label = { Text("Contact Person (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.contactEmail,
                onValueChange = viewModel::updateContactEmail,
                label = { Text("Contact Email (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.contactPhone,
                onValueChange = viewModel::updateContactPhone,
                label = { Text("Contact Phone (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true
            )

            Text("Area Configuration", style = MaterialTheme.typography.titleMedium)

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "After creating the branch, you can customize areas dynamically:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "• Add bays (Bay 1, Bay 2, etc.)",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "• Add baby rooms",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "• Add car parking areas",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "• Add any custom areas you need",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    viewModel.saveBranch { branchId ->
                        // Navigate to area management after creating branch
                        onManageAreas(branchId)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Create Branch & Setup Areas")
                }
            }
        }
    }
}
