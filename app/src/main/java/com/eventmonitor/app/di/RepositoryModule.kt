package com.eventmonitor.app.di

import com.eventmonitor.core.data.repository.AreaCountRepositoryImpl
import com.eventmonitor.core.data.repository.AreaRepositoryImpl
import com.eventmonitor.core.data.repository.VenueRepositoryImpl
import com.eventmonitor.core.data.repository.IncidentRepositoryImpl
import com.eventmonitor.core.data.repository.LostItemRepositoryImpl
import com.eventmonitor.core.data.repository.EventRepositoryImpl
import com.eventmonitor.core.data.repository.EventTypeRepositoryImpl
import com.eventmonitor.core.data.repository.interfaces.AreaCountRepository
import com.eventmonitor.core.data.repository.interfaces.AreaRepository
import com.eventmonitor.core.data.repository.interfaces.VenueRepository
import com.eventmonitor.core.data.repository.interfaces.IncidentRepository
import com.eventmonitor.core.data.repository.interfaces.LostItemRepository
import com.eventmonitor.core.data.repository.interfaces.EventRepository
import com.eventmonitor.core.data.repository.interfaces.EventTypeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindVenueRepository(
        venueRepositoryImpl: VenueRepositoryImpl
    ): VenueRepository

    @Binds
    @Singleton
    abstract fun bindAreaRepository(
        areaRepositoryImpl: AreaRepositoryImpl
    ): AreaRepository

    @Binds
    @Singleton
    abstract fun bindEventRepository(
        eventRepositoryImpl: EventRepositoryImpl
    ): EventRepository

    @Binds
    @Singleton
    abstract fun bindEventTypeRepository(
        eventTypeRepositoryImpl: EventTypeRepositoryImpl
    ): EventTypeRepository

    @Binds
    @Singleton
    abstract fun bindAreaCountRepository(
        areaCountRepositoryImpl: AreaCountRepositoryImpl
    ): AreaCountRepository

    @Binds
    @Singleton
    abstract fun bindLostItemRepository(
        lostItemRepositoryImpl: LostItemRepositoryImpl
    ): LostItemRepository

    @Binds
    @Singleton
    abstract fun bindIncidentRepository(
        incidentRepositoryImpl: IncidentRepositoryImpl
    ): IncidentRepository
}
