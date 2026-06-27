import Foundation
import shared

/// Notes on Kotlin <-> Swift state bridging
///
/// `kotlinx-coroutines` is not exported by the shared framework, so a Kotlin
/// `StateFlow<T>` surfaces in Swift as an opaque flow whose `.value` is `Any?`.
/// Each feature exposes an `ObservableObject` that:
///   1. resolves its `SharedViewModel` from Koin,
///   2. seeds `@Published state` from `viewModel.uiState.value`,
///   3. subscribes via `viewModel.observe(flow:onEach:)`, casting each emission,
///   4. cancels the returned `Cancellable` and calls `dispose()` in `deinit`.
///
/// See `VenueListView` / `EventTypeManagementView` for the concrete pattern.
/// (If you later add SKIE to the build, these casts can be replaced with typed
/// flow collection.)
enum FlowBridge {}
