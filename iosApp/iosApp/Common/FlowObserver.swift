import Foundation
import shared

/// Kotlin <-> Swift state bridging.
///
/// `kotlinx-coroutines` is not exported by the shared framework, so a Kotlin
/// `Flow<T>` surfaces in Swift as an opaque `Kotlinx_coroutines_coreFlow` whose
/// emissions are `Any?`. Each feature exposes an `ObservableObject` that owns a
/// `ViewModelLifetime`, seeds `@Published` state from `uiState.value`, and binds
/// flows with `observe(_:as:_:)`, which casts each emission.
///
/// When the owning model is deallocated, the lifetime disposes the shared
/// ViewModel, cancelling its `viewModelScope` (and every observation with it).
final class ViewModelLifetime {
    private let viewModel: SharedViewModel

    init(_ viewModel: SharedViewModel) {
        self.viewModel = viewModel
    }

    /// Delivers each emission of `flow` that casts to `T` to `onEach` on the main thread.
    @discardableResult
    func observe<T>(_ flow: Kotlinx_coroutines_coreFlow, as _: T.Type, _ onEach: @escaping (T) -> Void) -> Cancellable {
        viewModel.observe(flow: flow) { value in
            if let typed = value as? T {
                onEach(typed)
            }
        }
    }

    deinit {
        viewModel.dispose()
    }
}

/// Current wall-clock time in epoch millis, as the shared module stores dates.
func nowEpochMillis() -> Int64 {
    Int64(Date().timeIntervalSince1970 * 1000)
}

extension Date {
    init(epochMillis: Int64) {
        self.init(timeIntervalSince1970: TimeInterval(epochMillis) / 1000)
    }
}
