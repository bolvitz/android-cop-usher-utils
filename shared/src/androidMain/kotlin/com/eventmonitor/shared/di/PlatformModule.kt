package com.eventmonitor.shared.di

import com.eventmonitor.shared.platform.CoroutineDispatchers
import com.eventmonitor.shared.platform.FileExporter
import com.eventmonitor.shared.platform.HapticFeedback
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single { CoroutineDispatchers() }
    factory { HapticFeedback(get()) }
    factory { FileExporter(get()) }
}
