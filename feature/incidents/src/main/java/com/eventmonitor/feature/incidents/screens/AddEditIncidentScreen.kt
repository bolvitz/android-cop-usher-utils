package com.eventmonitor.feature.incidents.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.eventmonitor.core.domain.models.IncidentSeverity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditIncidentScreen(
    venueId: String,
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
    val events by viewModel.events.collectAsState()
    val selectedEventId by viewModel.selectedEventId.collectAsState()

    var showSeverityDialog by remember { mutableStateOf(false) }
    var showPhotoOptions by remember { mutableStateOf(false) }
    var showEventDropdown by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

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
            Text(
                text = "Photo Evidence",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
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
                        IconButton(
                            onClick = { showPhotoOptions = true },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Change Photo",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.AddAPhoto,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Tap to add photo evidence", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // Basic Information Section
            Text(
                text = "Incident Details",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            // Event Selection (Optional)
            ExposedDropdownMenuBox(
                expanded = showEventDropdown,
                onExpandedChange = { showEventDropdown = it }
            ) {
                OutlinedTextField(
                    value = selectedEventId?.let { id ->
                        events.find { it.event.id == id }?.let { event ->
                            "${event.eventType?.name ?: "Event"} - ${java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(event.event.date))}"
                        }
                    } ?: "None",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Event (Optional)") },
                    supportingText = { Text("Link this incident to a specific event") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = showEventDropdown)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )

                ExposedDropdownMenu(
                    expanded = showEventDropdown,
                    onDismissRequest = { showEventDropdown = false }
                ) {
                    // None option
                    DropdownMenuItem(
                        text = { Text("None") },
                        onClick = {
                            viewModel.updateSelectedEvent(null)
                            showEventDropdown = false
                        }
                    )

                    if (events.isNotEmpty()) {
                        HorizontalDivider()
                    }

                    // Event options
                    events.forEach { eventWithDetails ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = eventWithDetails.eventType?.name ?: "Event",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                                            .format(java.util.Date(eventWithDetails.event.date)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                viewModel.updateSelectedEvent(eventWithDetails.event.id)
                                showEventDropdown = false
                            }
                        )
                    }
                }
            }

            // Title (Required)
            OutlinedTextField(
                value = title,
                onValueChange = viewModel::updateTitle,
                label = { Text("Incident Title *") },
                placeholder = { Text("e.g., Slip and fall, Fire alarm") },
                supportingText = { Text("Brief summary of the incident") },
                modifier = Modifier.fillMaxWidth(),
                isError = title.isBlank(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            // Description (Required)
            OutlinedTextField(
                value = description,
                onValueChange = viewModel::updateDescription,
                label = { Text("Description *") },
                placeholder = { Text("Detailed description of what happened") },
                supportingText = { Text("What happened? When did it occur?") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 5,
                isError = description.isBlank(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = {
                        showSeverityDialog = true
                        focusManager.clearFocus()
                    }
                )
            )

            // Severity
            OutlinedTextField(
                value = IncidentSeverity.fromString(severity).displayName,
                onValueChange = {},
                label = { Text("Severity Level *") },
                supportingText = { Text("Tap to select severity") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showSeverityDialog = true },
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                trailingIcon = {
                    Icon(Icons.Default.ArrowDropDown, "Select Severity")
                }
            )

            // Location and Category Section
            Text(
                text = "Location & Category",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            // Location
            OutlinedTextField(
                value = location,
                onValueChange = viewModel::updateLocation,
                label = { Text("Specific Location") },
                placeholder = { Text("e.g., Main Hall, Parking Lot A") },
                supportingText = { Text("Where did this happen?") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            // Category
            OutlinedTextField(
                value = category,
                onValueChange = viewModel::updateCategory,
                label = { Text("Category") },
                placeholder = { Text("e.g., Safety, Security, Maintenance") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            // Reporter Information Section
            Text(
                text = "Reporter Information",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            // Reported By
            OutlinedTextField(
                value = reportedBy,
                onValueChange = viewModel::updateReportedBy,
                label = { Text("Reported By") },
                placeholder = { Text("Your name") },
                supportingText = { Text("Who is reporting this incident?") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            // Additional Notes
            OutlinedTextField(
                value = notes,
                onValueChange = viewModel::updateNotes,
                label = { Text("Additional Notes") },
                placeholder = { Text("Any other relevant information") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (title.isNotBlank() && description.isNotBlank()) {
                            viewModel.saveIncident()
                        }
                    }
                )
            )

            // Save Button
            Button(
                onClick = { viewModel.saveIncident() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving && title.isNotBlank() && description.isNotBlank()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Saving...")
                } else {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Report Incident")
                }
            }

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
