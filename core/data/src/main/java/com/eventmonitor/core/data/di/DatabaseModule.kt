package com.eventmonitor.core.data.di

import android.content.Context
import androidx.room.Room
import com.eventmonitor.core.data.local.dao.*
import com.eventmonitor.core.data.local.database.AppDatabase
import com.eventmonitor.core.data.local.database.MIGRATION_3_4
import com.eventmonitor.core.data.local.database.MIGRATION_4_5
import com.eventmonitor.core.data.local.database.MIGRATION_5_6
import com.eventmonitor.core.data.local.database.MIGRATION_6_7
import com.eventmonitor.core.data.local.database.MIGRATION_7_8
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
            .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
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
}
