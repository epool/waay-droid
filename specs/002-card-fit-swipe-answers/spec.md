# Feature Specification: Card Fit, Swipe Answers and Native Look

**Feature Branch**: `002-card-fit-swipe-answers`

**Created**: 2026-10-04

**Status**: Draft

**Input**: User description: "Two new requirements: 1. The numbers layout should fill the screen without needing to scroll. 2. New UI to answer yes or no with a swipe right and left gesture with animation, similar to Slack's Catch up feature." Follow-ups: "Layout should be similar to Slack's" (with reference screenshots); "the app must follow Material You on Android and crystal glass on iOS".

Builds on [spec 001 — Magic Cards Game v1](../001-magic-cards-game/spec.md). Everything spec 001 requires
still holds unless a requirement below says it replaces part of it.

## Clarifications

### Session 2026-10-04

- Q: When the system text size is so large that 64 numbers can't fit on a phone at that size, which
  wins, "no scrolling" or the text size? → A: The text size wins. Cards fit without scrolling whenever
  the chosen text size allows, which covers the default and the larger standard sizes. Only at sizes
  where that is impossible (accessibility sizes) may the card scroll, as in spec 001 (FR-004).
- Q: Do the visible Yes/No buttons stay alongside swiping? → A: Yes. Both buttons stay visible on
  every card, and tapping one throws the card in the matching direction, as a swipe does. Swiping is
  an additional, faster way to answer (FR-012, FR-013).
- Q: Should the Yes/No buttons be reordered to mirror the swipe directions? → A: Yes. "No" is on the
  left and "Yes" on the right in every layout. Where spec 001 stacked the buttons beside the numbers,
  they now sit side by side under the card (FR-012a).
- Q: What should the card screen look like? → A: Like Slack's "Catch up", from the owner's reference
  screenshots for phone portrait and a wide window:
  - a full-bleed dark brand backdrop;
  - a top bar with the progress in the middle and the screen's controls at its ends;
  - the current card as a large, light, rounded card filling most of the space, with a hint of the
    stack behind it;
  - in compact layouts, two large buttons side by side under the card: "No" on the left (outlined) and
    "Yes" on the right (filled);
  - in wide layouts, the two answers become tall panels flanking the card: "No" on its left, "Yes" on
    its right.

  See FR-019 to FR-021.
- Q: At the plan gate, given that 64 numbers can't fit at the default text size on small phones in
  landscape or in a phone's split-screen half, how should SC-001 read? → A: Narrow it. Those small
  windows use FR-004's scroll fallback; every other configuration still never scrolls (SC-001).
- Q: What visual style should each platform follow? → A: Each platform's current design language,
  across the whole app. On Android that is Material You: colours come from the user's wallpaper and
  theme, with Material 3 components, shapes and motion. On iOS it is Apple's Liquid Glass (the owner
  said "crystal glass"): translucent glass for controls and bars over the content. Older OS versions
  without these get the closest native look (FR-022 to FR-026).

### Session 2026-10-05

- Q: At the verification gate, should the spec match the as-built iOS styles (ADR-017) and the
  narrowed SC-001 everywhere? → A: Yes (T046, T047). Glass "No" is tinted with the system background,
  and iOS 17–25 use solid standard controls, because translucent surfaces took on the violet backdrop
  and failed contrast (FR-025). US1's landscape and split-screen promises follow SC-001's exceptions.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - See the whole card at once (Priority: P1)

A player answering a card sees every number on it at the same time, as large as the screen allows,
without scrolling. With 7 cards that is 64 numbers; with 3 cards it is 4. The numbers spread to fill
the space the card has, on any phone, tablet or foldable, in portrait or landscape, in split screen,
and in every fold posture.

**Why this priority**: Scrolling a card hides some of its numbers. A player who doesn't scroll can miss
their number, answer "No" by mistake and get a wrong reveal. Spec 001 had to add a rule (each card starts
scrolled to the top) to limit that risk. Showing everything at once removes it, and the trick feels faster
and more magical.

**Independent Test**: Play a 7-card game on the smallest supported phone in portrait, and a 3-card game
on a tablet. On every card, every number is visible and readable without scrolling, and the numbers fill
the card's area instead of sitting in a small block. In SC-001's exceptions (small phones in landscape,
phone split-screen halves) a 7-card game may scroll, starting at the top (FR-004).

**Acceptance Scenarios**:

1. **Given** a 7-card game on a small phone in portrait, **When** a card appears, **Then** all 64 numbers
   are visible at once, none is cut off or overlapping, and nothing scrolls.
2. **Given** the same game in landscape on a phone at least about 375 dp/pt tall, or in a half-width
   split-screen window on a tablet, **When** a card appears, **Then** all 64 numbers are still visible at
   once without scrolling. In the smaller windows SC-001 exempts, the card scrolls, starting at the top
   (FR-004).
3. **Given** a 3-card game on a tablet, **When** a card appears, **Then** its 4 numbers are shown large,
   using the available space, not as small cells in one corner.
4. **Given** a foldable in tabletop or book posture, **When** a card appears, **Then** all its numbers are
   visible at once on one side of the fold and none sits on the fold.
5. **Given** the player rotates, folds or resizes the window mid-card, **When** the layout changes,
   **Then** all numbers of the same card are still visible at once, re-arranged for the new space.
6. **Given** the system text size is set larger than the default but all numbers still fit at that
   size, **When** a card appears, **Then** they are shown at least at that size and nothing scrolls.
7. **Given** an accessibility text size at which the card's numbers cannot all fit, **When** a card
   appears, **Then** the numbers keep the chosen size and the card may scroll vertically, starting at
   the top (001/FR-026, 001/FR-003a).

---

### User Story 2 - Answer by swiping the card (Priority: P2)

The current card behaves like a real card the player can handle. Dragging it to the right means "Yes,
my number is on this card"; dragging it to the left means "No". While the player drags, the card
follows the finger and tilts, and the answer it is about to give appears on it. If the player lets go
far enough, or flicks it, the card flies off-screen in that direction and the next card comes in. If
the player lets go too early, the card springs back to the middle and nothing is answered. The layout
and style follow Slack's "Catch up" card stack (FR-019 to FR-021).

**Why this priority**: It makes answering faster and more playful, and closer to handling real magic cards.
It builds on Story 1: a card that fits without scrolling can be dragged without fighting a scroll
gesture.

**Independent Test**: Play a full 5-card game using only swipes, thinking of 27. Each swipe right
answers Yes, each swipe left answers No, short drags change nothing, and the reveal says 27.

**Acceptance Scenarios**:

1. **Given** a card is shown, **When** the player drags it right past the threshold and lets go, **Then**
   the card leaves to the right, the answer "Yes" is recorded for that card, and the next card (or the
   result) appears.
2. **Given** a card is shown, **When** the player drags it left past the threshold and lets go, **Then**
   the card leaves to the left and "No" is recorded.
3. **Given** a card is shown, **When** the player flicks it quickly right or left, even over a short
   distance, **Then** it counts as a swipe in that direction.
4. **Given** the player drags a card part-way and lets go before the threshold, **When** the card is
   released, **Then** it springs back to its resting place and no answer is recorded.
5. **Given** the player is dragging, **When** the card moves past the middle in either direction,
   **Then** a "Yes" or "No" label (in the current language) appears on the card, growing clearer as the
   card nears the threshold. It is never signalled by colour alone.
6. **Given** a card is flying off after an answer, **When** the player taps or swipes again before the
   next card has settled, **Then** nothing more is recorded: exactly one answer per card.
7. **Given** the player swipes the last card, **When** it leaves the screen, **Then** the result is
   revealed as in spec 001.

---

### User Story 3 - Answer without gestures, and with less motion (Priority: P3)

Players who can't or don't want to swipe, including screen-reader users, can still answer every card
in a single action. Players who have asked their device to reduce motion get the same game with
calmer transitions.

**Why this priority**: Spec 001 requires a full game to be possible with the platform screen reader
(001/FR-025). Gesture-only controls would break that, and accessibility guidance requires a simple
alternative to path-based gestures. Respecting reduced motion avoids discomfort for motion-sensitive
players.

**Independent Test**: With the screen reader on, complete a game without any swipe. Then turn on the
system's reduce-motion setting and play a game. Answers work, and cards change without flying or tilting.

**Acceptance Scenarios**:

1. **Given** the screen reader is on, **When** the player moves through the card, **Then** its progress
   and numbers are announced, and the player can answer with the visible Yes and No buttons without any
   swipe (FR-012).
2. **Given** a non-gesture answer is given, **When** it is recorded, **Then** the card leaves in the
   matching direction (right for Yes, left for No), the same as a swipe.
3. **Given** any layout (portrait, landscape, tablet, foldable posture, split screen), **When** a card is
   shown, **Then** the "No" button is to the left of the "Yes" button, on the same sides as the swipes.
   On a phone in portrait they are two large buttons side by side under the card. On a tablet, in
   landscape or on an unfolded foldable, they are tall panels on either side of the card.
4. **Given** the system's reduce-motion setting is on, **When** a card is answered, **Then** it is replaced
   by a short cross-fade instead of flying and tilting, and dragging still answers.

---

### User Story 4 - Feels at home on each platform (Priority: P2)

On Android the game looks and moves like a Material You app: its colours follow the player's
wallpaper and theme, and its buttons, cards, shapes and motion are Material 3. On iOS it looks like a
current iPhone or iPad app: the top bar and the answer controls are Liquid Glass, floating over the
content and reacting to it. On older OS versions without these styles the game still looks native, in
the closest style the device offers. Light and dark mode both work, and the numbers stay perfectly
readable.

**Why this priority**: A game that looks native feels polished and trustworthy, and matches the
other apps around it. It also shapes how the new card screen is drawn, so it belongs with Stories 1–3.

**Independent Test**:
- On an Android 12+ phone, change the wallpaper and theme colours: the game's colours follow.
- On an Android 8–11 phone: the game shows its own Material 3 colour scheme.
- On iOS 26+: the top bar and Yes/No controls are glass.
- On iOS 17–25: the controls use the system's standard controls.
- In every case, light and dark mode work and the accessibility checks pass.

**Acceptance Scenarios**:

1. **Given** an Android 12+ device with dynamic colours, **When** the player changes their wallpaper or
   theme colours and returns to the game, **Then** the backdrop, card, buttons and Settings use the new
   colours.
2. **Given** an Android device without dynamic colours (Android 8–11), **When** the game opens,
   **Then** it uses Wáay's own Material 3 colour scheme, in light or dark to match the device.
3. **Given** iOS 26 or later, **When** a card is shown, **Then** the top bar and the "No" and "Yes"
   controls are Liquid Glass, and the "Yes" control is tinted to stand out. The "No" glass is tinted
   enough for its label to meet the contrast rules.
4. **Given** iOS 17 to 25, **When** a card is shown, **Then** the same controls use the system's
   standard controls with solid fills, and everything else works the same.
5. **Given** any platform, **When** a card is shown, **Then** the numbers sit on a solid, opaque card,
   never on glass or a busy background, and meet the contrast rules in light and dark mode.
6. **Given** the device's Reduce Transparency or Increase Contrast setting is on, **When** the game
   is shown, **Then** glass and translucent surfaces become solid enough for the text on them to meet
   contrast rules.

---

### Edge Cases

- **Mostly vertical drag:** a drag that is mostly up or down does not answer and does not move the card
  sideways.
- **Drag interrupted:** if a rotation, fold, window resize, incoming call, app switch, opening Settings or
  "New game" happens mid-drag, the drag is cancelled with no answer. The game continues per spec 001
  (001/FR-029, 001/FR-006).
- **Answer cooldown:** spec 001 ignores answers within a short moment of a card appearing
  (001/FR-028). A swipe that lands in that moment must not look accepted: the card springs back
  instead of flying off. A card only leaves the screen once its answer has been recorded.
- **Two fingers or multiple touches:** only one drag is tracked at a time; extra touches are ignored.
- **Voice:** the next card's prompt interrupts any line still being spoken (001/FR-015). Swiping during
  speech is allowed.
- **Language switch mid-game:** the Yes/No hint labels follow the active language (001/FR-021).
- **Progress and title:** "Card N of M" stays visible and is not part of the moving card, or moves back
  with it. It is never lost while dragging.
- **Card stack:** a hint of the next card may be shown behind the current one. It never shows the next
  card's numbers or anything else that could reveal a card's bit value (001/FR-011).
- **Very wide or very tall windows:** the card keeps readable proportions and is centred. It does not
  stretch numbers into extreme aspect ratios.

## Requirements *(mandatory)*

Requirement IDs are local to this spec. "001/FR-xxx" refers to [spec 001](../001-magic-cards-game/spec.md).

### Functional Requirements

**Card fit**

- **FR-001**: When a card is shown, all of its numbers MUST be visible at the same time without
  scrolling, for every card count (3–7), at every text size where they fit (FR-004), and in every
  configuration spec 001 supports:
  - phones, tablets and foldables;
  - portrait and landscape;
  - split-screen and multi-window sizes;
  - folded, unfolded, tabletop and book postures.
- **FR-002**: The numbers MUST fill the space available for the card. The arrangement (rows and columns)
  MUST be chosen for the number count and the space's shape, so that the numbers are as large as
  possible while all fit. On large screens this does not mean small cells with wide empty margins.
- **FR-003**: Numbers MUST NOT be truncated, overlapped, or placed on or across a fold (001/FR-032).
  Every number MUST keep the same size on a given card, and stay readable.
- **FR-004**: Numbers MUST never be smaller than the player's chosen system text size. The text size
  takes precedence over fitting:
  - whenever all of the card's numbers fit at that size, FR-001 applies and nothing scrolls;
  - only when the chosen size makes that impossible, typically the accessibility text sizes with many
    numbers, MAY the card scroll vertically, keeping 001/FR-026 and starting at the top (001/FR-003a).

  Even then, a vertical scroll MUST NOT be taken for a swipe (FR-008).
- **FR-005**: When the available space changes mid-card (rotation, fold, resize), the same card's numbers
  MUST be re-arranged to fit the new space, keeping FR-001 to FR-003.

**Swipe to answer**

- **FR-006**: The current card MUST be draggable horizontally. Releasing it beyond the answer threshold
  to the right MUST record "Yes" for that card. Releasing it beyond the threshold to the left MUST
  record "No".
- **FR-007**: A quick flick right or left MUST count as a swipe in that direction even if the drag
  distance is below the threshold.
- **FR-008**: Releasing before the threshold, without a flick, MUST return the card to its resting
  position with no answer recorded. Mostly vertical drags MUST NOT answer.
- **FR-009**: While dragging, the card MUST follow the finger, tilt in the drag direction, and show a
  "Yes" or "No" label in the active language matching the answer it would give, growing clearer as it
  nears the threshold. Colour MAY reinforce the hint but MUST NOT be the only cue (001/FR-027).
- **FR-010**: An accepted answer MUST send the card off-screen in its direction with an animation,
  followed by the next card's entrance animation. After the last card, the result appears (001/FR-004,
  001/FR-005).
- **FR-011**: Exactly one answer MUST be recorded per card, whatever the mix of rapid swipes, flicks and
  taps (001/FR-028). A card MUST only leave the screen when its answer has been recorded; a rejected
  input returns the card to rest.
- **FR-012**: Visible "Yes" and "No" buttons MUST remain on every card alongside swiping, for every
  player. A game MUST stay completable with the buttons alone and with the platform screen reader alone
  (001/FR-025). Swiping is an additional way to answer, not a replacement.
- **FR-012a**: The buttons MUST mirror the swipe directions in every layout: "No" on the left and "Yes"
  on the right. This replaces spec 001's order ("Yes" first) and its vertical stacking beside the
  numbers. Where the buttons sit depends on the layout (FR-020, FR-021).
- **FR-013**: Answering with a button MUST trigger the same exit animation as a swipe, in the matching
  direction: right for Yes, left for No.
- **FR-014**: When the device's reduce-motion setting is on, cards MUST change with a short cross-fade
  instead of flying, tilting and springing. Dragging MUST still answer.
- **FR-015**: Nothing about dragging or the card's appearance may reveal which bit value a card represents
  (001/FR-011). A preview of the next card, if shown, MUST NOT show its numbers.
- **FR-016**: A drag in progress MUST be cancelled, with no answer, when the game is interrupted or
  changed: rotation, fold, resize, app switch, opening Settings, or "New game".
- **FR-017**: Progress ("Card N of M") and the screen's other controls (New game, Settings) MUST stay
  visible and usable while a card is shown and while it is dragged.

**Card screen layout (Slack "Catch up" style)**

- **FR-019**: During the card phase the screen MUST show:
  - a full-bleed backdrop in the platform's style (FR-022, FR-023);
  - a top bar with the progress ("Card N of M") centred and the screen's controls (New game,
    Settings) at its ends;
  - the current card as a large, light, rounded card in front of the backdrop, filling most of the
    remaining space;
  - the card's question ("Is your number on this card?") as the card's header, and the numbers as its
    body (FR-001 to FR-004);
  - a hint of the stack behind the card, showing no numbers (FR-015).

  Text and controls on the backdrop MUST meet the same contrast as elsewhere (001/FR-027).
- **FR-020**: In compact layouts (phone portrait, narrow split screen), the "No" and "Yes" buttons MUST
  be two large buttons side by side under the card, spanning its width:
  - "No" on the left, in the platform's secondary style (outlined on Android; a light control on iOS);
  - "Yes" on the right, in the primary (filled or tinted) style.
- **FR-021**: In wide layouts (tablets, phone landscape, unfolded foldables, wide split screen), the
  answers MUST be tall panels flanking the card: "No" to its left and "Yes" to its right, each
  labelled, with the card centred between them. In folded postures, the card and the answers MUST be
  on opposite sides of the fold, with "No" still left of "Yes" (001/FR-032).

**Platform design language (whole app: intro, card, result, Settings)**

- **FR-022**: On Android the app MUST follow Material You:
  - colours MUST come from the device's dynamic colour scheme (wallpaper and theme) where available,
    in light and dark;
  - buttons, cards, top bar, settings controls, shapes, typography and motion MUST use Material 3;
  - where dynamic colour is unavailable (Android 8–11), a Wáay Material 3 colour scheme MUST be used.
- **FR-023**: On iOS 26 and later the app MUST follow Liquid Glass:
  - the top bar, the "No" and "Yes" controls (buttons or flanking panels) and the screen's other
    floating controls MUST use the system glass material;
  - the "Yes" control MUST be prominent glass tinted with Wáay's violet; the "No" control MUST be
    regular glass tinted with the system background, so its label keeps contrast over the backdrop;
  - Settings MUST use the system's standard glass-era styling.
- **FR-024**: On iOS 17 to 25, which have no Liquid Glass, the same controls MUST use the closest native
  style: the system's standard controls with solid fills. Translucent materials would take on the
  backdrop's colour and lose contrast (FR-025). Behaviour stays identical.
- **FR-025**: Content legibility comes first. The numbers card MUST be a solid, opaque surface. Glass and
  translucency MUST only be used for controls and bars, never behind the numbers or messages. All text
  MUST meet the contrast rules (001/FR-027) in light and dark mode, with dynamic colours, and when
  Reduce Transparency or Increase Contrast is on.
- **FR-026**: The swipe hint labels, card motion and exit animations (FR-009, FR-010) MUST use each
  platform's own motion style (for example spring-based movement). Reduce motion (FR-014) still
  applies.

**Unchanged**

- **FR-018**: The magician voice, randomization, decoding, languages, settings, persistence and adaptive
  layouts behave as in spec 001. In particular, each new card's prompt is spoken (001/FR-012) and
  interrupts any line still playing (001/FR-015).

### Key Entities

- **Presented card**: as in spec 001: progress position, displayed numbers and hidden bit value. Here it
  is also the draggable object, with a resting position, a current offset and tilt, and an exit
  direction once answered.
- **Swipe**: one drag of the card. It has a direction (right or left), a distance, a release speed, and an
  outcome: answered Yes, answered No, or cancelled.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: In every configuration of spec 001's verification matrix, at the default text size and
  at every larger standard (non-accessibility) size, a card shows 100% of its numbers with no
  scrolling, for every card count from 3 to 7. The exceptions are windows too small for the card at
  the player's text size, where FR-004's scroll fallback applies:
  - phones in landscape less than about 375 dp/pt tall, with 7 cards;
  - phone split-screen halves, with 6 or 7 cards.
- **SC-002**: On every configuration, the numbers' area covers at least 80% of the space available for
  the card. On a typical phone in portrait, 64 numbers are each at least as tall as the default body text.
- **SC-003**: In testing, 100% of swipes released past the threshold record exactly one answer in the
  right direction, and 100% of releases before the threshold (without a flick) record none.
- **SC-004**: A player who has never seen the swipe interaction answers their first card by swiping within
  10 seconds without instructions, and finishes a 5-card game by swiping in under 30 seconds.
- **SC-005**: The card follows the finger with no visible lag or stutter on supported devices, smooth at
  the device's display rate.
- **SC-006**: A full game can be completed using only the Yes/No buttons, and using only the screen
  reader, on both platforms. With reduce motion on, no card flies or tilts.
- **SC-007**: Rapid or repeated input (double swipes, swipe plus tap, flick during the exit animation)
  never records more than one answer per card in testing.
- **SC-008**: On Android 12+, changing the wallpaper or theme colours changes the game's colours on the
  next visit, in 100% of tests. On every supported OS version of both platforms, the accessibility
  checks (labels, contrast, text size) pass in light and dark mode.
- **SC-009**: On iOS 26+, every floating control on the card screen (top bar, No, Yes) renders as
  glass. On iOS 17–25 and Android 8–11, players complete games exactly as on the newest versions.

## Assumptions

- **Swipe direction:** right means Yes and left means No in both languages, as the owner specified. Both
  supported languages read left to right.
- **Scope:** the card phase gets the new interaction. The intro and result screens only take on the
  shared look (see below).
- **Card stack look:** a hint of further cards behind the current one, showing backs only, is allowed but
  not required (FR-015).
- **Haptics:** a light haptic tick when the card crosses the answer threshold is a reasonable default
  where the device supports it, and respects the system's settings.
- **Threshold:** the answer threshold and flick speed are tuned for comfort (about a third of the card's
  width, or a clear flick). Exact values are a design decision recorded in the plan.
- **No undo:** answers stay final, as in spec 001. Slack's "Undo" and "close" controls are not copied.
  The top bar holds this game's own controls (New game, Settings).
- **Design language names:** "Material You" means Google's current Material 3 with dynamic colour.
  "Crystal glass" is read as Apple's Liquid Glass, introduced with iOS 26.
- **Brand colours as fallback:** Wáay's brand colours (from the app icon, and iOS's accent colour) are
  used where the platform offers no user-derived colours: on Android 8–11, and as iOS's tint and
  backdrop.
- **Intro and result screens:** they share the same backdrop and card styling for a consistent look,
  and keep their single action ("I'm ready", "New game") as in spec 001. Swiping them is out of scope.
- **Reference screenshots:** the owner's Slack screenshots set the layout direction only. They contain
  third-party content and are not stored in the repository.
- **Out of scope:** undoing an answer, new game modes, web and desktop.
