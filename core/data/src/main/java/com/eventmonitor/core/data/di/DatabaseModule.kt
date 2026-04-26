package com.eventmonitor.core.data.di

import android.content.Context
import androidx.room.Room
import com.eventmonitor.core.data.local.dao.AreaCountDao
import com.eventmonitor.core.data.local.dao.AreaTemplateDao
import com.eventmonitor.core.data.local.dao.EventDao
import com.eventmonitor.core.data.local.dao.EventTypeDao
import com.eventmonitor.core.data.local.dao.IncidentDao
import com.eventmonitor.core.data.local.dao.LostItemDao
import com.eventmonitor.core.data.local.dao.SeatDao
import com.eventmonitor.core.data.local.dao.SeatRowDao
import com.eventmonitor.core.data.local.dao.SeatStatusDao
import com.eventmonitor.core.data.local.dao.UserDao
import com.eventmonitor.core.data.local.dao.VenueDao
import com.eventmonitor.core.data.local.database.AppDatabase
import com.eventmonitor.core.data.local.database.MIGRATION_3_4
import com.eventmonitor.core.data.local.database.MIGRATION_4_5
import com.eventmonitor.core.data.local.database.MIGRATION_5_6
import com.eventmonitor.core.data.local.database.MIGRATION_6_7
import com.eventmonitor.core.data.local.database.MIGRATION_7_8
import com.eventmonitor.core.data.local.database.MIGRATION_8_9
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
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

    @Provides
    @Singleton
    fun provideVenueDao(database: AppDatabase): VenueDao {
        return database.venueDao()
    }

    @Provides
    @Singleton
    fun provideAreaTemplateDao(database: AppDatabase): AreaTemplateDao {
        return database.areaTemplateDao()
    }

    @Provides
    @Singleton
    fun provideEventDao(database: AppDatabase): EventDao {
        return database.eventDao()
    }

    @Provides
    @Singleton
    fun provideEventTypeDao(database: AppDatabase): EventTypeDao {
        return database.eventTypeDao()
    }

    @Provides
    @Singleton
    fun provideAreaCountDao(database: AppDatabase): AreaCountDao {
        return database.areaCountDao()
    }

    @Provides
    @Singleton
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    @Singleton
    fun provideLostItemDao(database: AppDatabase): LostItemDao {
        return database.lostItemDao()
    }

    @Provides
    @Singleton
    fun provideIncidentDao(database: AppDatabase): IncidentDao {
        return database.incidentDao()
    }

    @Provides
    @Singleton
    fun provideSeatRowDao(database: AppDatabase): SeatRowDao {
        return database.seatRowDao()
    }

    @Provides
    @Singleton
    fun provideSeatDao(database: AppDatabase): SeatDao {
        return database.seatDao()
    }

    @Provides
    @Singleton
    fun provideSeatStatusDao(database: AppDatabase): SeatStatusDao {
        return database.seatStatusDao()
    }
}
