# Research: Card Fit, Swipe Answers and Native Look

Phase 0 of the plan for [spec 002](./spec.md). ADR numbers continue the project sequence: spec 001
ended at ADR-012. Facts were checked against the SDKs and libraries resolved by the current build on
2026-10-04, not against memory:
- the Compose BOM 2026.09.00 resolves to Foundation and UI 1.12.1 and Material 3 1.4.0
  (`./gradlew :androidApp:dependencies`);
- the iOS facts come from the Xcode 27 iOS SDK `SwiftUI` and `SwiftUICore` `.swiftinterface` files.

## Version matrix

**Unchanged from [ADR-000](../001-magic-cards-game/research.md).** This feature adds no build or runtime
dependency.
- The only new tool is Python's `materialyoucolor` (Material Color Utilities), run once through `uvx`
  to generate the fallback colour scheme (ADR-016). Its output is committed as constants and nothing
  in the build depends on it.
- The swipe gesture uses only APIs already on the classpath.

---

## ADR-013 — Grid fit as shared, pure Kotlin (FR-001 to FR-005)

**Decision**: a pure function in `commonMain`, `CardGridFit.fit(...)`, chooses the grid for a card. It
picks the number of columns and rows, the cell size, and the number size, so that all numbers fit
the card's area at the largest possible size. Both UIs call it with:
- the area they measured;
- the minimum number size, taken from the player's text size.

It returns a `CardGrid` that the UIs draw as-is ([card-grid-fit contract](./contracts/card-grid-fit.md)).

**Algorithm** (O(n), n ≤ 64):
- For every column count c from 1 to n, take r = ⌈n / c⌉ rows. The cell size is
  `((W − (c−1)s) / c, (H − (r−1)s) / r)`.
- The largest number size that fits a cell is the smaller of two limits:
  - **width:** cell width / (0.6·d + 0.5);
  - **height:** cell height / (1.2 + 0.4).

  Here d is the digit count of the card's largest number, 0.6 em is the advance of a tabular digit,
  and 1.2 em is the line height. 0.5 em and 0.4 em are the cell's inner padding.
- Pick the c with the largest number size. Ties go to fewer empty cells, then to the squarer grid.
- Cap the size at `maxFontSize`, so a 3-card game doesn't render 4 giant numbers (spec edge case
  "readable proportions"). The cells still fill the area (FR-002).
- If even the best size is below `minFontSize`, the text size wins (FR-004):
  - use the widest column count whose cells hold `minFontSize`;
  - set the rows to `minFontSize`'s height;
  - return `scrolls = true`.

**Rationale**:
- Constitution II: both UIs need exactly the same choice, and sharing it keeps them identical and
  tests it once, exhaustively, in `commonTest` (constitution VI).
- It is pure arithmetic, so `commonMain` stays platform-free (constitution III).
- Swift calls it through a non-generic object with `Double` and `Int` parameters (constitution V).
- The em-based font model is conservative for both system fonts' tabular digits: Roboto Flex and
  SF Pro digits measure 0.55 to 0.6 em. The UIs then render with tabular (`monospacedDigit`)
  figures, so every number in a grid has the same width.

**Alternatives considered**:
- **Each UI fits its own grid** (Compose `LazyVerticalGrid(GridCells.Adaptive)`, SwiftUI
  `LazyVGrid(.adaptive)`, as spec 001 did): this can't guarantee no scrolling, and the two
  platforms would diverge.
- **Shrink text with auto-size** (`TextAutoSize` / `minimumScaleFactor`): each cell would shrink
  separately, giving uneven number sizes. FR-003 requires one size per card.
- **A uniform 8 × 8 grid**: wastes space for 16 numbers and is poor in landscape.

**Feasibility** (model of this algorithm, 64 numbers with 3 digits, s = 6 dp, typical chrome per
layout). Largest number size in dp:

| Configuration | 16 numbers | 32 | 64 | grid for 64 |
|---|---|---|---|---|
| Phone portrait 360×640 (small Android) | 42.1 | 32.9 | **19.8** | 6×11 |
| Phone portrait 393×852 (iPhone 17 class) | 61.5 | 45.2 | **26.3** | 5×13 |
| Phone landscape 640×360 (small Android) | 29.7 | 23.0 | **13.0** ❌ | 8×8 |
| Phone landscape 852×393 (iPhone) | 47.7 | 34.8 | **19.0** | 11×6 |
| Tablet portrait / landscape | 107 / 98 | 85 / 78 | **52 / 47** | |
| Foldable unfolded 673×841 / folded 360×780 | 68 / 54 | 50 / 40 | **29 / 23** | |
| Tabletop top half 841×400 / book left half 336×841 | 55 / 53 | 39 / 39 | **24 / 23** | |
| Split screen, half a phone 360×320 | 19.3 | 13.0 ❌ | **7.8** ❌ | |
| Split screen, half a tablet 640×800 | 83 | 54 | **32** | |

The minimum number size uses the platform's small body style:
- **Android:** `bodyMedium`, 14 sp at the default scale, 18.2 at 1.3×;
- **iOS:** `.subheadline`, 15 pt by default, 21 pt at xxxLarge.

At the default text size, everything fits except a 7-card game:
- in landscape on small Android phones;
- in a phone's split-screen half (and 6-card games there too).

In those cases FR-004's precedence applies and the card scrolls. **SC-001 as worded promises no
scrolling there, so it needs the amendment proposed in the plan.**

**Sources**:
- [Material 3 type scale](https://m3.material.io/styles/typography/type-scale-tokens)
- [Apple Typography / Dynamic Type sizes](https://developer.apple.com/design/human-interface-guidelines/typography#Specifications)
- [Compose font scaling](https://developer.android.com/develop/ui/compose/text/fonts#font-scaling)

---

## ADR-014 — Accept before exit: a synchronous `canAnswer` query (FR-011)

**Decision**: `GameViewModel` gains a side-effect-free query, `canAnswer(cardIndex: Int): Boolean`.
It returns true exactly when an `OnAnswerClick` for that card would be recorded now: the phase is
Asking, the index is the current card's, and the 300 ms cooldown (ADR-012) has passed.

When a swipe is released past the threshold, or a button is tapped, each UI does this:
1. Ask `canAnswer`.
2. If it says yes, start the exit animation, which uses the card snapshot the UI already holds, and
   send `OnAnswerClick`.
3. If it says no, spring the card back (a tap does nothing).

`onAction` still applies the same guard, so the ViewModel stays the single source of truth.

**Rationale**:
- Spec FR-011 forbids a card that flies off without its answer being recorded.
- Android updates state synchronously, but iOS receives the new state on the next main-actor hop
  through SKIE's `AsyncSequence`. So "send, then watch whether the state advanced" would need a
  timeout on iOS.
- A synchronous query is deterministic on both platforms, keeps the rule in `shared`, and can be
  unit-tested (contract guarantee G15).

**Alternatives considered**:
- **Observe the state after sending, with a timeout:** racy on iOS, and adds visible latency.
- **An `acceptsAnswers` flag in `GameState`, flipped by a delayed coroutine:** the 300 ms flip would
  have to run on the main dispatcher. Robolectric's paused looper doesn't advance with
  `Thread.sleep`, which would break the existing robot tests, and it duplicates ADR-012's
  `TimeSource` rule.
- **`AnchoredDraggableState.confirmValueChange`:** deprecated in Foundation 1.12 (see ADR-015).

---

## ADR-015 — Swipe mechanics per platform (FR-006 to FR-010, FR-016)

**Android decision**:
- The card uses `Modifier.draggable(orientation = Horizontal)` from Foundation 1.12.1 (stable).
  `onDragStopped` reports the release velocity.
- An `Animatable<Float>` drives the offset. Tilt is `offset / width × 12°`.
- On release:
  - the card leaves if |offset| ≥ ⅓ of its width, or if |velocity| ≥ 1,200 dp/s in the drag's
    direction (FR-007);
  - leaving means `canAnswer` (ADR-014), then an animation off-screen with
    `spring(stiffness = StiffnessMediumLow)` and the release velocity as the initial velocity;
  - otherwise the card springs back with `spring(dampingRatio = DampingRatioMediumBouncy)`.
- Vertical drags never reach a horizontal `draggable`, so FR-008 holds.
- When `scrolls` is true (ADR-013), the vertical scroll container nests inside the horizontal
  draggable, so each axis is handled by its own gesture.
- Interruptions (FR-016) are handled as follows:
  - a configuration change rebuilds the composable at offset 0;
  - leaving the screen or a new game cancels the drag through the composition, and the offset
    snaps back to 0.

**iOS decision**:
- `DragGesture(minimumDistance: 12)` with `.onChanged` and `.onEnded`.
- `.offset(x:)` and `.rotationEffect(.degrees(offset / width × 12))`.
- On end: `value.predictedEndTranslation.width` gives the flick (iOS 17).
- Animations use `withAnimation(.snappy)` to leave and `.bouncy` to return (iOS 17).
- Exit and entry are explicit transitions on a `ZStack` keyed by the card index.
- A drag that is mostly vertical (|dy| > |dx| at start) is ignored. FR-004's scroll case uses a
  `ScrollView` inside a horizontal-only drag.

**Rationale**:
- Both use each platform's stable, standard gesture and spring primitives (FR-026), with no
  dependency.
- The decision to leave stays explicit, because it depends on the ViewModel (ADR-014).

**Alternatives considered**:
- **`anchoredDraggable`** (stable): its anchors fit "settle into Left, Center or Right", but vetoing
  a settle needs the deprecated `confirmValueChange`. Settling and then animating back on a
  rejection looks broken.
- **Material 3 `SwipeToDismissBox`:** it's for list rows, and doesn't tilt or fling cards.
- **Third-party swipe-card libraries:** unnecessary for one card.

**Sources**:
- [Compose drag and swipe](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/drag-swipe-fling)
- [Compose spring animations](https://developer.android.com/develop/ui/compose/animation/customize#spring)
- [SwiftUI DragGesture](https://developer.apple.com/documentation/swiftui/draggesture)
- [SwiftUI Animation.snappy and bouncy](https://developer.apple.com/documentation/swiftui/animation)

---

## ADR-016 — Android: Material You with a generated fallback (FR-022, FR-026)

**Decision**:
- **Dynamic colour on API 31+:** `dynamicLightColorScheme` and `dynamicDarkColorScheme`. The app
  already does this; it now feeds every new surface.
- **API 26–30:** a Wáay Material 3 scheme, light and dark, generated with Material Color Utilities
  (TonalSpot, the same algorithm as Material Theme Builder) from the seed `#4527A0`, which is the
  iOS accent and the app icon's violet. The values are committed in `ui/theme/Color.kt` with the
  seed and generator noted. They were generated (T002) with **materialyoucolor 3.0.4** by
  `scripts/generate-android-fallback-colors.py`, covering all 48 roles that Material 3 1.4.0's
  `lightColorScheme` and `darkColorScheme` take.
- **Card screen roles:**
  - backdrop: `primaryContainer`, with `onPrimaryContainer` for top-bar content;
  - card: `surfaceContainerLowest`, with `onSurface` numbers;
  - "Yes": a filled `Button` (`primary`);
  - "No": an `OutlinedButton` on a `surface` container, the Slack-like light secondary;
  - wide-layout panels: tonal surfaces in the same roles.
- **Motion:** the stable `spring` specs above. **Material 3 Expressive is not adopted yet.** In
  Material 3 1.4.0, which is in the BOM, `MotionScheme.expressive()`, `MaterialExpressiveTheme`
  and `MaterialTheme(motionScheme = …)` are `internal`, as seen in the 1.4.0 sources. Constitution
  VII requires stable APIs.
- **Haptics:** `HapticFeedbackType.GestureThresholdActivate` when the threshold is crossed, and
  `Confirm` on an accepted answer (Compose UI 1.12.1).

**Revisit trigger**: a stable Material 3 release that makes the Expressive motion scheme and theme
public. Renovate proposes it, and the springs then come from `MaterialTheme.motionScheme`.

**Sources**:
- [Material Design 3 in Compose: dynamic colour](https://developer.android.com/develop/ui/compose/designsystems/material3)
- [Material Color Utilities](https://github.com/material-foundation/material-color-utilities)
- [M3 colour roles](https://m3.material.io/styles/color/roles)

---

## ADR-017 — iOS: Liquid Glass with a materials fallback (FR-023 to FR-025)

**Decision**, with every glass API behind `if #available(iOS 26, *)`. The API names and
availability were read from the SDK interfaces.

| Element | iOS 26+ (Liquid Glass) | iOS 17–25 (fallback) |
|---|---|---|
| Top bar | System navigation bar with toolbar items: glass automatically on 26 | System navigation bar |
| "Yes" button / panel | `.buttonStyle(.glassProminent)` tinted with the accent | `.buttonStyle(.borderedProminent)` |
| "No" button / panel | `.buttonStyle(.glass)` | `.buttonStyle(.bordered)` on `.thinMaterial` |
| Answer controls group | `GlassEffectContainer` so the two glass shapes blend and morph | `HStack` |
| Card (numbers) | **Opaque** `Color(.systemBackground)` rounded rectangle, no glass (FR-025) | same |
| Backdrop | Accent-tinted gradient (`#23143F` → `#4527A0`) for the glass to refract | same |

- Reduce Transparency and Increase Contrast are honoured by the system glass and materials.
- Text on the backdrop is white on `#23143F`/`#4527A0`, which is at least 10:1.

**Rationale**:
- Apple's guidance puts glass on the navigation and control layer, never on content, and that is
  also FR-025.
- The availability checks keep the iOS 17.0 deployment target.

**Alternatives considered**:
- **Glass on the card:** it fails legibility and Apple's guidance.
- **A custom blur:** it isn't Liquid Glass and doesn't adapt to accessibility settings.

**Sources**:
- [Applying Liquid Glass to custom views](https://developer.apple.com/documentation/swiftui/applying-liquid-glass-to-custom-views)
- [Adopting Liquid Glass](https://developer.apple.com/documentation/technologyoverviews/adopting-liquid-glass)
- SDK: `SwiftUICore` `glassEffect(_:in:)`, `Glass.tint(_:)`, `Glass.interactive(_:)` and
  `GlassEffectContainer`, plus `SwiftUI` `GlassButtonStyle` and `GlassProminentButtonStyle`, all
  `@available(iOS 26.0, *)`.

---

## ADR-018 — Reduce motion (FR-014)

**Decision**:
- **Android:** read `Settings.Global.ANIMATOR_DURATION_SCALE` ("Remove animations" sets it to 0)
  through a small `rememberReduceMotion()`. When it is on, the card exit and entry use a 150 ms
  cross-fade, with no tilt and no spring. Compose also scales its own animations by that factor.
- **iOS:** `@Environment(\.accessibilityReduceMotion)` chooses `.opacity` transitions, with no
  rotation.
- Dragging still answers on both platforms.

**Sources**:
- [Android animator duration scale](https://developer.android.com/reference/android/provider/Settings.Global#ANIMATOR_DURATION_SCALE)
- [SwiftUI accessibilityReduceMotion](https://developer.apple.com/documentation/swiftui/environmentvalues/accessibilityreducemotion)

---

## ADR-019 — Layout per window class (FR-012a, FR-019 to FR-021)

**Decision**: the existing `AdaptiveGameLayout` (Android) and the size-class switch (iOS) map the
spec's layouts. The answers always read No (left), then Yes (right).

| Layout | Answers |
|---|---|
| Compact: Android `Stacked`; iOS regular height with compact width | Row under the card |
| Wide: Android `SideBySide`; iOS regular width or compact height | Two flanking panels about 112 dp/pt wide |
| Tabletop | Card above the hinge, answer row below it |
| Book | Card left of the hinge, both answers side by side right of it |

- The top bar centres the progress during cards, and the app title on the intro and result.
- New game sits at the leading end and Settings at the trailing end.
- The card's header is the question.
- The stack hint is two blank card backs offset behind the card. They show no numbers (FR-015).

---

## ADR-020 — The iOS number size and the accessibility audit

**Risk**: SwiftUI numbers drawn at a computed `.system(size:)` might be flagged by
`performAccessibilityAudit` as "Dynamic Type unsupported".

**Decision**:
- Derive the minimum from `@ScaledMetric(relativeTo: .subheadline)`, so the computed size changes
  with Dynamic Type.
- **Spike first (task T001 of this feature):** render the grid at the computed size, then run the
  existing audit at the default size and at AX5.
- If the audit still flags the numbers, apply the scale-relative font (`Font.custom` with
  `relativeTo:`), with the base size computed at the default content size.
- An allowance is the last resort, and only with by-eye evidence, as in T112.

**Spike result (T001, 2026-10-04): approach A passes, so B is not needed.** The card's numbers were
drawn with `.font(.system(size: scaledMinimum × factor).monospacedDigit())`, where `scaledMinimum`
comes from `@ScaledMetric`. `performAccessibilityAudit` on the card screen reported **zero issues** on:
- iPhone 17 (iOS 27.0), at the default size (L) and at Accessibility XXXL;
- iPhone 16 (iOS 18.6), at the same two sizes.

The audit accepts a computed size as long as it follows the content size category.

---

## ADR-021 — Testing strategy for spec 002

- **`commonTest`:**
  - `CardGridFitTest`, exhaustive over n ∈ {4, 8, 16, 32, 64} × the configuration matrix above ×
    minimum sizes. It checks:
    - fits and no-scroll where the table says so;
    - every cell inside the area;
    - the chosen columns maximise the size (brute-force comparison);
    - the size is at least the minimum or `scrolls` is true;
    - the cap is respected.
  - `GameViewModelTest` for G15, `canAnswer`.
- **Android (Robolectric):**
  - `SwipeAnswerTest`: `performTouchInput { swipeRight() / swipeLeft() }`, a short drag, a vertical
    drag, a flick, and a swipe during the cooldown springing back.
  - `CardFitTest`: 7 cards on a 360×640 dp phone. The 64 number nodes are fully on screen, and no
    vertical-scroll semantics exist.
  - `ButtonOrderTest`: No is left of Yes, in compact, wide and fold layouts.
  - Roborazzi baselines re-recorded for every phase × size × light/dark × dynamic and fallback
    colours. The 64 existing images change, and each is reviewed.
- **iOS (XCUITest):**
  - `SwipeAnswerUITests`: `swipeRight` / `swipeLeft`, a short drag through
    `press(forDuration:thenDragTo:)`, and a swipe right after a card appears springing back.
  - A no-scroll check: 7 cards on the smallest CI iPhone, with all 64 number elements hittable
    without scrolling.
  - Button order.
  - The accessibility audit on every screen. The existing allowances stay, and nothing new is
    allowed without evidence.
- **CI:** the same four jobs. They run with the Xcode 26.4 SDK, which has Liquid Glass, and on iOS
  17.5, which takes the fallback path.
