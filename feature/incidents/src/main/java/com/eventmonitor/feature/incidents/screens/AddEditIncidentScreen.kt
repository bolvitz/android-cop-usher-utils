package com.eventmonitor.feature.incidents.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
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
import com.eventmonitor.core.common.ui.SoftKey
import com.eventmonitor.core.common.ui.SoftPrimaryButton
import com.eventmonitor.core.common.ui.SoftSection
import com.eventmonitor.core.common.ui.SoftToolButton
import com.eventmonitor.core.common.ui.Hairline
import com.eventmonitor.core.common.ui.HairlineSoft
import com.eventmonitor.core.common.ui.ZoneChip
import com.eventmonitor.core.common.ui.softHeadline
import com.eventmonitor.core.common.utils.rememberHapticFeedback
import com.eventmonitor.core.data.local.entities.EventWithDetails
import com.eventmonitor.core.domain.models.IncidentSeverity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ═════════════════════════════════════════════════════════════════════════════
// LOG INCIDENT — editorial redesign for the Add / Edit Incident screen.
//   · FieldAppBar     — [←] "LOG INCIDENT" · eyebrow "§ INCIDENT · NEW / AMEND" · [✓]
//   · Headline        — "File the report." + fraction "04 / 08 FILLED"
//   · Progress strip  — 8 ticks, filled as steps land
//   · Numbered steps  — 01 EVIDENCE · 02 HEADLINE · 03 BRIEF · 04 SEVERITY ·
//                       05 EVENT · 06 LOCUS · 07 CREDITS · 08 PREVIEW
//   · Action rail     — [CANCEL] [FILE / SAVE INCIDENT] inverted primary
// ═════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalLayoutApi::class)
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

    val haptic = rememberHapticFeedback()
    var showPhotoOptions by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.updatePhotoUri(it.toString()) }
    }

    LaunchedEffect(saveSuccess) {
        if (saveSuccess) onNavigateBack()
    }

    val canSave = title.isNotBlank() && description.isNotBlank() && !isSaving

    val filledSteps = remember(
        title, description, severity, category, location,
        photoUri, reportedBy, notes, selectedEventId,
    ) {
        var n = 0
        if (photoUri.isNotBlank()) n++                                    // 01 evidence
        if (title.isNotBlank()) n++                                       // 02 headline
        if (description.isNotBlank()) n++                                 // 03 brief
        if (severity.isNotBlank()) n++                                    // 04 severity (default LOW counts)
        if (selectedEventId != null) n++                                  // 05 event
        if (location.isNotBlank() || category.isNotBlank()) n++           // 06 locus
        if (reportedBy.isNotBlank() || notes.isNotBlank()) n++            // 07 credits
        if (canSave) n++                                                  // 08 preview ready
        n
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SoftAppBar(
                title = if (incidentId == null) "LOG INCIDENT" else "EDIT INCIDENT",
                subtitle = "§ Incident · " + if (incidentId == null) "New" else "Amend",
                onBack = { haptic.light(); onNavigateBack() },
                trailing = {
                    SoftIconButton(
                        glyph = "✓",
                        enabled = canSave,
                        onClick = { haptic.medium(); viewModel.saveIncident() },
                    )
                },
            )
        },
        bottomBar = {
            LogActionRail(
                primaryLabel = when {
                    isSaving -> "FILING…"
                    incidentId == null -> "FILE INCIDENT"
                    else -> "SAVE INCIDENT"
                },
                primaryEnabled = canSave,
                onCancel = { haptic.light(); onNavigateBack() },
                onPrimary = { haptic.medium(); viewModel.saveIncident() },
            )
        },
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            ArcadeBackground(modifier = Modifier.matchParentSize())
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding(),
            ) {
            // Headline
            LogHeadline(
                filledSteps = filledSteps,
                totalSteps = 8,
                isEdit = incidentId != null,
            )

            // Progress tick strip
            LogProgressStrip(filled = filledSteps, total = 8)
                Spacer(Modifier.height(20.dp))

            // 01 / EVIDENCE
            CaseStep(
                index = "01",
                label = "EVIDENCE",
                hint = "Photo of the scene — first frame for the file.",
                fulfilled = photoUri.isNotBlank(),
            ) {
                EvidencePanel(
                    photoUri = photoUri,
                    onEdit = { showPhotoOptions = true },
                    onClear = { viewModel.updatePhotoUri("") },
                )
            }
                Spacer(Modifier.height(20.dp))

            // 02 / HEADLINE (required)
            CaseStep(
                index = "02",
                label = "HEADLINE",
                hint = "One clear sentence. What happened?",
                required = true,
                fulfilled = title.isNotBlank(),
            ) {
                InkField(
                    value = title,
                    onValueChange = viewModel::updateTitle,
                    placeholder = "Slip and fall near Row 5",
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                )
            }
                Spacer(Modifier.height(20.dp))

            // 03 / BRIEF (required)
            CaseStep(
                index = "03",
                label = "BRIEF",
                hint = "Who · when · what — two or three lines.",
                required = true,
                fulfilled = description.isNotBlank(),
            ) {
                InkField(
                    value = description,
                    onValueChange = viewModel::updateDescription,
                    placeholder = "Guest slipped on wet floor at approximately 19:40…",
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                    singleLine = false,
                )
            }
                Spacer(Modifier.height(20.dp))

            // 04 / SEVERITY (required-ish, default LOW)
            CaseStep(
                index = "04",
                label = "SEVERITY",
                hint = "How serious is this? Four tones, pick one.",
                required = true,
                fulfilled = true, // always has a default
            ) {
                SeverityGrid(
                    selected = IncidentSeverity.fromString(severity),
                    onSelect = { viewModel.updateSeverity(it.name) },
                )
            }
                Spacer(Modifier.height(20.dp))

            // 05 / EVENT (optional — only if events exist)
            if (events.isNotEmpty()) {
                CaseStep(
                    index = "05",
                    label = "EVENT",
                    hint = "Optional — link this report to a specific session.",
                    fulfilled = selectedEventId != null,
                ) {
                    EventPicker(
                        events = events,
                        selected = selectedEventId,
                        onSelect = viewModel::updateSelectedEvent,
                    )
                }
                Spacer(Modifier.height(20.dp))
            }

            // 06 / LOCUS
            CaseStep(
                index = if (events.isNotEmpty()) "06" else "05",
                label = "LOCUS",
                hint = "Where exactly · what kind of incident.",
                fulfilled = location.isNotBlank() || category.isNotBlank(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    SubField(
                        caption = "A / LOCATION",
                        value = location,
                        onValueChange = viewModel::updateLocation,
                        placeholder = "Main Hall · Row 5 · Parking A",
                        capitalization = KeyboardCapitalization.Words,
                    )
                    SubField(
                        caption = "B / CATEGORY",
                        value = category,
                        onValueChange = viewModel::updateCategory,
                        placeholder = "Safety · Security · Medical",
                        capitalization = KeyboardCapitalization.Words,
                    )
                }
            }
                Spacer(Modifier.height(20.dp))

            // 07 / CREDITS
            CaseStep(
                index = if (events.isNotEmpty()) "07" else "06",
                label = "CREDITS",
                hint = "Who filed this and any side notes.",
                fulfilled = reportedBy.isNotBlank() || notes.isNotBlank(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    SubField(
                        caption = "FILED BY",
                        value = reportedBy,
                        onValueChange = viewModel::updateReportedBy,
                        placeholder = "Staff name or initials",
                        capitalization = KeyboardCapitalization.Words,
                    )
                    SubField(
                        caption = "NOTES",
                        value = notes,
                        onValueChange = viewModel::updateNotes,
                        placeholder = "Anything else worth knowing",
                        capitalization = KeyboardCapitalization.Sentences,
                        singleLine = false,
                    )
                }
            }
                Spacer(Modifier.height(20.dp))

            // 08 / PREVIEW
            CaseStep(
                index = if (events.isNotEmpty()) "08" else "07",
                label = "PREVIEW",
                hint = "How this report will read on the desk.",
                fulfilled = canSave,
            ) {
                PreviewRow(
                    title = title.ifBlank { "—" },
                    brief = description,
                    severity = IncidentSeverity.fromString(severity),
                    location = location,
                    category = category,
                )
            }

            // Error
            errorMessage?.let {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .border(FieldTokens.Hair, Signal)
                        .padding(12.dp),
                ) {
                    Text("ERR ·", style = MonoTiny, color = Signal)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = Signal,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("OK", style = MaterialTheme.typography.labelMedium, color = Signal)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Colophon(isEdit = incidentId != null)
            }
        }
    }

    if (showPhotoOptions) {
        EvidenceOptionsSheet(
            hasPhoto = photoUri.isNotBlank(),
            onDismiss = { showPhotoOptions = false },
            onPick = {
                showPhotoOptions = false
                galleryLauncher.launch("image/*")
            },
            onClear = {
                showPhotoOptions = false
                viewModel.updatePhotoUri("")
            },
        )
    }
}

// ---------------------------------------------------------------------------
// Headline + progress
// ---------------------------------------------------------------------------

@Composable
private fun LogHeadline(filledSteps: Int, totalSteps: Int, isEdit: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 18.dp, bottom = 10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "§ ${if (isEdit) "AMEND" else "NEW"} · INCIDENT",
                style = MonoTiny,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text("·", style = MonoTiny, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "${filledSteps.toString().padStart(2, '0')} / ${
                    totalSteps.toString().padStart(2, '0')
                } FILLED",
                style = MonoTiny,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                if (isEdit) "Amend the report." else "File the report.",
                style = softHeadline(28),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = filledSteps.toString().padStart(2, '0'),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Only HEADLINE, BRIEF and SEVERITY are required. The rest thickens the file.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun LogProgressStrip(filled: Int, total: Int) {
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.outline
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 16.dp)
            .height(6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        repeat(total) { i ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(if (i < filled) ink else muted),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// CaseStep scaffold
// ---------------------------------------------------------------------------

@Composable
private fun CaseStep(
    index: String,
    label: String,
    hint: String,
    required: Boolean = false,
    fulfilled: Boolean = false,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = index,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFamily = MonoTiny.fontFamily,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "/ $label",
                        style = MonoTiny,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (required) {
                        Spacer(Modifier.width(6.dp))
                        Text(text = "★ REQ", style = MonoTiny, color = Signal)
                    }
                    if (fulfilled && !required) {
                        Spacer(Modifier.width(6.dp))
                        Text(text = "✓ OK", style = MonoTiny, color = Sage)
                    } else if (fulfilled && required) {
                        Spacer(Modifier.width(6.dp))
                        Text(text = "✓ OK", style = MonoTiny, color = Sage)
                    }
                }
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        content()
    }
}

// ---------------------------------------------------------------------------
// Evidence panel
// ---------------------------------------------------------------------------

@Composable
private fun EvidencePanel(
    photoUri: String,
    onEdit: () -> Unit,
    onClear: () -> Unit,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val paper = MaterialTheme.colorScheme.background
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ink.copy(alpha = 0.04f))
            .clickable { onEdit() },
    ) {
        if (photoUri.isNotBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(photoUri)
                    .size(Size(512, 512))
                    .build(),
                contentDescription = "Incident photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            CornerMarks()
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .background(paper)
                    .border(FieldTokens.Hair, ink)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text("§ EVIDENCE", style = MonoTiny, color = ink)
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SmallInkChip("REPLACE", onClick = onEdit)
                SmallInkChip("CLEAR", onClick = onClear)
            }
        } else {
            CornerMarks()
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "⌑",
                    style = MaterialTheme.typography.displayMedium,
                    color = ink,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "TAP TO ATTACH EVIDENCE",
                    style = MaterialTheme.typography.labelMedium,
                    color = ink,
                )
                Spacer(Modifier.height(2.dp))
                Text("jpg · png · heic", style = MonoTiny, color = muted)
            }
        }
    }
}

@Composable
private fun CornerMarks() {
    val ink = MaterialTheme.colorScheme.onBackground
    Box(modifier = Modifier.fillMaxSize()) {
        Box(Modifier
            .align(Alignment.TopStart)
            .padding(4.dp)
            .size(10.dp)
            .background(ink))
        Box(Modifier
            .align(Alignment.TopEnd)
            .padding(4.dp)
            .size(10.dp)
            .background(ink))
        Box(Modifier
            .align(Alignment.BottomStart)
            .padding(4.dp)
            .size(10.dp)
            .background(ink))
        Box(Modifier
            .align(Alignment.BottomEnd)
            .padding(4.dp)
            .size(10.dp)
            .background(ink))
    }
}

@Composable
private fun SmallInkChip(label: String, onClick: () -> Unit) {
    SoftCard(
        onClick = onClick,
        cornerRadius = 8,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 10.dp,
            vertical = 6.dp
        ),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

// ---------------------------------------------------------------------------
// Fields
// ---------------------------------------------------------------------------

@Composable
private fun InkField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    imeAction: ImeAction = ImeAction.Next,
    singleLine: Boolean = true,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        textStyle = MaterialTheme.typography.headlineSmall.copy(color = ink),
        cursorBrush = SolidColor(ink),
        keyboardOptions = KeyboardOptions(
            capitalization = capitalization,
            imeAction = imeAction,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (singleLine) 48.dp else 72.dp),
        decorationBox = { inner ->
            Column {
                Box(Modifier.padding(vertical = 6.dp)) {
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    inner()
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(FieldTokens.Hair)
                        .background(ink),
                )
            }
        },
    )
}

@Composable
private fun SubField(
    caption: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    singleLine: Boolean = true,
) {
    Column {
        Text(
            caption,
            style = MonoTiny,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        val ink = MaterialTheme.colorScheme.onBackground
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = MaterialTheme.typography.titleLarge.copy(color = ink),
            cursorBrush = SolidColor(ink),
            keyboardOptions = KeyboardOptions(capitalization = capitalization),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (singleLine) 40.dp else 60.dp),
            decorationBox = { inner ->
                Column {
                    Box(Modifier.padding(vertical = 4.dp)) {
                        if (value.isEmpty()) {
                            Text(
                                placeholder,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        inner()
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(FieldTokens.Hair)
                            .background(MaterialTheme.colorScheme.outline),
                    )
                }
            },
        )
    }
}

// ---------------------------------------------------------------------------
// Severity grid + event picker
// ---------------------------------------------------------------------------

@Composable
private fun SeverityGrid(
    selected: IncidentSeverity,
    onSelect: (IncidentSeverity) -> Unit,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val paper = MaterialTheme.colorScheme.background
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        IncidentSeverity.entries.reversed().forEach { sev ->
            val isSelected = selected == sev
            val tone = severityTone(sev)
            SoftCard(
                modifier = Modifier.fillMaxWidth(),
                selected = isSelected,
                onClick = { onSelect(sev) },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 14.dp,
                    vertical = 12.dp
                ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(tone),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        severityTag(sev),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = MonoTiny.fontFamily,
                        ),
                        color = if (isSelected) paper else ink,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        severityBlurb(sev),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) paper.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    if (isSelected) {
                        Text(
                            "✓",
                            style = MaterialTheme.typography.titleLarge,
                            color = paper,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EventPicker(
    events: List<EventWithDetails>,
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    val dateFmt = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ZoneChip(
            label = "UNLINKED",
            selected = selected == null,
            onClick = { onSelect(null) },
        )
        events.forEach { ev ->
            val name = (ev.eventType?.name ?: "Event").uppercase()
            val date = dateFmt.format(Date(ev.event.date)).uppercase()
            ZoneChip(
                label = "$name · $date",
                selected = selected == ev.event.id,
                onClick = {
                    onSelect(if (selected == ev.event.id) null else ev.event.id)
                },
            )
        }
    }
}

private fun severityBlurb(sev: IncidentSeverity): String = when (sev) {
    IncidentSeverity.CRITICAL -> "Life safety. Notify now."
    IncidentSeverity.HIGH -> "Urgent — supervisor within 10 min."
    IncidentSeverity.MEDIUM -> "Needs attention today."
    IncidentSeverity.LOW -> "Log it. No immediate action."
}

// ---------------------------------------------------------------------------
// Preview row — mini desk row
// ---------------------------------------------------------------------------

@Composable
private fun PreviewRow(
    title: String,
    brief: String,
    severity: IncidentSeverity,
    location: String,
    category: String,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val tone = severityTone(severity)
    val dateFmt = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    SoftCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .width(FieldTokens.Lane)
                .fillMaxWidth()
                .background(tone)
                .heightIn(min = 1.dp),
        )
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.width(52.dp)) {
                    Text(
                        "01",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = MonoTiny.fontFamily,
                        ),
                        color = ink,
                    )
                    Text("/ 01", style = MonoTiny, color = muted)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge,
                        color = ink,
                    )
                    if (brief.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            brief,
                            style = MaterialTheme.typography.bodyMedium,
                            color = muted,
                            maxLines = 2,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        buildString {
                            append(location.ifBlank { "—" }.uppercase())
                            append("   ·   ")
                            append(dateFmt.format(Date()).uppercase())
                            append(" ")
                            append(timeFmt.format(Date()))
                            if (category.isNotBlank()) {
                                append("   ·   ")
                                append(category.uppercase())
                            }
                        },
                        style = MonoTiny,
                        color = muted,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    SeverityPill(severity)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier
                            .size(8.dp)
                            .background(Signal))
                        Spacer(Modifier.width(6.dp))
                        Text("REP", style = MonoTiny, color = ink)
                    }
                }
            }
        }
        }
    }
}

// ---------------------------------------------------------------------------
// Bottom rail
// ---------------------------------------------------------------------------

@Composable
private fun LogActionRail(
    primaryLabel: String,
    primaryEnabled: Boolean,
    onCancel: () -> Unit,
    onPrimary: () -> Unit,
) {
    val loading = primaryLabel.endsWith("…")
    SoftBottomDock(
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SoftToolButton(
                label = "Cancel",
                glyph = "×",
                enabled = true,
                modifier = Modifier.weight(1f),
                onClick = onCancel,
            )
            SoftPrimaryButton(
                label = primaryLabel.trimEnd('…'),
                onClick = onPrimary,
                enabled = primaryEnabled && !loading,
                modifier = Modifier.weight(2f),
                trailingGlyph = if (loading) "…" else "→",
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Evidence options sheet
// ---------------------------------------------------------------------------

@Composable
private fun EvidenceOptionsSheet(
    hasPhoto: Boolean,
    onDismiss: () -> Unit,
    onPick: () -> Unit,
    onClear: () -> Unit,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val paper = MaterialTheme.colorScheme.background
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(paper),
        ) {
            ArcadeBackground(modifier = Modifier.matchParentSize())
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SoftSection(
                        title = "Attach a photo",
                        eyebrow = "Evidence",
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(12.dp))
                    SoftIconButton(glyph = "×", onClick = onDismiss)
                }
                Spacer(Modifier.height(8.dp))
                SheetOption(
                    index = "01",
                    label = "FROM GALLERY",
                    hint = "Pick an image from this device.",
                    onClick = onPick,
                )
                if (hasPhoto) {
                    Spacer(Modifier.height(8.dp))
                    SheetOption(
                        index = "02",
                        label = "CLEAR EVIDENCE",
                        hint = "Detach the photo from this report.",
                        tone = Signal,
                        onClick = onClear,
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SheetOption(
    index: String,
    label: String,
    hint: String,
    tone: Color = MaterialTheme.colorScheme.onBackground,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            index,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = MonoTiny.fontFamily,
            ),
            color = tone,
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("/ $label", style = MaterialTheme.typography.labelMedium, color = tone)
            Text(
                hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text("→", style = MaterialTheme.typography.headlineSmall, color = tone)
    }
}

// ---------------------------------------------------------------------------
// Colophon
// ---------------------------------------------------------------------------

@Composable
private fun Colophon(isEdit: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                if (isEdit) "REPORT · AMENDED" else "REPORT · DRAFT",
                style = MonoTiny,
                color = Sage,
            )
            Text(
                SimpleDateFormat("dd MMM · HH:mm", Locale.getDefault())
                    .format(Date()).uppercase(),
                style = MonoTiny,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            "― FIELD · INCIDENTS ―",
            style = MonoTiny,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
