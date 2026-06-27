package com.eventmonitor.app.presentation.screens.venues

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.androidx.compose.koinViewModel
import com.eventmonitor.core.common.theme.DataMono
import com.eventmonitor.core.common.theme.MonoTiny
import com.eventmonitor.core.common.theme.Signal
import com.eventmonitor.core.common.ui.ArcadeBackground
import com.eventmonitor.core.common.ui.FieldTokens
import com.eventmonitor.core.common.ui.KeyTone
import com.eventmonitor.core.common.ui.SoftAppBar
import com.eventmonitor.core.common.ui.SoftBottomDock
import com.eventmonitor.core.common.ui.SoftCard
import com.eventmonitor.core.common.ui.SoftKey
import com.eventmonitor.core.common.ui.SoftPrimaryButton
import com.eventmonitor.core.common.ui.SoftLivePulseDot
import com.eventmonitor.core.common.ui.SoftSection
import com.eventmonitor.core.common.ui.softHeadline
import com.eventmonitor.core.common.utils.rememberHapticFeedback

@Composable
fun VenueSetupScreen(
    viewModel: VenueSetupViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
    onManageAreas: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val isEditMode = viewModel.isEditMode()
    val haptic = rememberHapticFeedback()

    val canSave = uiState.name.isNotBlank() && uiState.code.isNotBlank()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SoftAppBar(
                title = if (isEditMode) "Amend." else "New venue.",
                subtitle = if (isEditMode) "§ Record · Edit" else "§ Record · Draft",
                onBack = { haptic.light(); onNavigateBack() },
                trailing = {
                    if (isEditMode && uiState.isActive) LivePulseDot()
                },
            )
        },
        bottomBar = {
            SaveSlab(
                label = if (isEditMode) "COMMIT CHANGES" else "CREATE · MAP ZONES",
                hintLine = if (isEditMode) {
                    "Updates propagate to field teams instantly."
                } else {
                    "Next step: define zones and capacity."
                },
                enabled = canSave,
                loading = uiState.isLoading,
                onClick = {
                    haptic.medium()
                    viewModel.saveBranch { branchId ->
                        if (isEditMode) onNavigateBack() else onManageAreas(branchId)
                    }
                },
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
                    .verticalScroll(rememberScrollState()),
            ) {
                IdentityStanza(
                    name = uiState.name,
                    code = uiState.code,
                    location = uiState.location,
                    onNameChange = viewModel::updateName,
                    onCodeChange = viewModel::updateCode,
                    onLocationChange = viewModel::updateLocation,
                )

                LiaisonStanza(
                    person = uiState.contactPerson,
                    email = uiState.contactEmail,
                    phone = uiState.contactPhone,
                    onPersonChange = viewModel::updateContactPerson,
                    onEmailChange = viewModel::updateContactEmail,
                    onPhoneChange = viewModel::updateContactPhone,
                )

                CapabilitiesStanza(
                    headcount = uiState.isHeadCountEnabled,
                    lostFound = uiState.isLostAndFoundEnabled,
                    incidents = uiState.isIncidentReportingEnabled,
                    onHeadcount = viewModel::updateHeadCountEnabled,
                    onLostFound = viewModel::updateLostAndFoundEnabled,
                    onIncidents = viewModel::updateIncidentReportingEnabled,
                )

                if (isEditMode) {
                    VisibilityStanza(
                        isActive = uiState.isActive,
                        onChange = viewModel::updateActive,
                    )
                }

                AnimatedVisibility(
                    visible = uiState.error != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    uiState.error?.let { ErrorRibbon(it) }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 01 · Identity
// ---------------------------------------------------------------------------

@Composable
private fun IdentityStanza(
    name: String,
    code: String,
    location: String,
    onNameChange: (String) -> Unit,
    onCodeChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    SoftSection(title = "Identity", eyebrow = "01 · IDENTITY", hint = "Required")
    Spacer(Modifier.height(20.dp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        Text("VENUE NAME", style = MonoTiny, color = muted)
        Spacer(Modifier.height(10.dp))
        FieldInput(
            value = name,
            onChange = onNameChange,
            placeholder = "Untitled venue",
            textStyle = softHeadline(28).copy(color = ink),
            capitalization = KeyboardCapitalization.Words,
        )

        Spacer(Modifier.height(28.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.width(120.dp)) {
                Text("SHORT CODE", style = MonoTiny, color = muted)
                Spacer(Modifier.height(10.dp))
                FieldInput(
                    value = code,
                    onChange = { onCodeChange(it.take(6).uppercase()) },
                    placeholder = "——",
                    textStyle = TextStyle(
                        fontFamily = DataMono,
                        fontWeight = FontWeight.Medium,
                        fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                        letterSpacing = 2.sp,
                    ).copy(color = ink),
                    capitalization = KeyboardCapitalization.Characters,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "// 2–6 letters · appears on badges",
                    style = MonoTiny,
                    color = muted,
                )
            }

            Spacer(Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text("LOCATION", style = MonoTiny, color = muted)
                Spacer(Modifier.height(10.dp))
                FieldInput(
                    value = location,
                    onChange = onLocationChange,
                    placeholder = "Address or descriptor",
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = ink),
                    capitalization = KeyboardCapitalization.Words,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "// shown under venue name in roster",
                    style = MonoTiny,
                    color = muted,
                )
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

// ---------------------------------------------------------------------------
// 02 · Liaison — contact trio
// ---------------------------------------------------------------------------

@Composable
private fun LiaisonStanza(
    person: String,
    email: String,
    phone: String,
    onPersonChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
) {
    SoftSection(title = "Liaison", eyebrow = "02 · LIAISON", hint = "Optional")
    Spacer(Modifier.height(6.dp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
    ) {
        LabeledInput(
            label = "POINT OF CONTACT",
            value = person,
            onChange = onPersonChange,
            placeholder = "Name",
            capitalization = KeyboardCapitalization.Words,
        )
        Spacer(Modifier.height(20.dp))
        LabeledInput(
            label = "EMAIL",
            value = email,
            onChange = onEmailChange,
            placeholder = "name@venue.org",
            keyboardType = KeyboardType.Email,
            textStyle = TextStyle(
                fontFamily = DataMono,
                fontSize = MaterialTheme.typography.bodyMedium.fontSize,
            ).copy(color = MaterialTheme.colorScheme.onBackground),
        )
        Spacer(Modifier.height(20.dp))
        LabeledInput(
            label = "PHONE",
            value = phone,
            onChange = onPhoneChange,
            placeholder = "+1 555 000 0000",
            keyboardType = KeyboardType.Phone,
            textStyle = TextStyle(
                fontFamily = DataMono,
                fontSize = MaterialTheme.typography.bodyMedium.fontSize,
            ).copy(color = MaterialTheme.colorScheme.onBackground),
        )
    }
}

// ---------------------------------------------------------------------------
// 03 · Capabilities — toggle rows
// ---------------------------------------------------------------------------

@Composable
private fun CapabilitiesStanza(
    headcount: Boolean,
    lostFound: Boolean,
    incidents: Boolean,
    onHeadcount: (Boolean) -> Unit,
    onLostFound: (Boolean) -> Unit,
    onIncidents: (Boolean) -> Unit,
) {
    val enabledCount = listOf(headcount, lostFound, incidents).count { it }

    SoftSection(
        title = "Capabilities",
        eyebrow = "03 · CAPABILITIES",
        hint = "$enabledCount / 3 enabled",
    )

    Column(Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(10.dp))
        SoftCard(modifier = Modifier.fillMaxWidth()) {
            CapabilityRow(
                glyph = "▦",
                title = "Head count",
                caption = "Track seated, standing, and overflow in real time.",
                checked = headcount,
                onChange = onHeadcount,
            )
            Spacer(Modifier.height(20.dp))
            CapabilityRow(
                glyph = "⌧",
                title = "Lost & found",
                caption = "Tag items, attach photos, reconcile on claim.",
                checked = lostFound,
                onChange = onLostFound,
            )
            Spacer(Modifier.height(20.dp))
            CapabilityRow(
                glyph = "!",
                title = "Incident log",
                caption = "Record safety, medical, and security events.",
                checked = incidents,
                onChange = onIncidents,
            )
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun CapabilityRow(
    glyph: String,
    title: String,
    caption: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val haptic = rememberHapticFeedback()
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptic.light()
                onChange(!checked)
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .border(FieldTokens.Hair, if (checked) ink else muted.copy(alpha = 0.4f))
                .background(if (checked) ink else androidx.compose.ui.graphics.Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = glyph,
                style = MaterialTheme.typography.titleMedium,
                color = if (checked) MaterialTheme.colorScheme.background else muted,
            )
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = if (checked) ink else muted,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = muted,
            )
        }

        Spacer(Modifier.width(12.dp))

        ToggleMark(checked)
    }
}

@Composable
private fun ToggleMark(checked: Boolean) {
    val ink = MaterialTheme.colorScheme.onBackground
    val paper = MaterialTheme.colorScheme.background

    Box(
        modifier = Modifier
            .width(60.dp)
            .height(28.dp)
            .border(FieldTokens.Hair, ink)
            .background(if (checked) ink else paper),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (checked) "● ON" else "○ OFF",
            style = MaterialTheme.typography.labelSmall,
            color = if (checked) paper else ink,
        )
    }
}

// ---------------------------------------------------------------------------
// 04 · Visibility — twin slab toggle (edit mode only)
// ---------------------------------------------------------------------------

@Composable
private fun VisibilityStanza(
    isActive: Boolean,
    onChange: (Boolean) -> Unit,
) {
    SoftSection(
        title = "Visibility",
        eyebrow = "04 · VISIBILITY",
        hint = if (isActive) "● LIVE" else "○ IDLE",
    )

    val ink = MaterialTheme.colorScheme.onBackground
    val paper = MaterialTheme.colorScheme.background
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val haptic = rememberHapticFeedback()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .border(FieldTokens.Hair, ink),
        ) {
            VisibilityCell(
                title = "LIVE",
                caption = "visible to teams",
                selected = isActive,
                selectedBg = ink,
                selectedFg = paper,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                onClick = {
                    haptic.medium()
                    onChange(true)
                },
            )
            Box(
                modifier = Modifier
                    .width(FieldTokens.Hair)
                    .fillMaxSize()
                    .background(ink),
            )
            VisibilityCell(
                title = "IDLE",
                caption = "hidden from roster",
                selected = !isActive,
                selectedBg = ink,
                selectedFg = paper,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                onClick = {
                    haptic.medium()
                    onChange(false)
                },
            )
        }

        Spacer(Modifier.height(10.dp))
        Text(
            text = if (isActive) {
                "// This venue appears in the tonight roster and receives incoming counts."
            } else {
                "// This venue is archived. Historical data is preserved but no new events accrue."
            },
            style = MonoTiny,
            color = muted,
        )
    }
}

@Composable
private fun VisibilityCell(
    title: String,
    caption: String,
    selected: Boolean,
    selectedBg: androidx.compose.ui.graphics.Color,
    selectedFg: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .background(if (selected) selectedBg else androidx.compose.ui.graphics.Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selected && title == "LIVE") {
                    LivePulseDot()
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (selected) selectedFg else ink,
                )
            }
            Text(
                text = caption,
                style = MonoTiny,
                color = if (selected) selectedFg.copy(alpha = 0.65f) else muted,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Stanza header
// ---------------------------------------------------------------------------

@Composable
private fun StanzaHeader(number: String, label: String, note: String? = null) {
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Spacer(Modifier.height(20.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = number,
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Light),
            color = ink,
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = muted)) { append("§ ") }
                    append(label)
                },
                style = MaterialTheme.typography.labelMedium,
                color = ink,
            )
        }
        if (!note.isNullOrBlank()) {
            Text(
                text = note,
                style = MonoTiny,
                color = if (note.contains("LIVE")) Signal else muted,
            )
        }
    }
    Spacer(Modifier.height(20.dp))
}

// ---------------------------------------------------------------------------
// Inputs — BasicTextField with hairline underline
// ---------------------------------------------------------------------------

@Composable
private fun FieldInput(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    textStyle: TextStyle,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val ink = MaterialTheme.colorScheme.onBackground
    var focused by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = textStyle,
            singleLine = true,
            cursorBrush = SolidColor(Signal),
            keyboardOptions = KeyboardOptions(
                capitalization = capitalization,
                keyboardType = keyboardType,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused },
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.padding(vertical = 6.dp)) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = textStyle.copy(color = muted.copy(alpha = 0.5f)),
                        )
                    }
                    innerTextField()
                }
            },
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (focused) FieldTokens.HairStrong else FieldTokens.Hair)
                .background(if (focused) Signal else ink.copy(alpha = 0.55f)),
        )
    }
}

@Composable
private fun LabeledInput(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    keyboardType: KeyboardType = KeyboardType.Text,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge.copy(
        color = MaterialTheme.colorScheme.onBackground,
    ),
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val ink = MaterialTheme.colorScheme.onBackground
    var focused by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MonoTiny,
            color = muted,
            modifier = Modifier.width(120.dp),
        )
        Box(modifier = Modifier.weight(1f)) {
            BasicTextField(
                value = value,
                onValueChange = onChange,
                textStyle = textStyle,
                singleLine = true,
                cursorBrush = SolidColor(Signal),
                keyboardOptions = KeyboardOptions(
                    capitalization = capitalization,
                    keyboardType = keyboardType,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focused = it.isFocused },
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = textStyle.copy(color = muted.copy(alpha = 0.5f)),
                        )
                    }
                    innerTextField()
                },
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(if (focused) Signal else if (value.isBlank()) muted.copy(alpha = 0.3f) else ink),
        )
    }
}

// ---------------------------------------------------------------------------
// Error ribbon
// ---------------------------------------------------------------------------

@Composable
private fun ErrorRibbon(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Signal)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "!!",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.background,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = message.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.weight(1f),
        )
    }
}

// ---------------------------------------------------------------------------
// Sticky save slab — full-bleed ink CTA with hint line
// ---------------------------------------------------------------------------

@Composable
private fun SaveSlab(
    label: String,
    hintLine: String,
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val ink = MaterialTheme.colorScheme.onBackground

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 6.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (enabled) "READY TO COMMIT" else "COMPLETE IDENTITY TO CONTINUE",
                style = MonoTiny,
                color = if (enabled) ink else muted,
                modifier = Modifier.weight(1f),
            )
            Text(text = hintLine, style = MonoTiny, color = muted)
        }
        SoftBottomDock {
            SoftPrimaryButton(
                label = label,
                onClick = onClick,
                enabled = enabled && !loading,
                modifier = Modifier.fillMaxWidth(),
                trailingGlyph = if (loading) "…" else "→",
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Live pulse dot
// ---------------------------------------------------------------------------

@Composable
private fun LivePulseDot() {
    SoftLivePulseDot(size = 8)
}
