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
| `CardGridFitTest` F6: scrolling only in the windows the narrowed SC-001 exempts (small landscape phones with 7 cards, phone split-screen halves with 6–7 cards) | SC-001, FR-004 |
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

## Run log

**2026-10-04: verification run (T042)**

- **Section 1, automated:**
  - Gradle gates pass: `spotlessCheck :androidApp:lintDebug :shared:allTests :shared:koverVerify
    :androidApp:testDebugUnitTest :androidApp:verifyRoborazziDebug :androidApp:assembleDebug`. That
    is 81 shared tests on the Android host, 80 on the iOS simulator, 50 Robolectric tests and 69
    Roborazzi baselines.
  - The first invocation failed after 6 s. Its output wasn't kept, and two reruns passed, so the
    cause is unknown.
  - `scripts/swift-format-lint.sh` passes, and `xcodegen` leaves the project unchanged.
  - `xcodebuild test` passes, 16 UI tests and 7 unit tests each, on iPhone 17 (iOS 27), iPhone 16
    (iOS 18.6) and iPad Pro 11-inch (M5, iOS 27). This is after the last code change (69b70d0).
  - The accessibility audit class also passes on iOS 26.3.
- **Section 2, manual:**

  | # | Result |
  |---|---|
  | M1 | Pass. iPhone 17e (iOS 27), 7 cards: all 64 numbers visible with no scrolling, in portrait and landscape, light and dark. Android: `CardFitTest` at 360×640 dp. |
  | M2 | Pass. iPad Pro 13-inch (M5, iOS 27), 3 cards: 4 large numbers, "No" and "Yes" glass panels flanking the card in portrait and landscape, light and dark. |
  | M3 | Pass. Pixel 9 Pro XL emulator (API 36): a game answered by swiping only (adb swipes) revealed 27. iOS: `SwipeAnswerUITests`. |
  | M4 | Pass, automated on both platforms (short and vertical drags answer nothing). |
  | M5 | Android: `SwipeAnswerTest` flick. iOS: not checkable here, because XCUITest drags carry no release velocity. **Owner to check by hand.** |
  | M6 | Pass. On the emulator, a game answered with the buttons revealed 19. Buttons share the swipe's exit path (T038). |
  | M7 | Pass. With the emulator's system palette switched to orange (`theme_customization_overlay_packages`), the backdrop turned peach and the buttons brown. Restored afterwards. |
  | M8 | Automated only: `ThemeTest` at API 30, plus the fallback baselines. No API 26–30 emulator is installed (removed at the owner's request). |
  | M9 | Pass. iOS 27 screenshots: glass toolbar and No/Yes, opaque card. |
  | M10 | iOS 18.6 takes the solid fallback and the full suite passes. iOS 17.5 runs in CI (`ios-minimum-os`) once 002 is on `kmp`. |
  | M11 | Pass. On the emulator with "Remove animations" on, a swiped game revealed 5. `ReduceMotionTest` covers the no-tilt rule. On iOS with Reduce Motion on, the app saw the setting, and the swipe and flow UI tests passed. **The cross-fade itself is for the owner's eye.** |
  | M12 | Pass on iOS 27 and 18.6 (ADR-017). |
  | M13 | Automated: semantics tests on both platforms (T035) and the audit. **A real TalkBack/VoiceOver session is the owner's.** |
  | M14 | Automated: tabletop and book Roborazzi baselines. No foldable emulator is installed. |
  | M15 | Pass, automated: `CardScrollTest` (Android 2.0×) and `CardScrollUITests` (iOS Accessibility XXXL). |

- **Section 3, performance:**
  - SC-004 needs a first-time player, so it's the owner's check.
  - SC-005 couldn't be judged. The emulator renders with SwiftShader (a CPU software GPU), and a
    debug build dragging on it showed a 48 ms median frame. Neither the emulator nor a debug build
    represents a mid-range phone. **The owner should check SC-005 on a real device, with a release
    build.**
- **Defects found and fixed during T040–T042:**
  - Android: at 2.0× text, the result's "New game" was pushed off the card (569ed93).
  - iOS 17–25: the toolbar icons were nearly invisible over the backdrop (69b70d0).
  - iOS test: the button-order test assumed iPhone's portrait layout on iPad (d52668c).
  - Android tests: a drag animation leaked into later test classes, and the drag screenshot was
    unseeded (263896f).
