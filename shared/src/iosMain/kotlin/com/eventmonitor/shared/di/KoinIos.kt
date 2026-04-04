package com.eventmonitor.shared.di

import org.koin.core.context.startKoin

class KoinIosHelper {
    fun initKoinIos() {
        startKoin {
            modules(sharedModules)
        }
    }
}
