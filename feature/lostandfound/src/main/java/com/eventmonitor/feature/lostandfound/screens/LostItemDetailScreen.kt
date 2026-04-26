package com.eventmonitor.feature.lostandfound.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.eventmonitor.core.common.ui.ArcadeBackground
import com.eventmonitor.core.common.ui.BrandBlue
import com.eventmonitor.core.common.ui.BrandRed
import com.eventmonitor.core.common.ui.KeyTone
import com.eventmonitor.core.common.ui.SoftAlertDialog
import com.eventmonitor.core.common.ui.SoftButtonTone
import com.eventmonitor.core.common.ui.SoftAppBar
import com.eventmonitor.core.common.ui.SoftBottomDock
import com.eventmonitor.core.common.ui.SoftCard
import com.eventmonitor.core.common.ui.SoftIconButton
import com.eventmonitor.core.common.ui.SoftKey
import com.eventmonitor.core.common.ui.SoftPrimaryButton
import com.eventmonitor.core.common.ui.SoftSection
import com.eventmonitor.core.common.ui.SoftToolButton
import com.eventmonitor.core.common.ui.softHeadline
import com.eventmonitor.core.common.ui.softLabelStyle
import com.eventmonitor.core.domain.models.ItemStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LostItemDetailScreen(
    itemId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String, String) -> Unit,
    viewModel: LostItemDetailViewModel = hiltViewModel()
) {
    val item by viewModel.item.collectAsState(initial = null)
    var showClaimDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SoftAppBar(
                title = "Lost Item",
                subtitle = "§ Case File",
                onBack = onNavigateBack,
                trailing = {
                    item?.let { itm ->
                        SoftIconButton(
                            glyph = "✎",
                            onClick = { onNavigateToEdit(itm.locationId, itm.id) },
                        )
                        Spacer(Modifier.width(4.dp))
                        SoftIconButton(
                            glyph = "✕",
                            onClick = { showDeleteDialog = true },
                        )
                    }
                },
            )
        },
        bottomBar = {
            val itm = item
            if (itm != null && itm.status == ItemStatus.PENDING.name) {
                val canDonate = (System.currentTimeMillis() - itm.foundDate) >=
                        6L * 30L * 24L * 60L * 60L * 1000L
                SoftBottomDock {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SoftToolButton(
                            label = "Donate",
                            glyph = "♥",
                            enabled = canDonate,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.updateItemStatus(ItemStatus.DONATED.name) },
                        )
                        SoftPrimaryButton(
                            label = "Claim",
                            onClick = { showClaimDialog = true },
                            modifier = Modifier.weight(2f),
                            trailingGlyph = "→",
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            ArcadeBackground(modifier = Modifier.matchParentSize())

            item?.let { itm ->
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val status = ItemStatus.fromString(itm.status)

                val currentTime = System.currentTimeMillis()
                val sixMonthsInMillis = 6L * 30L * 24L * 60L * 60L * 1000L
                val timeSinceFound = currentTime - itm.foundDate
                val canDonate = timeSinceFound >= sixMonthsInMillis
                val totalDays = 180
                val daysElapsed = (timeSinceFound / (24L * 60L * 60L * 1000L)).toInt()
                val daysRemaining = (totalDays - daysElapsed).coerceAtLeast(0)
                val progress = (daysElapsed.toFloat() / totalDays).coerceIn(0f, 1f)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Photo
                    if (itm.photoUri.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .clip(RoundedCornerShape(16.dp)),
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(itm.photoUri)
                                    .size(Size(1024, 1024))
                                    .build(),
                                contentDescription = "Item photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }

                    // Headline + status pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            text = itm.description,
                            style = softHeadline(24),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(12.dp))
                        StatusPill(label = status.displayName, hex = status.color)
                    }

                    // Found
                    DetailSoftCard(eyebrow = "01 · FOUND", title = "Recovered") {
                        Text(
                            text = "${dateFormat.format(Date(itm.foundDate))} · " +
                                    timeFormat.format(Date(itm.foundDate)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Zone: ${itm.foundZone}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // Category
                    DetailSoftCard(eyebrow = "02 · CATEGORY", title = "Kind") {
                        Text(
                            text = com.eventmonitor.core.domain.models.ItemCategory
                                .fromString(itm.category).displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }

                    if (itm.color.isNotBlank() || itm.brand.isNotBlank() ||
                        itm.identifyingMarks.isNotBlank()
                    ) {
                        DetailSoftCard(eyebrow = "03 · DETAILS", title = "Marks") {
                            if (itm.color.isNotBlank()) {
                                LabelValueRow("Color", itm.color)
                            }
                            if (itm.brand.isNotBlank()) {
                                LabelValueRow("Brand", itm.brand)
                            }
                            if (itm.identifyingMarks.isNotBlank()) {
                                LabelValueRow("Identifying", itm.identifyingMarks)
                            }
                        }
                    }

                    if (itm.reportedBy.isNotBlank() || itm.notes.isNotBlank()) {
                        DetailSoftCard(eyebrow = "04 · CREDITS", title = "Filed by") {
                            if (itm.reportedBy.isNotBlank()) {
                                LabelValueRow("Reported by", itm.reportedBy)
                            }
                            if (itm.notes.isNotBlank()) {
                                LabelValueRow("Notes", itm.notes)
                            }
                        }
                    }

                    if (itm.claimedDate > 0) {
                        DetailSoftCard(eyebrow = "05 · CLAIMED", title = "Returned") {
                            LabelValueRow("Date", dateFormat.format(Date(itm.claimedDate)))
                            if (itm.claimedBy.isNotBlank()) {
                                LabelValueRow("By", itm.claimedBy)
                            }
                            if (itm.claimerContact.isNotBlank()) {
                                LabelValueRow("Contact", itm.claimerContact)
                            }
                        }
                    }

                    if (itm.status == ItemStatus.PENDING.name) {
                        SoftCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val pulseTransition = rememberInfiniteTransition(label = "donate")
                                val pulse by pulseTransition.animateFloat(
                                    initialValue = 0.4f,
                                    targetValue = 1f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(900, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse,
                                    ),
                                    label = "donatePulse",
                                )
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (canDonate) {
                                                BrandBlue.copy(alpha = pulse)
                                            } else {
                                                BrandRed.copy(alpha = 0.6f)
                                            },
                                        ),
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = if (canDonate) "Ready for donation" else "Holding period",
                                    style = softHeadline(18),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = "$daysElapsed/$totalDays",
                                    style = softLabelStyle(),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            // Slim progress bar — matches Counting screen aesthetic.
                            val animatedPct by animateFloatAsState(
                                targetValue = progress,
                                animationSpec = tween(520, easing = LinearOutSlowInEasing),
                                label = "donatePct",
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.10f),
                                    ),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(animatedPct)
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (canDonate) BrandBlue else BrandRed),
                                )
                            }
                            if (!canDonate) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "$daysRemaining days remaining until donation eligibility.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            } ?: Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onBackground,
                    strokeWidth = 2.dp,
                )
            }
        }
    }

    if (showClaimDialog) {
        var claimerName by remember { mutableStateOf("") }
        var contact by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }

        SoftAlertDialog(
            onDismiss = { showClaimDialog = false },
            eyebrow = "Claim · Item",
            title = "Claim this item",
            confirmLabel = "Claim",
            confirmEnabled = claimerName.isNotBlank(),
            onConfirm = {
                viewModel.claimItem(claimerName, contact, notes)
                showClaimDialog = false
            },
            body = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = claimerName,
                        onValueChange = { claimerName = it },
                        label = { Text("Claimer name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = contact,
                        onValueChange = { contact = it },
                        label = { Text("Contact (phone/email)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Verification notes") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
        )
    }

    if (showDeleteDialog) {
        SoftAlertDialog(
            onDismiss = { showDeleteDialog = false },
            eyebrow = "Delete · Item",
            title = "Delete this case file?",
            message = "This case file will be removed. You can't undo this.",
            confirmLabel = "Delete",
            confirmTone = SoftButtonTone.Destructive,
            onConfirm = {
                viewModel.deleteItem()
                showDeleteDialog = false
                onNavigateBack()
            },
        )
    }
}

@Composable
private fun DetailSoftCard(
    eyebrow: String,
    title: String,
    content: @Composable () -> Unit,
) {
    SoftCard(modifier = Modifier.fillMaxWidth()) {
        SoftSection(eyebrow = eyebrow, title = title)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun LabelValueRow(label: String, value: String) {
    Column(modifier = Modifier.padding(top = 4.dp)) {
        Text(
            text = label.uppercase(),
            style = softLabelStyle(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun StatusPill(label: String, hex: String) {
    val tint = runCatching { Color(android.graphics.Color.parseColor(hex)) }
        .getOrDefault(BrandBlue)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(tint.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = label.uppercase(),
            style = softLabelStyle(),
            color = tint,
            fontWeight = FontWeight.Bold,
        )
    }
}
