# Quickstart & Validation: Magic Cards Game v1

This guide proves the feature works end-to-end. Run it at the verification gate. Read it alongside:
- the contracts: [game](./contracts/game-viewmodel.md), [settings](./contracts/settings-viewmodel.md),
  [iOS bridge](./contracts/ios-bridge.md), [platform services](./contracts/platform-services.md);
- the [data model](./data-model.md).

## Prerequisites

- **JDK 21** to launch Gradle. `./gradlew` provisions Gradle 9.7.0. The Gradle daemon runs on
  JDK 25, which Gradle downloads itself (`gradle/gradle-daemon-jvm.properties`).
- **Android:**
  - Android SDK with platform 37.1: `android sdk install platforms/android-37.1`.
  - Android CLI (`android`).
  - Emulators:
    - **phone:** any `android emulator create` profile;
    - **foldable:** needs the command-line tools (`android sdk install cmdline-tools/23.0`), then
      `avdmanager create avd -n Waay_Fold -k "system-images;android-36;google_apis_playstore;arm64-v8a" -d pixel_9_pro_fold`.
      Drive postures with `adb emu fold|unfold|rotate` and `adb shell cmd device_state state 1|reset`;
    - **API 26 (Android 8.0):** for the minimum-version smoke run (FR-030). The arm64 API 26 image
      does not boot on Apple Silicon with emulator 37.2. Use an x86_64 host or a real device; the
      Robolectric `sdk = 26` flow covers it in the meantime.
- **iOS:**
  - Xcode 27 selected locally (`xcode-select -p`). CI uses Xcode 26.4.1.
  - `xcodegen`.
  - Simulators: the latest runtime plus the oldest one available. The **iOS 17.x** check runs in
    CI's `ios-minimum-os` job (`xcodebuild -downloadPlatform iOS -buildVersion 17.5`).

## 1. Automated checks (CI parity)

These mirror `.github/workflows/ci.yml`.

```sh
./gradlew spotlessCheck                         # Kotlin/KTS format + ktlint + compose-rules
./gradlew :androidApp:lintDebug                 # Android Lint, warnings are errors
./gradlew :shared:allTests                      # commonTest on the Android host + iosSimulatorArm64
./gradlew :shared:koverVerify                   # ≥ 90% lines on game.domain, *.presentation, core.i18n
./gradlew :androidApp:testDebugUnitTest         # Robolectric UI tests (API 26 and 36)
./gradlew :androidApp:verifyRoborazziDebug      # screenshot baselines (record: recordRoborazziDebug)
./gradlew :androidApp:assembleDebug
scripts/swift-format-lint.sh                    # swift-format lint --strict
xcodegen --spec iosApp/project.yml
xcodebuild test -project iosApp/iosApp.xcodeproj -scheme Waay \
  -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0' \
  -collect-test-diagnostics never               # Swift Testing + XCUITest
```

`-collect-test-diagnostics never` stops a failing UI test from waiting up to 10 minutes for
`simctl diagnose`.

**Expected:** all green. The following tests must exist and pass:

| Test | Proves |
|---|---|
| Exhaustive decode, every n in 1..2^N−1 for N = 3…7 (243 cases) | SC-001, FR-002, FR-004, FR-010 |
| Uniformity, 10,000 seeded games per N: each position within ±5%, and the first-shown number ≈ 1/numbersPerCard | SC-002, FR-008, FR-009 |
| All "No" gives Invalid | FR-005 |
| GameViewModel guarantees G1–G14 and SettingsViewModel guarantees S1–S6, with Turbine | Stories 1–6 |
| Strings: every `Strings` member is non-blank in EN and ES, and no state mixes languages | SC-004, FR-019 |
| Roborazzi: 3 widths × 3 heights, font scale 1.5 and 2.0, and the tabletop and book postures, for each Game phase | SC-009, FR-026, FR-031, FR-032 |
| `GameViewModelTest.speechFailuresNeverBlockTheGame` (a speaker that throws on every call) and Robolectric `TextToSpeechSpeakerTest` (flush queue mode, pending first line, `stop`, failed init) | FR-016, FR-015 |
| Roborazzi `SettingsScreenScreenshotTest`: EN with voice on, ES with voice off, font 2.0; reviewed for non-colour selection cues | FR-027, FR-026, FR-019 |

UI tests, kept the same on both platforms (Phase 13):

| Scenario | Android (Robolectric) | iOS (XCUITest) | Proves |
|---|---|---|---|
| Full round, reveal, new game | `GameFlowTest.playsAFullRoundThenStartsANewGame` (API 26 and 36) | `LaunchUITests`, `GameFlowUITests` | US1 |
| Rotation, background and a Settings round trip mid-game | `GameFlowTest.playsAFullRoundSurvivingInterruptionsThenStartsANewGame` (API 26 and 36) | `GameFlowUITests` | FR-029, FR-016b |
| A new card starts at the top | `CardScrollTest` | `CardScrollUITests` (skips where the grid fits) | FR-003a |
| A rapid double tap records one answer | `RapidInputTest` | `RapidInputUITests` | FR-028 |
| Spanish device, then English mid-game | `LanguageSwitchTest` | `LanguageSwitchUITests` | US5 |
| Fresh install defaults; settings survive a restart | `PreferencesPersistenceTest` | `PreferencesPersistenceUITests` | FR-024, SC-005 |
| A full round at the largest text size, portrait and landscape | `AccessibilityTest` (font 2.0) | `AccessibilityUITests` (AX5) | FR-026, SC-006 |
| Headings, labels and announcements | `GameFlowTest.gameIsUsableThroughSemantics` | `AccessibilitySemanticsUITests` (Xcode accessibility audit on every screen) | FR-025 |
| Activity recreation | `ConfigurationChangeTest` | covered by the rotation in `GameFlowUITests` | FR-029 |
| Screenshot baselines (sizes, fonts, fold postures, Settings) | `GameScreenScreenshotTest`, `SettingsScreenScreenshotTest` | none: see the T114 note in the run log | SC-009 |

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

**2026-10-03: device verification (T053, T066, T073, T082, T094)**

`adb` works again, so the Android emulator runs were done. Each was scripted with `android layout`
plus `adb shell input`, reading the screen the way a player would.

- **Android, Pixel 9 Pro XL (API 36):**
  - **A1:** fresh install, app locale `es-MX` (`cmd locale set-app-locales`). Spanish intro, cards
    and reveal ("¡27!"). Google TTS synthesized every line in Spanish; it maps es-MX to its es-US
    voice.
  - **A2:** two games show different first cards and number orders.
  - **A3:** all "No" gives the invalid message and "Nuevo juego".
  - **A4:** 7 cards mid-game resets to Intro with 1–127; scrolling the grid reaches all 64 numbers.
  - **A5:** voice off mid-game: the game continues, and no synthesis requests follow.
  - **A6:** English mid-game: the same card and numbers re-render in English, and the next speech
    is en-US.
  - **A7:** force-stop and relaunch: 7 cards, voice off and English are kept.
  - **FR-016:** with the Google TTS engine disabled, a full round plays silently with no crash or
    dialog.
  - **Speech evidence:** with voice on, one speech player per line (intro, 5 cards, reveal) is
    registered by `com.google.android.tts` in `dumpsys audio`. Audibility itself was not judged by
    ear.
  - **A10 (system `font_scale` 2.0, read back from settings):** a full 7-card round decodes 100
    correctly. Android 14+ scales large text non-linearly, so the visual evidence for the largest
    font is the Roborazzi `font2.0` baselines. **This run found T102:** a
    new card kept the previous card's scroll offset, hiding its first numbers. Fixed and re-verified
    on the emulator; see FR-003a.
- **Android, Pixel 9 Pro Fold (API 36),** AVD `Waay_Fold`, created with
  `avdmanager create avd -d pixel_9_pro_fold`. Controlled with `adb emu fold|unfold|rotate` and
  `cmd device_state state 1|reset`. A8 and A9 pass, and every transition keeps "Card 3 of 5" and
  its numbers:
  - unfolded → side by side;
  - folded (outer display) → stacked;
  - book posture → split at the vertical hinge;
  - tabletop (half-open + landscape) → numbers above the hinge, answers below;
  - half-width window (`wm size`, the split-screen reflow) → stacked.
- **iOS, iPhone 17 (iOS 27.0):**
  - **I1/I6:** `LanguageSwitchUITests`: Spanish device, then English mid-game re-renders the same
    card.
  - **I5:** with `voice_enabled = 0` in the app's defaults, no `TextToSpeech` audio-queue activity
    appears in the unified log. With voice on, the queue runs for the intro.
  - **I9 (Accessibility XXXL, portrait and landscape):** `AccessibilityUITests` plays a round with
    every control hittable. Screenshots were reviewed: nothing is clipped, and overflow scrolls.
- **iOS, iPad Pro 11-inch M5 (iOS 27.0) and iPhone 16 (iOS 18.6):** the full iOS suite (8 UI
  tests) passes, including rotation, backgrounding, the Settings round trip, the language switch
  and the T102 scroll fix. On iPad the card is side by side. `CardScrollUITests` skips there
  because all 64 numbers fit without scrolling.
- **Not done:**
  - **TalkBack and VoiceOver walkthroughs (A10, I9):** `adb input` taps bypass TalkBack's
    explore-by-touch, and simulators have no VoiceOver gestures. The semantics are covered by
    `gameIsUsableThroughSemantics` and the iOS labels and traits. Needs a person.
  - **iPad Split View (I8):** not scriptable with XCUITest. Resize reflow is covered on Android and
    by size classes on iOS. Needs a person.
  - **I10, iOS 17.x:** no 17.x runtime is installable with Xcode 27. iOS 18.6, the oldest available,
    passes. The CI job (T098) can add a 17.x simulator.
  - **API 26 emulator:** the `android-26;google_apis;arm64-v8a` image hangs in QEMU on this Apple
    Silicon host (emulator 37.2), even with a cold boot and SwiftShader. API 26 stays covered by the
    Robolectric `sdk = 26` flow.

**2026-10-03: full validation (T101), re-run at HEAD after convergence (T103–T105)**

- **Section 1, from `clean` with `--no-build-cache`:** all green.
  - spotless;
  - strict lint: 0 issues;
  - `:shared:allTests`: 69 Android-host tests (including Robolectric `TextToSpeechSpeakerTest`) and
    68 iosSimulatorArm64 tests;
  - `koverVerify`: 98.5%;
  - 13 Robolectric app tests;
  - Roborazzi verify: 64 baselines;
  - `assembleDebug`;
  - swift-format lint;
  - `xcodegen`: no project drift;
  - `xcodebuild test` on iPhone 17 / iOS 27.0: 11 of 11, which is 3 Swift Testing + 8 XCUITest.
- **Section 2:** see the device-verification entry above. Every Android scenario A1–A9 and iOS
  scenarios I1, I4–I7 and I9 pass. The gesture-driven screen-reader runs and iPad Split View are
  left for the acceptance gate.
- **Section 3, SC-003:** needs a first-time human player and a stopwatch, so it's left for the
  acceptance gate. For reference, a scripted 5-card round takes about 10 s.

**2026-10-03: acceptance**

The owner accepted spec 001 at the verification gate. The checks listed under T101 in `tasks.md`
were waived. iOS 17.x is still covered by CI's `ios-minimum-os` job once the branch is pushed.

**2026-10-03: first CI runs, test parity and CI speed-ups (T107–T114)**

- **I10 verified in CI:** run 37161441986 passed the full UI suite on an iPhone 15 with iOS 17.5,
  which is the minimum OS (FR-030). This was waived at acceptance and is now covered.
- **Test parity:** Android gained interruption, language-switch, fresh-install and largest-font
  tests; iOS gained rapid-double-tap and accessibility-audit tests.
  - The iOS audit found contrast below 4.5:1, which is fixed with a brand accent colour, primary-colour
    progress text and primary-colour Settings headers (ADR notes in `research.md`).
  - The Android tests found no new defects. The landscape-largest-font check uses a 640×360 dp phone.
  - iOS has no screenshot baselines. Adding them needs a snapshot-testing dependency, and a simulator
    runtime that is the same locally and in CI; that is a separate decision.
- **Local results:** Android has 19 Robolectric tests. iOS has 13 tests (3 unit + 10 UI), all passing
  on iPhone 16 (iOS 18.6) and iPhone 17 (iOS 27.0).
- **CI speed-ups:** see ADR-011. The before and after timings are recorded there once the new
  workflow has run twice.
