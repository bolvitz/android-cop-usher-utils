package com.cop.app.headcounter.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cop.app.headcounter.data.local.database.AppDatabase
import com.cop.app.headcounter.data.local.database.MIGRATION_3_4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate3To4_preservesAllData() {
        // Create database with version 3 schema
        helper.createDatabase(TEST_DB, 3).apply {
            // Insert test data into version 3 database

            // Insert branches
            execSQL("""
                INSERT INTO branches (id, name, location, code, isActive, logoUrl, color,
                    contactPerson, contactPhone, contactEmail, timezone, notes,
                    createdAt, updatedAt, isSyncedToCloud, cloudId)
                VALUES
                    ('branch-1', 'Main Campus', '123 Main St', 'MC', 1, '', '#1976D2',
                     'John Doe', '555-1234', 'john@church.org', 'UTC', 'Main branch',
                     1234567890000, 1234567890000, 0, ''),
                    ('branch-2', 'North Campus', '456 North Ave', 'NC', 1, '', '#4CAF50',
                     'Jane Smith', '555-5678', 'jane@church.org', 'UTC', 'North branch',
                     1234567890000, 1234567890000, 0, '')
            """)

            // Insert service types
            execSQL("""
                INSERT INTO service_types (id, name, dayType, time, description, isActive,
                    displayOrder, createdAt, updatedAt)
                VALUES
                    ('type-1', 'Sunday Morning Service', 'Sunday', '9:00 AM', 'Main service', 1, 0,
                     1234567890000, 1234567890000),
                    ('type-2', 'Wednesday Bible Study', 'Wednesday', '7:00 PM', 'Mid-week service', 1, 1,
                     1234567890000, 1234567890000)
            """)

            // Insert area templates
            execSQL("""
                INSERT INTO area_templates (id, branchId, name, type, capacity, isActive,
                    displayOrder, color, icon, notes, createdAt, updatedAt, isSyncedToCloud)
                VALUES
                    ('area-1', 'branch-1', 'Main Hall', 'BAY', 200, 1, 0, '#4CAF50', 'chair',
                     'Main seating area', 1234567890000, 1234567890000, 0),
                    ('area-2', 'branch-1', 'Balcony', 'BALCONY', 100, 1, 1, '#2196F3', 'stairs',
                     'Upper level', 1234567890000, 1234567890000, 0),
                    ('area-3', 'branch-2', 'Sanctuary', 'BAY', 300, 1, 0, '#FF9800', 'chair',
                     'Main area', 1234567890000, 1234567890000, 0)
            """)

            // Insert services
            execSQL("""
                INSERT INTO services (id, branchId, serviceTypeId, date, serviceType, serviceName,
                    totalAttendance, totalCapacity, notes, weather, countedBy, countedByUserId,
                    isLocked, isSpecialEvent, createdAt, updatedAt, completedAt, isSyncedToCloud, cloudId)
                VALUES
                    ('service-1', 'branch-1', 'type-1', 1704067200000, 'SUNDAY_AM', 'Sunday Service',
                     250, 300, 'Good attendance', 'Sunny', 'John Doe', '', 1, 0,
                     1704067200000, 1704067200000, 1704070800000, 0, ''),
                    ('service-2', 'branch-2', 'type-1', 1704067200000, 'SUNDAY_AM', 'Sunday Service',
                     400, 450, 'Great turnout', 'Rainy', 'Jane Smith', '', 0, 0,
                     1704067200000, 1704067200000, null, 0, '')
            """)

            // Insert area counts
            execSQL("""
                INSERT INTO area_counts (id, serviceId, areaTemplateId, count, capacity, notes,
                    countHistory, lastUpdated, isSyncedToCloud)
                VALUES
                    ('count-1', 'service-1', 'area-1', 150, 200, 'Full section', '[]',
                     1704067200000, 0),
                    ('count-2', 'service-1', 'area-2', 100, 100, 'At capacity', '[]',
                     1704067200000, 0),
                    ('count-3', 'service-2', 'area-3', 400, 300, 'Standing room only', '[]',
                     1704067200000, 0)
            """)

            close()
        }

        // Run the migration
        helper.runMigrationsAndValidate(TEST_DB, 4, true, MIGRATION_3_4).apply {

            // Verify all data is preserved

            // Check branches count
            query("SELECT COUNT(*) FROM branches").use { cursor ->
                cursor.moveToFirst()
                assert(cursor.getInt(0) == 2) { "Expected 2 branches, found ${cursor.getInt(0)}" }
            }

            // Check branch codes are unique (migration adds unique constraint)
            query("SELECT code FROM branches ORDER BY code").use { cursor ->
                cursor.moveToFirst()
                assert(cursor.getString(0) == "MC") { "Expected MC, got ${cursor.getString(0)}" }
                cursor.moveToNext()
                assert(cursor.getString(0) == "NC") { "Expected NC, got ${cursor.getString(0)}" }
            }

            // Check service types count
            query("SELECT COUNT(*) FROM service_types").use { cursor ->
                cursor.moveToFirst()
                assert(cursor.getInt(0) == 2) { "Expected 2 service types, found ${cursor.getInt(0)}" }
            }

            // Check service types are unique (migration adds unique constraint)
            query("SELECT name FROM service_types ORDER BY name").use { cursor ->
                cursor.moveToFirst()
                assert(cursor.getString(0) == "Sunday Morning Service")
                cursor.moveToNext()
                assert(cursor.getString(0) == "Wednesday Bible Study")
            }

            // Check area templates count
            query("SELECT COUNT(*) FROM area_templates").use { cursor ->
                cursor.moveToFirst()
                assert(cursor.getInt(0) == 3) { "Expected 3 area templates, found ${cursor.getInt(0)}" }
            }

            // Check services count
            query("SELECT COUNT(*) FROM services").use { cursor ->
                cursor.moveToFirst()
                assert(cursor.getInt(0) == 2) { "Expected 2 services, found ${cursor.getInt(0)}" }
            }

            // Verify service data integrity
            query("SELECT id, totalAttendance, isLocked FROM services ORDER BY id").use { cursor ->
                cursor.moveToFirst()
                assert(cursor.getString(0) == "service-1")
                assert(cursor.getInt(1) == 250) { "Expected attendance 250, got ${cursor.getInt(1)}" }
                assert(cursor.getInt(2) == 1) { "Expected locked=1, got ${cursor.getInt(2)}" }

                cursor.moveToNext()
                assert(cursor.getString(0) == "service-2")
                assert(cursor.getInt(1) == 400) { "Expected attendance 400, got ${cursor.getInt(1)}" }
                assert(cursor.getInt(2) == 0) { "Expected locked=0, got ${cursor.getInt(2)}" }
            }

            // Check area counts
            query("SELECT COUNT(*) FROM area_counts").use { cursor ->
                cursor.moveToFirst()
                assert(cursor.getInt(0) == 3) { "Expected 3 area counts, found ${cursor.getInt(0)}" }
            }

            // Verify area count data integrity
            query("SELECT count, capacity FROM area_counts WHERE id = 'count-1'").use { cursor ->
                cursor.moveToFirst()
                assert(cursor.getInt(0) == 150) { "Expected count 150, got ${cursor.getInt(0)}" }
                assert(cursor.getInt(1) == 200) { "Expected capacity 200, got ${cursor.getInt(1)}" }
            }

            // Verify composite indices exist
            query("SELECT name FROM sqlite_master WHERE type='index' AND name LIKE 'idx_%'").use { cursor ->
                val indices = mutableListOf<String>()
                while (cursor.moveToNext()) {
                    indices.add(cursor.getString(0))
                }
                assert(indices.contains("idx_services_branch_date")) {
                    "Missing index idx_services_branch_date. Found: $indices"
                }
                assert(indices.contains("idx_services_branch_type")) {
                    "Missing index idx_services_branch_type. Found: $indices"
                }
                assert(indices.contains("idx_area_counts_service_template")) {
                    "Missing index idx_area_counts_service_template. Found: $indices"
                }
            }

            // Verify unique index on branches.code
            query("SELECT name FROM sqlite_master WHERE type='index' AND tbl_name='branches' AND name='index_branches_code'").use { cursor ->
                assert(cursor.moveToFirst()) { "Missing unique index on branches.code" }
                assert(cursor.getString(0) == "index_branches_code") {
                    "Expected index_branches_code, got ${cursor.getString(0)}"
                }
            }

            // Verify unique index on service_types.name
            query("SELECT name FROM sqlite_master WHERE type='index' AND tbl_name='service_types' AND name='index_service_types_name'").use { cursor ->
                assert(cursor.moveToFirst()) { "Missing unique index on service_types.name" }
                assert(cursor.getString(0) == "index_service_types_name") {
                    "Expected index_service_types_name, got ${cursor.getString(0)}"
                }
            }

            close()
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrate3To4_handlesEmptyDatabase() {
        // Test migration on empty database
        helper.createDatabase(TEST_DB, 3).apply {
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 4, true, MIGRATION_3_4).apply {
            // Verify tables exist
            query("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name").use { cursor ->
                val tables = mutableListOf<String>()
                while (cursor.moveToNext()) {
                    tables.add(cursor.getString(0))
                }
                assert(tables.contains("branches"))
                assert(tables.contains("service_types"))
                assert(tables.contains("services"))
                assert(tables.contains("area_counts"))
            }
            close()
        }
    }
}
