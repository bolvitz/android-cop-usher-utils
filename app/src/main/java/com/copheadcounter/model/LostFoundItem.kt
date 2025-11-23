package com.copheadcounter.model

import java.util.UUID
import java.time.LocalDate
import java.time.LocalDateTime

data class LostFoundItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val category: ItemCategory,
    val dateFound: LocalDate,
    val location: String,
    val status: ItemStatus = ItemStatus.ACTIVE,
    val claimantName: String? = null,
    val claimantContact: String? = null,
    val claimDate: LocalDate? = null,
    val notes: String = "",
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun isOverdue(): Boolean {
        return dateFound.plusMonths(6).isBefore(LocalDate.now())
    }

    fun getDaysUntilDeadline(): Long {
        val deadline = dateFound.plusMonths(6)
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), deadline)
    }
}
