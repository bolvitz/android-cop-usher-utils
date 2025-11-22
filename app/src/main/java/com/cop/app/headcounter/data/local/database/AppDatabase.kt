package com.cop.app.headcounter.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.cop.app.headcounter.data.local.dao.*
import com.cop.app.headcounter.data.local.entities.*

@Database(
    entities = [
        BranchEntity::class,
        AreaTemplateEntity::class,
        ServiceEntity::class,
        AreaCountEntity::class,
        UserEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun branchDao(): BranchDao
    abstract fun areaTemplateDao(): AreaTemplateDao
    abstract fun serviceDao(): ServiceDao
    abstract fun areaCountDao(): AreaCountDao
    abstract fun userDao(): UserDao

    companion object {
        const val DATABASE_NAME = "church_attendance_db"
    }
}
