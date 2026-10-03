package dev.epool.waay.core.i18n

import assertk.assertThat
import assertk.assertions.isEqualTo
import dev.epool.waay.core.speech.SpeechLanguage
import dev.epool.waay.settings.domain.LanguageChoice
import kotlin.test.Test

/** FR-020 / FR-022: device-following default, explicit override, and the matching voice. */
class LanguageResolverTest {
    private val spanishMexico = SpeechLanguage("es", "MX")
    private val french = SpeechLanguage("fr", "FR")
    private val britishEnglish = SpeechLanguage("en", "GB")

    @Test
    fun deviceChoiceFollowsSpanishDevicesAndFallsBackToEnglish() {
        assertThat(LanguageResolver.resolve(LanguageChoice.Device, spanishMexico)).isEqualTo(AppLanguage.Spanish)
        assertThat(LanguageResolver.resolve(LanguageChoice.Device, SpeechLanguage("es", null))).isEqualTo(AppLanguage.Spanish)
        assertThat(LanguageResolver.resolve(LanguageChoice.Device, french)).isEqualTo(AppLanguage.English)
        assertThat(LanguageResolver.resolve(LanguageChoice.Device, britishEnglish)).isEqualTo(AppLanguage.English)
    }

    @Test
    fun explicitChoiceWinsOverTheDevice() {
        assertThat(LanguageResolver.resolve(LanguageChoice.English, spanishMexico)).isEqualTo(AppLanguage.English)
        assertThat(LanguageResolver.resolve(LanguageChoice.Spanish, french)).isEqualTo(AppLanguage.Spanish)
    }

    @Test
    fun speechPrefersTheDeviceRegionWhenTheLanguageMatches() {
        assertThat(LanguageResolver.speechLanguage(AppLanguage.Spanish, spanishMexico)).isEqualTo(SpeechLanguage("es", "MX"))
        assertThat(LanguageResolver.speechLanguage(AppLanguage.English, britishEnglish)).isEqualTo(SpeechLanguage("en", "GB"))
    }

    @Test
    fun speechUsesTheDefaultRegionOtherwise() {
        assertThat(LanguageResolver.speechLanguage(AppLanguage.Spanish, britishEnglish)).isEqualTo(SpeechLanguage("es", "MX"))
        assertThat(LanguageResolver.speechLanguage(AppLanguage.English, french)).isEqualTo(SpeechLanguage("en", "US"))
    }
}
