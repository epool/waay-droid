package dev.epool.waay.game.domain

/** Generates the magic cards: card k holds every number in range whose bit 2^k is set (FR-002). */
internal object MagicDeck {
    fun create(cardCount: CardCount): Deck {
        val cards =
            (0 until cardCount.value).map { k ->
                val bitValue = 1 shl k
                Card(bitValue = bitValue, numbers = (1..cardCount.maxNumber).filter { it and bitValue != 0 })
            }
        return Deck(cardCount, cards)
    }
}
