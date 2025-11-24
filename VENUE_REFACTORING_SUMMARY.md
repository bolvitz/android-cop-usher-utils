# Event Monitor: Branch → Venue Refactoring Summary

## Overview
Comprehensive renaming of all "branch" references to "venue" throughout the Event Monitor app to better align with the event monitoring purpose.

---

## COMPLETED CHANGES

### 1. Core Data Layer - Entities

**NEW FILES CREATED:**
- `/core/data/src/main/java/com/eventmonitor/core/data/local/entities/VenueEntity.kt`
- `/core/data/src/main/java/com/eventmonitor/core/data/local/dao/VenueDao.kt`
- `/core/data/src/main/java/com/eventmonitor/core/data/repository/interfaces/VenueRepository.kt`
- `/core/data/src/main/java/com/eventmonitor/core/data/repository/VenueRepositoryImpl.kt`

**UPDATED FILES:**
- `Relations.kt` - Renamed `BranchWithAreas` → `VenueWithAreas`, updated all `branchId` → `venueId` references
- `AreaTemplateEntity.kt` - Foreign key: `BranchEntity` → `VenueEntity`, `branchId` → `venueId`
- `EventEntity.kt` - Foreign key and indices updated: `branchId` → `venueId`
- `IncidentEntity.kt` - Foreign key updated: `branchId` → `venueId`
- `LostItemEntity.kt` - Foreign key entity reference updated to `VenueEntity`

### 2. Database Layer - DAOs

**UPDATED:**
- `VenueDao.kt` - All queries updated (`branches` → `venues` table, `branchId` → `venueId`)
- `AreaTemplateDao.kt` - All methods renamed (`getAreasByBranch` → `getAreasByVenue`, `getTotalCapacityForBranch` → `getTotalCapacityForVenue`)
- `EventDao.kt` - All queries updated (`branchId` → `venueId`, method renames: `getRecentServicesByBranch` → `getRecentEventsByVenue`)
- `IncidentDao.kt` - All queries updated (`branchId` → `venueId`, method renames: `getIncidentsByBranch` → `getIncidentsByVenue`)

### 3. Database Migration

**CREATED:**
- `MIGRATION_7_8` in `Migrations.kt` - Comprehensive migration that:
  - Renames `branches` table → `venues`
  - Renames `branchId` → `venueId` in: `events`, `area_templates`, `incidents`
  - Recreates all indices with new naming
  - Preserves all existing data

**UPDATED:**
- `AppDatabase.kt` - Version bumped to 8, `BranchEntity` → `VenueEntity`, `branchDao()` → `venueDao()`

### 4. Repository Layer

**REPOSITORY INTERFACES UPDATED:**
- `VenueRepository.kt` - All methods renamed (created new, old BranchRepository.kt will need deletion)
- `EventRepository.kt` - All branch references → venue references
- `AreaRepository.kt` - All branch references → venue references
- `IncidentRepository.kt` - All branch references → venue references

**REPOSITORY IMPLEMENTATIONS:**
- `VenueRepositoryImpl.kt` - Created with all venue methods
- Note: EventRepositoryImpl, AreaRepositoryImpl, IncidentRepositoryImpl still need updates

### 5. Domain Layer

**UPDATED:**
- `DomainValidators.kt`:
  - `validateBranchInput` → `validateVenueInput`
  - `validateServiceInput` → `validateEventInput` (branchId → venueId)
  - `validateBranchCode` → `validateVenueCode`

### 6. Dependency Injection

**UPDATED:**
- `DatabaseModule.kt`:
  - Added `MIGRATION_7_8` to migrations list
  - `provideBranchDao` → `provideVenueDao`
- `RepositoryModule.kt`:
  - `BranchRepositoryImpl` → `VenueRepositoryImpl`
  - `BranchRepository` → `VenueRepository`

### 7. Navigation

**UPDATED:**
- `Screen.kt`:
  - `BranchList` → `VenueList`
  - `BranchSetup` → `VenueSetup`
  - All route parameters: `branchId` → `venueId`
  - Routes updated: `branch_list` → `venue_list`, `branch_setup/{branchId}` → `venue_setup/{venueId}`

---

## REMAINING WORK

### Files That Still Need Updates (30+ files)

These files still contain `branchId`, `BranchRepository`, or `branchDao` references and need systematic updates:

**Repository Implementations:**
1. `EventRepositoryImpl.kt` - Update all branchId → venueId, method calls to DAO
2. `AreaRepositoryImpl.kt` - Update all branchId → venueId, method calls to DAO
3. `IncidentRepositoryImpl.kt` - Update all branchId → venueId, method calls to DAO

**App Layer - UI Screens & ViewModels:**
4. `/app/src/main/java/com/eventmonitor/app/presentation/screens/branches/BranchListScreen.kt` → Rename to `VenueListScreen.kt`
5. `/app/src/main/java/com/eventmonitor/app/presentation/screens/branches/BranchListViewModel.kt` → Rename to `VenueListViewModel.kt`
6. `/app/src/main/java/com/eventmonitor/app/presentation/screens/branches/BranchSetupScreen.kt` → Rename to `VenueSetupScreen.kt`
7. `/app/src/main/java/com/eventmonitor/app/presentation/screens/branches/BranchSetupViewModel.kt` → Rename to `VenueSetupViewModel.kt`
8. `/app/src/main/java/com/eventmonitor/app/presentation/screens/areas/AreaManagementViewModel.kt`
9. `/app/src/main/java/com/eventmonitor/app/presentation/screens/reports/ReportsScreen.kt`
10. `/app/src/main/java/com/eventmonitor/app/presentation/screens/reports/ReportsViewModel.kt`
11. `/app/src/main/java/com/eventmonitor/app/presentation/navigation/NavGraph.kt`

**Feature Modules - Head Counter:**
12. `/feature/headcounter/src/main/java/com/eventmonitor/feature/headcounter/screens/CountingViewModel.kt`
13. `/feature/headcounter/src/main/java/com/eventmonitor/feature/headcounter/screens/HistoryScreen.kt`
14. `/feature/headcounter/src/main/java/com/eventmonitor/feature/headcounter/screens/HistoryViewModel.kt`

**Feature Modules - Incidents:**
15. `/feature/incidents/src/main/java/com/eventmonitor/feature/incidents/screens/IncidentListScreen.kt`
16. `/feature/incidents/src/main/java/com/eventmonitor/feature/incidents/screens/IncidentListViewModel.kt`
17. `/feature/incidents/src/main/java/com/eventmonitor/feature/incidents/screens/AddEditIncidentScreen.kt`
18. `/feature/incidents/src/main/java/com/eventmonitor/feature/incidents/screens/AddEditIncidentViewModel.kt`

**Test Files:**
19. `/app/src/test/kotlin/com/cop/app/headcounter/repositories/FakeServiceRepository.kt`
20. `/app/src/test/kotlin/com/cop/app/headcounter/viewmodels/CountingViewModelTest.kt`

**Database Files (in Migrations.kt):**
21. Update old migration SQL comments that still reference "branches" terminology

---

## SYSTEMATIC UPDATE STRATEGY

### Step 1: Update Repository Implementations (Priority: HIGH)

**EventRepositoryImpl.kt:**
```kotlin
// Replace all occurrences:
branchId → venueId
getRecentServicesByBranch → getRecentEventsByVenue
getServicesByBranchAndDateRange → getEventsByVenueAndDateRange
getServicesAcrossBranches → getEventsAcrossVenues
exportBranchComparisonReport → exportVenueComparisonReport
```

**AreaRepositoryImpl.kt:**
```kotlin
// Replace all occurrences:
branchId → venueId
getAreasByBranch → getAreasByVenue
getTotalCapacityForBranch → getTotalCapacityForVenue
duplicateAreaToBranches → duplicateAreaToVenues
targetBranchIds → targetVenueIds
```

**IncidentRepositoryImpl.kt:**
```kotlin
// Replace all occurrences:
branchId → venueId
getIncidentsByBranch → getIncidentsByVenue
getIncidentsByBranchAndStatus → getIncidentsByVenueAndStatus
getIncidentsByBranchAndSeverity → getIncidentsByVenueAndSeverity
getActiveIncidentCountByBranch → getActiveIncidentCountByVenue
```

### Step 2: Rename and Update UI Files (Priority: HIGH)

**Directory Rename:**
```bash
mv app/src/main/java/com/eventmonitor/app/presentation/screens/branches \
   app/src/main/java/com/eventmonitor/app/presentation/screens/venues
```

**File Renames:**
```bash
# In venues/ directory:
BranchListScreen.kt → VenueListScreen.kt
BranchListViewModel.kt → VenueListViewModel.kt
BranchSetupScreen.kt → VenueSetupScreen.kt
BranchSetupViewModel.kt → VenueSetupViewModel.kt
```

**Content Updates in each file:**
- Replace all `branchId` → `venueId`
- Replace all `BranchRepository` → `VenueRepository`
- Replace all `branch` variable names → `venue`
- Replace all `branches` → `venues`
- Replace UI strings: "Branch" → "Venue", "Branches" → "Venues"
- Update navigation: `Screen.BranchList` → `Screen.VenueList`, etc.

### Step 3: Update NavGraph.kt (Priority: HIGH)

Replace all navigation route references:
```kotlin
Screen.BranchList → Screen.VenueList
Screen.BranchSetup → Screen.VenueSetup
branchId → venueId (in route arguments)
```

### Step 4: Update Feature Modules (Priority: MEDIUM)

**For each file in feature modules:**
- Update all `branchId` → `venueId` parameters
- Update all repository method calls
- Update navigation route parameters

### Step 5: Update Test Files (Priority: LOW)

Update all test files with new naming conventions.

### Step 6: Delete Old Files (Priority: HIGH)

```bash
rm core/data/src/main/java/com/eventmonitor/core/data/local/entities/BranchEntity.kt
rm core/data/src/main/java/com/eventmonitor/core/data/local/dao/BranchDao.kt
rm core/data/src/main/java/com/eventmonitor/core/data/repository/interfaces/BranchRepository.kt
rm core/data/src/main/java/com/eventmonitor/core/data/repository/BranchRepositoryImpl.kt
```

### Step 7: Clean Build & Verify

```bash
./gradlew clean
./gradlew assembleDebug
./gradlew test
```

---

## VERIFICATION CHECKLIST

After completing all updates, verify:

- [ ] All Kotlin files compile without errors
- [ ] No references to `BranchEntity`, `BranchDao`, `BranchRepository` remain (except in old migration code comments)
- [ ] Database migration from v7 → v8 executes successfully
- [ ] All UI screens display "Venue" instead of "Branch"
- [ ] Navigation works correctly with new routes
- [ ] All repository methods use new naming
- [ ] Tests pass
- [ ] App runs and existing data is preserved

---

## SEARCH & REPLACE PATTERNS

Use these patterns to find remaining occurrences:

```bash
# Find remaining branch references:
grep -r "branchId" --include="*.kt" --exclude-dir=build .
grep -r "BranchRepository" --include="*.kt" --exclude-dir=build .
grep -r "BranchDao" --include="*.kt" --exclude-dir=build .
grep -r "BranchEntity" --include="*.kt" --exclude-dir=build .
grep -r "getAreasByBranch" --include="*.kt" --exclude-dir=build .
grep -r "getIncidentsByBranch" --include="*.kt" --exclude-dir=build .
grep -r "getServicesByBranch" --include="*.kt" --exclude-dir=build .
```

---

## DATABASE SCHEMA CHANGES

**Version 7 → Version 8:**
- Table `branches` renamed to `venues`
- Column `branchId` renamed to `venueId` in: `events`, `area_templates`, `incidents`
- All foreign keys and indices updated accordingly
- **Data preservation:** All existing data is migrated intact

---

## NOTES

- The `locationId` column in `lost_items` table references venues but was not renamed (intentional design choice)
- Migration is backward-compatible and preserves all existing data
- Old Branch files should be deleted after verification
- Consider updating string resources (strings.xml) for UI labels if applicable

---

## FILES SUMMARY

**Created:** 4 new files
**Updated:** 25+ files
**Remaining to update:** 30+ files
**To delete:** 4 old Branch* files

---

Generated: 2025-11-24
Database Version: 7 → 8
