package com.eventmonitor.app.presentation.screens.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.eventmonitor.shared.presentation.scope.VenueScopeRow

/** Asks which venue a new record belongs to, when the list is showing all venues. */
@Composable
fun VenuePickerDialog(
    venues: List<VenueScopeRow>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Which venue?") },
        text = {
            LazyColumn {
                items(venues, key = { it.id }) { venue ->
                    ListItem(
                        headlineContent = { Text(venue.name) },
                        supportingContent = venue.location.ifBlank { null }?.let { { Text(it) } },
                        colors = ListItemDefaults.colors(containerColor = AlertDialogDefaults.containerColor),
                        modifier = Modifier
                            .heightIn(min = 56.dp)
                            .clickable(role = Role.Button) { onPick(venue.id) }
                    )
                }
            }
        }
    )
}
