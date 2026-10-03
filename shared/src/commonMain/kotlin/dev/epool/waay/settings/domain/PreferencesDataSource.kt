package dev.epool.waay.settings.domain

import dev.epool.waay.game.domain.CardCount
import kotlinx.coroutines.flow.Flow

/** Single-source preference store (contracts/platform-services.md). */
internal interface PreferencesDataSource {
    /** Emits the current value first, then every change. */
    val preferences: Flow<Preferences>

    suspend fun setCardCount(value: CardCount)

    suspend fun setVoiceEnabled(enabled: Boolean)

    suspend fun setLanguageChoice(choice: LanguageChoice)
}
