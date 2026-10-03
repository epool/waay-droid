package dev.epool.waay.android.game

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.epool.waay.android.MainActivity
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

/** FR-028: a rapid double tap on an answer records a single answer for the current card. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "w411dp-h914dp")
class RapidInputTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun doubleTapOnYesRecordsOneAnswer() {
        rule.onNodeWithTag("intro.ready").performClick()
        rule.onNodeWithTag("card.progress").assertTextEquals("Card 1 of 5")

        GameRobot.waitOutAnswerCooldown()
        // Two taps ~16 ms apart: the second lands on card 2 well within the answer cooldown.
        rule.onNodeWithTag("card.yes").performTouchInput {
            click()
            click()
        }

        rule.onNodeWithTag("card.progress").assertTextEquals("Card 2 of 5")
    }
}
