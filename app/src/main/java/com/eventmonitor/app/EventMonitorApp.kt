package com.eventmonitor.app

import android.app.Application
import com.eventmonitor.app.di.legacyDataModule
import com.eventmonitor.app.di.legacyViewModelModule
import com.eventmonitor.shared.di.sharedModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class EventMonitorApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Koin is the single DI container for the app: the shared KMP modules
        // (offline-first Room data, repositories, ViewModels) plus the legacy
        // Android data/ViewModels still backed by :core:data.
        startKoin {
            androidContext(this@EventMonitorApp)
            modules(sharedModules + legacyDataModule + legacyViewModelModule)
        }
    }
}
