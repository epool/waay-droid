package dev.epool.waay.core.i18n

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isEmpty
import assertk.assertions.isNotEqualTo
import kotlin.test.Test

/** SC-004 / FR-019: both catalogs are complete, and parameterised texts carry their arguments. */
class StringsTest {
    private val catalogs = listOf(EnglishStrings, SpanishStrings)

    private fun Strings.allTexts(): List<String> =
        listOf(
            appTitle,
            settingsLabel,
            settingsTitle,
            backLabel,
            intro(31),
            readyLabel,
            progress(3, 5),
            cardQuestion,
            yesLabel,
            noLabel,
            reveal(27),
            invalid(31),
            newGameLabel,
            numberLabel(9),
            voiceLabel,
            cardCountLabel,
            cardCountOption(5, 31),
            languageLabel,
            languageDevice,
            languageEnglish,
            languageSpanish,
        )

    @Test
    fun everyTextIsNonBlankInBothLanguages() {
        catalogs.forEach { strings ->
            assertThat(strings.allTexts().filter { it.isBlank() }).isEmpty()
        }
    }

    @Test
    fun parameterisedTextsContainTheirArguments() {
        catalogs.forEach { strings ->
            assertThat(strings.intro(127)).contains("127")
            assertThat(strings.progress(3, 7)).contains("3")
            assertThat(strings.progress(3, 7)).contains("7")
            assertThat(strings.reveal(27)).contains("27")
            assertThat(strings.invalid(63)).contains("63")
            assertThat(strings.cardCountOption(6, 63)).contains("63")
        }
    }

    @Test
    fun spanishIsActuallyTranslated() {
        assertThat(SpanishStrings.intro(31)).isNotEqualTo(EnglishStrings.intro(31))
        assertThat(SpanishStrings.cardQuestion).isNotEqualTo(EnglishStrings.cardQuestion)
        assertThat(SpanishStrings.settingsTitle).isNotEqualTo(EnglishStrings.settingsTitle)
    }

    // FR-007: the reveal is a statement in every language, never a question.
    @Test
    fun revealIsAStatementInEveryLanguage() {
        catalogs.forEach { strings ->
            assertThat(strings.reveal(27)).doesNotContain("?")
            assertThat(strings.reveal(27)).doesNotContain("¿")
        }
    }
}
