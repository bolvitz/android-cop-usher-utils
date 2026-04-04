package com.eventmonitor.feature.headcounter.seatmap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeatMapDemoScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: SeatMapDemoViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cinema Seat Map Prototype") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.resetSeats() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset seats"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
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
                    style = MaterialTheme.typography.headlineMedium,
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
                    color = MaterialTheme.colorScheme.primary
                )
                StatItem(
                    label = "Occupied",
                    value = occupiedSeats.toString(),
                    color = Color(0xFFF44336)
                )
                StatItem(
                    label = "Reserved",
                    value = reservedSeats.toString(),
                    color = Color(0xFFFF9800)
                )
                StatItem(
                    label = "Available",
                    value = availableSeats.toString(),
                    color = Color(0xFF4CAF50)
                )
            }
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
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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
