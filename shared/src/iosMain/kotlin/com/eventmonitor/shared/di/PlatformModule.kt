package com.eventmonitor.shared.di

import com.eventmonitor.shared.data.local.database.AppDatabase
import com.eventmonitor.shared.data.local.database.createAppDatabase
import com.eventmonitor.shared.platform.CoroutineDispatchers
import com.eventmonitor.shared.platform.FileExporter
import com.eventmonitor.shared.platform.HapticFeedback
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<AppDatabase> { createAppDatabase() }
    single { CoroutineDispatchers() }
    factory { HapticFeedback() }
    factory { FileExporter() }
}
