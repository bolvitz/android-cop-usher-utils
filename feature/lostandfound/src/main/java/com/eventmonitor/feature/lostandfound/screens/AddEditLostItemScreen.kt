package com.eventmonitor.feature.lostandfound.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.eventmonitor.core.common.theme.MonoTiny
import com.eventmonitor.core.common.theme.Sage
import com.eventmonitor.core.common.theme.Signal
import com.eventmonitor.core.common.ui.FieldAppBar
import com.eventmonitor.core.common.ui.FieldAppBarIcon
import com.eventmonitor.core.common.ui.FieldTokens
import com.eventmonitor.core.common.ui.Hairline
import com.eventmonitor.core.common.ui.HairlineSoft
import com.eventmonitor.core.common.ui.ZoneChip
import com.eventmonitor.core.common.utils.rememberHapticFeedback
import com.eventmonitor.core.data.local.entities.EventWithDetails
import com.eventmonitor.core.domain.models.ItemCategory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ═════════════════════════════════════════════════════════════════════════════
// CASE FILE — editorial redesign for the Add / Edit Lost Item screen.
//   · FieldAppBar     — [←] "FILE CASE" eyebrow "§ LOST · FOUND" trailing [✓]
//   · Headline        — editorial lede + fraction ("STEP 04 / 07")
//   · Numbered steps  — 01 / PHOTO, 02 / DESCRIPTION, 03 / ZONE, 04 / KIND,
//                       05 / EVENT, 06 / DETAILS, 07 / CREDITS
//   · Live preview    — a mini case-ledger row at the bottom
//   · Action rail     — [CANCEL] [FILE CASE] inverted primary
// ═════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalLayoutApi::class)
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

    val canSave = description.isNotBlank() && foundZone.isNotBlank() && !isSaving
    val filledSteps = remember(
        description, foundZone, photoUri, category, color, brand, reportedBy,
    ) {
        var n = 0
        if (description.isNotBlank()) n++
        if (foundZone.isNotBlank()) n++
        if (photoUri.isNotBlank()) n++
        if (category.isNotBlank() && category != ItemCategory.OTHER.name) n++
        if (color.isNotBlank() || brand.isNotBlank() || identifyingMarks.isNotBlank()) n++
        if (reportedBy.isNotBlank() || notes.isNotBlank()) n++
        if (selectedEventId != null) n++
        n
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            FieldAppBar(
                title = if (itemId == null) "FILE CASE" else "EDIT CASE",
                eyebrow = "§ LOST · FOUND",
                leading = {
                    FieldAppBarIcon(glyph = "←", onClick = {
                        haptic.light(); onNavigateBack()
                    })
                },
                trailing = {
                    FieldAppBarIcon(
                        glyph = "✓",
                        enabled = canSave,
                        onClick = {
                            haptic.medium(); viewModel.saveItem()
                        },
                    )
                },
            )
        },
        bottomBar = {
            FileActionRail(
                primaryLabel = if (isSaving) "FILING…" else if (itemId == null) "FILE CASE" else "SAVE CASE",
                primaryEnabled = canSave,
                onCancel = { haptic.light(); onNavigateBack() },
                onPrimary = { haptic.medium(); viewModel.saveItem() },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
        ) {
            // Headline
            FileHeadline(
                filledSteps = filledSteps,
                totalSteps = 7,
                isEdit = itemId != null,
            )

            // Progress tick strip
            FileProgressStrip(filled = filledSteps, total = 7)
            Hairline()

            // 01 / PHOTO
            CaseStep(
                index = "01",
                label = "PHOTO",
                hint = "Evidence frame — shot of the item as found."
            ) {
                PhotoPanel(
                    photoUri = photoUri,
                    onEdit = { showPhotoOptions = true },
                    onClear = { viewModel.updatePhotoUri("") },
                )
            }
            HairlineSoft()

            // 02 / DESCRIPTION (required)
            CaseStep(
                index = "02",
                label = "DESCRIPTION",
                hint = "What is the item? One sentence.",
                required = true,
                fulfilled = description.isNotBlank(),
            ) {
                InkField(
                    value = description,
                    onValueChange = viewModel::updateDescription,
                    placeholder = "Black leather wallet, embossed initials",
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                    singleLine = false,
                )
            }
            HairlineSoft()

            // 03 / ZONE (required)
            CaseStep(
                index = "03",
                label = "ZONE FOUND",
                hint = "Where exactly — a bay, a row, a door.",
                required = true,
                fulfilled = foundZone.isNotBlank(),
            ) {
                InkField(
                    value = foundZone,
                    onValueChange = viewModel::updateFoundZone,
                    placeholder = "Main Hall · Row 5",
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                )
            }
            HairlineSoft()

            // 04 / KIND
            CaseStep(
                index = "04",
                label = "KIND",
                hint = "Pick one — locks the case to a drawer.",
                fulfilled = category.isNotBlank() && category != ItemCategory.OTHER.name,
            ) {
                CategoryGrid(
                    selected = ItemCategory.fromString(category),
                    onSelect = { viewModel.updateCategory(it.name) },
                )
            }
            HairlineSoft()

            // 05 / EVENT (optional, only if events exist)
            if (events.isNotEmpty()) {
                CaseStep(
                    index = "05",
                    label = "EVENT",
                    hint = "Optional — link this case to a specific session.",
                    fulfilled = selectedEventId != null,
                ) {
                    EventPicker(
                        events = events,
                        selected = selectedEventId,
                        onSelect = viewModel::updateSelectedEvent,
                    )
                }
                HairlineSoft()
            }

            // 06 / DETAILS
            CaseStep(
                index = if (events.isNotEmpty()) "06" else "05",
                label = "DETAILS",
                hint = "Colour · brand · identifying marks.",
                fulfilled = color.isNotBlank() || brand.isNotBlank() || identifyingMarks.isNotBlank(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    SubField(
                        caption = "A / COLOUR",
                        value = color,
                        onValueChange = viewModel::updateColor,
                        placeholder = "Black · Navy",
                        capitalization = KeyboardCapitalization.Words,
                    )
                    SubField(
                        caption = "B / BRAND",
                        value = brand,
                        onValueChange = viewModel::updateBrand,
                        placeholder = "Apple · Herschel",
                        capitalization = KeyboardCapitalization.Words,
                    )
                    SubField(
                        caption = "C / MARKS",
                        value = identifyingMarks,
                        onValueChange = viewModel::updateIdentifyingMarks,
                        placeholder = "Engraved initials, worn corner…",
                        capitalization = KeyboardCapitalization.Sentences,
                        singleLine = false,
                    )
                }
            }
            HairlineSoft()

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
            HairlineSoft()

            // PREVIEW
            CaseStep(
                index = if (events.isNotEmpty()) "08" else "07",
                label = "PREVIEW",
                hint = "How this case will read in the ledger.",
            ) {
                PreviewRow(
                    description = description.ifBlank { "—" },
                    zone = foundZone.ifBlank { "—" },
                    category = ItemCategory.fromString(category),
                    color = color,
                    brand = brand,
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
            Colophon(isEdit = itemId != null)
        }
    }

    if (showPhotoOptions) {
        PhotoOptionsSheet(
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
private fun FileHeadline(filledSteps: Int, totalSteps: Int, isEdit: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 18.dp, bottom = 10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "§ ${if (isEdit) "AMEND" else "NEW"} · CASE",
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
                if (isEdit) "Amend the file." else "File the find.",
                style = MaterialTheme.typography.headlineLarge,
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
            "Only DESCRIPTION and ZONE are required. The rest thickens the case.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun FileProgressStrip(filled: Int, total: Int) {
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
                        Text(
                            text = "★ REQ",
                            style = MonoTiny,
                            color = Signal,
                        )
                    }
                    if (fulfilled) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "✓ OK",
                            style = MonoTiny,
                            color = Sage,
                        )
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
// Photo panel
// ---------------------------------------------------------------------------

@Composable
private fun PhotoPanel(
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
            .border(FieldTokens.HairStrong, ink)
            .clickable { onEdit() },
    ) {
        if (photoUri.isNotBlank()) {
            AsyncImage(
                model = photoUri,
                contentDescription = "Case photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            // Corner crop marks
            CornerMarks()
            // Top-left eyebrow tag
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
            // Replace + clear
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
                Text(
                    "jpg · png · heic",
                    style = MonoTiny,
                    color = muted,
                )
            }
        }
    }
}

@Composable
private fun CornerMarks() {
    val ink = MaterialTheme.colorScheme.onBackground

    @Composable
    fun Mark(alignment: Alignment) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .border(FieldTokens.Hair, ink)
                .background(MaterialTheme.colorScheme.background),
        )
    }
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
    val ink = MaterialTheme.colorScheme.onBackground
    val paper = MaterialTheme.colorScheme.background
    Box(
        modifier = Modifier
            .border(FieldTokens.Hair, ink)
            .background(paper)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = ink)
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
// Category + event pickers
// ---------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryGrid(
    selected: ItemCategory,
    onSelect: (ItemCategory) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ItemCategory.entries.forEach { cat ->
            ZoneChip(
                label = "${cat.shortTag()} · ${cat.displayName.uppercase()}",
                selected = selected == cat,
                onClick = { onSelect(cat) },
            )
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

// ---------------------------------------------------------------------------
// Preview row — mini ledger row
// ---------------------------------------------------------------------------

@Composable
private fun PreviewRow(
    description: String,
    zone: String,
    category: ItemCategory,
    color: String,
    brand: String,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val dateFmt = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(FieldTokens.Hair, ink)
            .padding(14.dp),
    ) {
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
                    description,
                    style = MaterialTheme.typography.titleLarge,
                    color = ink,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    buildString {
                        append(zone.uppercase())
                        append("   ·   ")
                        append(dateFmt.format(Date()).uppercase())
                        append("   ·   ")
                        append(category.shortTag())
                    },
                    style = MonoTiny,
                    color = muted,
                )
                if (color.isNotBlank() || brand.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        listOf(color, brand).filter { it.isNotBlank() }
                            .joinToString(" · ").uppercase(),
                        style = MonoTiny,
                        color = muted,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier
                        .size(8.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant))
                    Spacer(Modifier.width(6.dp))
                    Text("HOLD", style = MonoTiny, color = ink)
                }
                Spacer(Modifier.height(4.dp))
                Text("D+000", style = MonoTiny, color = muted)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Bottom rail
// ---------------------------------------------------------------------------

@Composable
private fun FileActionRail(
    primaryLabel: String,
    primaryEnabled: Boolean,
    onCancel: () -> Unit,
    onPrimary: () -> Unit,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val paper = MaterialTheme.colorScheme.background
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(paper)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Hairline()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(FieldTokens.ToolHeight)
                    .border(FieldTokens.Hair, ink)
                    .background(paper)
                    .clickable { onCancel() },
                contentAlignment = Alignment.Center,
            ) {
                Text("CANCEL", style = MaterialTheme.typography.labelMedium, color = ink)
            }
            Box(
                modifier = Modifier
                    .weight(2f)
                    .height(FieldTokens.ToolHeight)
                    .alpha(if (primaryEnabled) 1f else 0.35f)
                    .border(FieldTokens.Hair, ink)
                    .background(ink)
                    .clickable(enabled = primaryEnabled) { onPrimary() },
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (primaryLabel.endsWith("…")) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = paper,
                            strokeWidth = 1.5.dp,
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        primaryLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = paper,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Photo options sheet
// ---------------------------------------------------------------------------

@Composable
private fun PhotoOptionsSheet(
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(FieldTokens.HairStrong, ink)
                .background(paper),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "§ EVIDENCE",
                        style = MonoTiny,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Attach a photo",
                        style = MaterialTheme.typography.headlineMedium,
                        color = ink,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(FieldTokens.AppBarIconSize)
                        .border(FieldTokens.Hair, ink)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("×", style = MaterialTheme.typography.headlineSmall, color = ink)
                }
            }
            Hairline()
            PhotoOption(
                index = "01",
                label = "FROM GALLERY",
                hint = "Pick an image from this device.",
                onClick = onPick
            )
            HairlineSoft()
            if (hasPhoto) {
                PhotoOption(
                    index = "02",
                    label = "CLEAR EVIDENCE",
                    hint = "Detach the photo from this case.",
                    tone = Signal,
                    onClick = onClear,
                )
            }
        }
    }
}

@Composable
private fun PhotoOption(
    index: String,
    label: String,
    hint: String,
    tone: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onBackground,
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
            Text(
                "/ $label",
                style = MaterialTheme.typography.labelMedium,
                color = tone,
            )
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
        Hairline(color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                if (isEdit) "CASE · AMENDED" else "CASE · DRAFT",
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
            "― FIELD · LOST & FOUND ―",
            style = MonoTiny,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
