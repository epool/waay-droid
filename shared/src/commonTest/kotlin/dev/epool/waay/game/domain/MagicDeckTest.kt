package dev.epool.waay.game.domain

import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import dev.epool.waay.core.domain.Result
import kotlin.random.Random
import kotlin.test.Test

class MagicDeckTest {
    // FR-002: card membership is bit-exact for every card count, whatever the shuffle.
    @Test
    fun cardMembershipIsBitExactForEveryCardCount() {
        CardCount.all.forEach { cardCount ->
            val deck = MagicDeck.create(cardCount, Random(seed = cardCount.value))

            assertThat(deck.cardCount).isEqualTo(cardCount)
            assertThat(deck.cards).hasSize(cardCount.value)
            assertThat(deck.cards.map { it.bitValue })
                .containsExactlyInAnyOrder(*(0 until cardCount.value).map { 1 shl it }.toTypedArray())
            deck.cards.forEach { card ->
                val expected = (1..cardCount.maxNumber).filter { it and card.bitValue != 0 }
                assertThat(card.numbers).hasSize(cardCount.numbersPerCard)
                assertThat(card.numbers).containsExactlyInAnyOrder(*expected.toTypedArray())
            }
        }
    }

    // US2: the same seed yields the same deck (deterministic, testable randomness).
    @Test
    fun sameSeedYieldsTheSameDeck() {
        CardCount.all.forEach { cardCount ->
            assertThat(MagicDeck.create(cardCount, Random(42))).isEqualTo(MagicDeck.create(cardCount, Random(42)))
        }
    }

    // FR-008/FR-009: different seeds shuffle both the card order and the numbers on a card.
    @Test
    fun differentSeedsShuffleCardOrderAndNumbers() {
        val five = CardCount.DEFAULT
        val decks = (1..20).map { MagicDeck.create(five, Random(it)) }

        assertThat(decks.map { deck -> deck.cards.map { it.bitValue } }.distinct().size).isNotEqualTo(1)
        val firstCardOfEachDeck = decks.map { deck -> deck.cards.first { it.bitValue == 1 }.numbers }
        assertThat(firstCardOfEachDeck.distinct().size).isNotEqualTo(1)
    }

    // SC-001 under shuffling: truthful answers decode correctly for all 243 cases across 20 seeds (FR-010).
    @Test
    fun decodingIsCorrectForEveryNumberAcrossSeeds() {
        (1..20).forEach { seed ->
            CardCount.all.forEach { cardCount ->
                val deck = MagicDeck.create(cardCount, Random(seed))
                (1..cardCount.maxNumber).forEach { secret ->
                    val answers = deck.cards.map { if (secret in it.numbers) Answer.Yes else Answer.No }
                    assertThat(AnswerDecoder.decode(deck, answers)).isEqualTo(Result.Success(secret))
                }
            }
        }
    }
}
