package dev.epool.waay.game.domain

import kotlin.random.Random

/**
 * Generates the magic cards: card k holds every number in range whose bit 2^k is set (FR-002).
 * The card order (FR-008) and the numbers on each card (FR-009) are shuffled with the injected
 * [Random] (`shuffled` is a uniform Fisher–Yates shuffle). Each card keeps its hidden `bitValue`,
 * so decoding stays correct whatever the presentation order (FR-010).
 */
internal object MagicDeck {
    fun create(
        cardCount: CardCount,
        random: Random,
    ): Deck {
        val cards =
            (0 until cardCount.value).map { k ->
                val bitValue = 1 shl k
                Card(
                    bitValue = bitValue,
                    numbers = (1..cardCount.maxNumber).filter { it and bitValue != 0 }.shuffled(random),
                )
            }
        return Deck(cardCount, cards.shuffled(random))
    }
}
