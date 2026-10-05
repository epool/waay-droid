# Data Model: Card Fit, Swipe Answers and Native Look

This feature adds no persisted data and no new game state. The game's entities (deck, card, answer,
preferences) are unchanged from [spec 001's data model](../001-magic-cards-game/data-model.md). It
adds:
- one shared value type;
- one ViewModel query;
- UI-local gesture state.

## CardGrid (shared, `commonMain`, `dev.epool.waay.game.presentation`)

The grid a card is drawn with, from `CardGridFit.fit(...)` ([contract](./contracts/card-grid-fit.md),
ADR-013).

| Field | Type | Meaning | Rules |
|---|---|---|---|
| `columns` | `Int` | Numbers per row | 1 ≤ columns ≤ count |
| `rows` | `Int` | Rows needed | rows = ⌈count / columns⌉ |
| `cellWidth` | `Double` | Cell width, in the caller's units (dp/pt) | > 0 |
| `cellHeight` | `Double` | Cell height | > 0 |
| `fontSize` | `Double` | One number size for the whole card (FR-003) | `min ≤ fontSize ≤ max` |
| `scrolls` | `Boolean` | True only when nothing fits at `minFontSize` (FR-004) | When false: `columns·cellWidth + (columns−1)·spacing ≤ width` and `rows·cellHeight + (rows−1)·spacing ≤ height` |

Inputs:
- `count` (4–64);
- `maxDigits` (1–3);
- `width`, `height` and `spacing`, all ≥ 0;
- `minFontSize`, from the player's text size;
- `maxFontSize`, the readability cap.

## GameViewModel.canAnswer (shared)

`canAnswer(cardIndex: Int): Boolean` is a side-effect-free query. It returns true iff
`OnAnswerClick(_, cardIndex)` would be recorded now (ADR-014, guarantee G15 in the
[contract](./contracts/game-viewmodel-delta.md)). It reads only existing state:
- the phase;
- the current card's index;
- the `cardShownAt` mark against the 300 ms cooldown.

## Swipe (UI-local, per platform)

| Field | Meaning |
|---|---|
| `offset` | Horizontal displacement of the card. Positive is right (towards Yes). |
| `tiltDegrees` | `offset / cardWidth × 12`, clamped to ±12. |
| `hint` | `none`, `yes` or `no`, by the sign of `offset`. Its opacity grows to 1 at the threshold. |
| `outcome` | On release: `yes` or `no` if \|offset\| ≥ ⅓ of the width or a flick ≥ 1,200 dp/pt/s in that direction, **and** `canAnswer`; otherwise `cancelled`. |
| `exiting` | A snapshot of the answered card and its direction, animating off-screen while the next card enters. |

State transitions: `rest → dragging → (exiting → rest[next card]) | (springing back → rest)`.
An interruption (FR-016) goes from any state to `rest` with no answer.

**Direction mapping** (FR-006, FR-012a, FR-013): Yes = right, No = left. This holds for swipes,
buttons, panels and exit animations.
