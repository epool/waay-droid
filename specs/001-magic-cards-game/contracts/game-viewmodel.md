# Contract: GameViewModel

**Package:** `dev.epool.waay.game.presentation`.
**Consumers:** `androidApp` (`GameRoot`), `iosApp` (`GameRoot`), and a future `sharedUI`.

The signatures below are the public surface. The bodies belong to the implementation. Explicit API
mode applies.

```kotlin
public class GameViewModel internal constructor(/* deps via Koin */) : ViewModel() {
    public val state: StateFlow<GameState>
    public val events: Flow<GameEvent>
    public fun onAction(action: GameAction)
}

public data class GameState(
    val title: String,
    val settingsLabel: String,
    val newGameLabel: String,      // app-bar/toolbar action visible in EVERY phase (FR-006, SC-008)
    val content: GameContentUi,
)

public sealed interface GameContentUi {
    public data class Intro(val message: String, val readyLabel: String) : GameContentUi
    public data class Card(
        val index: Int,                       // 0-based position; echoed back by OnAnswerClick (FR-028)
        val progress: String,                 // "Card 3 of 5"
        val question: String,                 // "Is your number on this card?"
        val numbers: List<NumberUi>, // shuffled; no bit info (FR-011)
        val yesLabel: String,
        val noLabel: String,
    ) : GameContentUi
    public data class Revealed(val message: String, val number: Int, val newGameLabel: String) : GameContentUi
    public data class Invalid(val message: String, val newGameLabel: String) : GameContentUi
}

public data class NumberUi(val value: Int, val label: String)

public sealed interface GameAction {
    public data object OnReadyClick : GameAction
    public data class OnAnswerClick(val answer: Answer, val cardIndex: Int) : GameAction
    public data object OnNewGameClick : GameAction
    public data object OnSettingsClick : GameAction
}

public sealed interface GameEvent {
    public data object NavigateToSettings : GameEvent
}
```

## Behavioural guarantees (tested in `commonTest`)

| # | Given / When | Then | Spec |
|---|---|---|---|
| G1 | The first subscription to `state` | `content` is `Intro` for the current card count, and the invitation is spoken once if voice is on. Re-subscribing (returning from Settings) never repeats it. **Every spoken line equals the message currently shown on screen.** | FR-003, FR-012, FR-013 |
| G2 | `OnReadyClick` in Intro | `content` is `Card` with progress "Card 1 of N" and `numbersPerCard` numbers. The card prompt is spoken if voice is on. | FR-003a, FR-012 |
| G3 | `OnAnswerClick` × N with truthful answers for x | `content` is `Revealed(number = x)` with a statement message. The reveal is spoken. | FR-004, FR-007 |
| G4 | All answers are `No` | `content` is `Invalid`. Its message is spoken. | FR-005 |
| G5 | `OnNewGameClick` in any phase | `content` is `Intro` with a fresh deck (a new order). | FR-006, FR-008 |
| G6 | Extra `OnAnswerClick` outside the Card phase, or with a `cardIndex` other than the current card (a rapid double tap) | No state change: one tap records one answer for the card it was given on. Answers within 300 ms of a card appearing are also ignored (ADR-012). | FR-028 |
| G7 | The card count changes in preferences | `content` resets to `Intro` with the new N. If `state` has no subscriber at that moment (the player is in Settings), the intro speech is **pending**. It is spoken when the game screen subscribes again, never over Settings. | FR-018, FR-012 |
| G8 | The language changes in preferences | All text is re-resolved in place. The phase, deck and answers are unchanged. | FR-021 |
| G9 | Voice is disabled | No `Speaker.speak` calls happen. `stop()` is called when voice is turned off mid-speech. | FR-014 |
| G10 | Any new utterance | `Speaker.speak` is called. The speaker contract guarantees that it interrupts the previous utterance. | FR-015 |
| G11 | `OnSettingsClick` | Emits `NavigateToSettings` exactly once. | FR-016a |
| G12 | `onCleared` | `Speaker.stop()` is called, and no further emissions happen. | ADR-001 |
| G13 | Any `GameState` | No field exposes or encodes `bitValue`. | FR-011 |

## UI obligations (both platforms)

- `GameRoot` collects `state` with lifecycle awareness and handles `events`.
  `GameScreen(state, onAction)` is stateless and previewable.
- The gear button is visible in every phase, labelled with `settingsLabel` (FR-016a, FR-025).
- A "New game" action is visible in every phase, labelled with `newGameLabel`, as an app-bar or
  toolbar action. It sends `OnNewGameClick` (FR-006, SC-008).
- Number labels are read by the screen reader, and the progress line is announced (FR-025).
- Layout adapts to the window size and fold posture, per ADR-008 (FR-031, FR-032).
