package dev.epool.waay.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import platform.Foundation.NSThread
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import androidx.lifecycle.ViewModelProvider as AndroidXViewModelProvider

/**
 * Owns the shared ViewModels of one iOS screen (ADR-001).
 *
 * Swift never sees `ViewModelStore` or generics: it creates a [ScreenScope], asks the Swift-facing
 * `ViewModelProvider` for ViewModels scoped to it, and calls [close] when the screen leaves the
 * hierarchy for good (from the screen model's `deinit`, never on `.task` cancellation).
 */
public class ScreenScope {
    private val store = ViewModelStore()

    /** Returns the [VM] owned by this scope, creating it with [create] on first use. */
    internal inline fun <reified VM : ViewModel> obtain(noinline create: () -> VM): VM {
        val factory = viewModelFactory { initializer { create() } }
        return AndroidXViewModelProvider.create(store, factory)[VM::class]
    }

    /**
     * Clears every owned ViewModel (runs `onCleared` and closeables). Idempotent and safe from any
     * thread: Swift calls it from a `deinit`, which may run off the main thread.
     */
    public fun close() {
        if (NSThread.isMainThread) {
            store.clear()
        } else {
            dispatch_async(dispatch_get_main_queue()) { store.clear() }
        }
    }
}
