package com.eventmonitor.shared.util

import kotlinx.datetime.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Multiplatform replacement for java.util.UUID.randomUUID().toString(). */
@OptIn(ExperimentalUuidApi::class)
fun newId(): String = Uuid.random().toString()

/** Multiplatform replacement for System.currentTimeMillis(). */
fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()
