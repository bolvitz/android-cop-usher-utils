package com.eventmonitor.feature.headcounter.seatmap.models

import androidx.compose.ui.graphics.Color

enum class SeatStatus(val displayName: String, val color: Color) {
    AVAILABLE("Available", Color(0xFF4CAF50)),      // Green
    OCCUPIED("Occupied", Color(0xFFF44336)),        // Red
    RESERVED("Reserved", Color(0xFFFF9800)),        // Orange
    BLOCKED("Blocked", Color(0xFF9E9E9E)),          // Gray
    WHEELCHAIR("Wheelchair", Color(0xFF2196F3))     // Blue
}
