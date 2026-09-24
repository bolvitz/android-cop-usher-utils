package com.eventmonitor.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Base class for all shared ViewModels.
 *
 * On Android these are used directly as androidx [ViewModel]s (their
 * [viewModelScope] is cancelled by the framework). On iOS, SwiftUI cannot
 * collect a Kotlin flow directly, so [observe] bridges any flow to a Swift
 * closure and returns a [Cancellable]. iOS owners must call [dispose] when the
 * screen goes away so the ViewModel's scope is cancelled.
 */
abstract class SharedViewModel : ViewModel() {

    /**
     * Observe [flow], delivering every emission to [onEach] on the ViewModel
     * scope (main thread). Returns a [Cancellable]; call [Cancellable.cancel] to stop.
     */
    fun <T> observe(flow: Flow<T>, onEach: (T) -> Unit): Cancellable {
        val job: Job = viewModelScope.launch {
            flow.collect { onEach(it) }
        }
        return Cancellable(job)
    }

    /**
     * Clears this ViewModel (cancels [viewModelScope], runs onCleared). For iOS only;
     * Android ViewModels are cleared by their ViewModelStoreOwner.
     */
    fun dispose() {
        ViewModelStore().apply {
            put(DISPOSE_KEY, this@SharedViewModel)
            clear()
        }
    }

    private companion object {
        const val DISPOSE_KEY = "com.eventmonitor.shared.dispose"
    }
}

/** Swift-friendly handle around a collecting coroutine [Job]. */
class Cancellable(private val job: Job) {
    fun cancel() {
        job.cancel()
    }
}
