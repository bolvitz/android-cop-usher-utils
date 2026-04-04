package com.eventmonitor.feature.headcounter.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.eventmonitor.core.data.local.entities.EventTypeEntity
import com.eventmonitor.core.domain.models.ServiceType
import com.eventmonitor.core.common.utils.flipDownTransform
import com.eventmonitor.core.common.utils.flipUpTransform
import com.eventmonitor.core.common.utils.rememberHapticFeedback
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountingScreen(
    viewModel: CountingViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val haptic = rememberHapticFeedback()
    val uiState by viewModel.uiState.collectAsState()
    val eventTypes by viewModel.eventTypes.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    var showCreateDialog by remember { mutableStateOf(uiState.eventId == null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(uiState.branchName)
                        if (uiState.eventName.isNotEmpty()) {
                            Text(
                                text = uiState.eventName,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        haptic.light()
                        onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            haptic.medium()
                            viewModel.undo()
                        },
                        enabled = canUndo
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, "Undo")
                    }
                    IconButton(
                        onClick = {
                            haptic.medium()
                            viewModel.redo()
                        },
                        enabled = canRedo
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Redo, "Redo")
                    }
                    IconButton(onClick = {
                        haptic.light()
                        viewModel.shareReport()
                    }) {
                        Icon(Icons.Default.Share, "Share")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (showCreateDialog) {
            CreateServiceDialog(
                eventTypes = eventTypes,
                onDismiss = { showCreateDialog = false; onNavigateBack() },
                onCreate = { eventTypeId, eventTypeName, date, countedBy ->
                    viewModel.createNewService(eventTypeId, eventTypeName, date, countedBy)
                    showCreateDialog = false
                }
            )
        }

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.eventId != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Total attendance card - fixed at top
                    val totalPercentage = if (uiState.totalCapacity > 0) {
                        (uiState.totalAttendance.toFloat() / uiState.totalCapacity * 100).toInt()
                    } else 0
                    val totalCapacityLevel = when {
                        totalPercentage >= 95 -> CapacityLevel.CRITICAL
                        totalPercentage >= 80 -> CapacityLevel.WARNING
                        else -> CapacityLevel.NORMAL
                    }
                    val totalCardColor = when (totalCapacityLevel) {
                        CapacityLevel.CRITICAL -> MaterialTheme.colorScheme.errorContainer
                        CapacityLevel.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
                        CapacityLevel.NORMAL -> MaterialTheme.colorScheme.primaryContainer
                    }
                    val totalContentColor = when (totalCapacityLevel) {
                        CapacityLevel.CRITICAL -> MaterialTheme.colorScheme.onErrorContainer
                        CapacityLevel.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer
                        CapacityLevel.NORMAL -> MaterialTheme.colorScheme.onPrimaryContainer
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = totalCardColor)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "Total Attendance",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = totalContentColor.copy(alpha = 0.8f)
                                        )
                                        if (totalCapacityLevel != CapacityLevel.NORMAL) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Icon(
                                                imageVector = if (totalCapacityLevel == CapacityLevel.CRITICAL)
                                                    Icons.Default.Warning else Icons.Default.Info,
                                                contentDescription = "Capacity alert",
                                                modifier = Modifier.size(16.dp),
                                                tint = totalContentColor
                                            )
                                        }
                                    }
                                    if (uiState.totalCapacity > 0) {
                                        Text(
                                            text = "$totalPercentage% of ${uiState.totalCapacity}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = totalContentColor.copy(alpha = 0.7f)
                                        )
                                    }
                                }

                                AnimatedContent(
                                    targetState = uiState.totalAttendance,
                                    transitionSpec = {
                                        if (targetState > initialState) {
                                            flipUpTransform()
                                        } else {
                                            flipDownTransform()
                                        }
                                    },
                                    label = "totalAttendanceAnimation"
                                ) { attendance ->
                                    Text(
                                        text = attendance.toString(),
                                        style = MaterialTheme.typography.displayMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = totalContentColor
                                    )
                                }
                            }

                            // Total capacity progress bar
                            if (uiState.totalCapacity > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = {
                                        (uiState.totalAttendance.toFloat() / uiState.totalCapacity).coerceIn(0f, 1f)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp),
                                    color = totalContentColor.copy(alpha = 0.8f),
                                    trackColor = totalContentColor.copy(alpha = 0.2f),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Area counting cards - scrollable
                    if (uiState.areaCounts.isEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                "Loading areas...",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.areaCounts) { areaCount ->
                                AreaCountCard(
                                    areaCount = areaCount,
                                    isLocked = uiState.isLocked,
                                    onIncrement = { amount ->
                                        haptic.counter()
                                        viewModel.incrementCount(areaCount.id, amount)
                                    },
                                    onDecrement = { amount ->
                                        haptic.counter()
                                        viewModel.decrementCount(areaCount.id, amount)
                                    },
                                    onSetCount = { newCount ->
                                        viewModel.setCount(areaCount.id, newCount)
                                    },
                                    onToggleInclusion = {
                                        haptic.selection()
                                        viewModel.toggleAreaInclusion(areaCount.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Creating service...")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateServiceDialog(
    eventTypes: List<EventTypeEntity>,
    onDismiss: () -> Unit,
    onCreate: (String, String, Long, String) -> Unit
) {
    val haptic = rememberHapticFeedback()
    var selectedServiceType by remember { mutableStateOf<EventTypeEntity?>(eventTypes.firstOrNull()) }
    var countedBy by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start New Service") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (eventTypes.isEmpty()) {
                    Text(
                        "No service types configured. Please set up service types first.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedServiceType?.let {
                                "${it.name} - ${it.dayType} ${it.time}"
                            } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Service Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            eventTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(type.name, style = MaterialTheme.typography.bodyMedium)
                                            Text(
                                                "${type.dayType} • ${type.time}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        haptic.selection()
                                        selectedServiceType = type
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = countedBy,
                        onValueChange = { countedBy = it },
                        label = { Text("Counted By") },
                        placeholder = { Text("Your name") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    haptic.success()
                    selectedServiceType?.let { type ->
                        onCreate(type.id, type.name, System.currentTimeMillis(), countedBy)
                    }
                },
                enabled = countedBy.isNotBlank() && selectedServiceType != null && eventTypes.isNotEmpty()
            ) {
                Text("Start Counting")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                haptic.light()
                onDismiss()
            }) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AreaCountCard(
    areaCount: AreaCountState,
    isLocked: Boolean,
    onIncrement: (amount: Int) -> Unit,
    onDecrement: (amount: Int) -> Unit,
    onSetCount: (Int) -> Unit,
    onToggleInclusion: () -> Unit
) {
    val haptic = rememberHapticFeedback()
    var previousCount by remember { mutableStateOf(areaCount.count) }
    val contentAlpha = if (areaCount.isIncluded) 1f else 0.4f
    val capacityPercentage = if (areaCount.capacity > 0) {
        (areaCount.count.toFloat() / areaCount.capacity * 100).toInt()
    } else 0

    // Capacity alert state
    val capacityLevel = when {
        capacityPercentage >= 95 -> CapacityLevel.CRITICAL
        capacityPercentage >= 80 -> CapacityLevel.WARNING
        else -> CapacityLevel.NORMAL
    }

    // Haptic alert when crossing thresholds
    LaunchedEffect(capacityLevel) {
        when (capacityLevel) {
            CapacityLevel.CRITICAL -> haptic.strong()
            CapacityLevel.WARNING -> haptic.medium()
            CapacityLevel.NORMAL -> {}
        }
    }

    LaunchedEffect(areaCount.count) {
        previousCount = areaCount.count
    }

    // Resolve area template color
    val areaColor = remember(areaCount.template.color) {
        try {
            Color(android.graphics.Color.parseColor(areaCount.template.color))
        } catch (_: Exception) {
            null
        }
    }

    // Resolve area template icon
    val areaIcon = remember(areaCount.template.icon) {
        mapIconNameToVector(areaCount.template.icon)
    }

    // Card border color for capacity alerts
    val alertBorderColor = when (capacityLevel) {
        CapacityLevel.CRITICAL -> MaterialTheme.colorScheme.error
        CapacityLevel.WARNING -> MaterialTheme.colorScheme.tertiary
        CapacityLevel.NORMAL -> Color.Transparent
    }

    // Pulse animation for critical capacity
    val infiniteTransition = rememberInfiniteTransition(label = "capacityPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val cardBorderModifier = if (capacityLevel != CapacityLevel.NORMAL) {
        Modifier.border(
            width = 2.dp,
            color = alertBorderColor.copy(
                alpha = if (capacityLevel == CapacityLevel.CRITICAL) pulseAlpha else 0.7f
            ),
            shape = MaterialTheme.shapes.medium
        )
    } else {
        Modifier
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(cardBorderModifier),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isLocked -> MaterialTheme.colorScheme.surfaceVariant
                capacityLevel == CapacityLevel.CRITICAL ->
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Area name, icon, color indicator, and count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = areaCount.isIncluded,
                    onCheckedChange = { onToggleInclusion() },
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))

                // Area color dot + icon
                if (areaColor != null) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(areaColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = areaIcon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = areaColor
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = areaCount.template.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                    )
                    if (areaCount.capacity > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$capacityPercentage% · ${areaCount.capacity} capacity",
                                style = MaterialTheme.typography.bodyMedium,
                                color = when (capacityLevel) {
                                    CapacityLevel.CRITICAL -> MaterialTheme.colorScheme.error
                                    CapacityLevel.WARNING -> MaterialTheme.colorScheme.tertiary
                                    CapacityLevel.NORMAL -> MaterialTheme.colorScheme.onSurfaceVariant
                                }.copy(alpha = contentAlpha)
                            )
                            // Capacity alert badge
                            if (capacityLevel != CapacityLevel.NORMAL) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = when (capacityLevel) {
                                        CapacityLevel.CRITICAL -> MaterialTheme.colorScheme.error
                                        CapacityLevel.WARNING -> MaterialTheme.colorScheme.tertiary
                                        CapacityLevel.NORMAL -> Color.Transparent
                                    }
                                ) {
                                    Text(
                                        text = if (capacityLevel == CapacityLevel.CRITICAL) "FULL" else "HIGH",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = when (capacityLevel) {
                                            CapacityLevel.CRITICAL -> MaterialTheme.colorScheme.onError
                                            CapacityLevel.WARNING -> MaterialTheme.colorScheme.onTertiary
                                            CapacityLevel.NORMAL -> Color.Transparent
                                        },
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Count display with flip animation
                AnimatedContent(
                    targetState = areaCount.count,
                    transitionSpec = {
                        if (targetState > initialState) {
                            flipUpTransform()
                        } else {
                            flipDownTransform()
                        }
                    },
                    label = "countAnimation"
                ) { count ->
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 56.sp),
                        fontWeight = FontWeight.Bold,
                        color = when (capacityLevel) {
                            CapacityLevel.CRITICAL -> MaterialTheme.colorScheme.error
                            CapacityLevel.WARNING -> MaterialTheme.colorScheme.tertiary
                            CapacityLevel.NORMAL -> MaterialTheme.colorScheme.primary
                        }.copy(alpha = contentAlpha)
                    )
                }
            }

            // Progress bar - inline
            if (areaCount.capacity > 0) {
                val progress = (areaCount.count.toFloat() / areaCount.capacity).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = when (capacityLevel) {
                        CapacityLevel.CRITICAL -> MaterialTheme.colorScheme.error
                        CapacityLevel.WARNING -> MaterialTheme.colorScheme.tertiary
                        CapacityLevel.NORMAL -> if (progress < 0.5f)
                            MaterialTheme.colorScheme.tertiary
                        else
                            MaterialTheme.colorScheme.primary
                    },
                )
            }

            // Main ±1 buttons with long-press rapid count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Minus button with long-press
                RepeatableButton(
                    onClick = { onDecrement(1) },
                    enabled = !isLocked && areaCount.count > 0,
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrease by 1",
                        modifier = Modifier.size(40.dp)
                    )
                }

                // Plus button with long-press
                RepeatableButton(
                    onClick = { onIncrement(1) },
                    enabled = !isLocked,
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increase by 1",
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            // Lock indicator
            if (isLocked) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Lock,
                        "Locked",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Service locked",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

enum class CapacityLevel { NORMAL, WARNING, CRITICAL }

@Composable
fun RepeatableButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
    content: @Composable RowScope.() -> Unit
) {
    val haptic = rememberHapticFeedback()
    var isPressed by remember { mutableStateOf(false) }

    // Long-press rapid fire
    LaunchedEffect(isPressed, enabled) {
        if (isPressed && enabled) {
            delay(400) // Initial delay before rapid fire
            var interval = 200L
            while (isPressed) {
                onClick()
                haptic.counter()
                delay(interval)
                // Accelerate: reduce interval down to 50ms
                if (interval > 50L) interval = (interval * 0.85).toLong().coerceAtLeast(50L)
            }
        }
    }

    FilledTonalButton(
        onClick = {
            haptic.counter()
            onClick()
        },
        modifier = modifier.pointerInput(enabled) {
            if (enabled) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            }
        },
        enabled = enabled,
        colors = colors,
        content = content
    )
}

@Composable
fun BulkButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    isSubtract: Boolean,
    modifier: Modifier = Modifier
) {
    val haptic = rememberHapticFeedback()
    OutlinedButton(
        onClick = {
            haptic.medium()
            onClick()
        },
        modifier = modifier.height(44.dp),
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = if (isSubtract)
                MaterialTheme.colorScheme.error
            else
                MaterialTheme.colorScheme.primary
        ),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

fun mapIconNameToVector(iconName: String): ImageVector {
    return when (iconName.lowercase()) {
        "chair" -> Icons.Default.Chair
        "child_care", "baby" -> Icons.Default.ChildCare
        "balcony" -> Icons.Default.Balcony
        "overflow", "groups" -> Icons.Default.Groups
        "local_parking", "parking" -> Icons.Default.LocalParking
        "meeting_room", "room" -> Icons.Default.MeetingRoom
        "stairs" -> Icons.Default.Stairs
        "weekend", "vip" -> Icons.Default.Weekend
        "church" -> Icons.Default.Church
        "event_seat", "seat" -> Icons.Default.EventSeat
        "people" -> Icons.Default.People
        "accessibility" -> Icons.Default.AccessibleForward
        else -> Icons.Default.Place
    }
}
