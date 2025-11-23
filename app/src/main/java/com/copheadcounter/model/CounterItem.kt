package com.copheadcounter.model

import java.util.UUID

data class CounterItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val count: Int = 0
)
