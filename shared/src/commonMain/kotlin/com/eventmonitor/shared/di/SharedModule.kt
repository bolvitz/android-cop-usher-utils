package com.eventmonitor.shared.di

import com.eventmonitor.shared.data.local.database.AppDatabase
import com.eventmonitor.shared.data.repository.AreaCountRepository
import com.eventmonitor.shared.data.repository.AreaCountRepositoryImpl
import com.eventmonitor.shared.data.repository.EventRepository
import com.eventmonitor.shared.data.repository.EventRepositoryImpl
import com.eventmonitor.shared.data.repository.EventTypeRepository
import com.eventmonitor.shared.data.repository.EventTypeRepositoryImpl
import com.eventmonitor.shared.data.repository.IncidentRepository
import com.eventmonitor.shared.data.repository.IncidentRepositoryImpl
import com.eventmonitor.shared.data.repository.LostItemRepository
import com.eventmonitor.shared.data.repository.LostItemRepositoryImpl
import com.eventmonitor.shared.data.repository.SeatMapRepository
import com.eventmonitor.shared.data.repository.SeatMapRepositoryImpl
import com.eventmonitor.shared.data.repository.VenueRepository
import com.eventmonitor.shared.data.repository.VenueRepositoryImpl
import com.eventmonitor.shared.presentation.areas.AreaManagementViewModel
import com.eventmonitor.shared.presentation.areas.SeatMapEditorViewModel
import com.eventmonitor.shared.presentation.eventtypes.EventTypeManagementViewModel
import com.eventmonitor.shared.presentation.headcounter.CountingViewModel
import com.eventmonitor.shared.presentation.headcounter.HistoryViewModel
import com.eventmonitor.shared.presentation.headcounter.TrendsViewModel
import com.eventmonitor.shared.presentation.incidents.IncidentListViewModel
import com.eventmonitor.shared.presentation.lostandfound.LostAndFoundViewModel
import com.eventmonitor.shared.presentation.venues.VenueListViewModel
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
    single<SeatMapRepository> { SeatMapRepositoryImpl(get(), get(), get(), get()) }
    single<IncidentRepository> { IncidentRepositoryImpl(get()) }
    single<LostItemRepository> { LostItemRepositoryImpl(get()) }
}

val viewModelModule = module {
    factory { VenueListViewModel(get()) }
    factory { EventTypeManagementViewModel(get()) }
    factory { params -> AreaManagementViewModel(get(), venueId = params.get()) }
    factory { params -> SeatMapEditorViewModel(get(), get(), areaTemplateId = params.get()) }
    factory { params ->
        CountingViewModel(
            venueRepository = get(),
            eventRepository = get(),
            eventTypeRepository = get(),
            areaCountRepository = get(),
            seatMapRepository = get(),
            venueId = params.get(),
            existingEventId = params.getOrNull()
        )
    }
    factory { params -> IncidentListViewModel(get(), venueId = params.getOrNull()) }
    factory { params -> LostAndFoundViewModel(get(), locationId = params.getOrNull()) }
    factory { params -> HistoryViewModel(get(), venueId = params.getOrNull()) }
    factory { params -> TrendsViewModel(get(), venueId = params.getOrNull()) }
}

val sharedModules = listOf(
    platformModule,
    dataModule,
    viewModelModule
)
