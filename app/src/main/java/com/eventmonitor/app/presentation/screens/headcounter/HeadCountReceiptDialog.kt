package com.eventmonitor.app.presentation.screens.headcounter

import android.content.Intent
import android.os.Build
import android.view.WindowManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.eventmonitor.core.common.theme.HeadCounterTheme
import com.eventmonitor.core.common.theme.ReceiptAccent
import com.eventmonitor.core.common.theme.ReceiptInk
import com.eventmonitor.core.common.theme.ReceiptPaper
import com.eventmonitor.shared.presentation.headcounter.HeadCountReport
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val FEED_STEPS = 34

/**
 * Receipt-style summary of one head count. It feeds out of a printer slot line by line, and
 * can be shared as plain text. Pass [onNotesChange] to make the event notes editable.
 * Matches the iOS HeadCountReceiptView.
 */
@Composable
fun HeadCountReceiptDialog(
    report: HeadCountReport,
    onDismiss: () -> Unit,
    notes: String? = null,
    onNotesChange: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val reduceMotion = rememberReduceMotion()
    val savedNotes = notes ?: report.eventNotes
    var noteText by rememberSaveable(report.eventId) { mutableStateOf(savedNotes) }
    var shown by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }

    // Dismissing the window also closes the keyboard, so close() needs no focus handling.
    fun close() {
        if (closing) return
        closing = true
        if (onNotesChange != null && noteText != savedNotes) onNotesChange(noteText)
        shown = false
        scope.launch { delay(180); onDismiss() }
    }

    // Saves once typing pauses rather than on every keystroke.
    LaunchedEffect(noteText) {
        if (onNotesChange == null || noteText == savedNotes) return@LaunchedEffect
        delay(450)
        onNotesChange(noteText)
    }

    LaunchedEffect(Unit) { shown = true }

    // Called by History in the app's own theme, so put the bar icons back when the receipt closes.
    RestoreSystemBarsOnExit()
    HeadCounterTheme(darkTheme = true) {
        Dialog(
            onDismissRequest = ::close,
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            // The scrim below animates with the receipt; the window's own dim would double it.
            // It also draws under the status bar so the scrim covers the whole screen.
            val dialogView = LocalView.current
            val focus = LocalFocusManager.current
            val window = (dialogView.parent as? DialogWindowProvider)?.window
            DisposableEffect(window) {
                window ?: return@DisposableEffect onDispose { }
                window.setDimAmount(0f)
                WindowCompat.getInsetsController(window, dialogView).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = false
                }
                window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
                if (Build.VERSION.SDK_INT >= 28) {
                    window.attributes = window.attributes.apply {
                        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
                }
                onDispose { }
            }
            val scrim by animateFloatAsState(if (shown) 0.72f else 0f, tween(if (shown) 220 else 180), label = "scrim")
            val chrome by animateFloatAsState(if (shown) 1f else 0f, tween(if (shown) 220 else 180), label = "chrome")

            Surface(color = Color.Transparent, modifier = Modifier.fillMaxSize()) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .drawBehind { drawRect(Color.Black.copy(alpha = scrim)) }
                        .clickable(remember { MutableInteractionSource() }, indication = null) { focus.clearFocus() }
                        .systemBarsPadding()
                        .imePadding(),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 8.dp).graphicsLayer { alpha = chrome },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CounterPill(onClick = {
                            val text = if (onNotesChange != null && noteText != report.eventNotes) {
                                report.text + "\n\nEvent notes: $noteText"
                            } else report.text
                            val send = Intent(Intent.ACTION_SEND)
                                .setType("text/plain")
                                .putExtra(Intent.EXTRA_SUBJECT, "Head count, ${report.venueName}")
                                .putExtra(Intent.EXTRA_TEXT, text)
                            context.startActivity(Intent.createChooser(send, "Share report"))
                        }) {
                            Icon(Icons.Filled.Share, contentDescription = null)
                            Text("Share", fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.weight(1f))
                        CounterPill(onClick = { focus.clearFocus(); close() }) { Text("Done", fontWeight = FontWeight.SemiBold) }
                    }

                    Printer(
                        reduceMotion = reduceMotion,
                        modifier = Modifier.weight(1f).padding(top = 16.dp),
                    ) {
                        Paper(report, noteText.takeIf { onNotesChange != null }, { noteText = it })
                    }
                }
            }
        }
    }
}

// MARK: Printer

@Composable
private fun Printer(reduceMotion: Boolean, modifier: Modifier, paper: @Composable () -> Unit) {
    val haptics = rememberCounterHaptics()
    var fed by remember { mutableIntStateOf(if (reduceMotion) FEED_STEPS else 0) }
    val printing = fed < FEED_STEPS
    val fedSmooth by animateFloatAsState(fed.toFloat(), tween(50), label = "feed")
    var paperHeight by remember { mutableIntStateOf(0) }
    val scroll = rememberScrollState()

    LaunchedEffect(Unit) {
        if (reduceMotion) return@LaunchedEffect
        delay(180)
        for (step in 1..FEED_STEPS) {
            fed = step
            haptics.soft()
            delay(68)
        }
    }

    Column(modifier.fillMaxWidth()) {
        // The slot the paper feeds out of, with a status light that blinks while printing.
        Box(
            Modifier
                .padding(horizontal = 22.dp)
                .fillMaxWidth()
                .height(12.dp)
                .background(CounterPalette.plateHigh, CircleShape)
                .border(2.dp, Color.Black.copy(alpha = 0.4f), CircleShape)
                .zIndex(1f)
                .clearAndSetSemantics { },
            contentAlignment = Alignment.CenterEnd,
        ) {
            Box(
                Modifier
                    .padding(end = 10.dp)
                    .size(6.dp)
                    .background(if (printing && fed % 4 < 2) CounterPalette.brass else CounterPalette.plate, CircleShape),
            )
        }

        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 34.dp)
                .padding(bottom = 16.dp)
                .offset(y = (-6).dp)
                .clipToBounds(),
        ) {
            val viewport = constraints.maxHeight
            Column(
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        val visible = minOf(paperHeight, viewport).toFloat()
                        translationY = -visible * (1 - fedSmooth / FEED_STEPS)
                        translationX = if (printing && fed % 2 == 1) 0.6.dp.toPx() else 0f
                        alpha = if (paperHeight > 0) 1f else 0f
                    }
                    .verticalScroll(scroll, enabled = !printing),
            ) {
                Column(Modifier.fillMaxWidth().onSizeChanged { paperHeight = it.height }) {
                    paper()
                    Box(Modifier.fillMaxWidth().height(10.dp).background(ReceiptPaper, TornEdge))
                }
            }
        }
    }
}

/** Zigzag bottom edge of torn receipt paper. */
private object TornEdge : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val tooth = with(density) { 14.dp.toPx() }
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            var x = size.width
            var down = true
            while (x > 0f) {
                x -= tooth / 2
                lineTo(maxOf(x, 0f), if (down) size.height else 0f)
                down = !down
            }
            close()
        }
        return Outline.Generic(path)
    }
}

// MARK: Paper

@Composable
private fun Paper(report: HeadCountReport, editableNotes: String?, onNotes: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(ReceiptPaper)
            .padding(horizontal = 18.dp)
            .padding(top = 18.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Mono("Head count report", 16.sp, FontWeight.Bold, align = TextAlign.Center)
            Mono(report.venueName, 14.sp, FontWeight.Bold, align = TextAlign.Center)
            if (report.venueLocation.isNotEmpty()) {
                Mono(report.venueLocation, 12.sp, align = TextAlign.Center, modifier = Modifier.alpha(0.7f))
            }
        }
        Rule()

        Heading("Area breakdown")
        report.lines.forEach { line ->
            Row(
                Modifier.fillMaxWidth().alpha(if (line.isIncluded) 1f else 0.55f).semantics(mergeDescendants = true) { },
                verticalAlignment = Alignment.Bottom,
            ) {
                Mono(if (line.isIncluded) line.name else "${line.name} (excluded)", 13.sp, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(12.dp))
                Mono("${line.count}", 17.sp, FontWeight.Bold, color = if (line.isIncluded) ReceiptAccent else ReceiptInk)
            }
            if (line.notes.isNotEmpty()) Field("Note", line.notes)
        }
        Rule()

        Field("Total count", "${report.totalCount}", bold = true)
        Field("Total capacity", "${report.totalCapacity}")
        Field("Utilization", "${report.utilizationPercent}%")
        Rule()

        if (editableNotes != null) {
            Heading("Event notes")
            BasicTextField(
                value = editableNotes,
                onValueChange = onNotes,
                textStyle = monoStyle(13.sp),
                cursorBrush = SolidColor(ReceiptInk),
                minLines = 2,
                maxLines = 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Event notes" }
                    .dashedBorder()
                    .padding(8.dp),
                decorationBox = { inner ->
                    Box {
                        if (editableNotes.isEmpty()) {
                            Mono("Tap to add notes for this count", 13.sp, modifier = Modifier.alpha(0.5f))
                        }
                        inner()
                    }
                },
            )
        } else if (report.eventNotes.isNotEmpty()) {
            Heading("Event notes")
            Mono(report.eventNotes, 13.sp)
        }
        if (report.weather.isNotEmpty()) {
            Mono("Weather: ${report.weather}", 12.sp, modifier = Modifier.alpha(0.7f))
        }
        Rule()

        Field("Event", report.eventName)
        Field("Date", report.dateText)
        Field("Time", report.timeText)
        Field("Counted by", report.countedBy)
        Mono("Generated ${report.generatedText}", 11.sp, modifier = Modifier.padding(top = 8.dp).alpha(0.6f))
        Mono("ID ${report.reference}", 11.sp, modifier = Modifier.alpha(0.6f))
    }
}

private fun monoStyle(size: TextUnit, weight: FontWeight = FontWeight.Normal, color: Color = ReceiptInk) =
    TextStyle(fontFamily = FontFamily.Monospace, fontSize = size, fontWeight = weight, color = color)

@Composable
private fun Mono(
    text: String,
    size: TextUnit,
    weight: FontWeight = FontWeight.Normal,
    color: Color = ReceiptInk,
    align: TextAlign? = null,
    modifier: Modifier = Modifier,
) = Text(text, modifier, style = monoStyle(size, weight, color).copy(textAlign = align ?: TextAlign.Unspecified))

@Composable
private fun Heading(text: String) {
    Mono(text, 13.sp, FontWeight.Bold, modifier = Modifier.padding(top = 4.dp, bottom = 2.dp).semantics { heading() })
}

@Composable
private fun Field(label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Mono(label, 13.sp, modifier = Modifier.alpha(0.75f))
        Spacer(Modifier.width(12.dp))
        Mono(value, 13.sp, if (bold) FontWeight.Bold else FontWeight.Normal, align = TextAlign.End, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun Rule() {
    Canvas(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .height(1.dp)
            .clearAndSetSemantics { },
    ) {
        drawLine(
            ReceiptInk.copy(alpha = 0.35f),
            Offset(0f, size.height / 2),
            Offset(size.width, size.height / 2),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
        )
    }
}

private fun Modifier.dashedBorder() = drawBehind {
    val stroke = 1.dp.toPx()
    drawRoundRect(
        color = ReceiptInk.copy(alpha = 0.4f),
        topLeft = Offset(stroke / 2, stroke / 2),
        size = Size(size.width - stroke, size.height - stroke),
        cornerRadius = CornerRadius(4.dp.toPx()),
        style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
    )
}
