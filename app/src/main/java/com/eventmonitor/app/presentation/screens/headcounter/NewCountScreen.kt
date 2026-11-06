package com.eventmonitor.app.presentation.screens.headcounter

import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eventmonitor.shared.data.models.EventTypeDto

/** Intake shown before a new count opens: what is being counted, an automatic start time, and who is counting. */
@Composable
internal fun NewCountScreen(
    venueName: String,
    eventTypes: List<EventTypeDto>,
    onCancel: () -> Unit,
    onStart: (EventTypeDto?, String) -> Unit,
) {
    val haptics = rememberCounterHaptics()
    // null selects the built-in general "Head count" type.
    var selectedTypeId by rememberSaveable { mutableStateOf<String?>(null) }
    var countedBy by rememberSaveable { mutableStateOf("") }
    val openedAt = rememberSaveable { System.currentTimeMillis() }
    val focus = remember { FocusRequester() }
    val name = countedBy.trim()
    val context = LocalContext.current

    fun start() {
        if (name.isEmpty()) return
        haptics.success()
        onStart(eventTypes.firstOrNull { it.id == selectedTypeId }, name)
    }

    LaunchedEffect(Unit) { focus.requestFocus() }

    Column(
        Modifier
            .fillMaxSize()
            .background(CounterPalette.ground)
            .systemBarsPadding()
            .imePadding(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("New count", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text(venueName, style = MaterialTheme.typography.bodyMedium, color = CounterPalette.pew)
            }
            CounterPill(onClick = onCancel) { Text("Cancel", fontWeight = FontWeight.SemiBold) }
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Section("What are you counting?") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TypeRow("Head count", "A general count with no event type", selectedTypeId == null) {
                        haptics.select(); selectedTypeId = null
                    }
                    eventTypes.forEach { type ->
                        TypeRow(type.name, type.description.ifEmpty { "Event type" }, selectedTypeId == type.id) {
                            haptics.select(); selectedTypeId = type.id
                        }
                    }
                }
            }

            Section("Started") {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(CounterPalette.plate, RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        formatCountDate(context, openedAt),
                        style = digitStyle(30.dp),
                        modifier = Modifier.weight(1f),
                    )
                    Text("Set automatically", style = MaterialTheme.typography.bodySmall, color = CounterPalette.pew)
                }
            }

            Section("Who is counting?") {
                TextField(
                    value = countedBy,
                    onValueChange = { countedBy = it },
                    placeholder = { Text("Your name") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleLarge,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Go,
                    ),
                    keyboardActions = KeyboardActions(onGo = { start() }),
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = CounterPalette.plate,
                        unfocusedContainerColor = CounterPalette.plate,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = CounterPalette.brass,
                    ),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).focusRequester(focus),
                )
                Text("Your name goes on the report.", style = MaterialTheme.typography.bodySmall, color = CounterPalette.pew)
            }
        }

        CounterPill(
            onClick = ::start,
            enabled = name.isNotEmpty(),
            tint = CounterPalette.brass,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).height(60.dp),
        ) {
            Text("Start count", style = digitStyle(26.dp), color = CounterPalette.brassInk)
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        content()
    }
}

@Composable
private fun TypeRow(name: String, detail: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clip(shape)
            .background(if (selected) CounterPalette.plate else Color.Transparent)
            .border(1.dp, if (selected) CounterPalette.brass else CounterPalette.line, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier.size(20.dp).border(2.dp, if (selected) CounterPalette.brass else CounterPalette.pew, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            // Qualified: inside the Row, the unqualified name resolves to RowScope.AnimatedVisibility, which this Box cannot call.
            androidx.compose.animation.AnimatedVisibility(selected, enter = scaleIn(), exit = scaleOut()) {
                Box(Modifier.size(10.dp).background(CounterPalette.brass, CircleShape))
            }
        }
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = CounterPalette.pew, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
