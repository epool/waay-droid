package dev.epool.waay.android.game

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.epool.waay.android.MainActivity
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

/**
 * FR-003a: every card starts at the top of its numbers, however far the player scrolled the
 * previous card, so no number is hidden above the visible area.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "w411dp-h914dp")
class CardScrollTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @After
    fun tearDown() {
        stopKoin()
    }

    private fun scrolledBy(
        expected: (Float) -> Boolean,
        description: String,
    ) = SemanticsMatcher(description) { node ->
        expected(node.config[SemanticsProperties.VerticalScrollAxisRange].value())
    }

    @Test
    fun eachNewCardStartsAtTheTop() {
        rule.onNodeWithTag("toolbar.settings").performClick()
        rule.onNodeWithTag("settings.cardCount.7").performClick()
        rule.onNodeWithContentDescription("Back").performClick()

        val grid = rule.onNodeWithTag("card.numbers")
        GameRobot(rule).tapReady()
        grid.performScrollToIndex(63)
        grid.assert(scrolledBy({ it > 0f }, "scrolled down"))

        GameRobot(rule).answer("card.no")

        rule.onNodeWithTag("card.progress").assertTextEquals("Card 2 of 7")
        grid.assert(scrolledBy({ it == 0f }, "at the top"))
    }
}
