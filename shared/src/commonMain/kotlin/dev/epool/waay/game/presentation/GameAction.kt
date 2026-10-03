package dev.epool.waay.game.presentation

import dev.epool.waay.game.domain.Answer

public sealed interface GameAction {
    public data object OnReadyClick : GameAction

    public data class OnAnswerClick(
        val answer: Answer,
    ) : GameAction

    public data object OnNewGameClick : GameAction

    public data object OnSettingsClick : GameAction
}
