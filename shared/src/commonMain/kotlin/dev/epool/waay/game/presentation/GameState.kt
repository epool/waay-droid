package dev.epool.waay.game.presentation

/** Game screen UI state with resolved, localized text (contracts/game-viewmodel.md). */
public data class GameState(
    val title: String,
    val settingsLabel: String,
    /** Label of the "New game" action, visible in every phase (FR-006, SC-008). */
    val newGameLabel: String,
    val content: GameContentUi,
)

public sealed interface GameContentUi {
    public data class Intro(
        val message: String,
        val readyLabel: String,
    ) : GameContentUi

    public data class Card(
        /** Presentation position (0-based); sent back with answers so stale taps are ignored (FR-028). */
        val index: Int,
        val progress: String,
        val question: String,
        /** Displayed numbers only: no bit information (FR-011). */
        val numbers: List<NumberUi>,
        val yesLabel: String,
        val noLabel: String,
    ) : GameContentUi

    public data class Revealed(
        val message: String,
        val number: Int,
        val newGameLabel: String,
    ) : GameContentUi

    public data class Invalid(
        val message: String,
        val newGameLabel: String,
    ) : GameContentUi
}

public data class NumberUi(
    val value: Int,
    /** What screen readers announce (FR-025). */
    val label: String,
)
