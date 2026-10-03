package dev.epool.waay.di

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Spike-only probe (T013) proving the ADR-001 iOS ownership pattern end to end.
 * Removed in T043 once the real game flow covers the same behaviour.
 */
public class LifecycleProbeViewModel internal constructor() : ViewModel() {
    private val _count = MutableStateFlow(0)
    public val count: StateFlow<Int> = _count.asStateFlow()

    public fun increment() {
        _count.update { it + 1 }
    }
}

/** Spike-only factory for [LifecycleProbeViewModel]. Removed in T043. */
public object SpikeProbes {
    public fun lifecycleProbe(scope: ScreenScope): LifecycleProbeViewModel = scope.obtain { LifecycleProbeViewModel() }
}
