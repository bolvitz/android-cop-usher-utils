package com.eventmonitor.feature.lostandfound.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
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

    var showCategoryDialog by remember { mutableStateOf(false) }

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
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.saveItem() },
                        enabled = !isSaving && description.isNotBlank() && foundZone.isNotBlank()
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
                            contentDescription = "Item Photo",
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
                            Text("Add Photo")
                        }
                    }
                }
            }

            // Description (Required)
            OutlinedTextField(
                value = description,
                onValueChange = viewModel::updateDescription,
                label = { Text("Description *") },
                placeholder = { Text("e.g., Black leather wallet") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                isError = description.isBlank()
            )

            // Category
            OutlinedTextField(
                value = ItemCategory.fromString(category).displayName,
                onValueChange = {},
                label = { Text("Category") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showCategoryDialog = true }) {
                        Icon(Icons.Default.ArrowDropDown, "Select Category")
                    }
                }
            )

            // Found Zone (Required)
            OutlinedTextField(
                value = foundZone,
                onValueChange = viewModel::updateFoundZone,
                label = { Text("Found Location/Zone *") },
                placeholder = { Text("e.g., Main Hall, Row 5") },
                modifier = Modifier.fillMaxWidth(),
                isError = foundZone.isBlank()
            )

            // Color
            OutlinedTextField(
                value = color,
                onValueChange = viewModel::updateColor,
                label = { Text("Color") },
                placeholder = { Text("e.g., Black, Blue") },
                modifier = Modifier.fillMaxWidth()
            )

            // Brand
            OutlinedTextField(
                value = brand,
                onValueChange = viewModel::updateBrand,
                label = { Text("Brand") },
                placeholder = { Text("e.g., Nike, Apple") },
                modifier = Modifier.fillMaxWidth()
            )

            // Identifying Marks
            OutlinedTextField(
                value = identifyingMarks,
                onValueChange = viewModel::updateIdentifyingMarks,
                label = { Text("Identifying Marks") },
                placeholder = { Text("Unique features, scratches, engravings") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            // Reported By
            OutlinedTextField(
                value = reportedBy,
                onValueChange = viewModel::updateReportedBy,
                label = { Text("Reported By") },
                placeholder = { Text("Staff member name") },
                modifier = Modifier.fillMaxWidth()
            )

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = viewModel::updateNotes,
                label = { Text("Additional Notes") },
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
