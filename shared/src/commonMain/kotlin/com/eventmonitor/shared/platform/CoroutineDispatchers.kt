package com.eventmonitor.shared.platform

import kotlinx.coroutines.CoroutineDispatcher

expect class CoroutineDispatchers {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
}
