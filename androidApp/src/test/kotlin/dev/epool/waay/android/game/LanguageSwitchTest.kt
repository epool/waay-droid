package dev.epool.waay.android.game

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import dev.epool.waay.android.MainActivity
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

/**
 * US5 (quickstart A1, A6), mirroring iOS LanguageSwitchUITests: with the device in Spanish the game
 * starts in Spanish; switching the app to English mid-game re-renders the same card in English at
 * once, without restarting the game (FR-020, FR-021).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "es-rMX-w411dp-h914dp")
class LanguageSwitchTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun spanishDeviceThenEnglishMidGame() {
        rule.onNodeWithTag("intro.ready").assertTextEquals("Estoy listo")
        val robot = GameRobot(rule).tapReady()
        rule.onNodeWithTag("card.progress").assertTextEquals("Carta 1 de 5")
        val numbersBefore = robot.numbersShown()

        rule.onNodeWithTag("toolbar.settings").performClick()
        rule.onNodeWithTag("settings.language.English").performClick()
        rule.onNodeWithText("Settings").assertExists()
        rule.onNodeWithContentDescription("Back").performClick()

        rule.onNodeWithTag("card.progress").assertTextEquals("Card 1 of 5")
        rule.onNodeWithTag("card.yes").assertTextEquals("Yes")
        assertThat(robot.numbersShown()).isEqualTo(numbersBefore)
    }
}
