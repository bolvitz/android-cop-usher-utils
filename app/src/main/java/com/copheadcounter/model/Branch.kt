package com.copheadcounter.model

import java.util.UUID

data class Branch(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val location: String = "",
    val description: String = "",
    val isCounterEnabled: Boolean = true,
    val isLostFoundEnabled: Boolean = true
)
