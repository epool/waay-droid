package dev.epool.waay.game.domain

/** The N cards of one game, in presentation order. */
internal data class Deck(
    val cardCount: CardCount,
    val cards: List<Card>,
)

/** Builds a fresh [Deck] for a game (injected so tests and US2 randomization can vary it). */
internal fun interface DeckFactory {
    fun create(cardCount: CardCount): Deck
}
