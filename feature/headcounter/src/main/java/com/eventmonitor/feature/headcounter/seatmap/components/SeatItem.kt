package com.eventmonitor.feature.headcounter.seatmap.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Accessible
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eventmonitor.feature.headcounter.seatmap.models.Seat
import com.eventmonitor.feature.headcounter.seatmap.models.SeatStatus
import com.eventmonitor.feature.headcounter.seatmap.models.SeatType

@Composable
fun SeatItem(
    seat: Seat,
    onClick: (Seat) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(8.dp)
    val isClickable = seat.status != SeatStatus.BLOCKED

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(shape)
            .background(seat.status.color.copy(alpha = 0.2f))
            .border(
                width = 2.dp,
                color = if (seat.status == SeatStatus.OCCUPIED) {
                    seat.status.color
                } else {
                    seat.status.color.copy(alpha = 0.5f)
                },
                shape = shape
            )
            .then(
                if (isClickable) {
                    Modifier.clickable { onClick(seat) }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            seat.status == SeatStatus.BLOCKED -> {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = "Blocked seat",
                    tint = seat.status.color,
                    modifier = Modifier.size(24.dp)
                )
            }
            seat.seatType == SeatType.WHEELCHAIR -> {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Accessible,
                    contentDescription = "Wheelchair accessible",
                    tint = seat.status.color,
                    modifier = Modifier.size(24.dp)
                )
            }
            seat.status == SeatStatus.OCCUPIED -> {
                Icon(
                    imageVector = Icons.Default.EventSeat,
                    contentDescription = "Occupied seat",
                    tint = seat.status.color,
                    modifier = Modifier.size(28.dp)
                )
            }
            else -> {
                Text(
                    text = seat.number.toString(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = seat.status.color
                )
            }
        }
    }
}
