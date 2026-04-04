package com.eventmonitor.feature.headcounter.seatmap.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eventmonitor.feature.headcounter.seatmap.models.Seat

@Composable
fun SeatMapView(
    seats: List<Seat>,
    onSeatClick: (Seat) -> Unit,
    modifier: Modifier = Modifier
) {
    val seatsByRow = seats.groupBy { it.row }.toSortedMap()

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Screen indicator at top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                )
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "SCREEN",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        // Seat grid
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(seatsByRow.entries.toList()) { (rowLabel, rowSeats) ->
                SeatRow(
                    rowLabel = rowLabel,
                    seats = rowSeats.sortedBy { it.number },
                    onSeatClick = onSeatClick
                )
            }
        }
    }
}

@Composable
private fun SeatRow(
    rowLabel: String,
    seats: List<Seat>,
    onSeatClick: (Seat) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Row label
        Text(
            text = rowLabel,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(24.dp),
            textAlign = TextAlign.Center
        )

        // Seats in row
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            seats.forEach { seat ->
                SeatItem(
                    seat = seat,
                    onClick = onSeatClick
                )
            }
        }
    }
}
