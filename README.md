# Church Attendance Counter - Android App

A native Android application for tracking church service attendance across multiple church branches, with configurable areas per branch.

## Features

### Implemented (V1)
- ✅ Multi-branch management
- ✅ Dynamic area configuration per branch
- ✅ Offline-first with Room Database
- ✅ Service attendance counting with real-time totals
- ✅ Undo/Redo functionality for count changes
- ✅ Export service reports
- ✅ Branch comparison reports
- ✅ Material Design 3 UI with dynamic colors
- ✅ MVVM + Clean Architecture

### Coming Soon (V2)
- ⏳ Firebase cloud sync
- ⏳ Advanced analytics and charts
- ⏳ Service history with filtering
- ⏳ Settings and preferences
- ⏳ User roles and permissions

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material Design 3)
- **Architecture**: MVVM with Clean Architecture
- **Database**: Room Database
- **Dependency Injection**: Hilt
- **Async**: Kotlin Coroutines + Flow
- **Navigation**: Jetpack Compose Navigation
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 35 (Android 15)

## Project Structure

```
app/
├── data/
│   ├── local/
│   │   ├── dao/          # Room DAOs
│   │   ├── entities/     # Room Entities
│   │   └── database/     # Database & Converters
│   ├── repository/       # Repository implementations
│   └── models/           # Data models
├── domain/
│   ├── models/           # Domain models & enums
│   └── repository/       # Repository interfaces
├── presentation/
│   ├── screens/          # Composable screens
│   ├── navigation/       # Navigation setup
│   ├── theme/            # Material 3 theming
└── di/                   # Hilt modules
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
2. Tap the "+" button to add your first branch
3. Fill in branch details (name, location, code)
4. Set the number of bays (default: 6)
5. Tap on a branch to start counting attendance

## Database Schema

### Entities
- **BranchEntity**: Church branch information
- **AreaTemplateEntity**: Area configurations per branch
- **ServiceEntity**: Service session data
- **AreaCountEntity**: Individual area attendance counts
- **UserEntity**: User information (for future multi-user support)

## Version History

### v1.0.0 (Current)
- Initial release
- Multi-branch management
- Attendance counting
- Basic reporting
- Offline support

---

Built with ❤️ for churches using modern Android development practices
