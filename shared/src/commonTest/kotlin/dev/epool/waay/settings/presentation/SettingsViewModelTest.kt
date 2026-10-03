package dev.epool.waay.settings.presentation

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.russhwolf.settings.MapSettings
import dev.epool.waay.core.i18n.StringsProvider
import dev.epool.waay.fakes.FakeDeviceLocale
import dev.epool.waay.fakes.MainDispatcherTest
import dev.epool.waay.settings.data.KeyValuePreferencesDataSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class SettingsViewModelTest : MainDispatcherTest() {
    private fun viewModel(settings: MapSettings = MapSettings()) =
        SettingsViewModel(
            preferencesDataSource = KeyValuePreferencesDataSource(settings),
            stringsProvider = StringsProvider(FakeDeviceLocale()),
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

    // S2: card count selection is persisted for 3–7 inclusive; out-of-range values are ignored (FR-017).
    @Test
    fun cardCountSelectionIsPersistedWithinThreeToSeven() =
        runTest {
            val settings = MapSettings()
            val viewModel = viewModel(settings)
            viewModel.state.test {
                val initial = awaitItem()
                assertThat(initial.cardCountLabel).isEqualTo("Number of cards")
                assertThat(initial.cardCountOptions.map { it.value }).isEqualTo(listOf(3, 4, 5, 6, 7))
                assertThat(initial.cardCountOptions.first { it.value == 5 }.label).isEqualTo("5 cards (1–31)")
                assertThat(initial.selectedCardCount).isEqualTo(5)

                viewModel.onAction(SettingsAction.OnCardCountSelect(7))
                assertThat(awaitItem().selectedCardCount).isEqualTo(7)

                viewModel.onAction(SettingsAction.OnCardCountSelect(9))
                expectNoEvents()
            }
            assertThat(
                KeyValuePreferencesDataSource(settings)
                    .preferences
                    .first()
                    .cardCount.value,
            ).isEqualTo(7)
        }

    // S4: choosing a language switches every label immediately and is persisted (FR-021).
    @Test
    fun languageSelectionSwitchesLabelsImmediately() =
        runTest {
            val settings = MapSettings()
            val viewModel = viewModel(settings)
            viewModel.state.test {
                val initial = awaitItem()
                assertThat(initial.selectedLanguage).isEqualTo(LanguageChoiceUi.Device)
                assertThat(initial.languageOptions.map { it.choice })
                    .isEqualTo(listOf(LanguageChoiceUi.Device, LanguageChoiceUi.English, LanguageChoiceUi.Spanish))
                assertThat(initial.languageOptions.map { it.label }).isEqualTo(listOf("Device language", "English", "Español"))

                viewModel.onAction(SettingsAction.OnLanguageSelect(LanguageChoiceUi.Spanish))

                val spanish = awaitItem()
                assertThat(spanish.title).isEqualTo("Ajustes")
                assertThat(spanish.languageLabel).isEqualTo("Idioma")
                assertThat(spanish.selectedLanguage).isEqualTo(LanguageChoiceUi.Spanish)
            }
            assertThat(
                KeyValuePreferencesDataSource(settings)
                    .preferences
                    .first()
                    .languageChoice.key,
            ).isEqualTo("es")
        }
}
