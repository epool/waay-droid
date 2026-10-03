package dev.epool.waay.core.i18n

import dev.epool.waay.core.speech.SpeechLanguage
import dev.epool.waay.settings.domain.LanguageChoice

/**
 * Picks the [Strings] catalog for the player's [LanguageChoice].
 * English-only until US5 (T079) makes it language-aware (analyze finding U2).
 */
internal class StringsProvider {
    @Suppress("UNUSED_PARAMETER")
    fun stringsFor(choice: LanguageChoice): Strings = EnglishStrings

    /** Voice language for speech (FR-022). English-only until US5 (T079). */
    @Suppress("UNUSED_PARAMETER")
    fun speechLanguageFor(choice: LanguageChoice): SpeechLanguage = SpeechLanguage("en", null)
}
