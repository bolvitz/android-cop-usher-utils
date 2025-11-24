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

/**
 * Migration from version 6 to version 7
 * Adds:
 * - eventId column to lost_items table (optional link to event/service)
 * - eventId column to incidents table (optional link to event/service)
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add eventId column to lost_items table
        db.execSQL("ALTER TABLE lost_items ADD COLUMN eventId TEXT DEFAULT NULL")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_lost_items_eventId ON lost_items(eventId)")

        // Add eventId column to incidents table
        db.execSQL("ALTER TABLE incidents ADD COLUMN eventId TEXT DEFAULT NULL")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_incidents_eventId ON incidents(eventId)")
    }
}

/**
 * Migration from version 7 to version 8
 * Renames:
 * - Table: branches → venues
 * - Column: branchId → venueId in all referencing tables (events, area_templates, incidents, lost_items)
 * - Indices: All branch-related indices updated to venue naming
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Step 1: Rename branches table to venues
        db.execSQL("ALTER TABLE branches RENAME TO venues")

        // Step 2: Update events table - rename branchId to venueId
        // Create new events table with venueId column
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS events_new (
                id TEXT PRIMARY KEY NOT NULL,
                venueId TEXT NOT NULL,
                eventTypeId TEXT,
                date INTEGER NOT NULL,
                eventType TEXT NOT NULL,
                eventName TEXT NOT NULL,
                totalAttendance INTEGER NOT NULL,
                totalCapacity INTEGER NOT NULL,
                notes TEXT NOT NULL,
                weather TEXT NOT NULL,
                countedBy TEXT NOT NULL,
                countedByUserId TEXT NOT NULL,
                isLocked INTEGER NOT NULL,
                isSpecialEvent INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                completedAt INTEGER,
                isSyncedToCloud INTEGER NOT NULL,
                cloudId TEXT NOT NULL,
                FOREIGN KEY(venueId) REFERENCES venues(id) ON DELETE CASCADE,
                FOREIGN KEY(eventTypeId) REFERENCES service_types(id) ON DELETE SET NULL
            )
        """.trimIndent())

        // Copy data from old table to new table
        db.execSQL("""
            INSERT INTO events_new
            SELECT id, branchId, eventTypeId, date, eventType, eventName, totalAttendance,
                   totalCapacity, notes, weather, countedBy, countedByUserId, isLocked,
                   isSpecialEvent, createdAt, updatedAt, completedAt, isSyncedToCloud, cloudId
            FROM events
        """.trimIndent())

        // Drop old table and rename new table
        db.execSQL("DROP TABLE events")
        db.execSQL("ALTER TABLE events_new RENAME TO events")

        // Recreate indices for events table with new venueId column
        db.execSQL("CREATE INDEX IF NOT EXISTS index_events_venueId ON events(venueId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_events_date ON events(date)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_events_eventType ON events(eventType)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_events_eventTypeId ON events(eventTypeId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_events_venue_date ON events(venueId, date)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_events_venue_type ON events(venueId, eventTypeId)")

        // Step 3: Update area_templates table - rename branchId to venueId
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS area_templates_new (
                id TEXT PRIMARY KEY NOT NULL,
                venueId TEXT NOT NULL,
                name TEXT NOT NULL,
                type TEXT NOT NULL,
                capacity INTEGER NOT NULL,
                isActive INTEGER NOT NULL,
                displayOrder INTEGER NOT NULL,
                color TEXT NOT NULL,
                icon TEXT NOT NULL,
                notes TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                isSyncedToCloud INTEGER NOT NULL,
                FOREIGN KEY(venueId) REFERENCES venues(id) ON DELETE CASCADE
            )
        """.trimIndent())

        db.execSQL("""
            INSERT INTO area_templates_new
            SELECT id, branchId, name, type, capacity, isActive, displayOrder, color,
                   icon, notes, createdAt, updatedAt, isSyncedToCloud
            FROM area_templates
        """.trimIndent())

        db.execSQL("DROP TABLE area_templates")
        db.execSQL("ALTER TABLE area_templates_new RENAME TO area_templates")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_area_templates_venueId ON area_templates(venueId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_area_templates_displayOrder ON area_templates(displayOrder)")

        // Step 4: Update incidents table - rename branchId to venueId
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS incidents_new (
                id TEXT PRIMARY KEY NOT NULL,
                venueId TEXT NOT NULL,
                eventId TEXT,
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
                FOREIGN KEY(venueId) REFERENCES venues(id) ON DELETE CASCADE,
                FOREIGN KEY(eventId) REFERENCES events(id) ON DELETE SET NULL
            )
        """.trimIndent())

        db.execSQL("""
            INSERT INTO incidents_new
            SELECT id, branchId, eventId, title, description, severity, status, category,
                   location, photoUri, reportedBy, assignedTo, reportedAt, resolvedAt,
                   notes, actionsTaken, createdAt, updatedAt, isSyncedToCloud, cloudId
            FROM incidents
        """.trimIndent())

        db.execSQL("DROP TABLE incidents")
        db.execSQL("ALTER TABLE incidents_new RENAME TO incidents")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_incidents_venueId ON incidents(venueId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_incidents_eventId ON incidents(eventId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_incidents_severity ON incidents(severity)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_incidents_status ON incidents(status)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_incidents_reportedAt ON incidents(reportedAt)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_incidents_resolvedAt ON incidents(resolvedAt)")

        // Note: lost_items table uses locationId, not branchId, so it references venues via locationId
        // The foreign key already points to the renamed venues table, so no changes needed
    }
}
