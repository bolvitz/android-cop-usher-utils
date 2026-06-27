package com.eventmonitor.shared.di

import com.eventmonitor.shared.data.local.database.AppDatabase
import com.eventmonitor.shared.data.repository.AreaCountRepository
import com.eventmonitor.shared.data.repository.AreaCountRepositoryImpl
import com.eventmonitor.shared.data.repository.EventRepository
import com.eventmonitor.shared.data.repository.EventRepositoryImpl
import com.eventmonitor.shared.data.repository.EventTypeRepository
import com.eventmonitor.shared.data.repository.EventTypeRepositoryImpl
import com.eventmonitor.shared.data.repository.VenueRepository
import com.eventmonitor.shared.data.repository.VenueRepositoryImpl
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Platform module supplies the [AppDatabase] (Android needs a Context, iOS does not)
 * plus platform services (haptics, file export, dispatchers).
 */
expect val platformModule: Module

val dataModule = module {
    // DAOs are derived from the platform-provided AppDatabase.
    single { get<AppDatabase>().venueDao() }
    single { get<AppDatabase>().areaTemplateDao() }
    single { get<AppDatabase>().eventDao() }
    single { get<AppDatabase>().eventTypeDao() }
    single { get<AppDatabase>().areaCountDao() }
    single { get<AppDatabase>().userDao() }
    single { get<AppDatabase>().lostItemDao() }
    single { get<AppDatabase>().incidentDao() }
    single { get<AppDatabase>().seatRowDao() }
    single { get<AppDatabase>().seatDao() }
    single { get<AppDatabase>().seatStatusDao() }

    // Room-backed repository implementations (offline-first).
    single<VenueRepository> { VenueRepositoryImpl(get(), get()) }
    single<EventRepository> { EventRepositoryImpl(get()) }
    single<AreaCountRepository> { AreaCountRepositoryImpl(get(), get()) }
    single<EventTypeRepository> { EventTypeRepositoryImpl(get()) }
}

val viewModelModule = module {
    // Shared ViewModels are registered here in Phase 3.
}

val sharedModules = listOf(
    platformModule,
    dataModule,
    viewModelModule
)
