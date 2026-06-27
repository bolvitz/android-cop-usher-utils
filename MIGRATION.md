# KMP + SwiftUI Migration

This document tracks the migration of **Event Monitor** from a single Android
app into a Kotlin Multiplatform project with a **native SwiftUI** front end on
iOS. It records the architecture decisions, what has shipped, and the
remaining (largely mechanical) work.

## Goal & decisions

| Decision | Choice |
| --- | --- |
| Architecture | KMP shared module (`:shared`); Android keeps Compose, iOS uses native SwiftUI |
| Persistence | **Room KMP** (offline-first) in `commonMain`, bundled SQLite driver |
| Logic location | Models **+ repositories + ViewModels** all live in `:shared`; UI is thin per platform |
| DI | Koin (already used by `:shared`) |
| iOS state bridging | `SharedViewModel.observe(flow:onEach:)` → `Cancellable` (no SKIE yet) |

## Target module layout

```
:shared (KMP: androidTarget + iosX64/iosArm64/iosSimulatorArm64)
  commonMain
    data/local/entities      11 Room @Entity + relations
    data/local/dao           11 DAOs
    data/local/database      AppDatabase (@ConstructedBy expect), Converters
    data/models              DTOs (serializable, UI-facing)
    data/mappers             entity <-> DTO
    data/repository          interfaces + Room-backed impls
    domain                   Result/AppError, domain enums
    presentation             SharedViewModel + feature ViewModels
    di                       SharedModule (dataModule, viewModelModule), expect platformModule
  androidMain                DB builder (Context), platformModule actual
  iosMain                    DB builder (NSDocumentDirectory), platformModule actual, KoinIos
app/        Android Compose UI  (consumes :shared)
iosApp/     SwiftUI app         (consumes shared.framework)
```

## Status

### Done

- **Phase 1 — Room-KMP data foundation.** All 11 entities, relations, 11 DAOs,
  TypeConverters and the `AppDatabase` moved to `commonMain`. JVM-only
  `java.util.UUID` / `System.currentTimeMillis` replaced with multiplatform
  `util/Identifiers.kt` (`kotlin.uuid.Uuid`, `kotlinx-datetime`). Platform DB
  builders for Android (Context) and iOS (`NSDocumentDirectory`) use the
  bundled SQLite driver. Gradle wired for `androidx.room` + KSP across all
  targets; GitLive Firebase removed.
- **Phase 2 — Repositories.** Firebase/Firestore repo impls replaced with
  Room-backed implementations (`Venue`, `Event`, `EventType`, `AreaCount`),
  mapping entity↔DTO. Koin `dataModule` exposes DAOs + repositories;
  `platformModule` provides `AppDatabase` per platform.
- **Phase 3 (started) — Shared ViewModels.** `SharedViewModel` base with the
  iOS `observe(flow:onEach:)` bridge. Migrated `VenueListViewModel` and
  `EventTypeManagementViewModel`; registered in `viewModelModule`.
- **Phase 5 (scaffold) — SwiftUI app.** `iosApp/` with `@main` app, Koin start,
  `ContentView` tabs, and two feature screens (`VenueListView`,
  `EventTypeManagementView`) bound to shared ViewModels. Reproducible Xcode
  project via `project.yml` (XcodeGen) with the Kotlin framework embed phase.

### Remaining (mechanical, follows established patterns)

- **Phase 2 cont.** Add Room-backed repos for the remaining domains:
  `Incident`, `LostItem`, `Area`, `SeatMap`, `User` (interfaces exist in the
  Android `core/data`; mirror them in `:shared`).
- **Phase 3 cont.** Port the remaining ViewModels into `:shared/presentation`
  using the `SharedViewModel` + DTO-repository pattern: head counter
  (`Counting`, `History`, `Trends`, `SeatMapDemo`), `LostAndFound`,
  `Incidents`, plus `VenueSetup`/`VenueManagement`/`AreaManagement`/`Reports`.
- **Phase 4 — Android rewire.** Point `:app` and `feature/*` at `:shared`,
  replace Hilt with Koin (or bridge Hilt→Koin), and delete `core/data` /
  `core/domain` once nothing references them. The Android app currently still
  builds on its own `core/data` Room stack and is unaffected until this step.
- **Phase 5 cont.** Add SwiftUI screens for the remaining features and a
  shared design system; wire navigation.

## Building & verification

> ⚠️ The environment this migration was authored in blocks `dl.google.com`
> egress (Google Maven) and has no Xcode, so neither the Android nor the iOS
> build could be compiled here. Build on CI / a dev machine.

```bash
# Shared (Android target) + app
./gradlew :shared:compileDebugKotlinAndroid
./gradlew :app:assembleDebug

# Shared (iOS frameworks) — requires macOS
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64

# iOS app
cd iosApp && xcodegen generate && open iosApp.xcodeproj
```

## Notes & follow-ups

- **Migrations:** the shared `AppDatabase` is at schema v9 and currently uses
  `fallbackToDestructiveMigration`. Port the existing Android `Migrations.kt`
  to preserve existing-user data before shipping to Android.
- **`EventTypeDto`** is a simplified projection; `dayType`/`time` are preserved
  on update via the existing entity but are not part of the DTO. Widen the DTO
  if iOS needs to edit them.
- **SKIE:** adding [SKIE](https://skie.touchlab.co/) would let SwiftUI collect
  typed flows and use sealed/enum types directly, removing the `as!` casts in
  the iOS `ObservableObject`s.
