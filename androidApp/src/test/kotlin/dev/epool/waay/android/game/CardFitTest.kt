package dev.epool.waay.android.game

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isTrue
import dev.epool.waay.android.MainActivity
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

/** Spec 002 FR-001 / FR-002 / SC-001 / SC-002: every number visible at once, filling the card. */
@RunWith(AndroidJUnit4::class)
class CardFitTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @After
    fun tearDown() {
        stopKoin()
    }

    private val isNumber =
        SemanticsMatcher("is a card number") {
            SemanticsProperties.TestTag in it.config && it.config[SemanticsProperties.TestTag].startsWith("number.")
        }

    private fun startGameWith(cards: Int) {
        rule.onNodeWithTag("toolbar.settings").performClick()
        rule.onNodeWithTag("settings.cardCount.$cards").performClick()
        rule.onNodeWithContentDescription("Back").performClick()
        GameRobot(rule).tapReady()
    }

    private fun numberNodes(): List<SemanticsNode> = rule.onAllNodes(isNumber).fetchSemanticsNodes()

    @Test
    @Config(sdk = [36], qualifiers = "w360dp-h640dp")
    fun sevenCardsFitASmallPhoneWithoutScrolling() {
        startGameWith(cards = 7)

        rule.onAllNodes(hasScrollAction() and hasAnyAncestor(hasTestTag("card.numbers"))).assertCountEquals(0)
        val numbers = numberNodes()
        assertThat(numbers.size).isEqualTo(64)
        val screen = rule.onRoot().fetchSemanticsNode().boundsInRoot
        numbers.forEach { number ->
            val bounds = number.boundsInRoot
            assertThat(
                bounds.left >= screen.left && bounds.top >= screen.top && bounds.right <= screen.right && bounds.bottom <= screen.bottom,
                "number ${number.config[SemanticsProperties.TestTag]} on screen",
            ).isTrue()
        }
    }

    @Test
    @Config(sdk = [36], qualifiers = "w800dp-h1280dp")
    fun threeCardsFillATabletsCard() {
        startGameWith(cards = 3)

        val area = rule.onNodeWithTag("card.numbers").fetchSemanticsNode().boundsInRoot
        val numbers = numberNodes().map { it.boundsInRoot }
        assertThat(numbers.size).isEqualTo(4)
        val covered =
            (numbers.maxOf { it.right } - numbers.minOf { it.left }) * (numbers.maxOf { it.bottom } - numbers.minOf { it.top })
        assertThat(covered / (area.width * area.height)).isGreaterThanOrEqualTo(0.8f)
    }
}
