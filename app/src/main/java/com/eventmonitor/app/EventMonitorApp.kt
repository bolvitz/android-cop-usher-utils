package com.eventmonitor.app

import android.app.Application
import com.eventmonitor.shared.di.sharedModules
import dagger.hilt.android.HiltAndroidApp
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

@HiltAndroidApp
class EventMonitorApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Koin powers the shared KMP module (offline-first Room data,
        // repositories and ViewModels). It runs alongside Hilt while features
        // are migrated onto :shared one at a time.
        startKoin {
            androidContext(this@EventMonitorApp)
            modules(sharedModules)
        }
    }
}
