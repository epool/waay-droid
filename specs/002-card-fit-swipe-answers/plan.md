# Implementation Plan: Card Fit, Swipe Answers and Native Look

**Branch**: `002-card-fit-swipe-answers` | **Date**: 2026-10-04 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/002-card-fit-swipe-answers/spec.md`

## Summary

The card phase is redesigned as a Slack "Catch up"-style card stage. Every card's numbers fit its card
without scrolling: a pure, shared grid-fit algorithm picks the columns, rows and one number size for
the space each UI measures. The text size wins at accessibility sizes (ADR-013).

Players answer by dragging the card. Right is Yes, left is No, with tilt, a hint label, a flick, a
spring-back and fly-off animations. The ViewModel's new synchronous `canAnswer` query guarantees that
a card only leaves when its answer is recorded (ADR-014, G15). The Yes/No controls stay, mirroring
the swipe directions: a button row under the card in compact layouts, flanking panels in wide ones.

Each platform follows its own design language:
- **Android:** Material You, with dynamic colour on 12+ and a generated Material 3 brand scheme on
  8–11 (ADR-016);
- **iOS:** Liquid Glass on 26+, with system materials on 17–25 (ADR-017). The numbers card stays
  opaque.

No new dependencies, modules or persisted data.

## Technical Context

**Language/Version**: Kotlin 2.4.20 (KMP), Swift 6 (SwiftUI). Unchanged ([ADR-000](../001-magic-cards-game/research.md)).

**Primary Dependencies**: unchanged.
- Compose BOM 2026.09.00, which resolves Foundation and UI 1.12.1 and Material 3 1.4.0.
- SwiftUI with the iOS 26 SDK. The Liquid Glass APIs sit behind `#available`.
- SKIE, Koin, multiplatform-settings and Kermit.

**Storage**: N/A. Preferences are unchanged.

**Testing**:
- `kotlin.test` + Turbine (`commonTest`): `CardGridFitTest`, G15;
- Robolectric + Compose UI test + Roborazzi (Android): swipe, fit and order tests, and re-recorded
  baselines;
- XCUITest + `performAccessibilityAudit` (iOS): swipe, fit and order tests (ADR-021).

**Target Platform**: Android 8.0+ (API 26), with dynamic colour from API 31. iOS 17.0+, with Liquid
Glass from iOS 26.

**Project Type**: Mobile apps (KMP shared module + two native UIs).

**Performance Goals**: the drag tracks the finger at the display's frame rate (SC-005). The grid fit
is O(n) with n ≤ 64, run on layout only.

**Constraints**:
- Exactly one answer per card (FR-011, ADR-014).
- No scrolling where it fits (FR-001); the text size wins (FR-004).
- Glass never behind content (FR-025).
- iOS deployment target 17.0, minSdk 26.
- Stable APIs only (constitution VII; Material 3 Expressive deferred).

**Scale/Scope**: one screen phase (cards), restyled intro, result and Settings, and two platforms.
About 6 new source files and 8 new test files. 64 screenshots re-recorded, plus new ones.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Pre-research | Post-design | Evidence |
|---|---|---|---|
| I. Spec-First Delivery | ✅ | ✅ | Spec 002 approved after clarifications. ADR-013 to ADR-021 have sources. Tasks follow. |
| II. Shared Brain, Thin Native UIs | ✅ | ✅ | Grid fit and answer acceptance are in `shared` ([card-grid-fit](./contracts/card-grid-fit.md), [G15](./contracts/game-viewmodel-delta.md)). UIs keep gesture physics and styling only. |
| III. Future-Proof Common Code | ✅ | ✅ | `CardGridFit` is pure Kotlin arithmetic with no platform APIs, ready for a later CMP or web UI. |
| IV. Unidirectional Data Flow | ✅ | ✅ | No new state. `canAnswer` is a read-only query. Actions are unchanged. |
| V. Swift-Friendly Interop | ✅ | ✅ | `object CardGridFit`, a `data class CardGrid`, `Int`/`Double`/`Boolean`, no generics. `canAnswer(cardIndex: Int32)`. |
| VI. Test-First Verification | ✅ | ✅ | Exhaustive F1–F8, G15a–e, UI guarantees U1–U11 on both platforms, re-recorded baselines (ADR-021). |
| VII. Current, Compatible Dependencies | ✅ | ✅ | No new dependency. Material 3 Expressive is not adopted because its APIs are internal in stable 1.4.0 (ADR-016). The colour generator is a one-off developer tool, not in the build. |
| VIII. Quality Gates | ✅ | ✅ | The same four CI jobs, Kover over `CardGridFit` (`game.presentation`), and the audit test. |
| IX. Accessible & Localized | ✅ | ✅ | Buttons stay (FR-012). Reduce motion (ADR-018), contrast roles, the audit, and the text size winning (FR-004). Hint labels reuse the localized Yes/No. |
| X. Evolve Structure When Earned | ✅ | ✅ | Still a single `shared` module. No new module trigger. |

**Gate result:** PASS, with no violations. One spec-level finding follows.

## Spec feedback: SC-001 needs a narrower wording (owner decision)

The feasibility model in [ADR-013](./research.md#adr-013--grid-fit-as-shared-pure-kotlin-fr-001-to-fr-005)
shows that at the **default** text size, 64 numbers cannot all fit:
- on small Android phones in landscape (a 640×360 dp window gives 13 dp per number, below the 14 sp
  minimum), because 7-card games need a window about 375 dp tall;
- in a phone's split-screen half (360×320 dp gives about 8 dp), where 6-card games don't fit either.

FR-004 already resolves both cases (the text size wins and the card scrolls from the top), but
SC-001 promises no scrolling "in every configuration of spec 001's verification matrix".

**Proposed SC-001:**

> In every configuration of spec 001's verification matrix, at the default text size and at every
> larger standard size, a card shows 100% of its numbers with no scrolling, for every card count from
> 3 to 7. The exceptions are windows too small for the card at the player's text size, where FR-004's
> scroll fallback applies:
> - phones in landscape less than about 375 dp/pt tall, with 7 cards;
> - phone split-screen halves, with 6 or 7 cards.

Everything else in the spec is feasible as written. **Owner decision is needed at the plan gate.** The
tasks implement FR-004's fallback either way, so only the success criterion's wording and the
verification matrix depend on it.

## Project Structure

### Documentation (this feature)

```text
specs/002-card-fit-swipe-answers/
├── spec.md                       # approved
├── plan.md                       # this file
├── research.md                   # ADR-013 to ADR-021
├── data-model.md                 # CardGrid, canAnswer, swipe state
├── contracts/
│   ├── card-grid-fit.md          # shared API + F1–F8
│   ├── game-viewmodel-delta.md   # canAnswer + G15
│   └── card-screen-ui.md         # structure, identifiers, U1–U11, styling roles
├── quickstart.md
├── checklists/requirements.md
└── tasks.md                      # /speckit-tasks
```

### Source Code (repository root)

```text
shared/src/commonMain/kotlin/dev/epool/waay/game/presentation/
├── CardGridFit.kt                # NEW: CardGrid + CardGridFit (ADR-013)
└── GameViewModel.kt              # + canAnswer(cardIndex) (ADR-014)
shared/src/commonTest/kotlin/dev/epool/waay/game/presentation/
├── CardGridFitTest.kt            # NEW: F1–F8
└── GameViewModelTest.kt          # + G15a–e

androidApp/src/main/kotlin/dev/epool/waay/android/
├── ui/theme/Color.kt             # NEW: generated fallback scheme (seed #4527A0)
├── ui/theme/Theme.kt             # dynamic on 31+, fallback scheme below
├── ui/ReduceMotion.kt            # NEW: rememberReduceMotion() (ADR-018)
├── game/GameScreen.kt            # top bar with progress, backdrop, stage per phase
├── game/CardStage.kt             # NEW: draggable card, stack hint, hint labels, exit and entry (ADR-015)
├── game/CardGridView.kt          # NEW: draws a CardGrid (no lazy grid, no scroll unless scrolls)
├── game/AnswerControls.kt        # NEW: No/Yes row (compact) and flanking panels (wide)
└── adaptive/AdaptiveGameLayout.kt  # card + answers placement per mode (ADR-019)
androidApp/src/test/kotlin/dev/epool/waay/android/
├── game/SwipeAnswerTest.kt       # NEW: U1–U6, U11
├── game/CardFitTest.kt           # NEW: 7 cards, 360×640, no scroll
├── game/ButtonOrderTest.kt       # NEW: No left of Yes, compact, wide and fold
├── game/GameRobot.kt             # answers by button or by swipe
└── screenshots/*                 # re-recorded + light/dark/fallback variants

iosApp/iosApp/
├── Game/GameScreen.swift         # backdrop, toolbar progress, stage per phase
├── Game/CardStage.swift          # NEW: DragGesture card, stack hint, hints, transitions
├── Game/CardGridView.swift       # NEW: draws a CardGrid from CardGridFit
├── Game/AnswerControls.swift     # NEW: glass / material buttons and panels
├── Game/AdaptiveGameLayout.swift # placement per size class (ADR-019)
├── Game/GameModel.swift          # forwards canAnswer
└── Theme/Backdrop.swift          # NEW: accent gradient
iosApp/WaayUITests/
├── SwipeAnswerUITests.swift      # NEW: U1–U6, U11
├── CardFitUITests.swift          # NEW: 7 cards on the smallest CI iPhone, no scroll + button order
└── AccessibilitySemanticsUITests.swift  # audit extended to the new stage
```

**Structure Decision**: the existing `shared` + `androidApp` + `iosApp` layout from spec 001. Shared
logic goes in `game/presentation` next to the ViewModel that uses it. The UI pieces are new files
beside `GameScreen` on each platform.

## Complexity Tracking

No constitution violations to justify.
