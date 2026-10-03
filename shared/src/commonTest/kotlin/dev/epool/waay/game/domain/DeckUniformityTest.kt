package dev.epool.waay.game.domain

import assertk.assertThat
import assertk.assertions.isLessThanOrEqualTo
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test

/**
 * SC-002: over 10,000 seeded decks per card count, presentation is uniform.
 * "Within ±5%" is read as at most 5 percentage points from the uniform frequency.
 */
class DeckUniformityTest {
    private val games = 10_000
    private val tolerance = 0.05

    // FR-008: each card (bit value) appears in each position with frequency ≈ 1/N.
    @Test
    fun eachCardAppearsInEachPositionUniformly() {
        CardCount.all.forEach { cardCount ->
            val n = cardCount.value
            val counts = Array(n) { IntArray(n) }
            val random = Random(seed = 2026 + n)
            repeat(games) {
                MagicDeck.create(cardCount, random).cards.forEachIndexed { position, card ->
                    counts[card.bitValue.countTrailingZeroBits()][position]++
                }
            }
            counts.forEach { row ->
                row.forEach { count ->
                    assertThat(abs(count.toDouble() / games - 1.0 / n)).isLessThanOrEqualTo(tolerance)
                }
            }
        }
    }

    // FR-009: the first number shown is the card's smallest only by chance (≈ 1/numbersPerCard).
    @Test
    fun smallestNumberIsShownFirstOnlyByChance() {
        CardCount.all.forEach { cardCount ->
            val random = Random(seed = 4242 + cardCount.value)
            var smallestFirst = 0
            var cardsSeen = 0
            repeat(games) {
                MagicDeck.create(cardCount, random).cards.forEach { card ->
                    if (card.numbers.first() == card.numbers.min()) smallestFirst++
                    cardsSeen++
                }
            }
            val frequency = smallestFirst.toDouble() / cardsSeen
            assertThat(abs(frequency - 1.0 / cardCount.numbersPerCard)).isLessThanOrEqualTo(tolerance)
        }
    }
}
