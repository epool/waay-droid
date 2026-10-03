package dev.epool.waay.core.i18n

import dev.epool.waay.core.speech.SpeechLanguage
import dev.epool.waay.settings.domain.LanguageChoice

/** Resolves the active app language and speech voice (FR-020–FR-022, data-model.md AppLanguage). */
internal object LanguageResolver {
    /** Any Spanish device locale (`es`, `es-MX`, `es-ES`, …) means Spanish; everything else, English. */
    fun resolve(
        choice: LanguageChoice,
        device: SpeechLanguage,
    ): AppLanguage =
        when (choice) {
            LanguageChoice.English -> AppLanguage.English
            LanguageChoice.Spanish -> AppLanguage.Spanish
            LanguageChoice.Device -> if (device.languageCode.equals("es", ignoreCase = true)) AppLanguage.Spanish else AppLanguage.English
        }

    /** Prefers the device's regional variant when its language matches (e.g. `es-MX`), else a default region. */
    fun speechLanguage(
        language: AppLanguage,
        device: SpeechLanguage,
    ): SpeechLanguage {
        val code = language.code
        val region = if (device.languageCode.equals(code, ignoreCase = true)) device.regionCode else language.defaultRegion
        return SpeechLanguage(code, region)
    }

    private val AppLanguage.code: String
        get() =
            when (this) {
                AppLanguage.English -> "en"
                AppLanguage.Spanish -> "es"
            }

    private val AppLanguage.defaultRegion: String
        get() =
            when (this) {
                AppLanguage.English -> "US"
                AppLanguage.Spanish -> "MX"
            }
}
