# Event Monitor — iOS (SwiftUI)

Native SwiftUI front end for the shared Kotlin Multiplatform module
(`:shared`). All data (offline-first Room database), repositories and
ViewModels live in `:shared`; SwiftUI is a thin view layer that binds to
the shared `SharedViewModel`s.

## Prerequisites

- macOS with Xcode 15+
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

`kotlinx-coroutines` is not exported, so a Kotlin `StateFlow<T>` appears in
Swift with an `Any?` `value`. Each feature exposes an `ObservableObject` that
seeds from `viewModel.uiState.value` and subscribes via
`viewModel.observe(flow:onEach:)`, casting each emission. See
`Common/FlowObserver.swift` for the rationale and the feature views for the
pattern. Adding SKIE later would let you collect typed flows directly.
