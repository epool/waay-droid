# Contract: Card screen UI (Android Compose and iOS SwiftUI)

Both platforms implement the same structure, behaviour and test identifiers. The stateless `Screen`
still takes `state` and `onAction` (constitution II). It also takes `canAnswer: (Int) -> Boolean`
([ViewModel delta](./game-viewmodel-delta.md)); previews pass `{ true }`.

## Structure (FR-019)

```
┌ top bar ─────────────────────────────────────────┐
│ [New game]        Card N of M        [Settings]  │   progress centred (card phase); app title otherwise
└──────────────────────────────────────────────────┘
        backdrop (full bleed, platform style)
   ┌ card (opaque, rounded, draggable) ┐   ← stack hint: 2 blank card backs behind
   │ Is your number on this card?       │   header
   │  12  27   3  19  …                 │   CardGrid body (no scroll unless FR-004)
   └────────────────────────────────────┘
   [   No   ] [   Yes   ]                 ← compact; wide = flanking panels: [No] card [Yes]
```

## Identifiers (Compose `testTag` = iOS `accessibilityIdentifier`)

| Identifier | Element | Notes |
|---|---|---|
| `card.progress` | Progress text in the top bar | Unchanged id; it moves from the card body to the top bar (FR-017) |
| `card.surface` | The draggable card | Exposes the swipe. The screen reader reads its header and numbers. |
| `card.numbers` | The number grid | Android: no vertical-scroll semantics unless `CardGrid.scrolls` |
| `number.N` | One number cell | Unchanged |
| `card.hint.yes` / `card.hint.no` | Drag hint labels | Present only while dragging in that direction |
| `card.no` / `card.yes` | The answer controls: buttons when compact, panels when wide | Unchanged ids. No is always left of Yes (FR-012a). |
| `toolbar.newGame` / `toolbar.settings` | Top bar actions | Unchanged |

## Gesture and animation behaviour

| ID | Given | Then |
|---|---|---|
| U1 | Drag right ≥ ⅓ card width, release, `canAnswer` true | The card exits right and `OnAnswerClick(Yes, index)` is sent once. The next card enters from the stack (or the result appears). |
| U2 | Same to the left | The card exits left with `No` |
| U3 | Flick ≥ 1,200 dp/pt/s in a direction, any distance | Same as U1/U2 for that direction |
| U4 | Release below the threshold without a flick | Springs back, nothing sent |
| U5 | Release past the threshold but `canAnswer` is false (cooldown, stale) | Springs back, nothing sent |
| U6 | Mostly vertical drag | The card doesn't move sideways and nothing is sent. If `scrolls`, the grid scrolls. |
| U7 | Tap `card.yes` / `card.no` with `canAnswer` true | Same exit animation as U1/U2, then the action is sent |
| U8 | Reduce motion on | Exit and entry are a ≤ 150 ms cross-fade, with no tilt or fly. Gestures still answer. |
| U9 | Interruption mid-drag (configuration change, background, Settings, New game) | Back at rest, nothing sent |
| U10 | While dragging | Tilt is ±12° at most. The hint label (`yesLabel`/`noLabel`) fades in, reaching full opacity at the threshold. A haptic fires when the threshold is crossed (where supported). |
| U11 | The exit animation is running | Further input on the leaving card is ignored. The new card accepts input once `canAnswer` allows it (the 300 ms cooldown, which covers the entrance animation). |

## Platform styling (FR-022 to FR-026)

| Role | Android (Material You, API 31+ dynamic; generated fallback on 26–30) | iOS 26+ (Liquid Glass) | iOS 17–25 |
|---|---|---|---|
| Backdrop | `primaryContainer` | Accent gradient `#23143F` → `#4527A0` | same gradient |
| Top bar content | `onPrimaryContainer` | System bar (glass) | System bar |
| Card | `surfaceContainerLowest`, `onSurface` numbers, `shapes.extraLarge` | `systemBackground`, opaque, 28 pt corners | same |
| Yes | Filled `Button` (`primary`) | `.glassProminent`, accent tint | `.borderedProminent` |
| No | `OutlinedButton` on `surface` | `.glass` | `.bordered` on `.thinMaterial` |
| Wide panels | Tonal surfaces with large labels, same roles | Glass panels in a `GlassEffectContainer` | Material panels |
| Motion | `spring` specs (ADR-015) | `.snappy` / `.bouncy` | same |

Contrast for every text-on-surface pair MUST meet 4.5:1 (3:1 for large text). On iOS this is enforced
by the accessibility audit; on Android by the Material 3 roles plus the screenshot review.
