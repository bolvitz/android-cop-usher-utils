package com.eventmonitor.core.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.eventmonitor.core.data.local.dao.*
import com.eventmonitor.core.data.local.entities.*

@Database(
    entities = [
        VenueEntity::class,
        AreaTemplateEntity::class,
        EventEntity::class,
        EventTypeEntity::class,
        AreaCountEntity::class,
        UserEntity::class,
        LostItemEntity::class,
        IncidentEntity::class,
        SeatRowEntity::class,
        SeatEntity::class,
        SeatStatusEntity::class
    ],
    version = 9,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun venueDao(): VenueDao
    abstract fun areaTemplateDao(): AreaTemplateDao
    abstract fun eventDao(): EventDao
    abstract fun eventTypeDao(): EventTypeDao
    abstract fun areaCountDao(): AreaCountDao
    abstract fun userDao(): UserDao
    abstract fun lostItemDao(): LostItemDao
    abstract fun incidentDao(): IncidentDao
    abstract fun seatRowDao(): SeatRowDao
    abstract fun seatDao(): SeatDao
    abstract fun seatStatusDao(): SeatStatusDao

    companion object {
        const val DATABASE_NAME = "event_monitor_db"
    }
}
