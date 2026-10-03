# Implementation Plan: Magic Cards Game v1 ("Wáay")

**Branch**: `001-magic-cards-game` | **Date**: 2026-10-02 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/001-magic-cards-game/spec.md`

## Summary

Build the Wáay mind-reading game as a Kotlin Multiplatform app with **native UIs**: Jetpack Compose
on Android and SwiftUI on iOS. A single `shared` KMP module owns:
- the pure game domain: a randomized deck with hidden bit mapping, the decoder, and the
  `GameEngine` reducer;
- thin AndroidX ViewModels (MVI State / Action / Event);
- the EN/ES string catalog;
- speech orchestration;
- persisted preferences.

Each platform UI only renders `…State` and forwards `…Action`. The approach and every choice are
recorded as ADRs in [research.md](./research.md); ADR-001 is the hybrid ViewModel strategy.

## Technical Context

- **Language/Version:**
  - Kotlin 2.4.20 (KMP), with `androidTarget` provided by the Android-KMP library plugin, plus
    `iosArm64` and `iosSimulatorArm64`;
  - Swift 6 in Swift 6 language mode, with SwiftUI;
  - JDK 21 toolchain.
- **Primary Dependencies:**
  - Build: Gradle 9.7.0 (wrapper), AGP 9.3.3, SKIE 0.10.15.
  - Shared: kotlinx-coroutines 1.11.0, `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel`
    2.11.0, Koin 4.2.2, multiplatform-settings 1.3.0, Kermit 2.2.0.
  - Android: Compose BOM 2026.09.00 (Material 3 1.4.0), material3-adaptive 1.3.0, Navigation 3
    1.2.0, activity-compose 1.13.0.
  - Full matrix: ADR-000.
- **Storage:** key-value preferences through multiplatform-settings: SharedPreferences on Android,
  NSUserDefaults on iOS. No database.
- **Testing:**
  - `commonTest`: kotlin.test, AssertK, coroutines-test, Turbine, and fakes.
  - Android host: JUnit4, Robolectric, the Compose UI test APIs and Roborazzi.
  - iOS: Swift Testing and XCUITest.
  - Coverage: Kover.
- **Target Platform:**
  - Android 8.0+ (minSdk 26, compileSdk 37.1, targetSdk 37; AndroidX 2026.09 requires compileSdk ≥ 37);
  - iOS 17.0+;
  - phones, tablets and foldables, in every orientation.
- **Project Type:** a mobile app (KMP shared library plus two native app shells).
- **Performance Goals:**
  - instant UI response;
  - a card transition at 60 fps without jank;
  - a 7-card deck generated in well under 1 ms;
  - SC-003: a first game takes under 60 s.
- **Constraints:**
  - fully offline;
  - no analytics or accounts;
  - commonMain must stay platform-free and its dependencies must support jvm and wasmJs;
  - no Compose Multiplatform;
  - no generics or lifecycle types on Swift-facing APIs;
  - **visibility rule** (constitution V, analyze finding C1):
    - in `shared`, everything under `core`, `game.domain`, `settings.domain` and `settings.data`
      is `internal`;
    - only these are `public`: the presentation contracts (`…ViewModel`, `…State`, `…Action`,
      `…Event`, `…Ui`, `LanguageChoiceUi`), `Answer`, `ScreenScope`, `ViewModelProvider` and
      `initKoin`;
    - `commonTest` reaches `internal` code because it is in the same module.
- **Scale/Scope:**
  - 2 screens (Game with 4 phases, and Settings);
  - about 30 strings × 2 languages;
  - 7 deck sizes;
  - an expected size of about 2.5k lines of Kotlin and about 0.6k lines of Swift.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Pre-research | Post-design | Evidence |
|---|---|---|---|
| I. Spec-First Delivery | ✅ | ✅ | Spec approved (gate b). ADRs 000–011 with sources. Tasks follow. |
| II. Shared Brain, Thin Native UIs | ✅ | ✅ | Strings, speech orchestration and preferences are in `shared` (ADR-003/004/005). `Root`/`Screen` split in both [contracts](./contracts/). |
| III. Future-Proof Common Code | ✅ | ✅ | Every commonMain dependency publishes jvm and wasmJs. Platform services sit behind interfaces ([platform-services](./contracts/platform-services.md)). No CMP. |
| IV. Unidirectional Data Flow | ✅ | ✅ | Pure `GameEngine` reducer with injected `Random` (ADR-006). Thin AndroidX ViewModels (ADR-001). |
| V. Swift-Friendly Interop | ✅ | ✅ | SKIE (ADR-002). A non-generic `ScreenScope` and `ViewModelProvider` ([ios-bridge](./contracts/ios-bridge.md)). `List` rather than `ImmutableList` (ADR-011). |
| VI. Test-First Verification | ✅ | ✅ | Exhaustive and statistical tests, guarantees G1–G13 and S1–S6, one UI smoke test per platform (ADR-010, [quickstart](./quickstart.md)). |
| VII. Current, Compatible Dependencies | ⚠️ | ⚠️ (justified) | The matrix follows KGP 2.4.20's tested ranges. **Local Xcode 27 is outside the tested 26.4.** See Complexity Tracking. |
| VIII. Quality Gates | ✅ | ✅ | Spotless/ktlint/compose-rules, swift-format, Lint, Kover, and a two-job CI (ADR-011). |
| IX. Accessible & Localized | ✅ | ✅ | Labels in UI models, Dynamic Type and font-scale screenshots, EN/ES catalog enforced by the compiler. |
| X. Evolve Structure When Earned | ✅ | ✅ | A single `shared` module with feature-first packages. No `build-logic` yet. |

**Gate result:** PASS. The one deviation is justified below.

## Project Structure

### Documentation (this feature)

```text
specs/001-magic-cards-game/
├── spec.md
├── plan.md              # this file
├── research.md          # ADR-000…011
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── game-viewmodel.md
│   ├── settings-viewmodel.md
│   ├── ios-bridge.md
│   └── platform-services.md
├── checklists/requirements.md
└── tasks.md             # created by /speckit-tasks
```

### Source Code (repository root)

```text
settings.gradle.kts                 # includes :shared, :androidApp; foojay toolchains
build.gradle.kts                    # plugins apply false; Spotless config
gradle/libs.versions.toml           # single version catalog (ADR-000)
renovate.json
.github/workflows/ci.yml            # android (ubuntu) + ios (macos) jobs

shared/
├── build.gradle.kts                # kotlin.multiplatform + com.android.kotlin.multiplatform.library + SKIE + Kover; explicitApi()
└── src/
    ├── commonMain/kotlin/dev/epool/waay/
    │   ├── core/
    │   │   ├── domain/Result.kt, Error.kt                 # typed Result (Lackner error-handling)
    │   │   ├── i18n/Strings.kt, EnglishStrings.kt, SpanishStrings.kt, AppLanguage.kt, LanguageResolver.kt
    │   │   ├── locale/DeviceLocale.kt
    │   │   ├── speech/Speaker.kt, SpeechLanguage.kt
    │   │   └── logging/Log.kt                             # Kermit
    │   ├── game/
    │   │   ├── domain/CardCount.kt, Card.kt, Deck.kt, MagicDeck.kt, Answer.kt, AnswerDecoder.kt, GameEngine.kt
    │   │   └── presentation/GameState.kt, GameAction.kt, GameEvent.kt, GameViewModel.kt, GameUiMapper.kt
    │   ├── settings/
    │   │   ├── domain/Preferences.kt, LanguageChoice.kt, PreferencesDataSource.kt
    │   │   ├── data/KeyValuePreferencesDataSource.kt
    │   │   └── presentation/SettingsState.kt, SettingsAction.kt, SettingsEvent.kt, SettingsViewModel.kt
    │   └── di/SharedModule.kt, PlatformModule.kt (expect), InitKoin.kt
    ├── commonTest/kotlin/dev/epool/waay/…                 # domain, presentation, data tests + fakes
    ├── androidMain/kotlin/dev/epool/waay/                 # TextToSpeechSpeaker, AndroidDeviceLocale, PlatformModule.android.kt
    ├── iosMain/kotlin/dev/epool/waay/                     # AvSpeechSpeaker, IosDeviceLocale, PlatformModule.ios.kt, di/ScreenScope.kt, di/ViewModelProvider.kt
    └── iosTest/kotlin/dev/epool/waay/                     # ScreenScope → onCleared spike test

androidApp/
├── build.gradle.kts                # com.android.application (built-in Kotlin) + compose compiler + serialization
├── stability_config.conf
└── src/
    ├── main/AndroidManifest.xml    # TTS_SERVICE <queries>
    ├── main/res/values{,-es}/strings.xml   # app_name only
    ├── main/kotlin/dev/epool/waay/android/
    │   ├── WaayApp.kt (Application: initKoin), MainActivity.kt (enableEdgeToEdge)
    │   ├── navigation/WaayNavDisplay.kt, NavKeys.kt
    │   ├── game/GameScreen.kt (GameRoot + GameScreen + previews), components/…
    │   ├── settings/SettingsScreen.kt (SettingsRoot + SettingsScreen)
    │   └── ui/ObserveAsEvents.kt, theme/…, adaptive/…
    └── test/kotlin/dev/epool/waay/android/   # Robolectric robot smoke + Roborazzi matrix

iosApp/
├── project.yml                     # XcodeGen; iOS 17.0; Swift 6; pre-build embedAndSign script
├── iosApp.xcodeproj/               # generated, committed
├── iosApp/
│   ├── WaayApp.swift (initKoin), Info.plist, InfoPlist.xcstrings (display name EN/ES)
│   ├── Game/GameRoot.swift, GameModel.swift (@Observable), GameScreen.swift (+ #Preview)
│   └── Settings/SettingsRoot.swift, SettingsModel.swift, SettingsScreen.swift
├── WaayTests/                      # Swift Testing (target WaayTests)
└── WaayUITests/                    # XCUITest smoke (target WaayUITests)
```

**Structure decision**:
- The AGP 9 split: `androidApp` uses the application plugin only, and `shared` uses the
  KMP + Android-KMP library plugins.
- One shared module with feature-first packages (constitution X).
- The iOS app shell is generated by XcodeGen.
- This mirrors JetBrains KMP-App-Template-Native, Lackner's NativeKMPDemo and KaMPKit, and keeps
  room for `sharedUI`, `desktopApp` and `webApp` beside it later.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|---|---|---|
| Local builds use **Xcode 27.0**, outside Kotlin 2.4.20's tested **Xcode 26.4** (constitution VII) | Xcode 27 is the installed toolchain. CI pins 26.4, which is tested. | Downgrading the local Xcode before proving a problem costs time and disk. **The first implementation task is a spike** that builds and tests the SKIE framework and the iOS app with Xcode 27. If it fails, we install Xcode 26.4 side by side and use `DEVELOPER_DIR` (decided at that point with the human). |
| AGP **9.3.3**, a patch above KGP's tested 9.3.1 | Bug-fix patches only. Same minor version. | 9.3.1 would knowingly forgo shipped fixes. Patch releases don't change the compatibility contract. |
