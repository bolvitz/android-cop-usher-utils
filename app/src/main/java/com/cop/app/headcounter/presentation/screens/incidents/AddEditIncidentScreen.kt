package com.cop.app.headcounter.presentation.screens.incidents

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.cop.app.headcounter.domain.models.IncidentSeverity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditIncidentScreen(
    branchId: String,
    incidentId: String?,
    onNavigateBack: () -> Unit,
    viewModel: AddEditIncidentViewModel = hiltViewModel()
) {
    val title by viewModel.title.collectAsState()
    val description by viewModel.description.collectAsState()
    val severity by viewModel.severity.collectAsState()
    val category by viewModel.category.collectAsState()
    val location by viewModel.location.collectAsState()
    val photoUri by viewModel.photoUri.collectAsState()
    val reportedBy by viewModel.reportedBy.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var showSeverityDialog by remember { mutableStateOf(false) }
    var showPhotoOptions by remember { mutableStateOf(false) }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.updatePhotoUri(it.toString())
        }
    }

    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (incidentId == null) "Report Incident" else "Edit Incident") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.saveIncident() },
                        enabled = !isSaving && title.isNotBlank() && description.isNotBlank()
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Default.Check, "Save")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Photo Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clickable { showPhotoOptions = true },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (photoUri.isNotBlank()) {
                        AsyncImage(
                            model = photoUri,
                            contentDescription = "Incident Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.AddAPhoto,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Add Photo Evidence")
                        }
                    }
                }
            }

            // Title (Required)
            OutlinedTextField(
                value = title,
                onValueChange = viewModel::updateTitle,
                label = { Text("Incident Title *") },
                placeholder = { Text("Brief title of the incident") },
                modifier = Modifier.fillMaxWidth(),
                isError = title.isBlank(),
                singleLine = true
            )

            // Description (Required)
            OutlinedTextField(
                value = description,
                onValueChange = viewModel::updateDescription,
                label = { Text("Description *") },
                placeholder = { Text("Detailed description of what happened") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 5,
                isError = description.isBlank()
            )

            // Severity
            OutlinedTextField(
                value = IncidentSeverity.fromString(severity).displayName,
                onValueChange = {},
                label = { Text("Severity *") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showSeverityDialog = true }) {
                        Icon(Icons.Default.ArrowDropDown, "Select Severity")
                    }
                }
            )

            // Category
            OutlinedTextField(
                value = category,
                onValueChange = viewModel::updateCategory,
                label = { Text("Category") },
                placeholder = { Text("e.g., Safety, Security, Maintenance, Medical") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Location
            OutlinedTextField(
                value = location,
                onValueChange = viewModel::updateLocation,
                label = { Text("Specific Location") },
                placeholder = { Text("e.g., Main Hall, Parking Lot A") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Reported By
            OutlinedTextField(
                value = reportedBy,
                onValueChange = viewModel::updateReportedBy,
                label = { Text("Reported By") },
                placeholder = { Text("Your name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Additional Notes
            OutlinedTextField(
                value = notes,
                onValueChange = viewModel::updateNotes,
                label = { Text("Additional Notes") },
                placeholder = { Text("Any additional information") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4
            )

            Text(
                "* Required fields",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }

    // Severity Selection Dialog
    if (showSeverityDialog) {
        AlertDialog(
            onDismissRequest = { showSeverityDialog = false },
            title = { Text("Select Severity Level") },
            text = {
                Column {
                    IncidentSeverity.entries.forEach { severityOption ->
                        ListItem(
                            headlineContent = { Text(severityOption.displayName) },
                            leadingContent = {
                                RadioButton(
                                    selected = severity == severityOption.name,
                                    onClick = {
                                        viewModel.updateSeverity(severityOption.name)
                                        showSeverityDialog = false
                                    }
                                )
                            },
                            modifier = Modifier.clickable {
                                viewModel.updateSeverity(severityOption.name)
                                showSeverityDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSeverityDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Photo Options Dialog
    if (showPhotoOptions) {
        AlertDialog(
            onDismissRequest = { showPhotoOptions = false },
            title = { Text("Add Photo") },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text("Take Photo") },
                        leadingContent = { Icon(Icons.Default.CameraAlt, null) },
                        modifier = Modifier.clickable {
                            galleryLauncher.launch("image/*")
                            showPhotoOptions = false
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Choose from Gallery") },
                        leadingContent = { Icon(Icons.Default.PhotoLibrary, null) },
                        modifier = Modifier.clickable {
                            galleryLauncher.launch("image/*")
                            showPhotoOptions = false
                        }
                    )
                    if (photoUri.isNotBlank()) {
                        ListItem(
                            headlineContent = { Text("Remove Photo") },
                            leadingContent = { Icon(Icons.Default.Delete, null) },
                            modifier = Modifier.clickable {
                                viewModel.updatePhotoUri("")
                                showPhotoOptions = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPhotoOptions = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Error Snackbar
    errorMessage?.let { error ->
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("Dismiss")
                }
            }
        ) {
            Text(error)
        }
    }
}
