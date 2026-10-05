# Quickstart & Validation: Card Fit, Swipe Answers and Native Look

Run this at the verification gate. The prerequisites and tooling are the same as
[spec 001's quickstart](../001-magic-cards-game/quickstart.md#prerequisites).

## 1. Automated checks (CI parity)

These are the same commands as spec 001 §1, plus the new and updated tests below.

```sh
./gradlew spotlessCheck :androidApp:lintDebug :shared:allTests :shared:koverVerify \
  :androidApp:testDebugUnitTest :androidApp:verifyRoborazziDebug :androidApp:assembleDebug
scripts/swift-format-lint.sh && xcodegen --spec iosApp/project.yml
xcodebuild test -project iosApp/iosApp.xcodeproj -scheme Waay \
  -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0' -collect-test-diagnostics never
```

**Expected:** all green. The new tests:

| Test | Proves |
|---|---|
| `CardGridFitTest`: F1–F8 over n ∈ {4, 8, 16, 32, 64} × the configuration matrix × minimum sizes | FR-001 to FR-005, SC-001, SC-002 |
| `GameViewModelTest`: G15a–e | FR-011 |
| Android `SwipeAnswerTest`, iOS `SwipeAnswerUITests` (U1–U6, U11) | FR-006 to FR-011, SC-003, SC-007 |
| Android `CardFitTest`, iOS no-scroll check (7 cards, smallest phone) | FR-001, SC-001 |
| `ButtonOrderTest` (both platforms, compact and wide) | FR-012a, FR-020, FR-021 |
| Roborazzi baselines: phases × sizes × light/dark × dynamic/fallback colours, re-recorded and reviewed | FR-019 to FR-022, SC-002 |
| iOS `AccessibilitySemanticsUITests` audit on every screen | FR-025, SC-006, SC-008 |

## 2. Manual scenarios

| # | Scenario | Expected |
|---|---|---|
| M1 | 7 cards, smallest phone, portrait, default text | All 64 numbers visible, no scrolling, numbers fill the card |
| M2 | 3 cards, tablet landscape | 4 large numbers centred, flanking No/Yes panels |
| M3 | Think of 27; play by swiping only | Right = Yes, left = No; reveal 27 |
| M4 | Drag part-way and release; drag vertically | Springs back; nothing answered |
| M5 | Flick fast in each direction | Counts as that answer |
| M6 | Tap No/Yes | The card flies to the matching side |
| M7 | Android 12+: change wallpaper or theme colours | Game colours follow (FR-022) |
| M8 | Android 8–11 emulator or device | Wáay brand Material 3 scheme |
| M9 | iOS 26+ | Top bar and No/Yes are Liquid Glass; the card is solid |
| M10 | iOS 17.5 (CI `ios-minimum-os`) | Material fallback; full game passes |
| M11 | Reduce Motion (both) / Remove animations (Android) | Cross-fades, no flying or tilting |
| M12 | Reduce Transparency, Increase Contrast (iOS) | Controls become solid and readable |
| M13 | TalkBack / VoiceOver | Full game with the buttons; the card reads its header and numbers |
| M14 | Fold postures | Card on one side of the hinge, answers on the other, No left of Yes |
| M15 | Accessibility text size (iOS AX3+, Android 2.0×) with 7 cards on a phone | Numbers at the chosen size; the card may scroll, starting at the top (FR-004) |

## 3. Performance sanity check (SC-004, SC-005)

- **SC-004:** a first-time player answers their first card by swiping within 10 s without
  instructions, and finishes a 5-card game in under 30 s.
- **SC-005:** the drag shows no visible stutter on a mid-range device.
