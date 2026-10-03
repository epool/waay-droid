package dev.epool.waay.game.presentation

public sealed interface GameEvent {
    public data object NavigateToSettings : GameEvent
}
