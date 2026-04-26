package com.eventmonitor.app.presentation.screens.eventtypes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.eventmonitor.app.presentation.viewmodels.EventTypeManagementViewModel
import com.eventmonitor.core.common.theme.MonoTiny
import com.eventmonitor.core.common.theme.Sage
import com.eventmonitor.core.common.theme.Signal
import com.eventmonitor.core.common.ui.ArcadeBackground
import com.eventmonitor.core.common.ui.FieldTokens
import com.eventmonitor.core.common.ui.KeyTone
import com.eventmonitor.core.common.ui.SoftAppBar
import com.eventmonitor.core.common.ui.SoftBottomDock
import com.eventmonitor.core.common.ui.SoftCard
import com.eventmonitor.core.common.ui.SoftIconButton
import com.eventmonitor.core.common.ui.SoftAlertDialog
import com.eventmonitor.core.common.ui.SoftButtonTone
import com.eventmonitor.core.common.ui.SoftKey
import com.eventmonitor.core.common.ui.SoftPrimaryButton
import com.eventmonitor.core.common.ui.SoftSection
import com.eventmonitor.core.common.ui.SevPill
import com.eventmonitor.core.common.ui.Severity
import com.eventmonitor.core.common.ui.softHeadline
import com.eventmonitor.core.common.utils.rememberHapticFeedback
import com.eventmonitor.core.data.local.entities.EventTypeEntity

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------

@Composable
fun ServiceTypeManagementScreen(
    viewModel: EventTypeManagementViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
) {
    val haptic = rememberHapticFeedback()
    val eventTypes by viewModel.eventTypes.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<EventTypeEntity?>(null) }
    var pendingDelete by remember { mutableStateOf<EventTypeEntity?>(null) }

    LaunchedEffect(uiState.message, uiState.error) {
        if (uiState.message != null || uiState.error != null) {
            kotlinx.coroutines.delay(2200)
            viewModel.clearMessage()
        }
    }

    val active = eventTypes.count { it.isActive }
    val archived = eventTypes.size - active

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ArcadeBackground(modifier = Modifier.matchParentSize())

        Column(modifier = Modifier.fillMaxSize()) {
            SoftAppBar(
                title = "Event Types",
                subtitle = "§ Catalog",
                onBack = { haptic.light(); onNavigateBack() },
                trailing = {
                    SoftIconButton(glyph = "+", onClick = {
                        haptic.medium()
                        editing = null
                        showEditor = true
                    })
                },
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                item("stats") {
                    StatStrip(total = eventTypes.size, live = active, archived = archived)
                }

                item("roster-head") {
                    RosterHeader(count = eventTypes.size)
                }

                if (eventTypes.isEmpty()) {
                    item("empty") {
                        EmptyPanel(onAdd = {
                            haptic.medium(); editing = null; showEditor = true
                        })
                    }
                } else {
                    items(eventTypes, key = { it.id }) { type ->
                        EventTypeRow(
                            type = type,
                            onEdit = {
                                haptic.light()
                                editing = type
                                showEditor = true
                            },
                            onDelete = {
                                haptic.medium()
                                pendingDelete = type
                            },
                            onToggle = { active ->
                                haptic.medium()
                                viewModel.toggleServiceTypeStatus(type.id, active)
                            },
                        )
                    }
                    item("colophon") {
                        Spacer(Modifier.height(20.dp))
                        Colophon(total = eventTypes.size, live = active)
                    }
                }
            }
        }

        // Sticky bottom action — ink slab.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            AddSlab(onClick = {
                haptic.medium(); editing = null; showEditor = true
            })
        }

        // Toast-style banner for messages.
        AnimatedVisibility(
            visible = uiState.message != null || uiState.error != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            FieldBanner(
                message = uiState.message ?: uiState.error.orEmpty(),
                isError = uiState.error != null,
            )
        }
    }

    if (showEditor) {
        EventTypeEditorSheet(
            existing = editing,
            onDismiss = { showEditor = false; editing = null },
            onSave = { name, day, time, desc ->
                editing?.let {
                    viewModel.updateServiceType(
                        it.copy(name = name, dayType = day, time = time, description = desc),
                    )
                } ?: viewModel.createServiceType(name, day, time, desc)
                showEditor = false
                editing = null
            },
        )
    }

    pendingDelete?.let { target ->
        SoftAlertDialog(
            onDismiss = { haptic.light(); pendingDelete = null },
            eyebrow = "Delete · Event Type",
            title = "Are you sure?",
            message = "\"${target.name}\" will be removed from the catalog. You can't undo this.",
            confirmLabel = "Delete",
            confirmTone = SoftButtonTone.Destructive,
            onConfirm = {
                haptic.strong()
                viewModel.deleteServiceType(target.id)
                pendingDelete = null
            },
        )
    }
}

// ---------------------------------------------------------------------------
// Stat strip
// ---------------------------------------------------------------------------

@Composable
private fun StatStrip(total: Int, live: Int, archived: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 14.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        StatCell(label = "TOTAL", value = total.toString())
        StatDivider()
        StatCell(label = "LIVE", value = live.toString(), accent = live > 0)
        StatDivider()
        StatCell(label = "ARCHIVED", value = archived.toString())
    }
    Spacer(Modifier.height(20.dp))
}

@Composable
private fun StatCell(label: String, value: String, accent: Boolean = false) {
    Column {
        Text(
            text = label,
            style = MonoTiny,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (accent) {
                Spacer(Modifier.width(6.dp))
                Box(
                    Modifier
                        .background(Signal)
                        .width(7.dp)
                        .height(7.dp),
                )
            }
        }
    }
}

@Composable
private fun StatDivider() {
    Box(
        Modifier
            .width(FieldTokens.Hair)
            .height(32.dp)
            .background(MaterialTheme.colorScheme.outline),
    )
}

// ---------------------------------------------------------------------------
// Roster header / empty
// ---------------------------------------------------------------------------

@Composable
private fun RosterHeader(count: Int) {
    SoftSection(
        title = "Curate the catalog.",
        eyebrow = "§§ ROSTER",
        hint = "$count entries",
    )
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun EmptyPanel(onAdd: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {
        Text(
            text = "No event types yet.",
            style = softHeadline(20),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Add your first type to begin scheduling events across venues.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        SoftCard(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = "+ ADD FIRST TYPE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Row
// ---------------------------------------------------------------------------

@Composable
private fun EventTypeRow(
    type: EventTypeEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val isActive = type.isActive

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 10.dp),
    ) {
        SoftCard(
            onClick = onEdit,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = type.name,
                        style = softHeadline(20),
                        color = if (isActive) MaterialTheme.colorScheme.onBackground
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${type.dayType.uppercase()} · ${type.time.uppercase()}",
                        style = MonoTiny,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (type.description.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = type.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(horizontalAlignment = Alignment.End) {
                    if (isActive) {
                        SevPill(severity = Severity.LOW, label = "LIVE")
                    } else {
                        SevPill(severity = Severity.NEUTRAL, label = "IDLE")
                    }
                }
                Box {
                    Text(
                        text = "⋯",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .clickable { menuOpen = true },
                    )
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                        containerColor = MaterialTheme.colorScheme.background,
                    ) {
                        DropdownMenuItem(
                            text = { Text("EDIT", style = MaterialTheme.typography.labelMedium) },
                            onClick = { menuOpen = false; onEdit() },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (isActive) "ARCHIVE" else "REINSTATE",
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            },
                            onClick = { menuOpen = false; onToggle(!isActive) },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "DELETE",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Signal,
                                )
                            },
                            onClick = { menuOpen = false; onDelete() },
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Add slab + ink button + banner + colophon
// ---------------------------------------------------------------------------

@Composable
private fun AddSlab(onClick: () -> Unit) {
    val ink = MaterialTheme.colorScheme.onBackground
    val paper = MaterialTheme.colorScheme.background
    Column {
        Spacer(Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ink)
                .clickable { onClick() }
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        "NEW",
                        style = MonoTiny,
                        color = paper.copy(alpha = 0.6f),
                    )
                    Text(
                        "Add event type",
                        style = softHeadline(20),
                        color = paper,
                    )
                }
                Text(
                    text = "+",
                    style = MaterialTheme.typography.displaySmall,
                    color = paper,
                )
            }
        }
    }
}

@Composable
private fun FieldBanner(message: String, isError: Boolean) {
    val ink = MaterialTheme.colorScheme.onBackground
    val paper = MaterialTheme.colorScheme.background
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .background(if (isError) Signal else ink)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (isError) "ERR ·" else "OK ·",
            style = MaterialTheme.typography.labelMedium,
            color = paper,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = paper,
        )
    }
}

@Composable
private fun Colophon(total: Int, live: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("CATALOG · IN SYNC", style = MonoTiny, color = Sage)
            Text(
                "$total ENTRIES · $live LIVE",
                style = MonoTiny,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "com.eventmonitor.app",
                style = MonoTiny,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Editor sheet (Add + Edit, full-bleed)
// ---------------------------------------------------------------------------

@Composable
private fun EventTypeEditorSheet(
    existing: EventTypeEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, day: String, time: String, description: String) -> Unit,
) {
    val haptic = rememberHapticFeedback()
    val isEdit = existing != null

    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var day by remember { mutableStateOf(existing?.dayType ?: "Sunday") }
    var description by remember { mutableStateOf(existing?.description.orEmpty()) }
    val (initialHour, initialMinute) = remember(existing) {
        existing?.let { parseTime(it.time) } ?: (9 to 0)
    }
    var hour by remember { mutableStateOf(initialHour) }
    var minute by remember { mutableStateOf(initialMinute) }
    var dayMenuOpen by remember { mutableStateOf(false) }
    var showTimeDialog by remember { mutableStateOf(false) }

    val timeString = remember(hour, minute) { formatTime(hour, minute) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            ArcadeBackground(modifier = Modifier.matchParentSize())

            Column(modifier = Modifier.fillMaxSize()) {
                SoftAppBar(
                    title = if (isEdit) "Edit Type" else "Add Type",
                    subtitle = if (isEdit) "§ Edit · Entry" else "§ New · Entry",
                    onBack = { haptic.light(); onDismiss() },
                    backGlyph = "✕",
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 88.dp)
                        .imePadding(),
                ) {
                    // Lane: Name
                    FieldLane(label = "NAME", hint = "Required") {
                        InkInput(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = "e.g. Sunday Morning Service",
                            capitalization = KeyboardCapitalization.Words,
                            singleLine = true,
                        )
                    }
                    Spacer(Modifier.height(20.dp))

                    // Lane: Day (dropdown)
                    FieldLane(label = "DAY", hint = "Of the week") {
                        Box {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { dayMenuOpen = true },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = day,
                                    style = softHeadline(20),
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                                Text(
                                    text = "▾",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            DropdownMenu(
                                expanded = dayMenuOpen,
                                onDismissRequest = { dayMenuOpen = false },
                                containerColor = MaterialTheme.colorScheme.background,
                            ) {
                                DAYS.forEach { d ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                d.uppercase(),
                                                style = MaterialTheme.typography.labelMedium,
                                            )
                                        },
                                        onClick = {
                                            haptic.selection()
                                            day = d
                                            dayMenuOpen = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))

                    // Lane: Time (opens picker)
                    FieldLane(label = "TIME", hint = "Tap to set") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    haptic.light()
                                    showTimeDialog = true
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = timeString,
                                style = softHeadline(20),
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Text(
                                text = "◷",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))

                    // Lane: Description
                    FieldLane(label = "NOTES", hint = "Optional") {
                        InkInput(
                            value = description,
                            onValueChange = { description = it },
                            placeholder = "Additional details…",
                            capitalization = KeyboardCapitalization.Sentences,
                            singleLine = false,
                        )
                    }
                    Spacer(Modifier.height(20.dp))

                    if (existing != null) {
                        Spacer(Modifier.height(20.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "ID · ${existing.id.take(8).uppercase()}",
                                style = MonoTiny,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = if (existing.isActive) "STATUS · LIVE" else "STATUS · IDLE",
                                style = MonoTiny,
                                color = if (existing.isActive) Sage
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // Sticky save slab
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .imePadding(),
            ) {
                SaveSlab(
                    label = if (isEdit) "Save changes" else "Create entry",
                    enabled = name.isNotBlank(),
                    onClick = {
                        haptic.success()
                        onSave(name.trim(), day, timeString, description.trim())
                    },
                )
            }
        }
    }

    if (showTimeDialog) {
        TimePickerDialog(
            onDismiss = { showTimeDialog = false },
            onConfirm = { h, m ->
                haptic.selection()
                hour = h
                minute = m
                showTimeDialog = false
            },
            initialHour = hour,
            initialMinute = minute,
        )
    }
}

@Composable
private fun FieldLane(
    label: String,
    hint: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "§ $label",
                style = MonoTiny,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = hint.uppercase(),
                style = MonoTiny,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun InkInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    capitalization: KeyboardCapitalization,
    singleLine: Boolean,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val style = softHeadline(20).copy(color = ink)

    Box(modifier = Modifier.fillMaxWidth()) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                style = style.copy(color = muted.copy(alpha = 0.55f)),
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = style,
            cursorBrush = SolidColor(ink),
            keyboardOptions = KeyboardOptions(capitalization = capitalization),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SaveSlab(label: String, enabled: Boolean, onClick: () -> Unit) {
    SoftBottomDock {
        SoftPrimaryButton(
            label = label,
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            trailingGlyph = "→",
        )
    }
}

// ---------------------------------------------------------------------------
// Time picker
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    initialHour: Int = 9,
    initialMinute: Int = 0,
) {
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false,
    )

    SoftAlertDialog(
        onDismiss = onDismiss,
        eyebrow = "§ Set Time",
        title = "Pick a time",
        confirmLabel = "Confirm",
        onConfirm = { onConfirm(state.hour, state.minute) },
        body = { TimePicker(state = state) },
    )
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

private val DAYS = listOf(
    "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday",
)

private fun formatTime(hour: Int, minute: Int): String {
    val period = if (hour >= 12) "PM" else "AM"
    val display = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return String.format("%d:%02d %s", display, minute, period)
}

private fun parseTime(timeString: String): Pair<Int, Int> {
    return try {
        val parts = timeString.trim().split(" ")
        val timeParts = parts[0].split(":")
        val hour = timeParts[0].toInt()
        val minute = timeParts.getOrNull(1)?.toInt() ?: 0
        val isPM = parts.getOrNull(1)?.uppercase() == "PM"

        val hour24 = when {
            isPM && hour != 12 -> hour + 12
            !isPM && hour == 12 -> 0
            else -> hour
        }

        hour24 to minute
    } catch (e: Exception) {
        9 to 0
    }
}
