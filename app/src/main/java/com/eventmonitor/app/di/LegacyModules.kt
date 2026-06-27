package com.eventmonitor.app.di

import android.app.Application
import androidx.room.Room
import com.eventmonitor.app.presentation.screens.areas.AreaManagementViewModel
import com.eventmonitor.app.presentation.screens.areas.ZoneEditorViewModel
import com.eventmonitor.app.presentation.screens.reports.ReportsViewModel
import com.eventmonitor.app.presentation.screens.venues.VenueListViewModel
import com.eventmonitor.app.presentation.screens.venues.VenueManagementViewModel
import com.eventmonitor.app.presentation.screens.venues.VenueSetupViewModel
import com.eventmonitor.app.presentation.viewmodels.EventTypeManagementViewModel
import com.eventmonitor.core.data.local.database.AppDatabase
import com.eventmonitor.core.data.local.database.MIGRATION_3_4
import com.eventmonitor.core.data.local.database.MIGRATION_4_5
import com.eventmonitor.core.data.local.database.MIGRATION_5_6
import com.eventmonitor.core.data.local.database.MIGRATION_6_7
import com.eventmonitor.core.data.local.database.MIGRATION_7_8
import com.eventmonitor.core.data.local.database.MIGRATION_8_9
import com.eventmonitor.core.data.repository.AreaCountRepositoryImpl
import com.eventmonitor.core.data.repository.AreaRepositoryImpl
import com.eventmonitor.core.data.repository.EventRepositoryImpl
import com.eventmonitor.core.data.repository.EventTypeRepositoryImpl
import com.eventmonitor.core.data.repository.IncidentRepositoryImpl
import com.eventmonitor.core.data.repository.LostItemRepositoryImpl
import com.eventmonitor.core.data.repository.SeatMapRepositoryImpl
import com.eventmonitor.core.data.repository.VenueRepositoryImpl
import com.eventmonitor.core.data.repository.interfaces.AreaCountRepository
import com.eventmonitor.core.data.repository.interfaces.AreaRepository
import com.eventmonitor.core.data.repository.interfaces.EventRepository
import com.eventmonitor.core.data.repository.interfaces.EventTypeRepository
import com.eventmonitor.core.data.repository.interfaces.IncidentRepository
import com.eventmonitor.core.data.repository.interfaces.LostItemRepository
import com.eventmonitor.core.data.repository.interfaces.SeatMapRepository
import com.eventmonitor.core.data.repository.interfaces.VenueRepository
import com.eventmonitor.feature.headcounter.screens.CountingViewModel
import com.eventmonitor.feature.headcounter.screens.HistoryViewModel
import com.eventmonitor.feature.headcounter.screens.TrendsViewModel
import com.eventmonitor.feature.incidents.screens.AddEditIncidentViewModel
import com.eventmonitor.feature.incidents.screens.IncidentDetailViewModel
import com.eventmonitor.feature.incidents.screens.IncidentListViewModel
import com.eventmonitor.feature.lostandfound.screens.AddEditLostItemViewModel
import com.eventmonitor.feature.lostandfound.screens.LostAndFoundViewModel
import com.eventmonitor.feature.lostandfound.screens.LostItemDetailViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Koin replacement for the former Hilt DatabaseModule + RepositoryModule.
 * Wires the legacy :core:data Room database (with its real migrations), DAOs,
 * and repository bindings still used by the not-yet-fully-ported Android UI.
 */
val legacyDataModule = module {
    single { androidApplication() as Application }

    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .addMigrations(
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7,
                MIGRATION_7_8,
                MIGRATION_8_9
            )
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }

    // DAOs
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

    // Repository bindings (constructors auto-wired from the DAOs above)
    singleOf(::VenueRepositoryImpl) bind VenueRepository::class
    singleOf(::AreaRepositoryImpl) bind AreaRepository::class
    singleOf(::EventRepositoryImpl) bind EventRepository::class
    singleOf(::EventTypeRepositoryImpl) bind EventTypeRepository::class
    singleOf(::AreaCountRepositoryImpl) bind AreaCountRepository::class
    singleOf(::LostItemRepositoryImpl) bind LostItemRepository::class
    singleOf(::IncidentRepositoryImpl) bind IncidentRepository::class
    singleOf(::SeatMapRepositoryImpl) bind SeatMapRepository::class
}

/**
 * The remaining Android/feature ViewModels. `viewModelOf` auto-wires each
 * constructor (repositories above, plus SavedStateHandle for those that read
 * navigation arguments).
 */
val legacyViewModelModule = module {
    viewModelOf(::VenueListViewModel)
    viewModelOf(::VenueManagementViewModel)
    viewModelOf(::VenueSetupViewModel)
    viewModelOf(::EventTypeManagementViewModel)
    viewModelOf(::AreaManagementViewModel)
    viewModelOf(::ZoneEditorViewModel)
    viewModelOf(::ReportsViewModel)
    viewModelOf(::CountingViewModel)
    viewModelOf(::HistoryViewModel)
    viewModelOf(::TrendsViewModel)
    viewModelOf(::IncidentListViewModel)
    viewModelOf(::IncidentDetailViewModel)
    viewModelOf(::AddEditIncidentViewModel)
    viewModelOf(::LostAndFoundViewModel)
    viewModelOf(::LostItemDetailViewModel)
    viewModelOf(::AddEditLostItemViewModel)
}
