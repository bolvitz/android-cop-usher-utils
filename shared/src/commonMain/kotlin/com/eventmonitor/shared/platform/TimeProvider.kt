package com.eventmonitor.shared.platform

import kotlinx.datetime.Clock

object TimeProvider {
    fun currentTimeMillis(): Long {
        return Clock.System.now().toEpochMilliseconds()
    }
}
