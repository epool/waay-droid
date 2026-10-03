package dev.epool.waay.game.domain

import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import kotlin.test.Test

class MagicDeckTest {
    // FR-002: card membership is bit-exact for every card count.
    @Test
    fun cardMembershipIsBitExactForEveryCardCount() {
        CardCount.all.forEach { cardCount ->
            val deck = MagicDeck.create(cardCount)

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
}
