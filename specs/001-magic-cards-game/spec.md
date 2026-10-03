# Feature Specification: Magic Cards Game v1 ("Wáay")

**Feature Branch**: `001-magic-cards-game`

**Created**: 2026-10-02

**Status**: Draft

**Input**: User description: "Magic cards game v1 ("Wáay"): a mind-reading game for phones (Android and iOS) that recreates and improves the original 2014 Wáay Droid game. It adds randomized cards ('the cards and order of the question should be randomized to make the user harder to detect the pattern, so the system can decipher the actual order to verify the answers'), a magician voice, English and Spanish, a configurable number of cards (3–7) and remembered settings."

## Clarifications

### Session 2026-10-02

- Q: What does "verify the answers" mean for the player? → A: There is no extra step for the player.
  Verification is internal: the answers are decoded through the hidden card mapping (FR-010), and
  the all-"No" result is rejected (FR-005). The reveal is a statement, not a confirmation question.
- Q: Should each game begin with a "think of a number" screen and an "I'm ready" tap before the
  first card? → A: Yes. Every game, including each "New game", starts on an intro screen that shows
  the range and has an "I'm ready" action. Cards begin only after that tap.
- Q: What are the oldest operating system versions v1 must support? → A: Android 8.0 or later, and
  iOS 17.0 or later. iOS 17 is the lowest version that supports the current iOS app stack. This
  answer was revised from an initial "iOS 15.8.8" at the owner's request, to prefer the lowest
  version that supports that stack.
- Q: Where does the player change the card count, voice and language? → A: On a separate Settings
  screen, opened from a settings (gear) control that is visible on the intro, card and result
  screens.
- Q: Besides the intro and the reveal, should the voice speak during the cards? → A: Yes. A short
  prompt is spoken as each card appears, for example "Card 3: is your number here?". The numbers
  themselves are never read aloud by the magician voice.
- Q: Should the game support both portrait and landscape, or lock to portrait? → A: It supports all
  orientations on every device. The UI MUST adapt to the available window size, including foldable
  devices.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - The magician reads my mind (Priority: P1)

The player silently thinks of a whole number within the range shown on screen. For the default game
of 5 cards, the range is 1 to 31.

The game then shows the cards one at a time. Each card displays a set of numbers, and for each card
the player answers "Yes" (my number is on this card) or "No". After the last answer, the game
reveals the number the player was thinking of. The player can start a new game at any moment.

**Why this priority**: This is the whole trick. Without it there is no product. On its own it
reproduces the original game and is already a playable, demonstrable app.

**Independent Test**: Start a game, think of any number in range, answer every card truthfully,
and check that the revealed number matches. Repeat for the lowest and highest numbers in the range.

**Acceptance Scenarios**:

1. **Given** a new 5-card game, **When** the game starts, **Then** the player sees an intro screen
   inviting them to think of a number from 1 to 31, with an "I'm ready" action. No card is shown yet.
2. **Given** the intro screen, **When** the player taps "I'm ready", **Then** the first card appears
   with its numbers, a "Yes" and a "No" choice, and their progress (e.g., "Card 1 of 5").
3. **Given** the player thought of 27 and answers every card truthfully, **When** the last card is
   answered, **Then** the game reveals 27.
4. **Given** the player thought of 1 (the lowest number), **When** they answer every card truthfully,
   **Then** the game reveals 1. **Given** they thought of 31 (the highest), **Then** the game reveals 31.
5. **Given** a game in progress or a revealed result, **When** the player chooses "New game",
   **Then** a fresh game starts on the intro screen, with all previous answers discarded.
6. **Given** the player answered "No" to every card, **When** the last card is answered, **Then** the
   game does not reveal a number. It explains that the number must be between 1 and the range
   maximum, or that an answer may have been mistaken, and offers to play again.
7. **Given** a result has been revealed, **When** the player looks at the screen, **Then** the reveal
   is a statement, not a question asking the player to confirm, and a "New game" option is shown.

---

### User Story 2 - I can't figure out the trick (Priority: P2)

Every new game presents the cards in a different, unpredictable order. Within each card, the
numbers appear in a different, unpredictable arrangement. A player who plays repeatedly cannot work
out the trick from the presentation. In the original game, the cards always came in the same order
and each card's first number was always a tell-tale power of two. The game still reveals the correct
number, because it privately keeps track of which card is which.

**Why this priority**: This is the main improvement the owner asked for over the original. It
keeps the magic convincing over repeated play. It builds directly on Story 1.

**Independent Test**:
- Play several games with the same secret number. Check that the card order and the arrangement of
  numbers on each card change between games.
- Check that the revealed number is still correct every time.

**Acceptance Scenarios**:

1. **Given** two consecutive new games with the same card count, **When** the player compares the
   cards, **Then** the order in which the cards are asked and the arrangement of numbers within the
   cards differ between the games, except where pure chance makes them coincide.
2. **Given** any shuffled presentation, **When** the player answers truthfully for any number in
   range, **Then** the revealed number is correct.
3. **Given** many games, **When** looking at the first number shown on each card, **Then** it is not
   systematically the card's smallest number, nor a power of two.

---

### User Story 3 - The magician speaks (Priority: P3)

Like the original, the game talks to the player in a magician's voice:
- When a game starts, it invites the player to think of a number in the current range. For example:
  "Think of a number from 1 to 31 and let me guess it…".
- As each card appears, it speaks a short prompt. For example: "Card 3: is your number here?". It
  never reads the numbers aloud.
- When it reveals the result, it speaks the reveal. For example: "The number you thought of is… 27!".

The player can turn the voice on or off.

**Why this priority**: The voice gives the game its personality and was part of the original. The
game is still fully playable without it.

**Independent Test**: With the voice on, start a game and complete it. Check that the intro and the
reveal are spoken in the current language. Turn the voice off, repeat, and check that nothing is
spoken while the on-screen text still appears.

**Acceptance Scenarios**:

1. **Given** the voice is on, **When** a new game starts, **Then** the intro invitation is spoken and
   also shown on screen.
2. **Given** the voice is on, **When** a card appears, **Then** a short prompt naming the card's
   position is spoken. The card's numbers are not read aloud.
3. **Given** the voice is on, **When** the result is revealed, **Then** the reveal is spoken and shown
   on screen.
4. **Given** the voice is off, **When** playing a full game, **Then** nothing is spoken, and all the
   same messages appear on screen.
5. **Given** the device has no voice available for the current language, **When** playing, **Then**
   the game works normally with on-screen text and does not show errors that block play.
6. **Given** speech is in progress, **When** the player starts a new game or the reveal happens,
   **Then** the old speech stops and only the latest message is spoken.

---

### User Story 4 - Choose how hard the trick is (Priority: P3)

The player chooses how many cards to play with, from 3 to 7. The corresponding ranges are:

| Cards | Range |
|---|---|
| 3 | 1–7 |
| 4 | 1–15 |
| 5 | 1–31 (default, as in the original) |
| 6 | 1–63 |
| 7 | 1–127 |

**Why this priority**: It adds replay value and lets players pick a quick trick or a more impressive
one. The game works with the default alone.

**Independent Test**: Change the card count to 3 and play. Check that the range shown is 1–7, there
are 3 cards, and the reveal is correct. Repeat with 7 cards (1–127).

**Acceptance Scenarios**:

1. **Given** the player selects 7 cards, **When** a new game starts, **Then** the invitation states
   the range 1–127, the game asks 7 cards, and each card shows 64 numbers.
2. **Given** the player selects 3 cards, **When** they answer truthfully for any number from 1 to 7,
   **Then** the correct number is revealed.
3. **Given** a game is in progress, **When** the player changes the card count in Settings and
   closes Settings, **Then** a new game starts on the intro screen with the new count. The game in
   progress is discarded.
4. **Given** a game is in progress, **When** the player opens Settings and closes it without
   changing the card count, **Then** the game continues exactly where it was.

---

### User Story 5 - Play in my language (Priority: P3)

All on-screen and spoken text is available in English and Spanish. By default the game follows the
device language. The player can also choose English or Spanish explicitly inside the app, and can go
back to following the device language.

**Why this priority**: The original was Spanish-only. Bilingual support widens the audience. It is
independent of the game logic.

**Independent Test**: With the device in Spanish, open the app and check that all text and speech
are in Spanish. Switch the in-app language to English and check that everything, including speech,
is in English.

**Acceptance Scenarios**:

1. **Given** the device language is Spanish and no in-app choice was made, **When** the app opens,
   **Then** all text and speech are in Spanish.
2. **Given** the device language is neither English nor Spanish, **When** the app opens, **Then**
   English is used.
3. **Given** the player chooses English in the app, **When** they continue playing, **Then** all
   visible text switches to English immediately, without restarting the current game. From then on,
   speech is in English.

---

### User Story 6 - The game remembers my preferences (Priority: P3)

The card count, the voice on/off choice and the language choice are remembered between uses of the
app.

**Why this priority**: It avoids re-configuring the game every time. It depends on Stories 3–5
existing.

**Independent Test**: Change the card count to 6, turn the voice off and choose Spanish. Fully close
the app and reopen it. Check that all three choices are still in effect.

**Acceptance Scenarios**:

1. **Given** the player changed the card count, voice and language, **When** the app is closed and
   reopened, **Then** the same three choices are in effect.
2. **Given** a fresh install, **When** the app opens for the first time, **Then** the defaults apply:
   5 cards, voice on, and the language follows the device.

---

### Edge Cases

- **All answers "No":** the decoded result is 0, which is out of range. This must not be revealed as
  a number; see Story 1, scenario 5.
- **Other mistaken answers:** every other combination of answers corresponds to exactly one valid
  number. A mistaken answer therefore produces a wrong but valid-looking number. v1 does not try
  to detect this; the player simply starts a new game.
- **Interruptions:** if the app is interrupted (incoming call, switching apps, screen rotation) and
  the player returns, the current game continues exactly where it was. If the app is fully closed,
  the game in progress may be lost, but settings are kept.
- **Large games:** a 7-card game shows 64 numbers per card. All of them must be readable and
  reachable, including at the largest system text sizes, by scrolling if necessary. Every new card
  starts at the top of its numbers. Otherwise numbers left above the visible area by the previous
  card's scrolling could be missed, leading to a wrong answer.
- **Voice unavailable or failing mid-game:** play continues with on-screen text only, with no
  blocking error.
- **Screen reader and magician voice:** screen-reader users can turn the magician voice off so it
  does not compete with screen-reader announcements.
- **Rotating, folding or resizing mid-game:** rotating the device, folding or unfolding it, or
  resizing the window in split-screen keeps the current card, the answers given and any revealed
  result. The layout reflows to fit.
- **Rapid input:** tapping "Yes" or "No" several times quickly must record exactly one answer per
  card. Answers after the last card must not change the result.

## Requirements *(mandatory)*

### Functional Requirements

**Game**

- **FR-001**: The system MUST let the player start a game with N cards, where N is the selected card
  count (3–7, default 5). The valid secret numbers are 1 to 2^N − 1.
- **FR-002**: Each game MUST contain exactly N cards. The card associated with bit value 2^k (k = 0…N−1)
  MUST contain exactly the numbers in the game's range whose binary representation includes 2^k.
  Each card therefore holds 2^(N−1) numbers.
- **FR-003**: Every game MUST begin on an intro screen. That screen invites the player to think of a
  number in the current range (1 to 2^N − 1) and offers an "I'm ready" action. No card is shown
  before the player taps "I'm ready".
- **FR-003a**: After "I'm ready", the system MUST present the cards one at a time and collect exactly
  one Yes/No answer per card. It MUST show the player's progress, for example "Card 3 of 5". Each
  card MUST be shown from the start of its numbers, however far the player scrolled the previous
  card, so that no number is hidden when the player answers.
- **FR-004**: After the last answer, the system MUST compute the result as the sum of the bit values
  of the cards answered "Yes", and reveal it to the player.
- **FR-005**: If the computed result is 0 (all answers "No"), the system MUST NOT reveal a number. It
  MUST explain the valid range and the possibility of a mistaken answer, and offer a new game.
- **FR-006**: The player MUST be able to start a new game at any time, from any point in a game. This
  discards the game in progress without asking for confirmation.
- **FR-007**: Answer verification MUST be internal only. The system verifies the answers by decoding
  them through the hidden card mapping (FR-010) and by rejecting an out-of-range result (FR-005).
  The reveal MUST be a statement. v1 MUST NOT ask the player to confirm the revealed number.

**Randomization**

- **FR-008**: For every new game, the system MUST randomize the order in which the cards are
  presented. Every ordering MUST be equally likely, and each game MUST be independent of the
  previous ones.
- **FR-009**: For every new game, the system MUST randomize the arrangement of the numbers displayed
  on each card, independently for each card. Every arrangement MUST be equally likely.
- **FR-010**: The system MUST privately keep, for the duration of a game, which bit value each
  presented card represents. It MUST decode the answers through that mapping, so the result is
  correct regardless of presentation order.
- **FR-011**: The system MUST NOT reveal to the player which bit value a card represents. That
  includes visible ordering cues, labels, and screen-reader descriptions.

**Voice**

- **FR-012**: When voice is on, the system MUST speak:
  - the game invitation, including the current range, when the intro screen appears;
  - a short prompt naming the card's position (for example "Card 3: is your number here?") when each
    card appears, without reading the card's numbers;
  - the reveal message, when the result is revealed;
  - the invalid-result message, in the FR-005 case.
- **FR-013**: Every spoken message MUST also appear as on-screen text.
- **FR-014**: The player MUST be able to turn the voice on and off. When it is off, nothing is spoken.
- **FR-015**: When a new message is to be spoken, any message still being spoken MUST stop first.
- **FR-016**: If voice output is unavailable for the current language, or fails, the game MUST
  continue with on-screen text only, with no blocking error.

**Settings screen**

- **FR-016a**: The card count, voice and language choices MUST be changed on a dedicated Settings
  screen. That screen MUST be reachable through a settings (gear) control that is visible on the
  intro, card and result screens.
- **FR-016b**: Closing Settings MUST return the player to where they were. The only exception is
  FR-018.

**Card count**

- **FR-017**: The player MUST be able to choose the card count from 3 to 7.
- **FR-018**: Changing the card count MUST start a new game, on the intro screen, with the new count
  once the player leaves Settings. Changing only the voice or the language MUST NOT restart the game.

**Language**

- **FR-019**: All on-screen and spoken text MUST be available in English and Spanish.
- **FR-020**: By default the language MUST follow the device language: Spanish for Spanish-language
  devices, and English otherwise.
- **FR-021**: The player MUST be able to choose "Device language", "English" or "Español" in the app.
  A change MUST apply immediately to all visible text, without restarting the current game, and to
  all speech from then on.
- **FR-022**: Spoken text MUST use a voice matching the active language.

**Preferences**

- **FR-023**: The system MUST remember the card count, the voice setting and the language choice
  across app restarts.
- **FR-024**: On first use, the defaults MUST be: 5 cards, voice on, and the device language.

**Accessibility**

- **FR-025**: Every card number, choice, progress indicator, message and setting MUST be announced
  meaningfully by the platform screen reader. A game MUST be completable using only the screen
  reader.
- **FR-026**: All text MUST scale with the system text-size setting up to the largest size, without
  numbers being truncated or overlapping. Scrolling is acceptable.
- **FR-027**: No information may be conveyed by colour alone.

**Robustness**

- **FR-028**: Multiple rapid taps on an answer MUST record a single answer for the current card.
- **FR-029**: A game in progress MUST survive temporary interruptions: app switching, incoming calls
  and screen rotation.

**Adaptive layout**

- **FR-031**: Every screen MUST work in portrait and landscape, on phones, tablets and foldable
  devices.
  - The layout MUST adapt to the available window width and height, for example by showing more
    grid columns when there is more width.
  - The layout MUST also adapt to split-screen and multi-window sizes.
- **FR-032**: On foldable devices:
  - folding or unfolding mid-game MUST keep the game exactly where it was;
  - when the device is half-opened (tabletop or book posture), no card number, answer choice or
    message may sit on, or be split by, the fold.

**Platforms**

- **FR-030**: The game MUST install and be fully playable on iPhones running iOS 17.0 or later, and
  on Android phones running Android 8.0 or later. All requirements above apply on the oldest
  supported versions.

### Key Entities

- **Game**: one round of the trick. It has:
  - the card count N and the derived range 1…2^N − 1;
  - the shuffled sequence of cards;
  - the answers given so far;
  - its status: intro (waiting for "I'm ready"), in progress, revealed, or invalid result.
- **Card**: one presented set of numbers. It has:
  - its presentation position (visible);
  - its displayed, shuffled list of numbers (visible);
  - the bit value it represents (hidden from the player).
- **Answer**: the player's Yes/No for one presented card.
- **Result**: the decoded number, or "invalid" when the decoded value is 0.
- **Preferences**:
  - card count: 3–7;
  - voice: on or off;
  - language choice: Device, English or Español.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: For every card count from 3 to 7 and every number in its range, truthful answers lead
  to the correct reveal 100% of the time. That is 7 + 15 + 31 + 63 + 127 = 243 cases, all verified.
- **SC-002**: Across 10,000 simulated games per card count, each card appears in each position with
  a frequency within ±5% of uniform. A card's smallest number appears first no more often than
  chance (1 / numbers per card, ±5%).
- **SC-003**: A first-time player completes a 5-card game, from opening the app, through the intro
  screen, to the reveal, in under 60 seconds without instructions.
- **SC-004**: 100% of on-screen and spoken messages exist in both English and Spanish. No screen
  ever mixes the two languages.
- **SC-005**: Preferences are retained across 100% of app restarts in testing.
- **SC-006**: A full game can be completed using only the screen reader on both platforms, and at
  the largest system text size with no truncated numbers.
- **SC-007**: With voice unavailable or turned off, players complete games at the same rate as with
  voice. No step depends on hearing audio.
- **SC-008**: Starting a new game takes a single action from any point in the app.
- **SC-009**: A full game can be completed in each of these configurations with no lost progress, no
  clipped or overlapping content, and nothing placed across a fold:
  - phone portrait;
  - phone landscape;
  - tablet;
  - foldable folded;
  - foldable unfolded;
  - foldable half-opened;
  - split-screen.

  The device is rotated or folded at least once mid-game in each run.

## Assumptions

- **Platforms:** phones, tablets and foldables running iOS 17.0 or later, or Android 8.0 or later
  (FR-030). Layouts adapt to every orientation and window size (FR-031, FR-032). The oldest supported
  versions can be checked on simulators or emulators as well as on real devices.
- **Offline:** the game works fully offline. There are no accounts, scores, leaderboards, analytics
  or online features.
- **Voice:** speech uses the voices built into the device. Audio follows the device's normal volume
  and silent-mode behaviour.
- **Device language:** "Spanish" means any Spanish regional variant, and any other device language
  falls back to English.
- **Numerals and layout:** numbers are shown with Western Arabic numerals in both languages, in a
  grid that scrolls when needed.
- **No undo:** answers cannot be changed once given. The player starts a new game instead.
- **Process death:** a game in progress does not need to survive the app being fully closed or
  terminated by the system. Preferences do.
- **Out of scope:** web and desktop versions; a shared cross-platform UI; card themes and visual
  customization beyond a clean default look.
