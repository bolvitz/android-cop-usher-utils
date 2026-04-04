package com.eventmonitor.feature.headcounter.seatmap

import androidx.lifecycle.ViewModel
import com.eventmonitor.feature.headcounter.seatmap.models.Seat
import com.eventmonitor.feature.headcounter.seatmap.models.SeatStatus
import com.eventmonitor.feature.headcounter.seatmap.models.SeatType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

data class SeatMapDemoState(
    val seats: List<Seat> = emptyList(),
    val totalSeats: Int = 0,
    val occupiedSeats: Int = 0,
    val availableSeats: Int = 0,
    val reservedSeats: Int = 0,
    val occupancyPercentage: Int = 0
)

class SeatMapDemoViewModel : ViewModel() {
    private val _state = MutableStateFlow(SeatMapDemoState())
    val state: StateFlow<SeatMapDemoState> = _state.asStateFlow()

    init {
        generateSampleSeats()
    }

    private fun generateSampleSeats() {
        val rows = listOf("A", "B", "C", "D", "E", "F", "G", "H")
        val seatsPerRow = 10
        val seats = mutableListOf<Seat>()

        rows.forEach { row ->
            for (seatNumber in 1..seatsPerRow) {
                val seatType = when {
                    row == "A" && (seatNumber == 1 || seatNumber == 10) -> SeatType.WHEELCHAIR
                    row in listOf("A", "B") -> SeatType.VIP
                    else -> SeatType.STANDARD
                }

                val status = when {
                    // Block some seats in the middle for aisle
                    seatNumber == 5 || seatNumber == 6 -> SeatStatus.BLOCKED
                    // Make some seats occupied
                    row == "A" && seatNumber in listOf(2, 3, 7) -> SeatStatus.OCCUPIED
                    row == "B" && seatNumber in listOf(1, 4, 9) -> SeatStatus.OCCUPIED
                    row == "C" && seatNumber in listOf(3, 8) -> SeatStatus.RESERVED
                    row == "D" && seatNumber in listOf(2, 7, 8) -> SeatStatus.OCCUPIED
                    else -> SeatStatus.AVAILABLE
                }

                seats.add(
                    Seat(
                        id = UUID.randomUUID().toString(),
                        row = row,
                        number = seatNumber,
                        status = status,
                        seatType = seatType
                    )
                )
            }
        }

        updateSeats(seats)
    }

    fun toggleSeat(seat: Seat) {
        if (seat.status == SeatStatus.BLOCKED) return

        val updatedSeats = _state.value.seats.map { currentSeat ->
            if (currentSeat.id == seat.id) {
                currentSeat.copy(
                    status = when (currentSeat.status) {
                        SeatStatus.AVAILABLE -> SeatStatus.OCCUPIED
                        SeatStatus.OCCUPIED -> SeatStatus.RESERVED
                        SeatStatus.RESERVED -> SeatStatus.AVAILABLE
                        else -> currentSeat.status
                    }
                )
            } else {
                currentSeat
            }
        }

        updateSeats(updatedSeats)
    }

    private fun updateSeats(seats: List<Seat>) {
        val occupiedCount = seats.count { it.status == SeatStatus.OCCUPIED }
        val reservedCount = seats.count { it.status == SeatStatus.RESERVED }
        val availableCount = seats.count { it.status == SeatStatus.AVAILABLE }
        val totalCount = seats.count { it.status != SeatStatus.BLOCKED }
        val occupancyPercentage = if (totalCount > 0) {
            ((occupiedCount + reservedCount) * 100) / totalCount
        } else {
            0
        }

        _state.update {
            it.copy(
                seats = seats,
                totalSeats = totalCount,
                occupiedSeats = occupiedCount,
                availableSeats = availableCount,
                reservedSeats = reservedCount,
                occupancyPercentage = occupancyPercentage
            )
        }
    }

    fun resetSeats() {
        generateSampleSeats()
    }
}
