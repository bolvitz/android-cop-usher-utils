package com.eventmonitor.feature.lostandfound.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.eventmonitor.core.domain.models.ItemCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditLostItemScreen(
    locationId: String,
    itemId: String?,
    onNavigateBack: () -> Unit,
    viewModel: AddEditLostItemViewModel = hiltViewModel()
) {
    val description by viewModel.description.collectAsState()
    val category by viewModel.category.collectAsState()
    val foundZone by viewModel.foundZone.collectAsState()
    val photoUri by viewModel.photoUri.collectAsState()
    val color by viewModel.color.collectAsState()
    val brand by viewModel.brand.collectAsState()
    val identifyingMarks by viewModel.identifyingMarks.collectAsState()
    val reportedBy by viewModel.reportedBy.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val events by viewModel.events.collectAsState()
    val selectedEventId by viewModel.selectedEventId.collectAsState()

    var showCategoryDialog by remember { mutableStateOf(false) }
    var showEventDropdown by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val context = LocalContext.current

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            // Photo captured successfully, URI is already set
        }
    }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.updatePhotoUri(it.toString())
        }
    }

    var showPhotoOptions by remember { mutableStateOf(false) }

    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (itemId == null) "Add Lost Item" else "Edit Lost Item") },
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
                text = "Item Photo",
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
                            contentDescription = "Item Photo",
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
                            Text("Tap to add photo", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // Basic Information Section
            Text(
                text = "Basic Information",
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
                            "${event.eventType.name} - ${java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(event.event.date))}"
                        }
                    } ?: "None",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Event (Optional)") },
                    supportingText = { Text("Link this item to a specific event") },
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
                                        text = eventWithDetails.eventType.name,
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

            // Description (Required)
            OutlinedTextField(
                value = description,
                onValueChange = viewModel::updateDescription,
                label = { Text("Item Description *") },
                placeholder = { Text("e.g., Black leather wallet") },
                supportingText = { Text("What is the item?") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                isError = description.isBlank(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            // Found Zone (Required)
            OutlinedTextField(
                value = foundZone,
                onValueChange = viewModel::updateFoundZone,
                label = { Text("Found Location *") },
                placeholder = { Text("e.g., Main Hall, Row 5") },
                supportingText = { Text("Where was it found?") },
                modifier = Modifier.fillMaxWidth(),
                isError = foundZone.isBlank(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = {
                        showCategoryDialog = true
                        focusManager.clearFocus()
                    }
                )
            )

            // Category
            OutlinedTextField(
                value = ItemCategory.fromString(category).displayName,
                onValueChange = {},
                label = { Text("Category") },
                supportingText = { Text("Tap to select a category") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCategoryDialog = true },
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                trailingIcon = {
                    Icon(Icons.Default.ArrowDropDown, "Select Category")
                }
            )

            // Item Details Section
            Text(
                text = "Item Details (Optional)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            // Color
            OutlinedTextField(
                value = color,
                onValueChange = viewModel::updateColor,
                label = { Text("Color") },
                placeholder = { Text("e.g., Black, Blue") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            // Brand
            OutlinedTextField(
                value = brand,
                onValueChange = viewModel::updateBrand,
                label = { Text("Brand") },
                placeholder = { Text("e.g., Nike, Apple") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            // Identifying Marks
            OutlinedTextField(
                value = identifyingMarks,
                onValueChange = viewModel::updateIdentifyingMarks,
                label = { Text("Identifying Marks") },
                placeholder = { Text("Unique features, scratches, engravings") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            // Report Information Section
            Text(
                text = "Report Information",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            // Reported By
            OutlinedTextField(
                value = reportedBy,
                onValueChange = viewModel::updateReportedBy,
                label = { Text("Reported By") },
                placeholder = { Text("Staff member name") },
                supportingText = { Text("Who found this item?") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            // Notes
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
                        if (description.isNotBlank() && foundZone.isNotBlank()) {
                            viewModel.saveItem()
                        }
                    }
                )
            )

            // Save Button
            Button(
                onClick = { viewModel.saveItem() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving && description.isNotBlank() && foundZone.isNotBlank()
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
                    Text("Save Item")
                }
            }

            Text(
                "* Required fields",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
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
                            // Create temp file URI and launch camera
                            // For simplicity, using gallery for now
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

    // Category Selection Dialog
    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text("Select Category") },
            text = {
                Column {
                    ItemCategory.entries.forEach { cat ->
                        ListItem(
                            headlineContent = { Text(cat.displayName) },
                            modifier = Modifier.clickable {
                                viewModel.updateCategory(cat.name)
                                showCategoryDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Error Snackbar
    errorMessage?.let { error ->
        Snackbar(
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
