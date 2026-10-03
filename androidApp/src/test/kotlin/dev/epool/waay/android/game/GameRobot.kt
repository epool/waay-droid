package dev.epool.waay.android.game

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick

/**
 * Robot for the game screen (robot pattern, Lackner's testing skill). Answers are chosen from the
 * numbers actually displayed, so it works with shuffled decks (US2) too.
 */
class GameRobot(
    private val rule: AndroidComposeTestRule<*, *>,
) {
    companion object {
        /** Real players need far longer than the 300 ms answer cooldown (FR-028) to read a card. */
        fun waitOutAnswerCooldown() = Thread.sleep(350)
    }

    fun assertIntro() =
        apply {
            rule.onNodeWithTag("intro.ready").assertIsDisplayed()
        }

    fun tapReady() =
        apply {
            rule.onNodeWithTag("intro.ready").performClick()
        }

    fun answerTruthfully(
        secret: Int,
        cards: Int = 5,
    ) = apply {
        repeat(cards) {
            rule.waitForIdle()
            waitOutAnswerCooldown()
            val isOnCard = rule.onAllNodesWithTag("number.$secret").fetchSemanticsNodes().isNotEmpty()
            rule.onNodeWithTag(if (isOnCard) "card.yes" else "card.no").performClick()
        }
    }

    fun answer(tag: String) =
        apply {
            rule.waitForIdle()
            waitOutAnswerCooldown()
            rule.onNodeWithTag(tag).performClick()
        }

    /** The visible card's progress label, e.g. "Card 2 of 5". */
    fun progress(): String =
        rule
            .onNodeWithTag("card.progress")
            .fetchSemanticsNode()
            .config[SemanticsProperties.Text]
            .joinToString { it.text }

    /** The numbers on the visible card, as their `number.N` test tags. */
    fun numbersShown(): Set<String> =
        rule
            .onAllNodes(
                SemanticsMatcher("is a card number") {
                    SemanticsProperties.TestTag in it.config && it.config[SemanticsProperties.TestTag].startsWith("number.")
                },
            ).fetchSemanticsNodes()
            .map { it.config[SemanticsProperties.TestTag] }
            .toSet()

    fun assertRevealed(secret: Int) =
        apply {
            rule.onNodeWithTag("result.message").assertTextContains("$secret", substring = true)
        }

    fun tapNewGame() =
        apply {
            rule.onNodeWithTag("toolbar.newGame").performClick()
        }
}
