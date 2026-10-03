package dev.epool.waay.settings.presentation

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.russhwolf.settings.MapSettings
import dev.epool.waay.core.i18n.StringsProvider
import dev.epool.waay.fakes.MainDispatcherTest
import dev.epool.waay.settings.data.KeyValuePreferencesDataSource
import kotlinx.coroutines.flow.first
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

    // S3: the voice toggle is persisted and reflected in the state (FR-014, FR-023).
    @Test
    fun voiceToggleIsPersistedAndReflected() =
        runTest {
            val settings = MapSettings()
            val viewModel = viewModel(settings)
            viewModel.state.test {
                val initial = awaitItem()
                assertThat(initial.voiceLabel).isEqualTo("Magician's voice")
                assertThat(initial.voiceEnabled).isTrue()

                viewModel.onAction(SettingsAction.OnVoiceToggle(enabled = false))

                assertThat(awaitItem().voiceEnabled).isFalse()
            }
            assertThat(KeyValuePreferencesDataSource(settings).preferences.first().voiceEnabled).isFalse()
        }
}
