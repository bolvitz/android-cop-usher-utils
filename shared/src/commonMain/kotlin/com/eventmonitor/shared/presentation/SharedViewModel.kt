package com.eventmonitor.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Base class for all shared ViewModels.
 *
 * On Android these are used directly as androidx [ViewModel]s (their
 * [viewModelScope] is cancelled by the framework). On iOS, SwiftUI cannot
 * collect a Kotlin [StateFlow] directly, so [observe] bridges any state flow to
 * a Swift closure and returns a [Cancellable] the view stores and cancels in
 * `deinit` to stop observing.
 */
abstract class SharedViewModel : ViewModel() {

    /**
     * Observe [flow], delivering every emission to [onEach] on the ViewModel
     * scope. Returns a [Cancellable]; call [Cancellable.cancel] to stop.
     */
    fun <T> observe(flow: StateFlow<T>, onEach: (T) -> Unit): Cancellable {
        val job: Job = viewModelScope.launch {
            flow.collect { onEach(it) }
        }
        return Cancellable(job)
    }
}

/** Swift-friendly handle around a collecting coroutine [Job]. */
class Cancellable(private val job: Job) {
    fun cancel() {
        job.cancel()
    }
}
