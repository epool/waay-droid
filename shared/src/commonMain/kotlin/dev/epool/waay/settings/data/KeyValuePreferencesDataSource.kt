package dev.epool.waay.settings.data

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.getBooleanFlow
import com.russhwolf.settings.coroutines.getIntFlow
import com.russhwolf.settings.coroutines.getStringOrNullFlow
import dev.epool.waay.core.domain.Result
import dev.epool.waay.game.domain.CardCount
import dev.epool.waay.settings.domain.LanguageChoice
import dev.epool.waay.settings.domain.Preferences
import dev.epool.waay.settings.domain.PreferencesDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * [PreferencesDataSource] over a key-value store (multiplatform-settings, ADR-005):
 * SharedPreferences on Android, NSUserDefaults on iOS, `MapSettings` in tests.
 */
@OptIn(ExperimentalSettingsApi::class)
internal class KeyValuePreferencesDataSource(
    private val settings: ObservableSettings,
) : PreferencesDataSource {
    override val preferences: Flow<Preferences> =
        combine(
            settings.getIntFlow(KEY_CARD_COUNT, CardCount.DEFAULT.value),
            settings.getBooleanFlow(KEY_VOICE_ENABLED, defaultValue = true),
            settings.getStringOrNullFlow(KEY_LANGUAGE_CHOICE),
        ) { cardCount, voiceEnabled, languageKey ->
            Preferences(
                cardCount = (CardCount.of(cardCount) as? Result.Success)?.data ?: CardCount.DEFAULT,
                voiceEnabled = voiceEnabled,
                languageChoice = LanguageChoice.fromKey(languageKey),
            )
        }.distinctUntilChanged()

    override suspend fun setCardCount(value: CardCount) {
        settings.putInt(KEY_CARD_COUNT, value.value)
    }

    override suspend fun setVoiceEnabled(enabled: Boolean) {
        settings.putBoolean(KEY_VOICE_ENABLED, enabled)
    }

    override suspend fun setLanguageChoice(choice: LanguageChoice) {
        settings.putString(KEY_LANGUAGE_CHOICE, choice.key)
    }

    companion object {
        const val KEY_CARD_COUNT: String = "card_count"
        const val KEY_VOICE_ENABLED: String = "voice_enabled"
        const val KEY_LANGUAGE_CHOICE: String = "language_choice"
    }
}
