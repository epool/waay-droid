# Quickstart & Validation: Magic Cards Game v1

This guide proves the feature works end-to-end. Run it at the verification gate. Read it alongside:
- the contracts: [game](./contracts/game-viewmodel.md), [settings](./contracts/settings-viewmodel.md),
  [iOS bridge](./contracts/ios-bridge.md), [platform services](./contracts/platform-services.md);
- the [data model](./data-model.md).

## Prerequisites

- JDK 21. `./gradlew` provisions Gradle 9.7.0, and toolchains provision anything else.
- **Android:**
  - Android SDK with platform 37.1: `android sdk install platforms/android-37.1`.
  - Android CLI (`android`).
  - An emulator for each form factor: phone, foldable and tablet (`android emulator create …`), plus
    an **API 26 (Android 8.0)** phone emulator for the minimum-version smoke run (FR-030).
- **iOS:**
  - Xcode 27 selected (`xcode-select -p`).
  - `xcodegen`.
  - Simulators: the latest runtime plus an **iOS 17.x** runtime for the minimum-version check
    (`xcodebuild -downloadPlatform iOS -buildVersion 17.5` or similar).

## 1. Automated checks (CI parity)

```sh
./gradlew spotlessCheck                         # Kotlin/KTS format + ktlint + compose-rules
./gradlew :shared:allTests                      # commonTest on Android host + iosSimulatorArm64
./gradlew :shared:koverVerify                   # ≥ 90% on game.domain + presentation
./gradlew :androidApp:lintDebug :androidApp:testDebugUnitTest   # Lint + Robolectric UI + Roborazzi
./gradlew :androidApp:assembleDebug
xcodegen --spec iosApp/project.yml
xcodebuild test -project iosApp/iosApp.xcodeproj -scheme Waay \
  -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'   # Swift Testing + XCUITest smoke
xcrun swift-format lint --strict --recursive iosApp/
```

**Expected:** all green. The following tests must exist and pass:

| Test | Proves |
|---|---|
| Exhaustive decode, every n in 1..2^N−1 for N = 3…7 (243 cases) | SC-001, FR-002, FR-004, FR-010 |
| Uniformity, 10,000 seeded games per N: each position within ±5%, and the first-shown number ≈ 1/numbersPerCard | SC-002, FR-008, FR-009 |
| All "No" gives Invalid | FR-005 |
| GameViewModel guarantees G1–G13 and SettingsViewModel guarantees S1–S6, with Turbine | Stories 1–6 |
| Strings: every `Strings` member is non-blank in EN and ES, and no state mixes languages | SC-004, FR-019 |
| Roborazzi matrix of 3 widths × 3 heights plus font scale 1.5 for each Game phase | SC-009, FR-026, FR-031 |
| Robolectric robot smoke test: Intro → Ready → N answers → Revealed → New game | US1 |
| XCUITest smoke: the same flow on iOS | US1, FR-025 |

## 2. Manual scenarios

### Android

Install and run with `android run --apks=androidApp/build/outputs/apk/debug/androidApp-debug.apk`.
Inspect the UI with `android screen` and `android layout`.

| # | Scenario | Expected |
|---|---|---|
| A1 | Fresh install, device in Spanish | An intro in Spanish ("Piensa en un número del 1 al 31…") is spoken. Tap "Estoy listo". The cards appear with progress. Think of 27 and answer truthfully: the reveal says 27, as a statement. |
| A2 | Play twice and compare | The card order and the order of numbers within each card differ between the games. |
| A3 | Answer "No" to every card | The invalid message is shown and spoken, with a "New game" option. |
| A4 | Gear → 7 cards → back (mid-game) | A new game starts on Intro with range 1–127 and 64 numbers per card, scrollable. |
| A5 | Gear → voice off → back | Nothing is spoken. The game continues where it was. |
| A6 | Gear → English | All text switches to English immediately. The next speech is in English. |
| A7 | Kill the app and reopen | 7 cards, voice off and English are still in effect (SC-005). |
| A8 | Foldable emulator: fold and unfold mid-card, then tabletop posture | Same card and answers. Numbers above the hinge, answers below; nothing on the fold (FR-032). |
| A9 | Rotate, then split-screen | The layout reflows to two panes. Progress is kept (FR-029, FR-031). |
| A10 | TalkBack on, largest font | A full game is completable. Numbers are announced and none are clipped (SC-006). |

### iOS

Run `xcodebuild … -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'` and also with
`OS=17.x`.

| # | Scenario | Expected |
|---|---|---|
| I1–I7 | Same as A1–A7 | Same results. |
| I8 | iPad: rotate, then Split View | Layout adapts. Progress is kept. |
| I9 | VoiceOver on, Accessibility XXXL text | A full game is completable with no clipping. |
| I10 | iOS 17.x simulator | The full game runs on the minimum OS (FR-030). |

## 3. Performance sanity check (SC-003)

A first-time player completes a 5-card game in under 60 s. Time one unassisted run on each platform.

## Run log

**2026-10-02: US1 MVP checkpoint (T053)**

- Section 1, automated:
  - `:shared:allTests` passes on the Android host and `iosSimulatorArm64`.
  - `:androidApp:testDebugUnitTest` passes: the Robolectric robot flow on API 26 and API 36.
  - `xcodebuild test` passes on the iPhone 17 / iOS 27.0 simulator: Swift Testing, plus the
    XCUITest flow with rotation, backgrounding and a Settings round trip mid-game.
- iOS manual demo: the app was installed and launched on the iPhone 17 (iOS 27.0) simulator, and
  the intro screen rendered as specified.
- **Deviation:** the Android emulator demo was not run. On this machine, `adb start-server` hangs
  without binding `tcp:5037`, even outside the command sandbox, so `android emulator start` never
  launches QEMU. Likely cause: a pending macOS "Local Network" permission prompt for `adb`, which
  can't be answered over Remote Control. To run it, allow the prompt (System Settings → Privacy &
  Security → Local Network), then run:
  `android emulator start Pixel_9_Pro_XL && android run --apks=androidApp/build/outputs/apk/debug/androidApp-debug.apk`.

**2026-10-03: US3 voice (T066)**

- Automated: the speech guarantees G1–G4, G9, G10 and G12 pass in `commonTest`, on the Android host
  and iOS. They include spoken text matching on-screen text (FR-013).
- The Robolectric flow (API 26 and 36) and the XCUITest flow pass with the real
  `TextToSpeechSpeaker` and `AvSpeechSpeaker` in the graph. No crash, and no blocking error.
- **Deviation:** these manual checks were not done:
  - listening to the voice;
  - disabling the Android TTS engine to check FR-016.

  The agent cannot hear simulator audio, and the Android emulator is blocked by `adb` (see the
  T053 note above). To do it yourself:
  1. Play a round with the voice on, and listen.
  2. On Android, disable the TTS engine (Settings → Accessibility → Text-to-speech) and confirm the
     game still plays silently.

**2026-10-03: US4 card count (T073)**

- Automated:
  - S2: options 3–7 with range labels, values outside the range ignored, and the choice persisted.
  - G7: the reset to Intro states the new range, and 7 cards show 64 numbers.
  - The intro speech is deferred while the game screen is away.
- Both grids scroll: Android `LazyVerticalGrid` and iOS `LazyVGrid` in a `ScrollView`.
- **Pending:** the visual check at the largest font, with 64 numbers reachable and none clipped. It
  moves to Phase 9: the T088 font-scale screenshots and the T092 Dynamic Type pass.

**2026-10-03: US5 languages (T082)**

- Automated tests:
  - StringsTest: both catalogs complete, and the reveal is a statement in every language;
  - LanguageResolverTest: device-following default, explicit override, and the regional voice;
  - G8: switching language mid-game re-resolves the text in place, keeps the same numbers, and the
    next speech is in Spanish;
  - S4: the Settings labels switch immediately.
- **I1, verified on the iOS 27 simulator:** launching with the device language set to Spanish
  (`-AppleLanguages (es-MX)`) shows the Spanish intro, "Piensa en un número del 1 al 31…", with
  "Estoy listo".
- **Pending:**
  - the manual in-app switch to English mid-game on iOS (I6);
  - A1 and A6 on Android, blocked by `adb` (see the T053 note).

**2026-10-03: US6 preferences (T083–T087)**

- **Test-only launch argument:** `-resetPreferences` clears the app's `UserDefaults` domain before
  Koin starts, so every iOS UI test begins from the first-use defaults (FR-024). All UI tests pass
  it, so they cannot leak settings into each other.
- **Verified:**
  - `KeyValuePreferencesDataSourceTest`: a new instance reads back what an earlier one wrote;
  - Robolectric `PreferencesPersistenceTest`: UI changes, then a fresh Koin graph and Activity
    recreation;
  - XCUITest `PreferencesPersistenceUITests`: the defaults on a fresh install, and 6 cards, voice
    off and Spanish all surviving terminate and relaunch (SC-005).
