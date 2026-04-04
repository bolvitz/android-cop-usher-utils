# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Event Monitor** is a native Android app for managing events across multiple venues with features like head counting, lost & found tracking, and incident reporting. Previously church-specific, it has been refactored into a general-purpose event management tool.

**Package**: `com.eventmonitor.app`
**Database**: `event_monitor_db` (Room, currently v8)
**Min SDK**: 26 (Android 8.0) | **Target SDK**: 35 (Android 15)

## Build Commands

```bash
# Build the app
./gradlew build

# Build debug APK
./gradlew assembleDebug

# Build release APK (with minification)
./gradlew assembleRelease

# Run all tests
./gradlew test

# Run unit tests only
./gradlew testDebugUnitTest

# Run instrumentation tests (requires device/emulator)
./gradlew connectedDebugAndroidTest

# Run lint checks
./gradlew lint

# Clean build
./gradlew clean
```

## Architecture

### Modular Structure (Clean Architecture + MVVM)

```
app/                              # Main application module
├── presentation/
│   ├── MainActivity.kt           # Entry point with NavGraph
│   ├── navigation/               # NavGraph, Screen sealed class
│   ├── screens/
│   │   ├── venues/              # Venue (location/branch) management
│   │   └── settings/            # Settings and global features
│   └── viewmodels/              # App-level ViewModels (EventTypeManagement)

core/
├── common/                       # Shared UI components and theme
│   ├── theme/                   # Material 3 theme, colors, typography
│   └── utils/                   # Animations, haptic feedback
├── data/                        # Data layer (offline-first)
│   ├── local/
│   │   ├── database/            # AppDatabase, Converters
│   │   ├── dao/                 # Room DAOs (8 total)
│   │   └── entities/            # Room entities (8 total)
│   └── repository/              # Repository implementations + interfaces
└── domain/                      # Domain models and business logic
    ├── models/                  # EventType, ZoneType, ItemCategory, UserRole, etc.
    └── common/                  # Result wrapper, AppError

feature/                         # Feature modules (UI + ViewModels)
├── headcounter/                 # Head counting with undo/redo
├── lostandfound/               # Lost & found item tracking with photos
└── incidents/                  # Incident reporting and management
```

### Key Architecture Patterns

1. **Repository Pattern**: All data access goes through repository interfaces defined in `core/data/repository/interfaces/`. Implementations are in `core/data/repository/`.

2. **Dependency Injection**: Hilt is used throughout. Repository bindings are in `app/di/RepositoryModule.kt`.

3. **Navigation**: Type-safe navigation using sealed class `Screen` in `app/presentation/navigation/Screen.kt`. NavGraph is in `NavGraph.kt`.

4. **Database**: Room with Flow-based reactive queries. Schema exported to `core/data/schemas/`. Current version is 8.

5. **State Management**: ViewModels use StateFlow/SharedFlow. Feature modules have their own ViewModels with `@HiltViewModel`.

## Database Schema

### Core Entities (8 total)

- **VenueEntity**: Locations/branches (name, address, code)
- **AreaTemplateEntity**: Zone configurations per venue (seating areas, VIP, etc.)
- **EventEntity**: Event sessions with metadata
- **EventTypeEntity**: Configurable event types (Conference, Workshop, etc.)
- **AreaCountEntity**: Individual area head counts with timestamps
- **UserEntity**: User information for multi-user support
- **LostItemEntity**: Lost & found items with photo URIs and status tracking
- **IncidentEntity**: Incident reports with severity and status

### Database Migrations

Room migrations are defined in the database module. When adding migrations:
- Export schema to `core/data/schemas/` by incrementing version in `@Database`
- Add migration logic in database initialization
- Test with existing data before release

## Tech Stack

- **Language**: Kotlin 2.0.21
- **UI**: Jetpack Compose (Material 3) with Compose BOM 2024.11.00
- **Database**: Room 2.6.1
- **DI**: Hilt 2.51.1
- **Async**: Coroutines 1.9.0 + Flow
- **Navigation**: Compose Navigation 2.8.4
- **Build**: Gradle 8.7.3 with KSP 2.0.21-1.0.28
- **Image Loading**: Coil 2.7.0
- **Charts**: Vico 2.0.0-alpha.28
- **CSV Export**: OpenCSV 5.9
- **Testing**: JUnit 4, Mockk, Turbine, Coroutines Test

## Development Notes

### Adding New Features

1. **New Feature Module**: Create under `feature/` with standard structure (screens/, ViewModels). Add to `settings.gradle.kts` and depend on core modules.

2. **New Entity**: Add to `core/data/local/entities/`, create DAO in `dao/`, update `AppDatabase`, increment version, add migration.

3. **New Repository**: Interface in `core/data/repository/interfaces/`, implementation in `core/data/repository/`, bind in `app/di/RepositoryModule.kt`.

4. **New Screen**: Add route to `Screen` sealed class, add composable to `NavGraph.kt`, create ViewModel with `@HiltViewModel`.

### Testing

- Unit tests go in module's `test/` directory
- Instrumentation tests in `androidTest/`
- Room schema tests can use exported schemas in `core/data/schemas/`
- Mock repositories available in `app/src/test/kotlin/.../repositories/`

### Code Style

- Use wildcard imports (already configured in modules)
- Follow Material 3 design guidelines
- ViewModels expose StateFlow/SharedFlow for UI state
- Use `Result` wrapper from `core/domain/common/` for error handling

### Gradle Optimization

- Configuration cache enabled: `org.gradle.configuration-cache=true`
- Build cache enabled: `org.gradle.caching=true`
- Parallel builds: `org.gradle.parallel=true`

## Refactoring History

This app was refactored from a church-specific tool to a general event monitor:
- Old: `ServiceType` → New: `EventType`
- Old: `AreaType` → New: `ZoneType`
- Old: `BranchEntity` → New: `VenueEntity`
- Old: `ServiceEntity` → New: `EventEntity`

See `REFACTORING_NOTES.md` and `VENUE_REFACTORING_SUMMARY.md` for complete migration details.

## Known Issues & TODOs

- Firebase cloud sync is planned but not yet implemented (dependencies commented out in `app/build.gradle.kts`)
- Some UI strings may still reference old terminology - update as encountered
- Advanced analytics and charts planned for V2
