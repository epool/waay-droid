package dev.epool.waay.android.game

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
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

    // FR-029 / FR-016b, mirroring iOS GameFlowUITests: rotation (Activity recreation), going to the
    // background and a Settings round trip mid-game all keep the same card and numbers.
    @Test
    fun playsAFullRoundSurvivingInterruptionsThenStartsANewGame() {
        val robot =
            GameRobot(rule)
                .tapReady()
                .answerTruthfully(secret = 27, cards = 2)
        val progressBefore = robot.progress()
        val numbersBefore = robot.numbersShown()

        rule.activityRule.scenario.recreate()
        assertThat(robot.progress()).isEqualTo(progressBefore)
        assertThat(robot.numbersShown()).isEqualTo(numbersBefore)

        rule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        rule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        assertThat(robot.progress()).isEqualTo(progressBefore)

        rule.onNodeWithTag("toolbar.settings").performClick()
        rule.onNodeWithContentDescription("Back").performClick()
        assertThat(robot.progress()).isEqualTo(progressBefore)
        assertThat(robot.numbersShown()).isEqualTo(numbersBefore)

        robot
            .answerTruthfully(secret = 27, cards = 3)
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

        // Spec 002 FR-012, FR-019: the draggable card reads as its question, then every number.
        val onCard = hasAnyAncestor(hasTestTag("card.surface"))
        rule.onNode(onCard and isHeading() and hasText("Is your number on this card?")).assertIsDisplayed()
        val numbers = rule.onAllNodes(onCard and SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription))
        assertThat(numbers.fetchSemanticsNodes().size).isEqualTo(16)

        // ...and a whole game completes with the Yes/No buttons alone, no gestures.
        GameRobot(rule).answerTruthfully(secret = 21).assertRevealed(secret = 21)
        rule.onNode(hasTestTag("result.message") and isHeading()).assertIsDisplayed()
    }
}
