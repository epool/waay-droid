package dev.epool.waay.settings.presentation

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.russhwolf.settings.MapSettings
import dev.epool.waay.core.i18n.StringsProvider
import dev.epool.waay.fakes.MainDispatcherTest
import dev.epool.waay.settings.data.KeyValuePreferencesDataSource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class SettingsViewModelTest : MainDispatcherTest() {
    private fun viewModel(settings: MapSettings = MapSettings()) =
        SettingsViewModel(
            preferencesDataSource = KeyValuePreferencesDataSource(settings),
            stringsProvider = StringsProvider(),
        )

    // S1: reflects the persisted preferences (or defaults) with localized labels.
    @Test
    fun stateReflectsPreferencesWithLocalizedLabels() =
        runTest {
            viewModel().state.test {
                val state = awaitItem()
                assertThat(state.title).isEqualTo("Settings")
                assertThat(state.backLabel).isEqualTo("Back")
            }
        }

    // S5: OnBackClick emits NavigateBack exactly once.
    @Test
    fun backClickEmitsNavigateBackOnce() =
        runTest {
            val viewModel = viewModel()
            viewModel.events.test {
                viewModel.onAction(SettingsAction.OnBackClick)
                assertThat(awaitItem()).isEqualTo(SettingsEvent.NavigateBack)
                expectNoEvents()
            }
        }
}
