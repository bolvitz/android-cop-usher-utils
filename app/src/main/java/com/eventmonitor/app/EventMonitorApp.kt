package com.eventmonitor.app

import android.app.Application
import com.eventmonitor.shared.di.sharedModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class EventMonitorApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Koin is the single DI container; everything is backed by the shared
        // KMP module (offline-first Room data, repositories, ViewModels).
        startKoin {
            androidContext(this@EventMonitorApp)
            modules(sharedModules)
        }
    }
}
