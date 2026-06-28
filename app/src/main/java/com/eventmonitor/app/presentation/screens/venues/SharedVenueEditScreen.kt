package com.eventmonitor.app.presentation.screens.venues

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
import com.eventmonitor.shared.data.models.VenueDto
import com.eventmonitor.shared.presentation.venues.VenueDetailViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedVenueEditScreen(
    venueId: String,
    onBack: () -> Unit,
    viewModel: VenueDetailViewModel = koinViewModel { parametersOf(venueId) }
) {
    val venue by viewModel.venue.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Venue") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val current = venue
        if (current == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            VenueForm(Modifier.padding(padding), current, onSave = { viewModel.save(it); onBack() })
        }
    }
}

@Composable
private fun VenueForm(modifier: Modifier, venue: VenueDto, onSave: (VenueDto) -> Unit) {
    var name by remember(venue.id) { mutableStateOf(venue.name) }
    var location by remember(venue.id) { mutableStateOf(venue.location) }
    var code by remember(venue.id) { mutableStateOf(venue.code) }
    var contactPerson by remember(venue.id) { mutableStateOf(venue.contactPerson) }
    var contactPhone by remember(venue.id) { mutableStateOf(venue.contactPhone) }
    var headCount by remember(venue.id) { mutableStateOf(venue.isHeadCountEnabled) }
    var lostFound by remember(venue.id) { mutableStateOf(venue.isLostAndFoundEnabled) }
    var incidents by remember(venue.id) { mutableStateOf(venue.isIncidentReportingEnabled) }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Code") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = contactPerson, onValueChange = { contactPerson = it }, label = { Text("Contact person") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = contactPhone, onValueChange = { contactPhone = it }, label = { Text("Contact phone") }, modifier = Modifier.fillMaxWidth())

        Text("Features", style = MaterialTheme.typography.titleSmall)
        ToggleRow("Head count", headCount) { headCount = it }
        ToggleRow("Lost & found", lostFound) { lostFound = it }
        ToggleRow("Incident reporting", incidents) { incidents = it }

        Button(
            onClick = {
                onSave(
                    venue.copy(
                        name = name, location = location, code = code,
                        contactPerson = contactPerson, contactPhone = contactPhone,
                        isHeadCountEnabled = headCount,
                        isLostAndFoundEnabled = lostFound,
                        isIncidentReportingEnabled = incidents
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save") }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
