package dev.epool.waay.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.epool.waay.core.domain.Result
import dev.epool.waay.core.i18n.Strings
import dev.epool.waay.core.i18n.StringsProvider
import dev.epool.waay.game.domain.CardCount
import dev.epool.waay.settings.domain.Preferences
import dev.epool.waay.settings.domain.PreferencesDataSource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Thin MVI adapter over [PreferencesDataSource] (contracts/settings-viewmodel.md). */
public class SettingsViewModel internal constructor(
    private val preferencesDataSource: PreferencesDataSource,
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
            is SettingsAction.OnVoiceToggle -> viewModelScope.launch { preferencesDataSource.setVoiceEnabled(action.enabled) }
            is SettingsAction.OnCardCountSelect -> selectCardCount(action.value)
        }
    }

    private fun Preferences.toSettingsState(): SettingsState {
        val strings: Strings = stringsProvider.stringsFor(languageChoice)
        return SettingsState(
            title = strings.settingsTitle,
            backLabel = strings.backLabel,
            voiceLabel = strings.voiceLabel,
            voiceEnabled = voiceEnabled,
            cardCountLabel = strings.cardCountLabel,
            cardCountOptions = CardCount.all.map { CardCountOptionUi(it.value, strings.cardCountOption(it.value, it.maxNumber)) },
            selectedCardCount = cardCount.value,
        )
    }

    /** Only 3–7 are accepted; anything else is ignored (FR-017). */
    private fun selectCardCount(value: Int) {
        val cardCount = (CardCount.of(value) as? Result.Success)?.data ?: return
        viewModelScope.launch { preferencesDataSource.setCardCount(cardCount) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
