package com.eventmonitor.app.presentation.screens.areas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
import com.eventmonitor.core.domain.models.AreaType

// ═══════════════════════════════════════════════════════════════════════════
// ZONE EDITOR — full-screen composing page (replaces add/edit/quick dialogs)
// Layout beats:
//   · FieldAppBar         ← back · COMPOSING ZONE · ✕ close
//   · Mode bar            SOLO · BATCH · EDIT (locked in EDIT mode)
//   · HERO PROOF SHEET    inverted ink panel — live name, type tag, capacity
//                         capacity ribbon (visual fill), draft / ready stamp
//   · 01 NAMING           big inline input · char counter · suggestion chips
//   · 02 CLASSIFICATION   2-col grid of "type cards" w/ letter mark + tag
//   · 03 CAPACITY         stepper + visual scale + preset chips
//   · 04 BATCH SERIES     count + start steppers + quick multipliers (BATCH)
//   · 05 REGISTRATION     mini ledger row preview / batch sample list
//   · Sticky action rail  CANCEL · primary morphing label
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun ZoneEditorScreen(
    viewModel: ZoneEditorViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val haptic = rememberHapticFeedback()

    LaunchedEffect(state.finished) {
        if (state.finished) onNavigateBack()
    }

    val title = when (state.mode) {
        EditorMode.SOLO -> "COMPOSING ZONE"
        EditorMode.BATCH -> "COMPOSING BATCH"
        EditorMode.EDIT -> "REVISING ZONE"
    }
    val eyebrow = state.venueName.ifBlank { "VENUE" }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            FieldAppBar(
                title = title,
                eyebrow = eyebrow,
                leading = {
                    FieldAppBarIcon(glyph = "←", onClick = { haptic.light(); onNavigateBack() })
                },
                trailing = {
                    FieldAppBarIcon(glyph = "✕", onClick = { haptic.light(); onNavigateBack() })
                },
            )
        },
        bottomBar = {
            EditorActionRail(
                state = state,
                onCancel = { haptic.light(); onNavigateBack() },
                onSubmit = { haptic.medium(); viewModel.submit() },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            EditorBody(
                state = state,
                onModeChange = { haptic.light(); viewModel.setMode(it) },
                onNameChange = viewModel::setName,
                onTypeChange = { haptic.light(); viewModel.setType(it) },
                onCapacityChange = viewModel::setCapacity,
                onBatchCountChange = viewModel::setBatchCount,
                onBatchStartChange = viewModel::setBatchStart,
            )

            AnimatedVisibility(
                visible = state.error != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 76.dp),
            ) {
                state.error?.let { err ->
                    Snackbar(
                        modifier = Modifier.padding(16.dp),
                        containerColor = MaterialTheme.colorScheme.onBackground,
                        contentColor = MaterialTheme.colorScheme.background,
                        action = {
                            TextButton(onClick = { viewModel.clearError() }) {
                                Text(
                                    "OK",
                                    color = MaterialTheme.colorScheme.background,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        },
                    ) {
                        Text(err, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

// ─── Body ──────────────────────────────────────────────────────────────────

@Composable
private fun EditorBody(
    state: ZoneEditorState,
    onModeChange: (EditorMode) -> Unit,
    onNameChange: (String) -> Unit,
    onTypeChange: (AreaType) -> Unit,
    onCapacityChange: (Int) -> Unit,
    onBatchCountChange: (Int) -> Unit,
    onBatchStartChange: (Int) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        if (state.mode != EditorMode.EDIT) {
            item("mode") {
                ModeStrip(active = state.mode, onModeChange = onModeChange)
            }
        }
        item("hero") { ProofSheet(state) }

        if (state.mode != EditorMode.BATCH) {
            item("step-name") {
                ComposerStep(
                    index = "01",
                    label = "CHRISTENING",
                    hint = if (state.mode == EditorMode.EDIT) "Rename without breaking the ledger."
                    else "Tap. Type. Make it short and unmistakable.",
                ) {
                    NameField(
                        value = state.name,
                        onValueChange = onNameChange,
                        suggestions = suggestionsFor(state.type),
                    )
                }
            }
        }

        item("step-type") {
            ComposerStep(
                index = if (state.mode == EditorMode.BATCH) "01" else "02",
                label = "CLASSIFICATION",
                hint = if (state.typeLocked) "Type is sealed once a zone exists."
                else "Pick its species. Sets the colour of the ledger.",
            ) {
                if (state.typeLocked) LockedTypePanel(state.type)
                else TypeCardGrid(selected = state.type, onSelect = onTypeChange)
            }
        }

        item("step-capacity") {
            ComposerStep(
                index = when (state.mode) {
                    EditorMode.SOLO -> "03"
                    EditorMode.BATCH -> "02"
                    EditorMode.EDIT -> "03"
                },
                label = "CAPACITY",
                hint = "Bodies in the room — soft cap, not a fence.",
            ) {
                CapacityComposer(state.capacity, onCapacityChange)
            }
        }

        if (state.mode == EditorMode.BATCH) {
            item("step-batch") {
                ComposerStep(
                    index = "03",
                    label = "SERIES",
                    hint = "How many to mint and where to start the count.",
                ) {
                    BatchSeriesEditor(
                        count = state.batchCount,
                        start = state.batchStart,
                        onCountChange = onBatchCountChange,
                        onStartChange = onBatchStartChange,
                    )
                }
            }
        }

        item("step-registration") {
            ComposerStep(
                index = when (state.mode) {
                    EditorMode.SOLO -> "04"
                    EditorMode.BATCH -> "04"
                    EditorMode.EDIT -> "04"
                },
                label = "REGISTRATION",
                hint = "How it appears on the books.",
            ) {
                if (state.mode == EditorMode.BATCH) {
                    BatchPreview(state)
                } else {
                    LedgerRowPreview(state)
                }
            }
        }

        item("colophon") {
            EditorColophon(state)
        }
    }
}

// ─── Mode strip ────────────────────────────────────────────────────────────

@Composable
private fun ModeStrip(active: EditorMode, onModeChange: (EditorMode) -> Unit) {
    val ink = MaterialTheme.colorScheme.onBackground
    val paper = MaterialTheme.colorScheme.background
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 14.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ModeTab(
                code = "01",
                label = "SOLO",
                caption = "ONE",
                selected = active == EditorMode.SOLO,
                onClick = { onModeChange(EditorMode.SOLO) },
                ink = ink,
                paper = paper,
                modifier = Modifier.weight(1f),
            )
            ModeTab(
                code = "02",
                label = "BATCH",
                caption = "+N",
                selected = active == EditorMode.BATCH,
                onClick = { onModeChange(EditorMode.BATCH) },
                ink = ink,
                paper = paper,
                modifier = Modifier.weight(1f),
            )
        }
        Hairline()
    }
}

@Composable
private fun ModeTab(
    code: String,
    label: String,
    caption: String,
    selected: Boolean,
    onClick: () -> Unit,
    ink: Color,
    paper: Color,
    modifier: Modifier = Modifier,
) {
    val bg = if (selected) ink else paper
    val fg = if (selected) paper else ink
    Column(
        modifier = modifier
            .border(FieldTokens.HairStrong, ink)
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = code,
                style = MonoTiny,
                color = fg.copy(alpha = 0.7f),
            )
            Text(
                text = caption,
                style = MonoTiny,
                color = fg.copy(alpha = 0.7f),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.headlineSmall,
            color = fg,
        )
    }
}

// ─── Hero proof sheet ──────────────────────────────────────────────────────

@Composable
private fun ProofSheet(state: ZoneEditorState) {
    val ink = MaterialTheme.colorScheme.onBackground
    val paper = MaterialTheme.colorScheme.background

    val displayName = when (state.mode) {
        EditorMode.BATCH -> {
            val first = quickNameFor(state.type, state.batchStart)
            val last = quickNameFor(state.type, state.batchStart + state.batchCount - 1)
            if (state.batchCount > 1) "$first → $last" else first
        }

        else -> state.name.ifBlank { "—" }
    }

    val stamp = when {
        state.mode == EditorMode.EDIT -> "§ REVISION"
        state.mode == EditorMode.BATCH -> "§ ${state.batchCount.toString().padStart(2, '0')} DRAFTS"
        state.name.isBlank() -> "§ DRAFT"
        else -> "§ READY"
    }
    val stampColor = if (state.mode == EditorMode.EDIT) Sage
    else if (state.name.isBlank() && state.mode != EditorMode.BATCH) paper.copy(alpha = 0.55f)
    else paper

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 14.dp, bottom = 18.dp)
            .background(ink)
            .border(FieldTokens.HairStrong, ink),
    ) {
        // Top register
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 14.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stamp,
                style = MonoTiny,
                color = stampColor,
            )
            Text(
                text = "FIELD · ${state.type.shortTagX()}",
                style = MonoTiny,
                color = paper.copy(alpha = 0.55f),
            )
        }
        Box(Modifier
            .fillMaxWidth()
            .height(FieldTokens.Hair)
            .background(paper.copy(alpha = 0.18f)))

        // The name (huge) + letter mark
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .border(FieldTokens.Hair, paper)
                    .background(ink),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = state.type.letterMark(),
                    style = MaterialTheme.typography.displaySmall,
                    color = paper,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ZONE NAME",
                    style = MonoTiny,
                    color = paper.copy(alpha = 0.5f),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = displayName,
                    style = if (displayName.length > 24)
                        MaterialTheme.typography.headlineSmall
                    else MaterialTheme.typography.headlineLarge,
                    color = paper,
                    maxLines = 2,
                )
            }
        }

        Box(Modifier
            .fillMaxWidth()
            .height(FieldTokens.Hair)
            .background(paper.copy(alpha = 0.18f)))

        // Capacity ribbon
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 14.dp, bottom = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column {
                    Text("CAPACITY", style = MonoTiny, color = paper.copy(alpha = 0.55f))
                    Text(
                        text = state.capacity.toString(),
                        style = MaterialTheme.typography.displaySmall,
                        color = paper,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (state.mode == EditorMode.BATCH) {
                        Text("TOTAL", style = MonoTiny, color = paper.copy(alpha = 0.55f))
                        Text(
                            text = (state.capacity * state.batchCount).groupedThousandsX(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = paper,
                        )
                    } else {
                        Text("PERSONS", style = MonoTiny, color = paper.copy(alpha = 0.55f))
                        Text(
                            text = state.capacity.bandLabel(),
                            style = MaterialTheme.typography.headlineSmall,
                            color = paper,
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            CapacityRibbon(value = state.capacity)
        }
    }
}

@Composable
private fun CapacityRibbon(value: Int) {
    val paper = MaterialTheme.colorScheme.background
    val band = (value.coerceIn(0, 1000)) / 1000f
    val animatedFill by animateFloatAsState(
        targetValue = band,
        animationSpec = tween(360),
        label = "capRibbon",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp)
            .border(FieldTokens.Hair, paper.copy(alpha = 0.5f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            // tick marks
            repeat(20) { i ->
                Box(
                    modifier = Modifier
                        .padding(start = (i * 5).dp)
                        .width(FieldTokens.Hair)
                        .fillMaxSize()
                        .background(paper.copy(alpha = 0.18f)),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize(fraction = 1f)
                .background(Color.Transparent),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize(fraction = 1f),
            ) {
                Box(
                    modifier = Modifier
                        .background(paper)
                        .height(14.dp)
                        .fillMaxWidth(animatedFill),
                )
            }
        }
    }
}

// ─── Composer step shell ───────────────────────────────────────────────────

@Composable
private fun ComposerStep(
    index: String,
    label: String,
    hint: String? = null,
    content: @Composable () -> Unit,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 22.dp, bottom = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .border(FieldTokens.Hair, ink)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = index,
                    style = MonoTiny,
                    color = ink,
                )
            }
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier
                    .height(FieldTokens.Hair)
                    .width(28.dp)
                    .background(ink),
            )
            Spacer(Modifier.width(10.dp))
            Text(text = label, style = MonoTiny, color = muted)
        }
        if (hint != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = hint,
                style = MaterialTheme.typography.bodyMedium,
                color = ink,
            )
        }
        Spacer(Modifier.height(14.dp))
        content()
    }
    HairlineSoft()
}

// ─── Step content components ───────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NameField(
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<String>,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = MaterialTheme.typography.headlineMedium.copy(color = ink),
                    cursorBrush = SolidColor(ink),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        keyboardType = KeyboardType.Text,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    decorationBox = { inner ->
                        Box {
                            if (value.isEmpty()) {
                                Text(
                                    text = "Bay 1  ·  VIP North  ·  Care Room",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = muted.copy(alpha = 0.45f),
                                )
                            }
                            inner()
                        }
                    },
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = "${value.length.toString().padStart(2, '0')}/48",
                style = MonoTiny,
                color = muted,
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(FieldTokens.HairStrong)
                .background(ink),
        )
        if (suggestions.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("SUGGESTED", style = MonoTiny, color = muted)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                suggestions.forEach { s ->
                    ZoneChip(label = s, selected = value == s, onClick = { onValueChange(s) })
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TypeCardGrid(selected: AreaType, onSelect: (AreaType) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AreaType.entries.forEach { type ->
            TypeCard(type = type, selected = type == selected, onClick = { onSelect(type) })
        }
    }
}

@Composable
private fun TypeCard(type: AreaType, selected: Boolean, onClick: () -> Unit) {
    val ink = MaterialTheme.colorScheme.onBackground
    val paper = MaterialTheme.colorScheme.background
    val bg = if (selected) ink else paper
    val fg = if (selected) paper else ink
    val mutedFg =
        if (selected) paper.copy(alpha = 0.65f) else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .border(FieldTokens.HairStrong, ink)
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .border(FieldTokens.Hair, fg),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = type.letterMark(),
                style = MaterialTheme.typography.titleLarge,
                color = fg,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = type.displayName,
                style = MaterialTheme.typography.titleSmall,
                color = fg,
            )
            Text(
                text = type.shortTagX(),
                style = MonoTiny,
                color = mutedFg,
            )
        }
    }
}

@Composable
private fun LockedTypePanel(type: AreaType) {
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(FieldTokens.Hair, ink)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .border(FieldTokens.Hair, ink),
            contentAlignment = Alignment.Center,
        ) { Text(type.letterMark(), style = MaterialTheme.typography.titleLarge, color = ink) }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = type.displayName, style = MaterialTheme.typography.titleMedium, color = ink)
            Text(text = "TYPE LOCKED · ${type.shortTagX()}", style = MonoTiny, color = muted)
        }
        Text("⌛", style = MaterialTheme.typography.titleMedium, color = muted)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CapacityComposer(value: Int, onValue: (Int) -> Unit) {
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val paper = MaterialTheme.colorScheme.background
    val nudge = if (value < 50) 5 else if (value < 200) 10 else 25
    val bigNudge = if (value < 200) 50 else 100

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(FieldTokens.HairStrong, ink),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CapKey(
                glyph = "−",
                caption = "−$nudge",
                onClick = { onValue(value - nudge) },
                modifier = Modifier.weight(1f)
            )
            Box(Modifier
                .width(FieldTokens.Hair)
                .height(96.dp)
                .background(ink))
            Column(
                modifier = Modifier
                    .weight(2.4f)
                    .background(paper)
                    .padding(vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = value.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    color = ink,
                )
                Text(text = "PERSONS", style = MonoTiny, color = muted)
            }
            Box(Modifier
                .width(FieldTokens.Hair)
                .height(96.dp)
                .background(ink))
            CapKey(
                glyph = "+",
                caption = "+$nudge",
                onClick = { onValue(value + nudge) },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(10.dp))

        // Big nudge row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ZoneChip(
                label = "−$bigNudge",
                selected = false,
                onClick = { onValue(value - bigNudge) },
                modifier = Modifier.weight(1f),
            )
            ZoneChip(
                label = "RESET 100",
                selected = value == 100,
                onClick = { onValue(100) },
                modifier = Modifier.weight(1f),
            )
            ZoneChip(
                label = "+$bigNudge",
                selected = false,
                onClick = { onValue(value + bigNudge) },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text("PRESETS", style = MonoTiny, color = muted)
        Spacer(Modifier.height(6.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(25, 50, 100, 150, 200, 300, 500, 750, 1000).forEach { p ->
                ZoneChip(label = p.toString(), selected = value == p, onClick = { onValue(p) })
            }
        }
    }
}

@Composable
private fun CapKey(
    glyph: String,
    caption: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .height(96.dp)
            .clickable { onClick() },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = glyph, style = MaterialTheme.typography.displaySmall, color = ink)
        Text(text = caption, style = MonoTiny, color = muted)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BatchSeriesEditor(
    count: Int,
    start: Int,
    onCountChange: (Int) -> Unit,
    onStartChange: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BatchStepper(
                label = "COUNT",
                value = count,
                onValue = onCountChange,
                modifier = Modifier.weight(1f),
            )
            BatchStepper(
                label = "START #",
                value = start,
                onValue = onStartChange,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text("QUICK SIZE", style = MonoTiny, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(3, 6, 9, 12, 18, 24, 36).forEach { p ->
                ZoneChip(label = "×$p", selected = count == p, onClick = { onCountChange(p) })
            }
        }
    }
}

@Composable
private fun BatchStepper(
    label: String,
    value: Int,
    onValue: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier = modifier.border(FieldTokens.HairStrong, ink)) {
        Text(
            text = label,
            style = MonoTiny,
            color = muted,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
        HairlineSoft()
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onValue(value - 1) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "−",
                    style = MaterialTheme.typography.headlineMedium,
                    color = ink,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
            Box(
                Modifier
                    .width(FieldTokens.Hair)
                    .height(56.dp)
                    .background(MaterialTheme.colorScheme.outline)
            )
            Box(modifier = Modifier.weight(2f), contentAlignment = Alignment.Center) {
                Text(
                    text = value.toString(),
                    style = MaterialTheme.typography.headlineLarge,
                    color = ink,
                    textAlign = TextAlign.Center,
                )
            }
            Box(
                Modifier
                    .width(FieldTokens.Hair)
                    .height(56.dp)
                    .background(MaterialTheme.colorScheme.outline)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onValue(value + 1) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "+",
                    style = MaterialTheme.typography.headlineMedium,
                    color = ink,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
        }
    }
}

// ─── Previews ──────────────────────────────────────────────────────────────

@Composable
private fun LedgerRowPreview(state: ZoneEditorState) {
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier = Modifier
        .fillMaxWidth()
        .border(FieldTokens.Hair, ink)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("LEDGER PREVIEW", style = MonoTiny, color = muted)
            Text(state.type.shortTagX(), style = MonoTiny, color = muted)
        }
        HairlineSoft()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.width(48.dp)) {
                Text("01", style = MaterialTheme.typography.titleLarge, color = ink)
                Text("/ 01", style = MonoTiny, color = muted)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.name.ifBlank { "—" },
                    style = MaterialTheme.typography.titleLarge,
                    color = ink,
                    maxLines = 1,
                )
                Text(
                    text = "${state.type.shortTagX()} · ${
                        if (state.zoneId != null) "ID ${
                            state.zoneId.take(
                                6
                            ).uppercase()
                        }" else "DRAFT"
                    }",
                    style = MonoTiny,
                    color = muted,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("CAP", style = MonoTiny, color = muted)
                Text(
                    state.capacity.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    color = ink
                )
            }
        }
    }
}

@Composable
private fun BatchPreview(state: ZoneEditorState) {
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val previewCount = minOf(5, state.batchCount)
    val names = (0 until previewCount).map { quickNameFor(state.type, state.batchStart + it) }

    Column(modifier = Modifier
        .fillMaxWidth()
        .border(FieldTokens.Hair, ink)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("BATCH PREVIEW", style = MonoTiny, color = muted)
            Text(
                text = "${
                    state.batchCount.toString().padStart(2, '0')
                } · ${state.type.shortTagX()}",
                style = MonoTiny,
                color = muted,
            )
        }
        HairlineSoft()
        names.forEachIndexed { i, n ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = (state.batchStart + i).toString().padStart(2, '0'),
                    style = MaterialTheme.typography.titleMedium,
                    color = ink,
                    modifier = Modifier.width(40.dp),
                )
                Text(
                    text = n,
                    style = MaterialTheme.typography.titleMedium,
                    color = ink,
                    modifier = Modifier.weight(1f)
                )
                Text("CAP ${state.capacity}", style = MonoTiny, color = muted)
            }
            if (i < names.lastIndex) HairlineSoft()
        }
        if (state.batchCount > previewCount) {
            HairlineSoft()
            Text(
                text = "… +${state.batchCount - previewCount} MORE",
                style = MonoTiny,
                color = muted,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
}

// ─── Action rail ───────────────────────────────────────────────────────────

@Composable
private fun EditorActionRail(
    state: ZoneEditorState,
    onCancel: () -> Unit,
    onSubmit: () -> Unit,
) {
    val primaryLabel = when (state.mode) {
        EditorMode.SOLO -> "MINT ZONE"
        EditorMode.BATCH -> "MINT ${state.batchCount.toString().padStart(2, '0')} ZONES"
        EditorMode.EDIT -> "SAVE CHANGES"
    }
    val enabled = !state.isSubmitting && when (state.mode) {
        EditorMode.SOLO -> state.name.isNotBlank()
        EditorMode.BATCH -> state.batchCount > 0
        EditorMode.EDIT -> state.name.isNotBlank()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Hairline()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RailButton(
                label = "CANCEL",
                inverted = false,
                onClick = onCancel,
                modifier = Modifier.weight(1f),
            )
            RailButton(
                label = if (state.isSubmitting) "MINTING…" else primaryLabel,
                inverted = true,
                enabled = enabled,
                onClick = onSubmit,
                modifier = Modifier.weight(1.8f),
            )
        }
    }
}

@Composable
private fun RailButton(
    label: String,
    inverted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val paper = MaterialTheme.colorScheme.background
    Box(
        modifier = modifier
            .height(FieldTokens.ToolHeight)
            .alpha(if (enabled) 1f else 0.35f)
            .border(FieldTokens.HairStrong, ink)
            .background(if (inverted) ink else paper)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (inverted) paper else ink,
        )
    }
}

// ─── Colophon ──────────────────────────────────────────────────────────────

@Composable
private fun EditorColophon(state: ZoneEditorState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Hairline(color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = when (state.mode) {
                    EditorMode.SOLO -> "PROOF · SOLO"
                    EditorMode.BATCH -> "PROOF · BATCH"
                    EditorMode.EDIT -> "PROOF · REVISION"
                },
                style = MonoTiny,
                color = if (state.name.isBlank() && state.mode != EditorMode.BATCH) Signal else Sage,
            )
            Text(
                text = state.venueName.ifBlank { "VENUE" }.uppercase(),
                style = MonoTiny,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            "― FIELD · ZONE EDITOR ―",
            style = MonoTiny,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ─── Helpers ───────────────────────────────────────────────────────────────

private fun suggestionsFor(type: AreaType): List<String> = when (type) {
    AreaType.SEATING -> listOf("Bay 1", "Bay 2", "Front Rows", "Balcony")
    AreaType.STANDING -> listOf("Pit", "Floor", "Back")
    AreaType.VIP -> listOf("VIP North", "VIP South", "Suite A")
    AreaType.GENERAL_ADMISSION -> listOf("GA East", "GA West", "GA Floor")
    AreaType.OVERFLOW -> listOf("Overflow Hall", "Side Room")
    AreaType.PARKING -> listOf("Lot A", "Lot B", "Valet")
    AreaType.REGISTRATION -> listOf("Front Desk", "Will-Call", "Press")
    AreaType.LOBBY -> listOf("Main Lobby", "East Entry", "West Entry")
    AreaType.OUTDOOR -> listOf("Courtyard", "Garden", "Plaza")
    AreaType.STAGE -> listOf("Main Stage", "Side Stage")
    AreaType.BACKSTAGE -> listOf("Green Room", "Crew", "Talent")
    AreaType.CARE_ROOM -> listOf("Care Room", "Quiet Room")
    AreaType.FOOD_AREA -> listOf("Bar", "Concessions", "Cafe")
    AreaType.RESTROOMS -> listOf("Restrooms", "WC East", "WC West")
    AreaType.EMERGENCY_EXIT -> listOf("Exit A", "Exit B", "Fire Exit")
    AreaType.OTHER -> emptyList()
}

private fun Int.bandLabel(): String = when {
    this <= 0 -> "—"
    this < 25 -> "INTIMATE"
    this < 100 -> "SMALL"
    this < 300 -> "MID"
    this < 750 -> "LARGE"
    this < 1500 -> "ARENA"
    else -> "STADIUM"
}

private fun Int.groupedThousandsX(): String =
    toString().reversed().chunked(3).joinToString(",").reversed()
