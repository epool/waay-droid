# Feature Specification: Card Fit and Swipe Answers

**Feature Branch**: `002-card-fit-swipe-answers`

**Created**: 2026-10-04

**Status**: Draft

**Input**: User description: "Two new requirements: 1. The numbers layout should fill the screen without needing to scroll. 2. New UI to answer yes or no with a swipe right and left gesture with animation, similar to Slack's Catch up feature."

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

**Independent Test**: Play a 7-card game on the smallest supported phone in landscape, and a 3-card game
on a tablet. On every card, every number is visible and readable without scrolling, and the numbers fill
the card's area instead of sitting in a small block.

**Acceptance Scenarios**:

1. **Given** a 7-card game on a small phone in portrait, **When** a card appears, **Then** all 64 numbers
   are visible at once, none is cut off or overlapping, and nothing scrolls.
2. **Given** the same game in landscape or in a half-width split-screen window, **When** a card appears,
   **Then** all 64 numbers are still visible at once without scrolling.
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
the player lets go too early, the card springs back to the middle and nothing is answered. The style
follows Slack's "Catch up" card stack.

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
4. **Given** the system's reduce-motion setting is on, **When** a card is answered, **Then** it is replaced
   by a short cross-fade instead of flying and tilting, and dragging still answers.

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
  on the right, side by side. This replaces spec 001's order ("Yes" first) and its vertical stacking
  beside the numbers in side-by-side layouts. There the buttons sit side by side under the card.
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

- **SC-001**: In every configuration of spec 001's verification matrix, at the default text size and at
  every larger standard (non-accessibility) size, a card shows 100% of its numbers with no scrolling,
  for every card count from 3 to 7.
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

## Assumptions

- **Swipe direction:** right means Yes and left means No in both languages, as the owner specified. Both
  supported languages read left to right.
- **Scope:** only the card phase changes. The intro and result screens keep their buttons as in spec 001;
  swiping them is out of scope.
- **Card stack look:** a hint of further cards behind the current one, showing backs only, is allowed but
  not required (FR-015).
- **Haptics:** a light haptic tick when the card crosses the answer threshold is a reasonable default
  where the device supports it, and respects the system's settings.
- **Threshold:** the answer threshold and flick speed are tuned for comfort (about a third of the card's
  width, or a clear flick). Exact values are a design decision recorded in the plan.
- **No undo:** answers stay final, as in spec 001.
- **Out of scope:** undoing an answer, new game modes, web and desktop.
