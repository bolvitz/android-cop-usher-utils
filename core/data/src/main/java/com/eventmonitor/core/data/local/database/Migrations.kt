package com.eventmonitor.core.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from version 3 to version 4
 * Adds:
 * - Composite indices for better query performance
 * - Unique constraints for data integrity
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add composite indices for common query patterns

        // Services table - optimize branch + date queries
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_services_branch_date ON services(branchId, date)")

        // Services table - optimize branch + service type queries
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_services_branch_type ON services(branchId, eventTypeId)")

        // Area counts table - optimize service + template queries
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_area_counts_service_template ON area_counts(eventId, areaTemplateId)")

        // Add unique constraints to prevent duplicates

        // Branch codes should be unique (temporarily create new table with constraint)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS branches_new (
                id TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                location TEXT NOT NULL,
                code TEXT NOT NULL,
                isActive INTEGER NOT NULL,
                logoUrl TEXT NOT NULL,
                color TEXT NOT NULL,
                contactPerson TEXT NOT NULL,
                contactPhone TEXT NOT NULL,
                contactEmail TEXT NOT NULL,
                timezone TEXT NOT NULL,
                notes TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                isSyncedToCloud INTEGER NOT NULL,
                cloudId TEXT NOT NULL
            )
        """.trimIndent())

        // Create unique index on code
        db.execSQL("CREATE UNIQUE INDEX index_branches_code ON branches_new(code)")

        // Copy data from old table to new table
        db.execSQL("""
            INSERT INTO branches_new
            SELECT * FROM branches
        """.trimIndent())

        // Drop old table
        db.execSQL("DROP TABLE branches")

        // Rename new table
        db.execSQL("ALTER TABLE branches_new RENAME TO branches")

        // Service type names should be unique per branch (for now, just unique globally)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS service_types_new (
                id TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                dayType TEXT NOT NULL,
                time TEXT NOT NULL,
                description TEXT NOT NULL,
                isActive INTEGER NOT NULL,
                displayOrder INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
        """.trimIndent())

        // Create unique index on name
        db.execSQL("CREATE UNIQUE INDEX index_service_types_name ON service_types_new(name)")

        // Copy data from old table to new table
        db.execSQL("""
            INSERT INTO service_types_new
            SELECT * FROM service_types
        """.trimIndent())

        // Drop old table
        db.execSQL("DROP TABLE service_types")

        // Rename new table
        db.execSQL("ALTER TABLE service_types_new RENAME TO service_types")
    }
}

/**
 * Migration from version 4 to version 5
 * Adds:
 * - Lost & Found feature: lost_items table
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create lost_items table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS lost_items (
                id TEXT PRIMARY KEY NOT NULL,
                locationId TEXT NOT NULL,
                description TEXT NOT NULL,
                category TEXT NOT NULL,
                foundZone TEXT NOT NULL,
                foundDate INTEGER NOT NULL,
                photoUri TEXT NOT NULL,
                color TEXT NOT NULL,
                brand TEXT NOT NULL,
                identifyingMarks TEXT NOT NULL,
                status TEXT NOT NULL,
                claimedBy TEXT NOT NULL,
                claimedDate INTEGER NOT NULL,
                claimerContact TEXT NOT NULL,
                verificationNotes TEXT NOT NULL,
                reportedBy TEXT NOT NULL,
                notes TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                isSyncedToCloud INTEGER NOT NULL,
                cloudId TEXT NOT NULL,
                FOREIGN KEY(locationId) REFERENCES branches(id) ON DELETE CASCADE
            )
        """.trimIndent())

        // Create indices for better query performance
        db.execSQL("CREATE INDEX IF NOT EXISTS index_lost_items_locationId ON lost_items(locationId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_lost_items_status ON lost_items(status)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_lost_items_category ON lost_items(category)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_lost_items_foundDate ON lost_items(foundDate)")
    }
}

/**
 * Migration from version 5 to version 6
 * Adds:
 * - Incident Reporting feature: incidents table
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create incidents table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS incidents (
                id TEXT PRIMARY KEY NOT NULL,
                branchId TEXT NOT NULL,
                title TEXT NOT NULL,
                description TEXT NOT NULL,
                severity TEXT NOT NULL,
                status TEXT NOT NULL,
                category TEXT NOT NULL,
                location TEXT NOT NULL,
                photoUri TEXT NOT NULL,
                reportedBy TEXT NOT NULL,
                assignedTo TEXT NOT NULL,
                reportedAt INTEGER NOT NULL,
                resolvedAt INTEGER,
                notes TEXT NOT NULL,
                actionsTaken TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                isSyncedToCloud INTEGER NOT NULL,
                cloudId TEXT NOT NULL,
                FOREIGN KEY(branchId) REFERENCES branches(id) ON DELETE CASCADE
            )
        """.trimIndent())

        // Create indices for better query performance
        db.execSQL("CREATE INDEX IF NOT EXISTS index_incidents_branchId ON incidents(branchId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_incidents_severity ON incidents(severity)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_incidents_status ON incidents(status)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_incidents_reportedAt ON incidents(reportedAt)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_incidents_resolvedAt ON incidents(resolvedAt)")
    }
}
