package com.eventmonitor.feature.headcounter.seatmap

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.eventmonitor.core.common.ui.ArcadeBackground
import com.eventmonitor.core.common.ui.BrandBlue
import com.eventmonitor.core.common.ui.SoftAppBar
import com.eventmonitor.core.common.ui.SoftCard
import com.eventmonitor.core.common.ui.SoftIconButton
import com.eventmonitor.core.common.ui.softHeadline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eventmonitor.feature.headcounter.seatmap.components.SeatMapView
import com.eventmonitor.feature.headcounter.seatmap.models.SeatStatus

@Composable
fun SeatMapDemoScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: SeatMapDemoViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            SoftAppBar(
                title = "Seat Map",
                subtitle = "Cinema Prototype",
                onBack = onNavigateBack,
                trailing = {
                    SoftIconButton(glyph = "↺", onClick = { viewModel.resetSeats() })
                },
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) {
            ArcadeBackground(modifier = Modifier.matchParentSize())
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Statistics card
            SeatStatisticsCard(
                totalSeats = state.totalSeats,
                occupiedSeats = state.occupiedSeats,
                availableSeats = state.availableSeats,
                reservedSeats = state.reservedSeats,
                occupancyPercentage = state.occupancyPercentage,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            // Legend
            SeatLegend(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Seat map
            SeatMapView(
                seats = state.seats,
                onSeatClick = { seat ->
                    viewModel.toggleSeat(seat)
                },
                modifier = Modifier.weight(1f)
            )
        }
        } // end Box
    }
}

@Composable
private fun SeatStatisticsCard(
    totalSeats: Int,
    occupiedSeats: Int,
    availableSeats: Int,
    reservedSeats: Int,
    occupancyPercentage: Int,
    modifier: Modifier = Modifier
) {
    SoftCard(modifier = modifier) {
        // Title and total
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Seat Occupancy",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$occupancyPercentage%",
                style = softHeadline(24),
                fontWeight = FontWeight.Bold,
                color = when {
                    occupancyPercentage >= 80 -> Color(0xFFF44336)
                    occupancyPercentage >= 50 -> Color(0xFFFF9800)
                    else -> Color(0xFF4CAF50)
                }
            )
        }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { occupancyPercentage / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(MaterialTheme.shapes.small),
                color = when {
                    occupancyPercentage >= 80 -> Color(0xFFF44336)
                    occupancyPercentage >= 50 -> Color(0xFFFF9800)
                    else -> Color(0xFF4CAF50)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Statistics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Total",
                    value = totalSeats.toString(),
                    color = BrandBlue
                )
                StatItem(
                    label = "Occupied",
                    value = occupiedSeats.toString(),
                    color = Color(0xFFE63946) // BrandRed
                )
                StatItem(
                    label = "Reserved",
                    value = reservedSeats.toString(),
                    color = Color(0xFFB8851A) // Amber
                )
                StatItem(
                    label = "Available",
                    value = availableSeats.toString(),
                    color = Color(0xFF4CAF50)
                )
            }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SeatLegend(
    modifier: Modifier = Modifier
) {
    SoftCard(
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LegendItem(
                label = "Available",
                color = SeatStatus.AVAILABLE.color
            )
            LegendItem(
                label = "Occupied",
                color = SeatStatus.OCCUPIED.color
            )
            LegendItem(
                label = "Reserved",
                color = SeatStatus.RESERVED.color
            )
            LegendItem(
                label = "Blocked",
                color = SeatStatus.BLOCKED.color
            )
        }
    }
}

@Composable
private fun LegendItem(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
