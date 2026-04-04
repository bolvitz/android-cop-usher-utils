package com.eventmonitor.shared.di

import com.eventmonitor.shared.data.repository.*
import com.eventmonitor.shared.platform.CoroutineDispatchers
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import org.koin.core.module.Module
import org.koin.dsl.module

expect val platformModule: Module

val dataModule = module {
    // Firebase Firestore instance
    single { Firebase.firestore }

    // Repository implementations
    single<VenueRepository> { VenueRepositoryImpl(get()) }
    single<EventRepository> { EventRepositoryImpl(get()) }
    single<AreaCountRepository> { AreaCountRepositoryImpl(get()) }
    single<EventTypeRepository> { EventTypeRepositoryImpl(get()) }
}

val viewModelModule = module {
    // ViewModels will be added here in Phase 4
}

val sharedModules = listOf(
    platformModule,
    dataModule,
    viewModelModule
)
