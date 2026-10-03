package dev.epool.waay.game.presentation

import dev.epool.waay.game.domain.Answer

public sealed interface GameAction {
    public data object OnReadyClick : GameAction

    /** [cardIndex] is `GameContentUi.Card.index` of the card the player answered (FR-028). */
    public data class OnAnswerClick(
        val answer: Answer,
        val cardIndex: Int,
    ) : GameAction

    public data object OnNewGameClick : GameAction

    public data object OnSettingsClick : GameAction
}
