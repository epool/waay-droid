package dev.epool.waay.game.domain

import dev.epool.waay.core.domain.Result

/** One game's state: deck, answers so far and phase (data-model.md). */
internal data class GameSnapshot(
    val deck: Deck,
    val answers: List<Answer>,
    val phase: GamePhase,
)

internal sealed interface GamePhase {
    /** Waiting for "I'm ready" (FR-003). */
    data object Intro : GamePhase

    /** Card [index] of N is shown (FR-003a). */
    data class Asking(
        val index: Int,
    ) : GamePhase

    /** The decoded number, shown as a statement (FR-004, FR-007). */
    data class Revealed(
        val number: Int,
    ) : GamePhase

    /** All answers were "No" (FR-005). */
    data object Invalid : GamePhase
}

internal sealed interface GameCommand {
    data object Ready : GameCommand

    /** [cardIndex] is the card the answer was given on; answers for any other card are ignored (FR-028). */
    data class AnswerCard(
        val answer: Answer,
        val cardIndex: Int,
    ) : GameCommand

    data object NewGame : GameCommand

    data class CardCountChanged(
        val cardCount: CardCount,
    ) : GameCommand
}

/** The pure game reducer: no lifecycle, no coroutines, no randomness of its own (ADR-006). */
internal object GameEngine {
    fun start(deck: Deck): GameSnapshot = GameSnapshot(deck = deck, answers = emptyList(), phase = GamePhase.Intro)

    /** Commands a phase doesn't allow return [snapshot] unchanged (FR-028). */
    fun reduce(
        snapshot: GameSnapshot,
        command: GameCommand,
        newDeck: (CardCount) -> Deck,
    ): GameSnapshot =
        when (command) {
            GameCommand.NewGame -> start(newDeck(snapshot.deck.cardCount))
            is GameCommand.CardCountChanged -> start(newDeck(command.cardCount))
            GameCommand.Ready -> if (snapshot.phase == GamePhase.Intro) snapshot.copy(phase = GamePhase.Asking(0)) else snapshot
            is GameCommand.AnswerCard -> snapshot.answer(command.answer, command.cardIndex)
        }

    private fun GameSnapshot.answer(
        answer: Answer,
        cardIndex: Int,
    ): GameSnapshot {
        val asking = phase as? GamePhase.Asking ?: return this
        if (asking.index != cardIndex) return this
        val answers = answers + answer
        val nextIndex = asking.index + 1
        val phase =
            if (nextIndex < deck.cards.size) {
                GamePhase.Asking(nextIndex)
            } else {
                when (val result = AnswerDecoder.decode(deck, answers)) {
                    is Result.Success -> GamePhase.Revealed(result.data)
                    is Result.Error -> GamePhase.Invalid
                }
            }
        return copy(answers = answers, phase = phase)
    }
}
