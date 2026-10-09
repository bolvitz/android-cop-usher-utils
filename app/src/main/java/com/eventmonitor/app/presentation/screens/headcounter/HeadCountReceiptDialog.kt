package com.eventmonitor.app.presentation.screens.headcounter

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.eventmonitor.shared.presentation.headcounter.HeadCountReport

/** Receipt-style head count summary for one event, with a share action for the plain-text copy. */
@Composable
fun HeadCountReceiptDialog(report: HeadCountReport, onDismiss: () -> Unit) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.85f).fillMaxHeight(0.82f),
            shape = MaterialTheme.shapes.large
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Summary", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = {
                        val send = Intent(Intent.ACTION_SEND)
                            .setType("text/plain")
                            .putExtra(Intent.EXTRA_SUBJECT, "Head count · ${report.venueName}")
                            .putExtra(Intent.EXTRA_TEXT, report.text)
                        context.startActivity(Intent.createChooser(send, "Share report"))
                    }) { Icon(Icons.Filled.Share, contentDescription = "Share report") }
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Close") }
                }
                HorizontalDivider()
                Surface(Modifier.fillMaxWidth().weight(1f), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Receipt(report, Modifier.verticalScroll(rememberScrollState()).padding(16.dp))
                }
            }
        }
    }
}

@Composable
private fun Receipt(report: HeadCountReport, modifier: Modifier = Modifier) {
    val ink = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary

    Column(modifier.fillMaxWidth()) {
        Mono("HEAD COUNT REPORT", ink, MaterialTheme.typography.titleLarge, FontWeight.Bold, TextAlign.Center)
        Mono(report.venueName.uppercase(), ink, MaterialTheme.typography.titleMedium, FontWeight.Bold, TextAlign.Center)
        if (report.venueLocation.isNotEmpty()) {
            Mono(report.venueLocation, muted, align = TextAlign.Center)
        }
        Rule(muted)

        Heading("AREA BREAKDOWN", ink)
        report.lines.forEach { line ->
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Mono(
                    if (line.isIncluded) line.name else "${line.name} (excluded)",
                    muted,
                    MaterialTheme.typography.bodySmall.copy(fontSize = 18.sp),
                    modifier = Modifier.weight(1f)
                )
                Mono(
                    "${line.count}",
                    if (line.isIncluded) accent else muted,
                    MaterialTheme.typography.displayLarge.copy(fontSize = 24.sp, letterSpacing = 3.sp),
                    FontWeight.Black
                )
            }
            if (line.notes.isNotEmpty()) Field("Note", line.notes, ink, muted)
            HorizontalDivider(Modifier.padding(vertical = 1.dp), color = muted.copy(alpha = 0.2f))
        }
        Rule(muted)

        Heading("TOTAL", ink)
        Field("Total Count", "${report.totalCount}", ink, muted)
        Field("Total Capacity", "${report.totalCapacity}", ink, muted)
        Field("Utilization", "${report.utilizationPercent}%", ink, muted)
        Rule(muted)

        if (report.eventNotes.isNotEmpty()) {
            Heading("EVENT NOTES", ink)
            Mono(report.eventNotes, ink)
            Spacer(Modifier.height(8.dp))
        }
        if (report.weather.isNotEmpty()) {
            Meta("Weather", report.weather, muted)
            Spacer(Modifier.height(8.dp))
        }
        Field("Event", report.eventName, ink, muted)
        Field("Date", report.dateText, ink, muted)
        Field("Time", report.timeText, ink, muted)
        Field("Counted By", report.countedBy, ink, muted)
        Spacer(Modifier.height(8.dp))
        Meta("Generated", report.generatedText, muted)
        Meta("ID", report.reference, muted)
    }
}

@Composable
private fun Mono(
    text: String,
    color: Color,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    weight: FontWeight? = null,
    align: TextAlign? = null,
    modifier: Modifier = Modifier.fillMaxWidth()
) = Text(text, modifier, color = color, style = style, fontWeight = weight, fontFamily = FontFamily.Monospace, textAlign = align)

@Composable
private fun Heading(text: String, color: Color) {
    Spacer(Modifier.height(8.dp))
    Mono(text, color, MaterialTheme.typography.displaySmall.copy(fontSize = 18.sp, letterSpacing = 3.sp), FontWeight.Bold)
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun Field(label: String, value: String, ink: Color, muted: Color) {
    Row(Modifier.fillMaxWidth()) {
        Mono("$label:", muted, modifier = Modifier.width(130.dp))
        Mono(value, ink, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun Meta(label: String, value: String, muted: Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Mono("$label:", muted, modifier = Modifier)
        Spacer(Modifier.width(4.dp))
        Mono(value, muted.copy(alpha = 0.7f), MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun Rule(color: Color) {
    Spacer(Modifier.height(8.dp))
    HorizontalDivider(color = color.copy(alpha = 0.3f))
    Spacer(Modifier.height(4.dp))
}
