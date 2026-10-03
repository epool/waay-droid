package dev.epool.waay.di

import androidx.lifecycle.ViewModel
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isSameInstanceAs
import kotlin.test.Test

class ScreenScopeTest {
    private class ProbeViewModel : ViewModel() {
        var clearedCount = 0
            private set

        override fun onCleared() {
            clearedCount++
        }
    }

    @Test
    fun closeClearsOwnedViewModels() {
        val scope = ScreenScope()
        val viewModel = scope.obtain { ProbeViewModel() }
        assertThat(viewModel.clearedCount).isEqualTo(0)

        scope.close()

        assertThat(viewModel.clearedCount).isEqualTo(1)
    }

    @Test
    fun obtainReturnsTheSameInstanceUntilClosed() {
        val scope = ScreenScope()
        val first = scope.obtain { ProbeViewModel() }
        val second = scope.obtain { ProbeViewModel() }

        assertThat(second).isSameInstanceAs(first)
        assertThat(first.clearedCount).isEqualTo(0)
    }

    @Test
    fun closeIsIdempotent() {
        val scope = ScreenScope()
        val viewModel = scope.obtain { ProbeViewModel() }

        scope.close()
        scope.close()

        assertThat(viewModel.clearedCount).isEqualTo(1)
    }
}
