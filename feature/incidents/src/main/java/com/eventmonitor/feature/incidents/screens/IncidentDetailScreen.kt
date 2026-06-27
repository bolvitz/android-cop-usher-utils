package com.eventmonitor.feature.incidents.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.koin.androidx.compose.koinViewModel
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
import com.eventmonitor.core.common.ui.softHeadline
import com.eventmonitor.core.common.utils.rememberHapticFeedback
import com.eventmonitor.core.data.local.entities.IncidentEntity
import com.eventmonitor.core.domain.models.IncidentSeverity
import com.eventmonitor.core.domain.models.IncidentStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ═════════════════════════════════════════════════════════════════════════════
// CASE FILE — editorial redesign for the Incident Detail screen.
//   · FieldAppBar     — [←] "FILE" eyebrow "§ INCIDENT · ID"  trailing [✎]
//   · Dossier head    — severity stripe, headline, sev + status pills, filed at
//   · Evidence        — photo with corner marks + §EVIDENCE eyebrow
//   · Numbered sects  — 01 BRIEF · 02 LOCUS · 03 CREDITS · 04 HANDLER · 05 ACTIONS · 06 NOTES
//   · Resolve rail    — [← BACK] [✓ MARK RESOLVED] when still open
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun IncidentDetailScreen(
    incidentId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    viewModel: IncidentDetailViewModel = koinViewModel()
) {
    val incident by viewModel.incident.collectAsState(initial = null)
    var showResolveDialog by remember { mutableStateOf(false) }
    val haptic = rememberHapticFeedback()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SoftAppBar(
                title = "File",
                subtitle = incident?.let { "§ Incident · ${it.id.take(6).uppercase()}" }
                    ?: "§ Incident",
                onBack = { haptic.light(); onNavigateBack() },
                trailing = {
                    SoftIconButton(
                        glyph = "✎",
                        enabled = incident != null,
                        onClick = {
                            haptic.light()
                            incident?.let { onNavigateToEdit(it.id) }
                        },
                    )
                },
            )
        },
        bottomBar = {
            incident?.let { inc ->
                val status = IncidentStatus.fromString(inc.status)
                val open = status != IncidentStatus.RESOLVED && status != IncidentStatus.CLOSED
                if (open) {
                    FileResolveRail(
                        onBack = { haptic.light(); onNavigateBack() },
                        onResolve = { haptic.medium(); showResolveDialog = true },
                    )
                }
            }
        },
    ) { padding ->
        val current = incident
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            ArcadeBackground(modifier = Modifier.matchParentSize())
            if (current == null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onBackground,
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "OPENING FILE…",
                        style = MonoTiny,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding(),
                ) {
                DossierHead(incident = current)
                    Spacer(Modifier.height(20.dp))

                if (current.photoUri.isNotBlank()) {
                    EvidenceStill(photoUri = current.photoUri)
                    Spacer(Modifier.height(20.dp))
                }

                // 01 / BRIEF
                DossierSection(index = "01", label = "BRIEF") {
                    Text(
                        current.description.ifBlank { "—" },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                    Spacer(Modifier.height(20.dp))

                // 02 / LOCUS
                if (current.location.isNotBlank() || current.category.isNotBlank()) {
                    DossierSection(index = "02", label = "LOCUS") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (current.location.isNotBlank()) {
                                KeyValue(caption = "LOCATION", value = current.location)
                            }
                            if (current.category.isNotBlank()) {
                                KeyValue(caption = "CATEGORY", value = current.category)
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }

                // 03 / CREDITS
                DossierSection(index = "03", label = "CREDITS") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        KeyValue(
                            caption = "FILED BY",
                            value = current.reportedBy.ifBlank { "Anonymous" },
                        )
                        KeyValue(
                            caption = "FILED AT",
                            value = SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.getDefault())
                                .format(Date(current.reportedAt)),
                        )
                    }
                }
                    Spacer(Modifier.height(20.dp))

                // 04 / HANDLER
                if (current.assignedTo.isNotBlank()) {
                    DossierSection(index = "04", label = "HANDLER") {
                        KeyValue(caption = "ASSIGNED TO", value = current.assignedTo)
                    }
                    Spacer(Modifier.height(20.dp))
                }

                // 05 / ACTIONS
                if (current.actionsTaken.isNotBlank()) {
                    DossierSection(
                        index = nextIndex(current),
                        label = "ACTIONS TAKEN",
                    ) {
                        Text(
                            current.actionsTaken,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                }

                // 06 / NOTES
                if (current.notes.isNotBlank()) {
                    DossierSection(
                        index = nextIndex(current, plusActions = true),
                        label = "NOTES",
                    ) {
                        Text(
                            current.notes,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                }

                // Resolved stamp
                current.resolvedAt?.let { resolvedAt ->
                    ResolvedStamp(
                        at = resolvedAt,
                        label = if (current.status == IncidentStatus.CLOSED.name) "CLOSED" else "RESOLVED",
                    )
                }

                Spacer(Modifier.height(8.dp))
                FileColophon(incident = current)
                }
            }
        }
    }

    if (showResolveDialog && incident != null) {
        ResolveCaseSheet(
            incident = incident!!,
            onDismiss = { showResolveDialog = false },
            onResolve = { actions ->
                viewModel.resolveIncident(actions)
                showResolveDialog = false
            },
        )
    }
}

// ---------------------------------------------------------------------------
// Dossier head
// ---------------------------------------------------------------------------

@Composable
private fun DossierHead(incident: IncidentEntity) {
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val severity = IncidentSeverity.fromString(incident.severity)
    val status = IncidentStatus.fromString(incident.status)
    val sevTone = severityTone(severity)

    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .width(FieldTokens.Lane)
                .heightIn(min = 1.dp)
                .fillMaxWidth()
                .background(sevTone),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 17.dp, vertical = 18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "§ CASE FILE",
                    style = MonoTiny,
                    color = muted,
                )
                Text(
                    "ID ${incident.id.take(6).uppercase()}",
                    style = MonoTiny,
                    color = muted,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                incident.title.ifBlank { "—" },
                style = softHeadline(28),
                color = ink,
            )
            Spacer(Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SeverityPill(severity)
                Spacer(Modifier.width(6.dp))
                StatusPill(status)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                buildString {
                    append("FILED ")
                    append(
                        SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.getDefault())
                            .format(Date(incident.reportedAt)).uppercase()
                    )
                    if (incident.reportedBy.isNotBlank()) {
                        append("   ·   BY ${incident.reportedBy.uppercase()}")
                    }
                },
                style = MonoTiny,
                color = muted,
            )
        }
    }
}

@Composable
private fun StatusPill(status: IncidentStatus) {
    val ink = MaterialTheme.colorScheme.onBackground
    val tone = statusTone(status)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(ink.copy(alpha = 0.08f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Box(Modifier
            .size(8.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(tone))
        Spacer(Modifier.width(6.dp))
        Text(
            statusTag(status),
            style = MaterialTheme.typography.labelMedium,
            color = ink,
        )
    }
}

// ---------------------------------------------------------------------------
// Evidence still
// ---------------------------------------------------------------------------

@Composable
private fun EvidenceStill(photoUri: String) {
    val ink = MaterialTheme.colorScheme.onBackground
    val paper = MaterialTheme.colorScheme.background
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .height(280.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ink.copy(alpha = 0.04f)),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(photoUri)
                .size(Size(1024, 1024))
                .build(),
            contentDescription = "Incident photo",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
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
    }
}

// ---------------------------------------------------------------------------
// Dossier section scaffold
// ---------------------------------------------------------------------------

@Composable
private fun DossierSection(
    index: String,
    label: String,
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
            Text(
                "/ $label",
                style = MonoTiny,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .weight(1f),
            )
        }
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun KeyValue(caption: String, value: String) {
    Column {
        Text(
            caption,
            style = MonoTiny,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

// ---------------------------------------------------------------------------
// Resolved stamp + colophon
// ---------------------------------------------------------------------------

@Composable
private fun ResolvedStamp(at: Long, label: String) {
    SoftCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("✓ $label", style = MonoTiny, color = Sage)
                Spacer(Modifier.height(2.dp))
                Text(
                    "Case closed",
                    style = softHeadline(20),
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                SimpleDateFormat("dd MMM · HH:mm", Locale.getDefault())
                    .format(Date(at)).uppercase(),
                style = MonoTiny,
                color = Sage,
            )
        }
    }
}

@Composable
private fun FileColophon(incident: IncidentEntity) {
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
                "FILE · ${incident.id.take(8).uppercase()}",
                style = MonoTiny,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "UPDATED " + SimpleDateFormat("dd MMM · HH:mm", Locale.getDefault())
                    .format(Date(incident.updatedAt)).uppercase(),
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

// ---------------------------------------------------------------------------
// Resolve rail + sheet
// ---------------------------------------------------------------------------

@Composable
private fun FileResolveRail(onBack: () -> Unit, onResolve: () -> Unit) {
    SoftBottomDock(
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SoftToolButton(
                label = "Back",
                glyph = "←",
                enabled = true,
                modifier = Modifier.weight(1f),
                onClick = onBack,
            )
            SoftPrimaryButton(
                label = "Resolve",
                onClick = onResolve,
                modifier = Modifier.weight(2f),
                trailingGlyph = "✓",
            )
        }
    }
}

@Composable
private fun ResolveCaseSheet(
    incident: IncidentEntity,
    onDismiss: () -> Unit,
    onResolve: (String) -> Unit,
) {
    var actions by remember { mutableStateOf("") }
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
                        title = "Close the report",
                        eyebrow = "Resolve · Case",
                        hint = "ID ${
                            incident.id.take(6).uppercase()
                        } · ${incident.title.uppercase()}",
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(12.dp))
                    SoftIconButton(glyph = "×", onClick = onDismiss)
                }
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                ) {
                    Text("ACTIONS TAKEN", style = MonoTiny, color = Signal)
                    Text(
                        "Describe what was done to resolve this.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    BasicTextField(
                        value = actions,
                        onValueChange = { actions = it },
                        textStyle = MaterialTheme.typography.titleLarge.copy(color = ink),
                        cursorBrush = SolidColor(ink),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 96.dp),
                        decorationBox = { inner ->
                            Column {
                                Box(Modifier.padding(vertical = 4.dp)) {
                                    if (actions.isEmpty()) {
                                        Text(
                                            "Cleaned spill · notified supervisor · signage posted…",
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
                                        .background(ink),
                                )
                            }
                        },
                    )
                }
                Spacer(Modifier.height(8.dp))
                val enabled = actions.isNotBlank()
                SoftBottomDock {
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
                            onClick = onDismiss,
                        )
                        SoftPrimaryButton(
                            label = "Close Case",
                            onClick = { onResolve(actions) },
                            enabled = enabled,
                            modifier = Modifier.weight(2f),
                            trailingGlyph = "✓",
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

private fun nextIndex(incident: IncidentEntity, plusActions: Boolean = false): String {
    // Naïve numbering — mirrors the sections rendered above.
    var idx = 3 // after BRIEF, LOCUS, CREDITS
    if (incident.location.isNotBlank() || incident.category.isNotBlank()) {
        // LOCUS counted
    } else {
        idx--
    }
    if (incident.assignedTo.isNotBlank()) idx++
    if (plusActions && incident.actionsTaken.isNotBlank()) idx++
    return (idx + 1).toString().padStart(2, '0')
}
