package dev.epool.waay.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.epool.waay.core.i18n.Strings
import dev.epool.waay.core.i18n.StringsProvider
import dev.epool.waay.settings.domain.Preferences
import dev.epool.waay.settings.domain.PreferencesDataSource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn

/** Thin MVI adapter over [PreferencesDataSource] (contracts/settings-viewmodel.md). */
public class SettingsViewModel internal constructor(
    preferencesDataSource: PreferencesDataSource,
    private val stringsProvider: StringsProvider,
) : ViewModel() {
    public val state: StateFlow<SettingsState> =
        preferencesDataSource.preferences
            .map { it.toSettingsState() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = Preferences().toSettingsState(),
            )

    private val eventChannel = Channel<SettingsEvent>(Channel.BUFFERED)
    public val events: Flow<SettingsEvent> = eventChannel.receiveAsFlow()

    public fun onAction(action: SettingsAction) {
        when (action) {
            SettingsAction.OnBackClick -> eventChannel.trySend(SettingsEvent.NavigateBack)
        }
    }

    private fun Preferences.toSettingsState(): SettingsState = stringsProvider.stringsFor(languageChoice).toSettingsState()

    private fun Strings.toSettingsState(): SettingsState = SettingsState(title = settingsTitle, backLabel = backLabel)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
