package dev.epool.waay.android.game

import android.provider.Settings
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isGreaterThan
import assertk.assertions.isLessThan
import assertk.assertions.isTrue
import dev.epool.waay.android.MainActivity
import dev.epool.waay.android.ui.animationsRemoved
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config
import kotlin.math.abs

/**
 * Spec 002 US3 on Android, FR-014 and contract U8: with "Remove animations" on (animator duration
 * scale 0) the card follows the finger without tilting and cross-fades instead of flying, and swipes
 * and buttons still answer, once.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "w411dp-h914dp")
class ReduceMotionTest {
    private val resolver get() = ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver

    /** "Remove animations", set before the activity starts and restored afterwards. */
    private val removeAnimations =
        object : ExternalResource() {
            override fun before() {
                Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
            }

            override fun after() {
                Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
            }
        }

    private val rule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(removeAnimations).around(rule)

    @After
    fun tearDown() {
        stopKoin()
    }

    private val card get() = rule.onNodeWithTag("card.surface")

    private fun progressIs(text: String) = rule.onNodeWithTag("card.progress").assertTextEquals(text)

    private fun startGame(): GameRobot = GameRobot(rule).tapReady().also { rule.waitForIdle() }

    @Test
    fun removeAnimationsMeansReduceMotion() {
        assertThat(resolver.animationsRemoved()).isTrue()
    }

    // FR-014: dragging still follows the finger (direct manipulation), with no tilt.
    @Test
    fun theCardFollowsTheFingerWithoutTilting() {
        startGame()
        GameRobot.waitOutAnswerCooldown()
        val rest = card.fetchSemanticsNode().boundsInRoot
        card.performTouchInput {
            down(center)
            moveBy(Offset(width * 0.3f, 0f))
        }
        rule.waitForIdle()
        val dragged = card.fetchSemanticsNode().boundsInRoot
        assertThat(dragged.left - rest.left).isGreaterThan(rest.width * 0.2f)
        assertThat(abs(dragged.height - rest.height)).isLessThan(1f)
        card.performTouchInput { up() }
        // Let the card settle: an animation still running when the test ends leaks into later tests.
        rule.waitForIdle()
    }

    // FR-014: "Dragging MUST still answer."
    @Test
    fun aSwipeStillAnswers() {
        startGame()
        GameRobot.waitOutAnswerCooldown()
        card.performTouchInput { swipeLeft() }
        rule.waitForIdle()
        progressIs("Card 2 of 5")
    }

    // FR-012, FR-028: a button tap answers exactly once.
    @Test
    fun aButtonTapAnswersOnce() {
        startGame()
        GameRobot.waitOutAnswerCooldown()
        rule.onNodeWithTag("card.yes").performClick()
        rule.waitForIdle()
        progressIs("Card 2 of 5")
    }
}
