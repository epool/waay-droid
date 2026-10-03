package dev.epool.waay.settings.data

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import com.russhwolf.settings.MapSettings
import dev.epool.waay.core.domain.Result
import dev.epool.waay.game.domain.CardCount
import dev.epool.waay.settings.domain.LanguageChoice
import dev.epool.waay.settings.domain.Preferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class KeyValuePreferencesDataSourceTest {
    private fun cardCount(value: Int) = (CardCount.of(value) as Result.Success).data

    @Test
    fun defaultsAreFiveCardsVoiceOnAndDeviceLanguage() =
        runTest {
            val dataSource = KeyValuePreferencesDataSource(MapSettings())

            val preferences = dataSource.preferences.first()

            assertThat(preferences).isEqualTo(Preferences(CardCount.DEFAULT, voiceEnabled = true, LanguageChoice.Device))
        }

    @Test
    fun invalidStoredCardCountFallsBackToTheDefault() =
        runTest {
            val settings = MapSettings(KeyValuePreferencesDataSource.KEY_CARD_COUNT to 9)

            val preferences = KeyValuePreferencesDataSource(settings).preferences.first()

            assertThat(preferences.cardCount).isEqualTo(CardCount.DEFAULT)
        }

    @Test
    fun unknownLanguageKeyFallsBackToDevice() =
        runTest {
            val settings = MapSettings(KeyValuePreferencesDataSource.KEY_LANGUAGE_CHOICE to "klingon")

            val preferences = KeyValuePreferencesDataSource(settings).preferences.first()

            assertThat(preferences.languageChoice).isEqualTo(LanguageChoice.Device)
        }

    @Test
    fun emitsTheCurrentValueFirstThenEveryChange() =
        runTest {
            val dataSource = KeyValuePreferencesDataSource(MapSettings())

            dataSource.preferences.test {
                assertThat(awaitItem()).isEqualTo(Preferences())

                dataSource.setCardCount(cardCount(7))
                assertThat(awaitItem().cardCount).isEqualTo(cardCount(7))

                dataSource.setVoiceEnabled(false)
                assertThat(awaitItem().voiceEnabled).isFalse()

                dataSource.setLanguageChoice(LanguageChoice.Spanish)
                assertThat(awaitItem().languageChoice).isEqualTo(LanguageChoice.Spanish)
            }
        }
}
