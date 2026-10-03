package dev.epool.waay.game.domain

import assertk.assertThat
import assertk.assertions.isEqualTo
import dev.epool.waay.core.domain.Result
import kotlin.random.Random
import kotlin.test.Test

class AnswerDecoderTest {
    private fun truthfulAnswers(
        deck: Deck,
        secret: Int,
    ): List<Answer> = deck.cards.map { if (secret in it.numbers) Answer.Yes else Answer.No }

    // SC-001: truthful answers decode to the secret for all 243 cases (N = 3…7).
    @Test
    fun everyNumberDecodesForEveryCardCount() {
        var cases = 0
        CardCount.all.forEach { cardCount ->
            val deck = MagicDeck.create(cardCount, Random(cardCount.value))
            (1..cardCount.maxNumber).forEach { secret ->
                assertThat(AnswerDecoder.decode(deck, truthfulAnswers(deck, secret))).isEqualTo(Result.Success(secret))
                cases++
            }
        }
        assertThat(cases).isEqualTo(7 + 15 + 31 + 63 + 127)
    }

    // FR-005: all "No" decodes to 0, which is out of range.
    @Test
    fun allNoIsOutOfRange() {
        val deck = MagicDeck.create(CardCount.DEFAULT, Random(1))

        val result = AnswerDecoder.decode(deck, List(deck.cards.size) { Answer.No })

        assertThat(result).isEqualTo(Result.Error(DecodeError.OutOfRange))
    }

    @Test
    fun answerCountMustMatchTheDeck() {
        val deck = MagicDeck.create(CardCount.DEFAULT, Random(1))

        assertThat(AnswerDecoder.decode(deck, listOf(Answer.Yes))).isEqualTo(Result.Error(DecodeError.IncompleteAnswers))
    }
}
