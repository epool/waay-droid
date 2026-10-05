# Contract delta: GameViewModel (spec 001 contract + this feature)

The [spec 001 contract](../../001-magic-cards-game/contracts/game-viewmodel.md) holds unchanged,
including state, actions, events and G1–G14. This feature adds one query.

```kotlin
public class GameViewModel {
    // … unchanged …

    /**
     * True iff OnAnswerClick(_, cardIndex) would be recorded now: Asking phase, cardIndex is the
     * current card, and the answer cooldown (ADR-012) has elapsed. Side-effect free.
     */
    public fun canAnswer(cardIndex: Int): Boolean
}
```

Swift: `viewModel.canAnswer(cardIndex: Int32)`. The iOS `GameModel` forwards it.

| ID | Given | Then |
|---|---|---|
| G15a | Asking, current index i, cooldown elapsed | `canAnswer(i) == true`, and calling it does not change state or emit |
| G15b | Within 300 ms of card i appearing | `canAnswer(i) == false` |
| G15c | Any index other than the current card | `false` |
| G15d | Intro, Revealed or Invalid phase | `false` for any index |
| G15e | `canAnswer(i) == true`, then `OnAnswerClick(a, i)` | The answer is recorded: it agrees with `onAction` |

UI rule (both platforms): **a card animates off-screen only after `canAnswer` returned true for it**,
and the UI then sends `OnAnswerClick` at once. Otherwise it springs back (FR-011).
