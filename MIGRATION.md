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
  iOS `observe(flow:onEach:)` bridge. Migrated `VenueListViewModel`,
  `EventTypeManagementViewModel`, and the full head-counter `CountingViewModel`
  (with a `SeatMapRepository`); registered in `viewModelModule`.
- **Phase 4 — Android rewire / Hilt removal.** **Hilt is fully removed**; Koin
  is the single DI container. `EventMonitorApp.onCreate()` starts Koin with the
  shared modules plus `legacyDataModule` + `legacyViewModelModule` (the former
  Hilt `DatabaseModule`/`RepositoryModule`, now Koin: legacy `core/data` Room DB
  with its real migrations, DAOs, repo bindings, and the remaining Android
  ViewModels via `viewModelOf`). All 16 `@HiltViewModel`s became plain classes;
  all screens use `koinViewModel()`. The shared Room DB uses a distinct filename
  so it doesn't collide with the legacy DB. Head counter, incidents and lost &
  found are cut over to shared ViewModels via `koinViewModel { parametersOf(…) }`.
- **Phase 5 (scaffold) — SwiftUI app.** `iosApp/` with `@main` app, Koin start,
  `ContentView` tabs (Venues, Event Types, Incidents, Lost & Found), plus
  `HeadCounterView` reached from the venue list — all bound to shared
  ViewModels. Reproducible Xcode project via `project.yml` (XcodeGen) with the
  Kotlin framework embed phase.

### Remaining (mechanical, follows established patterns)

- **Phase 2 cont.** Add Room-backed repos for the remaining domains:
  `Area`, `User` (`SeatMap`, `Incident`, `LostItem` are done; interfaces exist
  in the Android `core/data` to mirror).
- **Phase 3 cont.** Port the remaining ViewModels into `:shared/presentation`
  using the `SharedViewModel` + DTO-repository pattern: head counter
  (`History`, `Trends`, `SeatMapDemo`), lost-item & incident add/edit & detail,
  plus `VenueSetup`/`VenueManagement`/`AreaManagement`/`Reports`.
- **Phase 4 cont. — finish the data unification.** Hilt is gone, but the legacy
  Android ViewModels still resolve `core/data` repositories (Koin-bound). Port
  the remaining ViewModels to the shared DTO repositories, then delete
  `core/data` / `core/domain` and the `legacy*Module`s, leaving a single Room DB
  (and migrate any existing legacy DB data into it).
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

## Single-DB entry point (keystone done) + remaining regressions

The app now **starts at `SharedVenueListScreen`**, and the entire primary flow
runs on the shared KMP database: create venue → manage area templates → head
count (seeds area counts) → history / trends, plus per-venue incidents and lost
& found, and global event types. All of these read/write the **shared** DB, so
there is no split-brain along that path.

The legacy `core/data` database and its Android screens still compile (and are
registered in Koin) but are **no longer reachable from the new entry point**.
That means a few legacy-only features regress until they are ported to the
shared repositories (the shared DAOs already support them):

- **Seat-map editor** (`ZoneEditor`) — seat rows/seats/per-event statuses
- **Reports** (CSV export)
- **Incident / lost-item detail & add-edit** screens (the shared list screens
  cover create + status + claim, but not the full detail/edit forms)
- **Advanced venue setup** (logo, colour, contact, feature toggles)

Once these are ported, delete `core/data` / `core/domain` and the
`legacy*Module`s, and add a one-time migration of any existing legacy-DB data
into the shared DB.

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
