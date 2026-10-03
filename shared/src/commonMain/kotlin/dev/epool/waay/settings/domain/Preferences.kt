package dev.epool.waay.settings.domain

import dev.epool.waay.game.domain.CardCount

/** Persisted player preferences (FR-023); the defaults are the first-use values (FR-024). */
internal data class Preferences(
    val cardCount: CardCount = CardCount.DEFAULT,
    val voiceEnabled: Boolean = true,
    val languageChoice: LanguageChoice = LanguageChoice.Device,
)
