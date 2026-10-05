# Tasks: Card Fit, Swipe Answers and Native Look

**Input**: Design documents from `specs/002-card-fit-swipe-answers/`: [plan](./plan.md),
[spec](./spec.md), [research](./research.md) (ADR-013 to ADR-021), [data model](./data-model.md),
[contracts](./contracts/) and [quickstart](./quickstart.md).

**Tests**: required. Constitution VI is test-first: each story writes its tests first and sees them
fail.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: can run in parallel (different files, no dependency on an unfinished task).
- **[Story]**: US1 card fit (P1), US2 swipe (P2), US4 native look (P2), US3 no-gesture and reduced
  motion (P3).

**Paths**:
- `S/` = `shared/src`;
- `A/` = `androidApp/src`, with main code under `A/main/kotlin/dev/epool/waay/android/` and tests
  under `A/test/kotlin/dev/epool/waay/android/`;
- `I/` = `iosApp/`.

---

## Phase 1: Setup

- [X] T001 Run the spike for ADR-020 to settle how iOS sizes the numbers.
  1. Add a throwaway view that draws 64 numbers in a hand-computed 6 × 11 grid at a size derived from
     `@ScaledMetric(relativeTo: .subheadline)`.
  2. Run `performAccessibilityAudit` on it at the default size and at Accessibility XXXL, on iPhone 17
     (iOS 27) and iPhone 16 (iOS 18.6).
  3. Record in `specs/002-card-fit-swipe-answers/research.md` ADR-020 which approach passes:
     A, a computed `.system(size:)`; or B, `Font.custom(…, relativeTo:)`.
  4. Delete the spike code.
- [X] T002 [P] Generate the Android fallback colour scheme with Material Color Utilities:
  `uvx --from materialyoucolor`, TonalSpot, seed `#4527A0`, light and dark, every M3 role.
  - Write it to `A/main/kotlin/dev/epool/waay/android/ui/theme/Color.kt`, with a header naming the
    generator, its version and the seed.
  - Record the version in research ADR-016.

---

## Phase 2: Foundational (blocks every story)

- [X] T003 [P] Write `S/commonTest/kotlin/dev/epool/waay/game/presentation/CardGridFitTest.kt`:
  - cover F1–F8 from [card-grid-fit](./contracts/card-grid-fit.md);
  - use the configuration matrix from ADR-013, n ∈ {4, 8, 16, 32, 64}, and minimum sizes
    {14, 18.2, 21, 28};
  - for F3, brute-force every column count.

  It must fail to compile or fail.
- [X] T004 Implement `CardGrid` and `CardGridFit` in
  `S/commonMain/kotlin/dev/epool/waay/game/presentation/CardGridFit.kt`, following the contract: the
  algorithm, font model constants, cap and scroll fallback of ADR-013, with explicit API.
  - Green on the Android host and `iosSimulatorArm64`.
  - `koverVerify` still ≥ 90%.
- [X] T005 [P] Write G15a–e in `S/commonTest/kotlin/dev/epool/waay/game/presentation/GameViewModelTest.kt`,
  using `TestTimeSource` for the cooldown.
  - They must fail.
- [X] T006 Implement `GameViewModel.canAnswer(cardIndex: Int): Boolean` in
  `S/commonMain/kotlin/dev/epool/waay/game/presentation/GameViewModel.kt`. One private guard serves
  both `canAnswer` and `onAction`, so they can't diverge (G15e).
- [X] T007 Pass `canAnswer` through to both UIs, with no visible change yet:
  - Android: `GameRoot` passes `viewModel::canAnswer` to `GameScreen(canAnswer = …)`, and previews and
    screenshot tests pass `{ true }`.
  - iOS: `I/iosApp/Game/GameModel.swift` gets `canAnswer(cardIndex:)`, and `GameScreen` takes a
    `canAnswer` closure.
  - Both suites stay green.

**Checkpoint**: the shared logic is done and tested. The UIs compile against the new API.

---

## Phase 3: User Story 1 - See the whole card at once (P1) 🎯 MVP

**Goal**: every card's numbers fill the card without scrolling (FR-001 to FR-005). The text size wins
at accessibility sizes.

**Independent test**: 7 cards on the smallest phone in portrait shows 64 numbers with no scrolling.
3 cards on a tablet shows 4 large numbers.

### Tests first

- [X] T008 [P] [US1] Write `A/test/kotlin/dev/epool/waay/android/game/CardFitTest.kt`:
  - 7 cards at `w360dp-h640dp`, default font: all 64 `number.N` nodes are fully within the root bounds,
    and `card.numbers` has no `VerticalScrollAxisRange`;
  - 3 cards at `w800dp-h1280dp`: the numbers' bounds cover ≥ 80% of `card.numbers` (SC-002).
- [ ] T009 [P] [US1] Write `I/WaayUITests/CardFitUITests.swift`: 7 cards on the smallest CI iPhone class
  (iPhone 16e or 17e). Every `number.N` element is hittable without any swipe or scroll.
- [ ] T010 [US1] Move FR-003a's coverage to FR-004's fallback configuration. Rework
  `A/test/kotlin/dev/epool/waay/android/game/CardScrollTest.kt` (font scale 2.0 on `w360dp-h640dp`)
  and `I/WaayUITests/CardScrollUITests.swift` (AX5), so the scrolling case still starts each card at
  the top.

### Implementation

- [ ] T011 [US1] Create `A/main/kotlin/dev/epool/waay/android/game/CardGridView.kt`:
  - `BoxWithConstraints` measures the area;
  - the minimum is `typography.bodyMedium` converted to dp (font-scale aware), with a 72 dp cap;
  - it calls `CardGridFit.fit`, then draws rows and columns with tabular digits (`tnum`);
  - it uses `verticalScroll` only when `scrolls`;
  - it keeps the `card.numbers` and `number.N` tags and the number semantics.
- [ ] T012 [US1] Use `CardGridView` in `A/main/kotlin/dev/epool/waay/android/game/GameScreen.kt` in place
  of the `LazyVerticalGrid`. Keep `key(content.index)` so a fallback scroll still starts at the top.
- [ ] T013 [P] [US1] Create `I/iosApp/Game/CardGridView.swift`:
  - `GeometryReader` measures the area;
  - the minimum follows T001's approach;
  - it calls `CardGridFit.shared.fit`, then lays out a fixed-column grid with `monospacedDigit`;
  - it wraps the grid in a `ScrollView` with `.id(card.index)` only when `scrolls`;
  - the identifiers are unchanged.
- [ ] T014 [US1] Use `CardGridView` in `I/iosApp/Game/GameScreen.swift` (`CardView`).
- [ ] T015 [US1] Re-record the card-phase Roborazzi baselines in
  `A/test/kotlin/dev/epool/waay/android/screenshots/GameScreenScreenshotTest.kt` and review every image.
  - Run both suites green, including iOS 18.6.

**Checkpoint**: US1 works alone, with no scrolling, on the current layout.

---

## Phase 4: User Story 2 - Answer by swiping the card (P2)

**Goal**: a Slack-style card stage (FR-019 to FR-021) with a draggable card. Right is Yes and left is
No, with tilt, hint labels, a flick and a spring-back. A card leaves only when its answer is accepted
(FR-006 to FR-012a, FR-015 to FR-017).

**Independent test**: play a full game by swiping only. Short and vertical drags answer nothing.

### Tests first

- [ ] T016 [P] [US2] Write `A/test/kotlin/dev/epool/waay/android/game/SwipeAnswerTest.kt` for U1–U6 and
  U11 from [card-screen-ui](./contracts/card-screen-ui.md):
  - `performTouchInput { swipeRight() }` and `swipeLeft()` play a game to the secret 27;
  - a short drag and a vertical drag answer nothing;
  - a fast short flick answers;
  - a swipe right after a card appears springs back;
  - the hint `card.hint.yes` or `card.hint.no` appears mid-drag;
  - FR-015: with the stack hint visible, exactly `numbersPerCard` `number.*` nodes exist, so the
    preview shows no numbers;
  - FR-016 (U9): hold a drag part-way, recreate the Activity, and nothing is answered;
  - FR-017: mid-drag, `card.progress`, `toolbar.newGame` and `toolbar.settings` are displayed;
  - multi-touch: a second pointer during a drag records no extra answer (spec edge case);
  - FR-026: a unit test that the stage's motion specs (`CardMotion`) are springs, plus a Roborazzi
    frame mid-drag showing the tilt and hint.
- [ ] T017 [P] [US2] Write `I/WaayUITests/SwipeAnswerUITests.swift` with the same cases:
  - `swipeRight` and `swipeLeft(velocity:)`;
  - `press(forDuration:thenDragTo:)` for the short drag;
  - `swipeUp` for the vertical drag.
- [ ] T018 [P] [US2] Write `A/test/kotlin/dev/epool/waay/android/game/ButtonOrderTest.kt`:
  - compact `w411dp-h914dp`, wide `w900dp-h600dp`, and tabletop and book via the `layout` parameter;
  - check `card.no` is left of `card.yes`;
  - when wide, check `card.no` is left of `card.surface` and `card.yes` is right of it.

  Add the equivalent assertions to `I/WaayUITests/CardFitUITests.swift` for iPhone, and to an iPad
  case that's skipped when the device isn't regular width.

### Implementation

- [ ] T019 [US2] Rework `A/main/kotlin/dev/epool/waay/android/adaptive/AdaptiveGameLayout.kt` for the card
  stage (ADR-019):
  - Stacked: the card, with the answer row below;
  - SideBySide: [No panel] card [Yes panel];
  - Tabletop: the card above the hinge, the row below;
  - Book: the card left of the hinge, the row right of it.

  Keep the hinge gutters and the `movableContentOf` slots.
- [ ] T020 [P] [US2] Create `A/main/kotlin/dev/epool/waay/android/game/AnswerControls.kt`:
  - a No/Yes row (compact) and flanking panels (wide), tagged `card.no` and `card.yes`, with No on
    the left;
  - a tap checks `canAnswer`, then starts the stage's exit animation and sends `OnAnswerClick`
    (FR-013).
- [ ] T021 [US2] Create `A/main/kotlin/dev/epool/waay/android/game/CardStage.kt`, following ADR-015:
  - the card's question header and `CardGridView`;
  - `Modifier.draggable(Horizontal)` with an `Animatable` offset, and tilt of ±12°;
  - hint labels `card.hint.*` that fade in, using `yesLabel` and `noLabel`;
  - a threshold of ⅓ of the width and a flick at 1,200 dp/s;
  - the `canAnswer` gate;
  - an exiting snapshot that flies out while the next card enters from the stack, which is two blank
    backs;
  - haptics: `GestureThresholdActivate` and `Confirm`;
  - cancel on interruption (U9).
- [ ] T022 [US2] Update `A/main/kotlin/dev/epool/waay/android/game/GameScreen.kt`:
  - a `CenterAlignedTopAppBar`, with the progress centred during cards (tag `card.progress`) and the
    title otherwise;
  - New game at the start and Settings at the end;
  - the backdrop;
  - intro and result drawn as cards on the backdrop.
- [ ] T023 [P] [US2] Rework `I/iosApp/Game/AdaptiveGameLayout.swift`: a row under the card for compact
  size classes, and flanking panels for wide (ADR-019).
- [ ] T024 [P] [US2] Create `I/iosApp/Game/AnswerControls.swift` for the row and panels. It uses
  standard styles for now; US4 adds the glass. A tap checks `canAnswer`, then animates and sends.
- [ ] T025 [US2] Create `I/iosApp/Game/CardStage.swift`:
  - `DragGesture`, offset and rotation;
  - hint labels;
  - the flick from `predictedEndTranslation`;
  - the `canAnswer` gate;
  - exit and entry transitions keyed by the card index;
  - the stack hint;
  - `sensoryFeedback`.
- [ ] T026 [US2] Update `I/iosApp/Game/GameScreen.swift`:
  - the toolbar's `.principal` item shows the progress (`card.progress`);
  - New game is leading and Settings trailing;
  - add `I/iosApp/Theme/Backdrop.swift` (the accent gradient);
  - the question is the card header;
  - intro and result are drawn as cards.
- [ ] T027 [US2] Keep every existing suite green:
  - update `A/test/kotlin/dev/epool/waay/android/game/GameRobot.kt` and the iOS UI test helpers in
    `I/WaayUITests/`, adding swipe helpers;
  - run all Android tests, and iOS on iPhone 17 (27), iPhone 16 (18.6) and iPad.

**Checkpoint**: the full game can be played by swiping or by buttons on both platforms.

---

## Phase 5: User Story 4 - Feels at home on each platform (P2)

**Goal**: Material You on Android, with the fallback on 8–11. Liquid Glass on iOS 26+, with
materials on 17–25. The card stays solid, and contrast holds (FR-022 to FR-026).

**Independent test**: on Android 12+, the colours follow the wallpaper; on Android 8–11, the brand
scheme shows. On iOS 26, the controls are glass. Light and dark modes pass the accessibility checks.

### Tests first

- [ ] T028 [P] [US4] Write `A/test/kotlin/dev/epool/waay/android/ui/ThemeTest.kt`:
  - at `sdk = 30`, `MaterialTheme.colorScheme` equals the generated fallback in light and dark;
  - at `sdk = 36`, it uses the dynamic scheme.

  Add screenshot variants for light and dark × `sdk` 30 and 36 to the Roborazzi tests.
- [ ] T029 [P] [US4] Extend `I/WaayUITests/AccessibilitySemanticsUITests.swift` to audit the card stage
  in light and dark (`XCUIDevice.shared.appearance`). For FR-023 and SC-009, add
  `I/WaayTests/AnswerControlStyleTests.swift` (Swift Testing): the pure helper
  `AnswerControlStyle.for(majorVersion:)` returns glass for 26+ and materials below.

### Implementation

- [ ] T030 [US4] Update `A/main/kotlin/dev/epool/waay/android/ui/theme/Theme.kt`:
  - dynamic colour on API 31+, and the `Color.kt` fallback on 26–30;
  - apply the card-stage roles from [card-screen-ui](./contracts/card-screen-ui.md) across `CardStage`,
    `AnswerControls` and `GameScreen`;
  - `shapes.extraLarge` for the card;
  - Material 3 components on intro, result and Settings.
- [ ] T031 [US4] Add Liquid Glass in `I/iosApp/Game/AnswerControls.swift` and
  `I/iosApp/Game/GameScreen.swift`, choosing the style through `AnswerControlStyle` (T029's helper):
  - under `if #available(iOS 26, *)`, `.glassProminent` with the accent tint for Yes, `.glass` for No,
    and a `GlassEffectContainer` around the answers;
  - the fallback is `.borderedProminent`, and `.bordered` on `.thinMaterial`;
  - the toolbar is the system bar;
  - the card stays an opaque `systemBackground`.
- [ ] T032 [US4] Verify contrast:
  - Android: review the screenshot pairs (text on the backdrop, the panels and the card) in light and
    dark;
  - iOS: the audit passes in light and dark;
  - check Reduce Transparency and Increase Contrast by hand (quickstart M12), and record the result.
- [ ] T033 [US4] Re-record and review the Roborazzi baselines for light/dark × dynamic/fallback.

**Checkpoint**: each platform looks native, and both pass their accessibility checks.

---

## Phase 6: User Story 3 - Answer without gestures, and with less motion (P3)

**Goal**: button answers animate like swipes (FR-013). Reduce motion swaps in cross-fades (FR-014).
Screen readers complete a game (FR-012).

**Independent test**: play with the screen reader and buttons only. With reduce motion on, cards
cross-fade.

### Tests first

- [ ] T034 [P] [US3] Write `A/test/kotlin/dev/epool/waay/android/game/ReduceMotionTest.kt`:
  - with `Settings.Global.ANIMATOR_DURATION_SCALE = 0`, `rememberReduceMotion()` is true, a swipe
    still answers, and the card shows no tilt mid-drag (`rotationZ == 0` in the graphics layer);
  - a button tap answers once.
- [ ] T035 [P] [US3] Extend `A/test/kotlin/dev/epool/waay/android/game/GameFlowTest.kt` (`gameIsUsableThroughSemantics`)
  and `I/WaayUITests/AccessibilitySemanticsUITests.swift`: the card surface reads its header and
  numbers, and a game completes with the buttons alone.

### Implementation

- [ ] T036 [US3] Create `A/main/kotlin/dev/epool/waay/android/ui/ReduceMotion.kt`
  (`rememberReduceMotion()`, ADR-018). Use it in `CardStage.kt`: a 150 ms cross-fade, with no tilt or
  fly.
- [ ] T037 [P] [US3] In `I/iosApp/Game/CardStage.swift`, use `@Environment(\.accessibilityReduceMotion)`
  to switch to `.opacity` transitions with no rotation.
- [ ] T038 [US3] Make sure button answers run the same directional exit animation as swipes on both
  platforms (FR-013, U7), if T020 and T024 didn't already finish it.

**Checkpoint**: all four stories are complete.

---

## Phase 7: Polish & cross-cutting

- [ ] T039 Align the F6 expectations in `CardGridFitTest` and the quickstart with the narrowed SC-001.
  The owner accepted it at the plan gate, and `spec.md` was updated then.
- [ ] T040 Re-record the full Roborazzi set (intro, card and result × sizes × fonts × postures ×
  light/dark × dynamic/fallback, plus Settings). Review every image, delete obsolete baselines, and
  commit.
- [ ] T041 [P] Update the documentation:
  - the quickstart run log in `specs/002-card-fit-swipe-answers/quickstart.md`;
  - the `README.md` status row for 002;
  - `AGENTS.md` §6 if any command changed.
- [ ] T042 Run the full `specs/002-card-fit-swipe-answers/quickstart.md` §1 and §3 on both platforms:
  - Android plus iOS 27, iOS 18.6 and iPad;
  - the manual scenarios M1–M15 that the emulators and simulators allow;
  - record the results.
- [ ] T043 Run `/speckit-converge` until it reports converged. Any new tasks are appended to
  `specs/002-card-fit-swipe-answers/tasks.md`.

---

## Dependencies & Execution Order

- **Setup (T001, T002) → Foundational (T003–T007)** before any story. T001 decides T013's approach.
- **US1 (T008–T015)** depends on T004 and T007. It is the MVP and ships on its own.
- **US2 (T016–T027)** depends on US1, because the stage embeds `CardGridView`, and on T006 and T007.
- **US4 (T028–T033)** depends on US2, because it styles the stage pieces.
- **US3 (T034–T038)** depends on US2. It can run in parallel with US4 except in the shared
  `CardStage` files, so sequence T036 and T037 after T030 and T031 if they collide.
- **Polish (T039–T043)** comes last. T039 needs the owner's SC-001 decision.

## Parallel examples

- Foundational: T003 ∥ T005, the two test files; then T004 ∥ T006.
- US1: T008 ∥ T009 (tests), then T011 → T012 on Android ∥ T013 → T014 on iOS.
- US2: T016 ∥ T017 ∥ T018 (tests), then Android T019 → T020 ∥ T021 → T022 ∥ iOS T023 ∥ T024 → T025 → T026.
- US4: T028 ∥ T029, then T030 (Android) ∥ T031 (iOS).

## Implementation strategy

1. **MVP = US1.** The no-scroll card fit is the correctness win, with the risk of missed numbers
   removed. Stop and demo after T015 if wanted.
2. **US2** adds the new interaction and the Slack-style stage.
3. **US4** makes each platform native.
4. **US3** completes accessibility and motion preferences.
5. Each story ends green on both platforms, so `kmp` could take any prefix of them.
