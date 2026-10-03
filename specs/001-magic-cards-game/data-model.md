# Data Model: Magic Cards Game v1

**Feature**: [spec.md](./spec.md) · **Research**: [research.md](./research.md)

The model has three layers, following the constitution: **domain** (pure, lifecycle-free),
**presentation** (UI state), and **preferences** (persisted). The `bitValue` field never leaves the
domain (FR-011).

## Domain (`dev.epool.waay.game.domain`)

### CardCount

| Field | Type | Rules |
|---|---|---|
| `value` | Int | 3–7 inclusive (FR-017). The default is 5 (FR-024). |
| `maxNumber` | Int (derived) | `2^value − 1`: 7, 15, 31, 63 or 127 (FR-001). |
| `numbersPerCard` | Int (derived) | `2^(value−1)` (FR-002). |

- Constructed through `CardCount.of(value): Result<CardCount, CardCountError>`. Out-of-range input
  returns `CardCountError.OutOfRange`.

### Card

| Field | Type | Rules |
|---|---|---|
| `bitValue` | Int | `2^k`, where `k ∈ 0..N−1`. Unique within a deck. **Hidden.** |
| `numbers` | List<Int> | Exactly the `n ∈ 1..maxNumber` with `n and bitValue ≠ 0`. Size `numbersPerCard`. Order randomized (FR-009). |

### Deck

| Field | Type | Rules |
|---|---|---|
| `cardCount` | CardCount | |
| `cards` | List<Card> | N cards in randomized presentation order (FR-008). The set of `bitValue`s is exactly {1, 2, 4, …, 2^(N−1)}. |

- Built by `MagicDeck.create(cardCount, random)`. Deterministic for a given seeded `Random`.

### Answer

- An `enum Answer { Yes, No }`. A game holds one answer per presented card, in presentation order.

### DecodeResult

- `AnswerDecoder.decode(deck, answers): Result<Int, DecodeError>`. Success carries a value in
  `1..maxNumber` (FR-004).
- Errors:
  - `DecodeError.OutOfRange` when the sum is 0 (FR-005);
  - `DecodeError.IncompleteAnswers` when `answers.size ≠ N`. This is a programming error and is
    guarded by tests.

### GameSnapshot (the reducer state) and its phases

```
            ┌─────────────── NewGame / CardCountChanged ──────────────┐
            ▼                                                          │
         Intro ──Ready──▶ Asking(0) ──Answer──▶ … ──Answer──▶ Asking(N−1)
                                                                   │ Answer (last)
                                       ┌───────────────────────────┴──────────┐
                                       ▼ decode ok                            ▼ decode = 0
                                 Revealed(number)                          Invalid
```

| Phase | Meaning | Allowed commands |
|---|---|---|
| `Intro` | Waiting for "I'm ready" (FR-003) | `Ready`, `NewGame` |
| `Asking(index)` | Card `index` of N is shown (FR-003a) | `Answer(Yes/No)`, `NewGame` |
| `Revealed(number)` | The result is shown as a statement (FR-004, FR-007) | `NewGame` |
| `Invalid` | All "No" (FR-005) | `NewGame` |

- `GameSnapshot` fields: `deck`, `answers: List<Answer>`, `phase`.
- `GameEngine.reduce(snapshot, command, newDeck: () -> Deck)` is a pure function.
- Commands that a phase doesn't allow are no-ops. In particular, extra `Answer`s after the last card
  do nothing (FR-028).
- `NewGame` and `CardCountChanged(n)` always produce `Intro` with a fresh deck (FR-006, FR-018).

## Preferences (`dev.epool.waay.settings.domain`)

| Field | Type | Default | Rules |
|---|---|---|---|
| `cardCount` | CardCount | 5 | Stored as Int. Invalid stored values fall back to the default. |
| `voiceEnabled` | Boolean | true | |
| `languageChoice` | `LanguageChoice { Device, English, Spanish }` | Device | Stored as a stable string key. Unknown keys fall back to `Device`. |

### AppLanguage

- An `enum AppLanguage { English, Spanish }`, resolved by `LanguageResolver(choice, deviceLocale)`.
  - `English` or `Spanish` resolves to itself.
  - `Device` resolves to `Spanish` when the device language tag starts with `es`, and `English`
    otherwise (FR-020).
- `languageTag` gives the speech voice code, for example `es-MX` or `en-US`. It prefers the device
  region when the language matches (ADR-004).

## Presentation (`dev.epool.waay.game.presentation`, `dev.epool.waay.settings.presentation`)

UI models hold **resolved, localized text** (ADR-003). They are immutable data classes exposing
read-only `List`s, which bridge to Swift `[T]` (ADR-011). Their contracts are in [contracts/](./contracts/).

### GameState

| Field | Type | Notes |
|---|---|---|
| `title` | String | App or screen title |
| `content` | `GameContentUi` (sealed) | One case per phase, below |
| `settingsLabel` | String | Accessibility label for the gear button (FR-025) |
| `newGameLabel` | String | "New game" action, visible in every phase (FR-006, SC-008) |

`GameContentUi` cases:
- **`Intro`**: `message` (for example "Think of a number from 1 to 31…") and `readyLabel`.
- **`Card`**: `index` (0-based position), `progress` ("Card 3 of 5"), `question` ("Is your number on this card?"),
  `numbers: List<NumberUi>`, `yesLabel` and `noLabel`. `NumberUi` has `value: Int` and
  `label: String`, which is what the screen reader announces.
- **`Revealed`**: `message` ("The number you thought of is… 27!"), `number: Int`, `newGameLabel`.
- **`Invalid`**: `message` and `newGameLabel`.

### GameAction

- `OnReadyClick`
- `OnAnswerClick(answer: Answer, cardIndex: Int)`: `cardIndex` echoes `Card.index`. Answers for any other card are ignored (FR-028).
- `OnNewGameClick`
- `OnSettingsClick`

### GameEvent

- `NavigateToSettings`

### SettingsState

| Field | Type |
|---|---|
| `title`, `cardCountLabel`, `voiceLabel`, `languageLabel`, `backLabel` | String |
| `cardCountOptions` | `List<CardCountOptionUi>`, values 3–7, with a range hint such as "5 cards (1–31)" |
| `selectedCardCount` | Int |
| `voiceEnabled` | Boolean |
| `languageOptions` | `List<LanguageOptionUi>`: "Device language", "English", "Español" |
| `selectedLanguage` | `LanguageChoiceUi` |

Option types are non-generic for Swift (ADR-002): `CardCountOptionUi` and `LanguageOptionUi`.

### SettingsAction

- `OnCardCountSelect(value: Int)`
- `OnVoiceToggle(enabled: Boolean)`
- `OnLanguageSelect(choice: LanguageChoiceUi)`
- `OnBackClick`

### SettingsEvent

- `NavigateBack`

## Validation rules mapped to requirements

| Rule | Requirement | Enforced in |
|---|---|---|
| N ∈ 3..7 | FR-017 | `CardCount.of` |
| Card membership is bit-exact | FR-002 | `MagicDeck.create`. Checked by the exhaustive test (SC-001). |
| Uniform card order and number order | FR-008, FR-009 | `shuffled(random)`. Checked by the statistical test (SC-002). |
| Decode through the hidden mapping; 0 means invalid | FR-004, FR-005, FR-010 | `AnswerDecoder` |
| `bitValue` never reaches UI models | FR-011 | Mapper tests: no `bitValue` field in the `…Ui` types |
| Exactly one answer per card; extra taps ignored | FR-003a, FR-028 | `GameEngine` |
| A card-count change resets to Intro; language changes don't reset | FR-018, FR-021 | `GameViewModel` preference observation |
| Defaults and fallback for invalid stored values | FR-024 | `KeyValuePreferencesDataSource` |
