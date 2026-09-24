# Event Monitor — iOS (SwiftUI)

Native SwiftUI front end for the shared Kotlin Multiplatform module
(`:shared`). All data (offline-first Room database), repositories and
ViewModels live in `:shared`; SwiftUI is a thin view layer that binds to
the shared `SharedViewModel`s.

## Prerequisites

- macOS with Xcode 15+ (deployment target iOS 17)
- JDK 17 (for the Gradle framework build)
- [XcodeGen](https://github.com/yonyz/XcodeGen) (`brew install xcodegen`)

## Generate the Xcode project

The `.xcodeproj` is generated from `project.yml` so it never has to be
committed or hand-edited:

```bash
cd iosApp
xcodegen generate
open iosApp.xcodeproj
```

Then select the `iosApp` scheme and run on a simulator or device.

## How the shared framework is wired

- `project.yml` adds a pre-build script that runs
  `./gradlew :shared:embedAndSignAppleFrameworkForXcode`, which produces and
  embeds `shared.framework` for the active configuration/SDK/arch.
- Swift imports it with `import shared`.
- Koin is started in `iOSApp.init()` via `KoinIosKt.startKoinIos()`.
- ViewModels are resolved through helper factories in `KoinIos.kt`
  (`venueListViewModel()`, `eventTypeManagementViewModel()`).

## State bridging

`kotlinx-coroutines` is not exported, so a Kotlin `Flow<T>` appears in Swift
as an opaque `Kotlinx_coroutines_coreFlow` with `Any?` emissions. Each feature
exposes an `ObservableObject` that owns a `ViewModelLifetime`
(`Common/FlowObserver.swift`): it seeds from `viewModel.uiState.value`, binds
flows with `lifetime.observe(flow, as: T.self) { ... }`, and calls the shared
ViewModel's `dispose()` on deinit so its coroutines are cancelled. New shared
ViewModels need a factory in `shared/src/iosMain/.../di/KoinIos.kt`.

Interop names to watch: `template` bridges as `template_`, `description` as
`description_`, and the DTO `EventWithDetails` as `EventWithDetails_` (it
clashes with the Room relation class of the same name).

## Tests

```bash
# Shared logic on the iOS target (in-memory Room)
./gradlew :shared:iosSimulatorArm64Test

# UI smoke test: venue -> area -> count -> undo/redo -> lock -> history
cd iosApp && xcodegen generate
xcodebuild -project iosApp.xcodeproj -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 16' test
```

## Releasing to the App Store

1. Set `DEVELOPMENT_TEAM` in `Configuration/Config.xcconfig`.
2. Bump `CURRENT_PROJECT_VERSION` (every upload) and `MARKETING_VERSION`
   (each user-facing release) in the same file.
3. `xcodegen generate`, then in Xcode: Product > Archive > Distribute App.

Already in place: 1024px app icon, `PrivacyInfo.xcprivacy` (no tracking, no
data collection), `ITSAppUsesNonExemptEncryption = NO`, iPad orientations.
