package dev.epool.waay.android.game

import androidx.compose.animation.core.SpringSpec
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEqualTo
import com.github.takahirom.roborazzi.captureRoboImage
import dev.epool.waay.android.MainActivity
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Spec 002 US2 on Android, contract U1–U6, U9–U11 (card-screen-ui.md): right is Yes, left is No,
 * short or vertical drags answer nothing, a card only leaves when its answer counts.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h914dp")
class SwipeAnswerTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @After
    fun tearDown() {
        stopKoin()
    }

    private val card get() = rule.onNodeWithTag("card.surface")

    private fun progressIs(text: String) = rule.onNodeWithTag("card.progress").assertTextEquals(text)

    private fun startGame(): GameRobot = GameRobot(rule).tapReady().also { rule.waitForIdle() }

    // U1, U2: a whole game by swiping only reveals the secret.
    @Test
    fun swipingRightForYesAndLeftForNoRevealsTheSecret() {
        startGame()
        GameRobot(rule).swipeTruthfully(secret = 27).assertRevealed(secret = 27)
    }

    // U4: a short, slow drag springs back and answers nothing.
    @Test
    fun aShortDragAnswersNothing() {
        startGame()
        GameRobot.waitOutAnswerCooldown()
        card.performTouchInput { swipe(center, center + Offset(width * 0.15f, 0f), durationMillis = 1_500) }
        rule.waitForIdle()
        progressIs("Card 1 of 5")
    }

    // U6: a vertical drag never answers.
    @Test
    fun aVerticalDragAnswersNothing() {
        startGame()
        GameRobot.waitOutAnswerCooldown()
        card.performTouchInput { swipeUp() }
        rule.waitForIdle()
        progressIs("Card 1 of 5")
    }

    // U3: a fast flick counts even below the distance threshold.
    @Test
    fun aFastFlickAnswers() {
        startGame()
        GameRobot.waitOutAnswerCooldown()
        card.performTouchInput { swipe(center, center + Offset(width * 0.25f, 0f), durationMillis = 60) }
        rule.waitForIdle()
        progressIs("Card 2 of 5")
    }

    // U5 / U11: a swipe landing inside the answer cooldown springs back instead of leaving.
    @Test
    fun aSwipeInsideTheCooldownSpringsBack() {
        startGame()
        card.performTouchInput { swipeRight() }
        rule.waitForIdle()
        progressIs("Card 1 of 5")
    }

    // U10 + FR-017: mid-drag the hint shows, and progress and the top-bar controls stay usable.
    @Test
    fun whileDraggingTheHintShowsAndTheControlsStay() {
        startGame()
        GameRobot.waitOutAnswerCooldown()
        card.performTouchInput {
            down(center)
            moveBy(Offset(width * 0.3f, 0f))
        }
        rule.onNodeWithTag("card.hint.yes").assertIsDisplayed()
        rule.onNodeWithTag("card.progress").assertIsDisplayed()
        rule.onNodeWithTag("toolbar.newGame").assertIsDisplayed()
        rule.onNodeWithTag("toolbar.settings").assertIsDisplayed()
        rule.onRoot().captureRoboImage("src/test/screenshots/game_card_drag_yes.png")

        card.performTouchInput {
            advanceEventTime(300)
            up()
        }
        rule.waitForIdle()
        progressIs("Card 1 of 5")
    }

    // U9 / FR-016: an interruption mid-drag (here a configuration change) records nothing.
    @Test
    fun anInterruptedDragAnswersNothing() {
        startGame()
        GameRobot.waitOutAnswerCooldown()
        card.performTouchInput {
            down(center)
            moveBy(Offset(width * 0.6f, 0f))
        }
        rule.activityRule.scenario.recreate()
        rule.waitForIdle()
        progressIs("Card 1 of 5")
    }

    // Spec edge case: a second finger during a drag never records an extra answer.
    @Test
    fun twoFingersRecordAtMostOneAnswer() {
        startGame()
        GameRobot.waitOutAnswerCooldown()
        card.performTouchInput {
            down(0, center)
            down(1, center + Offset(0f, height * 0.2f))
            moveBy(0, Offset(width * 0.6f, 0f))
            moveBy(1, Offset(width * 0.6f, 0f))
            up(0)
            up(1)
        }
        rule.waitForIdle()
        assertThat(GameRobot(rule).progress()).isNotEqualTo("Card 3 of 5")
    }

    // FR-015: the stack behind the card shows no numbers; only the current card's are on screen.
    @Test
    fun theStackShowsNoNumbers() {
        startGame()
        val numbers =
            rule
                .onAllNodes(
                    SemanticsMatcher("is a card number") {
                        SemanticsProperties.TestTag in it.config && it.config[SemanticsProperties.TestTag].startsWith("number.")
                    },
                ).fetchSemanticsNodes()
        assertThat(numbers.size).isEqualTo(16)
    }

    // U2 by itself: left is No.
    @Test
    fun swipingLeftAnswersNo() {
        startGame()
        GameRobot.waitOutAnswerCooldown()
        card.performTouchInput { swipeLeft() }
        rule.waitForIdle()
        progressIs("Card 2 of 5")
    }

    // FR-026: card motion uses spring physics.
    @Test
    fun cardMotionUsesSprings() {
        assertThat(CardMotion.settle).isInstanceOf<SpringSpec<Float>>()
        assertThat(CardMotion.exit).isInstanceOf<SpringSpec<Float>>()
        assertThat(CardMotion.enter).isInstanceOf<SpringSpec<Float>>()
    }
}
