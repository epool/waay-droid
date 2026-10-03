package dev.epool.waay.android.game

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.epool.waay.android.MainActivity
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

/** US1 smoke flow on Android 8.0 (API 26, FR-030) and API 36. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [26, 36], qualifiers = "w411dp-h914dp")
class GameFlowTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @After
    fun tearDown() {
        // WaayApp starts Koin per Robolectric Application; stop it so the next test can start again.
        stopKoin()
    }

    @Test
    fun playsAFullRoundThenStartsANewGame() {
        GameRobot(rule)
            .assertIntro()
            .tapReady()
            .answerTruthfully(secret = 27)
            .assertRevealed(secret = 27)
            .tapNewGame()
            .assertIntro()
    }

    // FR-025: semantic matchers first (official testing-setup skill, step 9): headings, a live
    // progress region, labelled actions and number cells announced by value.
    @Test
    fun gameIsUsableThroughSemantics() {
        rule.onNode(hasTestTag("intro.message") and isHeading()).assertIsDisplayed()
        rule.onNode(hasText("I'm ready") and hasClickAction()).performClick()

        rule
            .onNode(hasTestTag("card.progress") and SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion))
            .assertIsDisplayed()
        rule.onNode(hasContentDescription("New game") and hasClickAction()).assertIsDisplayed()
        rule.onNode(hasContentDescription("Settings") and hasClickAction()).assertIsDisplayed()
        rule.onNodeWithTag("card.numbers").assertIsDisplayed()
        rule.onNode(hasText("Yes") and hasClickAction()).assertIsDisplayed()
    }
}
