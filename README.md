# Event Monitor - Android App

A native Android application for monitoring events with features like head counting, lost & found management, and incident reporting across multiple locations.

## Features

### Implemented (V1)
- ✅ Multi-location/branch management
- ✅ Dynamic area configuration per location
- ✅ Offline-first with Room Database
- ✅ Event head counting with real-time totals
- ✅ Undo/Redo functionality for count changes
- ✅ Lost & Found item tracking
- ✅ Incident reporting and management
- ✅ Export event reports
- ✅ Location comparison reports
- ✅ Material Design 3 UI with dynamic colors
- ✅ Modular architecture with feature modules
- ✅ Clean Architecture + MVVM

### Coming Soon (V2)
- ⏳ Firebase cloud sync
- ⏳ Advanced analytics and charts
- ⏳ Enhanced filtering and search
- ⏳ Settings and preferences
- ⏳ User roles and permissions

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material Design 3)
- **Architecture**: Modular MVVM with Clean Architecture
- **Database**: Room Database
- **Dependency Injection**: Hilt
- **Async**: Kotlin Coroutines + Flow
- **Navigation**: Jetpack Compose Navigation
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 35 (Android 15)

## Modular Architecture

```
app/                          # Main app module
├── Branch & Area Management
├── Event Type Management
├── Settings & Reports
└── Main Navigation

core/
├── common/                  # Shared UI, theme, utilities
├── data/                    # Database, DAOs, repositories
└── domain/                  # Domain models, validation

feature/
├── headcounter/            # Head counting feature
├── lostandfound/           # Lost & found management
└── incidents/              # Incident reporting
```

## Getting Started

### Prerequisites
- Android Studio Hedgehog or later
- JDK 17
- Android SDK 35

### Building the App
1. Open the project in Android Studio
2. Sync Gradle files
3. Run the app on an emulator or physical device (Min SDK 26)

### First Time Setup
1. Launch the app
2. Tap the "+" button to add your first location/branch
3. Fill in location details (name, address, code)
4. Configure areas for the location
5. Add event types for your specific use case
6. Tap on a location to start counting attendance

## Database Schema

### Core Entities
- **BranchEntity**: Location/branch information
- **AreaTemplateEntity**: Area configurations per location
- **EventEntity**: Event session data
- **EventTypeEntity**: Configurable event types
- **AreaCountEntity**: Individual area head counts
- **LostItemEntity**: Lost & found item tracking
- **IncidentEntity**: Incident reports
- **UserEntity**: User information (for multi-user support)

## Use Cases

This app can be used for various events and venues:
- **Religious Services**: Churches, temples, mosques
- **Conferences**: Tracking attendance across rooms/halls
- **Concerts & Festivals**: Crowd management and safety
- **Sports Events**: Stadium section monitoring
- **Corporate Events**: Multi-room event tracking
- **Educational Institutions**: Campus event management

## Version History

### v1.0.0 (Current)
- Multi-location management
- Head counting with undo/redo
- Lost & found tracking
- Incident reporting
- Basic reporting and analytics
- Offline-first support
- Modular architecture

---

Built with modern Android development practices
