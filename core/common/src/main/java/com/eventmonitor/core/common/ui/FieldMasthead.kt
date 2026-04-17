package com.eventmonitor.core.common.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eventmonitor.core.common.theme.MonoTiny
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Newspaper masthead — volume/issue marque on top, giant serif brand underneath
 * with a live clock on the right, then tagline + hairline.
 *
 * Designed to own the top 28% of the home screen.
 */
@Composable
fun FieldMasthead(
    title: String = "Field",
    tagline: String = "The Operations Console",
    volume: String = "VOL. 01",
    issue: String = "NO. 007",
    modifier: Modifier = Modifier,
) {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(30_000L)
        }
    }
    val dateFmt = remember { SimpleDateFormat("EEE d MMM · yyyy", Locale.getDefault()) }
    val clockFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 4.dp),
    ) {
        // Top meta row — vol/issue, dateline
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$volume · $issue".uppercase(),
                style = MonoTiny,
                color = muted,
            )
            Text(
                text = dateFmt.format(now).uppercase(),
                style = MonoTiny,
                color = muted,
            )
        }
        Spacer(Modifier.height(6.dp))

        // Brand + clock — kerned huge serif next to tabular mono time.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = title.substring(0, 1),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 72.sp,
                        lineHeight = 70.sp
                    ),
                    color = ink,
                )
                if (title.length > 2) {
                    Text(
                        text = title.substring(1, 2),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 72.sp,
                            lineHeight = 70.sp,
                            fontStyle = FontStyle.Italic,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = title.substring(2),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 72.sp,
                            lineHeight = 70.sp
                        ),
                        color = ink,
                    )
                }
            }
            Text(
                text = clockFmt.format(now),
                style = MaterialTheme.typography.headlineMedium,
                color = ink,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        Spacer(Modifier.height(2.dp))

        // Tagline
        Text(
            text = tagline,
            style = MaterialTheme.typography.labelMedium,
            color = muted,
        )
        Spacer(Modifier.height(10.dp))
        // Hairline rule
        Box(
            Modifier
                .fillMaxWidth()
                .height(FieldTokens.Hair)
                .background(ink),
        )
    }
}
