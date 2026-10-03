package dev.epool.waay.android.game

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.epool.waay.android.MainActivity
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

/**
 * FR-026 / SC-006, mirroring iOS AccessibilityUITests: at the largest font scale, in portrait and in
 * landscape, every phase stays reachable — the answers, the result and its action are on screen,
 * scrolling to them where needed, never clipped away.
 */
@RunWith(AndroidJUnit4::class)
class AccessibilityTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @After
    fun tearDown() {
        stopKoin()
    }

    private fun SemanticsNodeInteraction.isFullyOnScreen(): Boolean {
        val bounds = fetchSemanticsNode().boundsInRoot
        val screen = rule.onRoot().fetchSemanticsNode().boundsInRoot
        return bounds.top >= screen.top && bounds.bottom <= screen.bottom &&
            bounds.left >= screen.left && bounds.right <= screen.right
    }

    /**
     * Brings the control into view when its content scrolls, then requires all of it on screen — the
     * equivalent of XCUITest's `isHittable` (partly clipped controls fail).
     */
    private fun reveal(tag: String) =
        rule.onNodeWithTag(tag).apply {
            if (!isFullyOnScreen()) performScrollTo()
            assertIsDisplayed()
            assertWithMessage("$tag is clipped") { isFullyOnScreen() }
        }

    private fun assertWithMessage(
        message: String,
        condition: () -> Boolean,
    ) {
        if (!condition()) throw AssertionError(message)
    }

    private fun playARound() {
        reveal("intro.ready").performClick()
        repeat(5) { card ->
            rule.waitForIdle()
            reveal("card.no")
            reveal("card.yes")
            GameRobot.waitOutAnswerCooldown()
            rule.onNodeWithTag("card.yes").performClick()
        }
        reveal("result.message").assertTextContains("31", substring = true)
        reveal("result.newGame")
    }

    @Test
    @Config(sdk = [36], qualifiers = "w411dp-h914dp", fontScale = 2.0f)
    fun largestFontScalePortrait() = playARound()

    @Test
    @Config(sdk = [36], qualifiers = "w640dp-h360dp-land", fontScale = 2.0f)
    fun largestFontScaleLandscape() = playARound()
}
