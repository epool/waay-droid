package dev.epool.waay.core.i18n

import dev.epool.waay.core.locale.DeviceLocale
import dev.epool.waay.core.speech.SpeechLanguage
import dev.epool.waay.settings.domain.LanguageChoice

/**
 * Resolves the player's [LanguageChoice] plus the current device locale into the [Strings] catalog
 * and the speech voice language (FR-020–FR-022). The device locale is read on every call, so a
 * device language change is picked up on the next update.
 */
internal class StringsProvider(
    private val deviceLocale: DeviceLocale,
) {
    fun stringsFor(choice: LanguageChoice): Strings =
        when (LanguageResolver.resolve(choice, deviceLocale.current())) {
            AppLanguage.English -> EnglishStrings
            AppLanguage.Spanish -> SpanishStrings
        }

    fun speechLanguageFor(choice: LanguageChoice): SpeechLanguage {
        val device = deviceLocale.current()
        return LanguageResolver.speechLanguage(LanguageResolver.resolve(choice, device), device)
    }
}
