# Research & ADRs: Magic Cards Game v1

**Feature**: [spec.md](./spec.md) · **Plan**: [plan.md](./plan.md) · **Date**: 2026-10-02

Each ADR uses the following structure:
- Decision
- Rationale
- Alternatives considered
- Revisit triggers
- Sources

All versions were verified on 2026-10-02 against Maven Central, Google Maven, the Gradle services
and the vendors' docs.

---

## ADR-000 — Version matrix (newest *mutually supported* set)

**Decision**

| Component | Version | Constraint that pins it |
|---|---|---|
| Kotlin (KMP, Compose compiler plugin, serialization plugin) | **2.4.20** | Latest stable. 2.5.0 is Beta. |
| Gradle (wrapper) | **9.7.0** | KGP 2.4.20 is tested up to 9.7.0 (9.8.0 exists). |
| Android Gradle Plugin | **9.3.3** | KGP 2.4.20 is tested up to AGP 9.3.1, so we take the latest 9.3 patch. 9.4.1 exists but is untested with KGP. |
| JDK (toolchain) | **21** | Installed LTS. AGP 9 and Kotlin 2.4 support it. |
| Android SDK | **compile 36 / target 36 / min 26** | API 36 is installed (36.1 if `android sdk` offers it). Min 26 is Android 8.0 (FR-030). |
| Xcode | **27.0 locally** / 26.4 in CI | Kotlin 2.4.20 is tested with Xcode 26.4. See the spike in ADR-001 and the Complexity Tracking table in the plan. |
| iOS deployment target | **17.0** | FR-030 and the constitution. Kotlin/Native 2.4 itself only needs 15.0. |
| SKIE | **0.10.15** | The first release that supports Kotlin 2.4.20. |
| kotlinx-coroutines (core, test) | **1.11.0** | |
| kotlinx-serialization | **1.11.0** | Only used for Navigation 3 keys in `androidApp`. |
| `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel` | **2.11.0** | KMP ViewModel. Publishes for jvm and wasmJs (constitution III). |
| Koin (core, core-viewmodel, android, androidx-compose) | **4.2.2** | |
| multiplatform-settings (+ coroutines, test) | **1.3.0** | |
| Kermit | **2.2.0** | |
| Compose BOM | **2026.09.00** | UI 1.12.1, Material 3 1.4.0. |
| material3-adaptive (+ adaptive-navigation3) | **1.3.0** | Window size class and posture (tabletop/book, hinge). |
| androidx.activity:activity-compose | **1.13.0** | |
| androidx.lifecycle (runtime-compose, viewmodel-navigation3) | **2.11.0** | |
| androidx.navigation3 (runtime, ui) | **1.2.0** | |
| Turbine | **1.2.1** | |
| AssertK | **0.28.1** | ADR-010. |
| Robolectric | **4.17** | |
| Roborazzi | **1.76.0** | ADR-010. |
| androidx.test (runner 1.7.0, ext-junit 1.3.0), Compose ui-test (BOM) | latest stable | |
| Spotless / ktlint / compose-rules | **8.10.3 / 1.8.0 / 0.6.7** | ADR-011. |
| Kover | **0.9.9** | |
| foojay-resolver-convention | **1.0.0** | |
| XcodeGen | **2.46.0** | |

**Rationale**
- Constitution VII asks for the newest versions that are *compatible with each other*, not the
  newest of each. Kotlin's compatibility table is the binding constraint.
- The same caution shows in the references: Lackner's NativeKMPDemo pins AGP 9.0.1 and Kotlin 2.4.0,
  and KaMPKit pins AGP 9.1.1 and Kotlin 2.3.20.

**Alternatives considered**
- AGP 9.4.1 with Gradle 9.8.0: newest, but outside KGP 2.4.20's tested range.
- Kotlin 2.5.0-Beta1: pre-release, which violates "stable".

**Revisit triggers**
- Kotlin 2.4.30 or 2.5.0 stable widening the AGP, Gradle or Xcode ranges. Renovate proposes the
  bump, and this table gets updated.

**Sources**
- [KMP compatibility guide](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html)
- [AGP ↔ Gradle](https://developer.android.com/build/releases/about-agp)
- [Kotlin 2.4.20 release](https://blog.jetbrains.com/kotlin/2026/09/kotlin-2-4-20-released/)
- [SKIE releases](https://github.com/touchlab/SKIE/releases)
- [Xcode 27 deployment targets](https://blakecrosley.com/blog/xcode-27-release)
- [Kotlin/Native 2.4 iOS 15 floor](https://byteiota.com/kotlin-2-4-swift-export-alpha-k1-compiler-removed/)

---

## ADR-001 — Where presentation logic lives on iOS and Android (ViewModel strategy)

**Decision**: a hybrid.

1. **Game rules are plain Kotlin and lifecycle-free.** `MagicDeck`, `AnswerDecoder` and the
   `GameEngine` reducer take an injected `Random`.
2. **Screen ViewModels extend AndroidX `ViewModel`** from
   `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel` in `commonMain`. They are thin adapters:
   - `StateFlow<State>`;
   - `onAction(Action)`;
   - `Flow<Event>` backed by a `Channel`;
   - side effects (speech, preferences).
3. **iOS never sees lifecycle types or generics.**
   - `iosMain` defines a non-generic `ScreenScope`. It owns a private `ViewModelStore` and exposes
     only `close()`, which calls `ViewModelStore.clear()`, so `onCleared` and the closeables run.
   - `ViewModelProvider.gameViewModel(scope:)` and `settingsViewModel(scope:)` create the ViewModels
     inside that store through Koin.
4. **One iOS ownership pattern.** `<Screen>Root` creates the scope and the ViewModel inside
   `.task`, and closes them when the task is cancelled (KaMPKit style).
   - This avoids throwaway `@State` initial values creating extra ViewModels.
   - ViewModel `init` is inert: side effects start on the first subscription or action.

**Rationale**

| Criterion | AndroidX VM | Pure Kotlin + hosts | expect/actual DIY |
|---|---|---|---|
| Android rotation, SavedStateHandle, Koin and Nav3 scoping | ✅ | ⚠️ wrapper | ✅ |
| Future CMP `sharedUI` | ✅ zero change | ⚠️ | ⚠️ swap actuals |
| Future jvm/wasmJs | ✅ | ✅ | ⚠️ new actuals |
| Idiomatic iOS | ⚠️ needs a bridge (that is point 3) | ✅ | ✅ |
| Official guidance | ✅ Google + JetBrains | — | — |

- Touchlab's "it depends" criteria point to Jetpack ViewModel for a *new project with future CMP*.
- Their native-iOS concern is answered by point 3.
- Osuala's concern, that logic should not depend on the lifecycle, is answered by point 1.
- Lackner's `dispose()` (cancelling `viewModelScope` only) skips `onCleared` and the closeables.
  `ViewModelStore.clear()` fixes that.

**Alternatives considered**
- Pure Kotlin ViewModels with an injected scope.
- Touchlab's expect/actual base. This is the **fallback** if the spike fails.
- KMP-ObservableViewModel. SKIE already covers it.

**Verification (first implementation task, a spike)**
- `ViewModelStore()` and `ViewModelProvider.create(store, factory)` (or Koin's `resolveViewModel`)
  work from `iosMain`.
- `store.clear()` triggers `onCleared`. Proven by a Turbine + `onCleared` test in
  `iosSimulatorArm64Test`.
- Swift compiles against `ScreenScope` and `ViewModelProvider`.
- The SKIE framework builds with Xcode 27.

**Revisit triggers**
- Swift export reaches Beta or Stable.
- The iOS bridge grows beyond about 50 lines.
- The `sharedUI` (CMP) spec.

**Sources**
- [Touchlab — Is AndroidX ViewModel the best choice for KMP?](https://touchlab.co/kmp-view-models) (2026-03-02)
- [Osuala — The Case for Pure Kotlin ViewModels](https://proandroiddev.com/kmp-architecture-the-case-for-pure-kotlin-viewmodels-c85ce95499ee)
- [Lackner — NativeKMPDemo](https://github.com/philipplackner/NativeKMPDemo)
- [Lackner — "Why I'm Not Much Using Compose Multiplatform Anymore"](https://youtu.be/uEGT1qVeHZM) (2026-07-29)
- [KaMPKit](https://github.com/touchlab/KaMPKit)
- [Google — ViewModel for KMP](https://developer.android.com/kotlin/multiplatform/viewmodel)

---

## ADR-002 — Swift interop: SKIE

**Decision**: use SKIE 0.10.15 on the static `Shared` framework. It provides:
- Flow and StateFlow as `AsyncSequence`, plus `.value`;
- sealed types as Swift enums through `onEnum(of:)`;
- `suspend` as `async`.

Explicit API mode keeps the exported header small.

**Rationale**
- SKIE is the de-facto production bridge, used by both Touchlab's KaMPKit and Lackner's
  NativeKMPDemo.
- It supports Kotlin 2.4.20.

**Alternatives considered**
- Kotlin Swift export: Alpha in 2.4. It supports Flow → AsyncSequence but is not production-ready,
  and it only works with direct integration.
- KMP-NativeCoroutines + KMP-ObservableViewModel: what the JetBrains native template uses. Two
  libraries instead of one, and more Swift boilerplate.

**Revisit**: when Swift export reaches Beta (planned spec 005).

**Sources**
- [SKIE](https://skie.touchlab.co)
- [Swift export docs](https://kotlinlang.org/docs/native-swift-export.html)
- [KMP-App-Template-Native](https://github.com/Kotlin/KMP-App-Template-Native)

---

## ADR-003 — Localization: shared typed Kotlin strings

**Decision**
- All user-facing and spoken text lives in `shared` as a typed Kotlin catalog:
  - a `Strings` interface, implemented by `EnglishStrings` and `SpanishStrings`;
  - functions for parameterised messages, for example `intro(max: Int)` and `reveal(n: Int)`.
- `LanguageResolver` maps the `LanguageChoice` (Device / English / Español) and the device locale,
  read through `expect`/`actual` `DeviceLocale`, to an `AppLanguage` (EN or ES). Any `es-*` locale
  maps to ES; everything else maps to EN.
- ViewModels expose **resolved text** inside `…Ui` models, so both UIs only render it.
- Only the app's display name is localized natively:
  - Android: `strings.xml` plus `values-es`;
  - iOS: `InfoPlist.xcstrings`.

**Rationale**
- The same text must be shown *and* spoken. TTS is orchestrated in shared code (constitution II).
- Implementing an interface makes the compiler enforce that both languages exist (SC-004).
- No plugin and no codegen.
- Strings are switchable at runtime without restarting (FR-021).
- About 30 strings: small enough for a typed catalog.
- Lackner's `UiText` exists to carry Android `R.string` ids across layers, which is not needed when
  the strings are already shared.

**Alternatives considered**
- moko-resources: generates native resources, but adds a Gradle plugin and maintenance risk.
- Native per-platform strings: duplicated, and TTS text would have to cross the bridge.
- Compose Multiplatform resources: requires Compose, which conflicts with the v1 constraint. Revisit
  in the `sharedUI` spec.
- `UiText` + `StringResource`: Android-only.

**Revisit**: more than about 150 strings, a third language, or the CMP `sharedUI` spec.

**Sources**
- [Touchlab — strings/resources live in KMP](https://www.strv.com/blog/kotlin-multiplatform-in-production-what-worked-what-didn-t)
- [Lackner — SwitchLanguageKMP](https://github.com/philipplackner/SwitchLanguageKMP)

---

## ADR-004 — Speech (magician voice)

**Decision**
- `commonMain` has an interface `Speaker { fun speak(text: String, language: AppLanguage); fun stop() }`.
  - `speak` always interrupts first (FR-015).
  - Failures and unavailability are swallowed and logged with Kermit (FR-016).
- **`androidMain`:** `android.speech.tts.TextToSpeech`.
  - Created lazily from the application `Context` (Koin `androidContext`).
  - Text requested before initialisation is held as a single pending utterance (last one wins).
  - Speaks with `QUEUE_FLUSH`.
  - Voice language set with `setLanguage(Locale)`, checked with `isLanguageAvailable`.
  - The manifest declares `<queries><intent><action android:name="android.intent.action.TTS_SERVICE"/></intent></queries>`,
    which Android 11+ package visibility requires.
  - Shut down from the Koin scope or the `Application`'s lifecycle.
- **`iosMain`:** `AVSpeechSynthesizer` through `platform.AVFAudio`.
  - Calls `stopSpeakingAtBoundary(.immediate)` and then speaks an `AVSpeechUtterance`.
  - Uses `AVSpeechSynthesisVoice(language:)`, preferring the device's regional variant of the same
    language (for example `es-MX` over `es-ES`), with a plain `es`/`en` fallback.
  - Keeps the default audio session, which respects the silent switch (spec assumption).
- **Orchestration** is in `GameViewModel`. On each phase transition it computes the line to speak:
  - Intro → invitation;
  - Card k → prompt;
  - Revealed → reveal;
  - Invalid → invalid message.

  It calls `speaker.speak` only when `voiceEnabled` is true, and calls `stop()` in `onCleared`.

**Rationale**
- Both engines are reachable from Kotlin, so the Swift UI stays free of logic.
- Tests use `FakeSpeaker`, which records utterances.

**Alternatives considered**
- A Swift-side speaker implementing a Kotlin protocol. This only makes sense if a Swift-only API is
  needed, which it is not.
- A third-party KMP TTS library: none is mature and supports wasm.

**Sources**
- [Android TextToSpeech](https://developer.android.com/reference/android/speech/tts/TextToSpeech)
- [Package visibility](https://developer.android.com/training/package-visibility)
- [AVSpeechSynthesizer](https://developer.apple.com/documentation/avfaudio/avspeechsynthesizer)

---

## ADR-005 — Persistence of preferences

**Decision**
- multiplatform-settings 1.3.0, using `ObservableSettings` with the `-coroutines` extensions for
  `Flow`s.
  - Android: `SharedPreferencesSettings`.
  - iOS: `NSUserDefaultsSettings`.
- The domain interface is `PreferencesDataSource`: a `preferences: Flow<Preferences>` plus setters.
- The data implementation is `KeyValuePreferencesDataSource`, named for what makes it unique, with
  no `Impl` suffix (Lackner's data-layer skill).
- Invalid stored values fall back to the defaults (FR-024): card count outside 3–7, or an unknown
  language.

**Rationale**
- It is Touchlab's library, supports android, ios, jvm and wasmJs (constitution III), and has an
  in-memory `MapSettings` for tests.
- There are only three scalar preferences.

**Alternatives considered**
- DataStore (`datastore-preferences-core`): no wasmJs (constitution III).
- Room or SQLDelight: overkill.

**Sources**
- [multiplatform-settings](https://github.com/russhwolf/multiplatform-settings)
- [KaMPKit](https://github.com/touchlab/KaMPKit)

---

## ADR-006 — Game domain and randomness

**Decision**
- `MagicDeck.create(cardCount: Int, random: Random): Deck` builds N `Card(bitValue = 2^k, numbers)`.
  - Each card's numbers are all `n` in `1..2^N−1` where `n and bitValue != 0` (FR-002).
  - Each number list is shuffled with `List.shuffled(random)`, a Fisher–Yates shuffle, which is
    uniform (FR-009).
  - The card list is shuffled the same way (FR-008).
- `AnswerDecoder.decode(cards, answers)` sums the `bitValue` of the "yes" cards (FR-004 and
  FR-010). A result of 0 is reported as `DecodeError.OutOfRange`, using Lackner's typed `Result`
  (FR-005).
- `GameEngine` is a pure reducer, `(GameSnapshot, GameCommand) -> GameSnapshot`, over these phases:
  - `Intro`;
  - `Asking(index)`;
  - `Revealed(number)`;
  - `Invalid`.

  It ignores answers outside `Asking` (FR-028).
- `bitValue` never leaves the domain. UI models carry only position, total and numbers (FR-011).
- `Random` is injected through Koin as `Random.Default`. Tests use `Random(seed)`.

**Rationale**
- Determinism under a seed allows an exhaustive correctness test (SC-001: 243 cases) and a
  statistical uniformity test (SC-002: 10,000 games per N, every position frequency within ±5%).

**Alternatives considered**
- A hand-written shuffle (bug risk).
- A secure random source: not needed for a party trick.

---

## ADR-007 — Screens, navigation and state ownership

**Decision**
- There are two screens and two ViewModels.
- **Game screen** (`GameViewModel`). It renders one of these phases from a single `GameState`:
  - Intro;
  - Card;
  - Revealed;
  - Invalid.

  It observes `PreferencesDataSource`:
  - a card-count change resets the game to Intro with the new N (FR-018);
  - a language change re-resolves the strings in place (FR-021).
- **Settings screen** (`SettingsViewModel`): card count (3–7), voice toggle, language choice.
- **Android:** Navigation 3.
  - `NavDisplay` with `@Serializable` keys `GameKey` and `SettingsKey` in a saveable back stack.
  - ViewModels are scoped per entry with `rememberViewModelStoreNavEntryDecorator`.
- **iOS:** `NavigationStack` with `navigationDestination` for Settings.
- **Gear button:** on Android it lives in the top app bar; on iOS, in the toolbar (FR-016a).

**Rationale**
- A single state machine per game keeps the phases consistent.
- Settings is the only other screen.
- This follows the official navigation-3 skill and Lackner's Nav3 usage.
- Push navigation is idiomatic on both platforms.

**Alternatives considered**
- One screen per phase: duplicates state, and back-navigation semantics get confusing.
- Settings as a sheet on iOS: viable, but rejected for consistency with Android and simpler UI tests.

---

## ADR-008 — Adaptive layouts and foldables

**Decision**
- **Android:**
  - `currentWindowAdaptiveInfo()` from material3-adaptive 1.3.0 provides `windowSizeClass` and
    `windowPosture`.
  - Width class *compact*: one column. Header, then the numbers grid, then the answer bar pinned at
    the bottom.
  - *Medium* or *expanded* width, or landscape: two panes, with the numbers grid next to the
    question and answers.
  - **Tabletop posture** (horizontal hinge): numbers above the hinge, question and answers below it.
  - **Book posture** (vertical hinge): split left and right at the hinge bounds.
  - In both postures, nothing is placed across the hinge (FR-032).
  - Numbers use `LazyVerticalGrid(GridCells.Adaptive(minSize))`, following the official adaptive
    skill, step 4.
  - We avoid the experimental `Grid`, `FlexBox` and `MediaQuery` APIs.
  - Edge-to-edge through `enableEdgeToEdge()` with insets handled (official edge-to-edge skill).
- **iOS:**
  - `horizontalSizeClass` and `verticalSizeClass` choose a stacked or side-by-side layout.
  - Numbers use `LazyVGrid(columns: [GridItem(.adaptive(minimum:))])`.
  - Respects safe areas and Dynamic Type.
- **State:** survives configuration changes through the ViewModel (Android) and through
  `@State`-owned task scope plus the shared state (iOS) (FR-029, FR-032).

**Sources**
- Official `adaptive` and `edge-to-edge` skills (android/skills, updated 2026-09)
- [Material 3 adaptive](https://developer.android.com/develop/ui/compose/layouts/adaptive)

---

## ADR-009 — iOS app shell

**Decision**
- The project is generated by XcodeGen from `iosApp/project.yml` (synced folders), and the generated
  `.xcodeproj` is committed.
- Swift 6 language mode, SwiftUI, and the Observation framework (`@Observable` and `@State`).
- Deployment target iOS 17.0.
- A pre-build script runs `./gradlew :shared:embedAndSignAppleFrameworkForXcode`, with
  `ENABLE_USER_SCRIPT_SANDBOXING = NO`.
- The `App` init calls `KoinKt.doInitKoin()`.
- **Tests:**
  - Swift Testing (`import Testing`) for the small Swift-side unit tests;
  - XCUITest for the smoke run (FR-025 and SC-006 checks with accessibility identifiers).

**Rationale**
- This is JetBrains' direct-integration pattern.
- XcodeGen keeps the project diffable.
- Observation is the modern SwiftUI data flow (constitution, iOS 17).

---

## ADR-010 — Testing stack

**Decision**
- **`commonTest`:**
  - `kotlin.test` as the runner;
  - **AssertK 0.28.1** for fluent assertions. It is KMP-wide (wasm included), used in Lackner's
    testing skill, and from the Jake Wharton-era Android ecosystem;
  - kotlinx-coroutines-test (`UnconfinedTestDispatcher` + `Dispatchers.setMain`) and Turbine;
  - fakes: `FakeSpeaker`, `FakeDeviceLocale`, and `MapSettings`-backed preferences;
  - it runs on Android host and on `iosSimulatorArm64`.
- **`androidApp` (`src/test`, host):**
  - JUnit4, Robolectric 4.17 and the Compose UI test APIs, with the **robot pattern** for the smoke
    flow (official testing-setup skill: UI tests on Robolectric, in the `test` source set);
  - **Roborazzi 1.76.0** screenshot tests of the Game screen phases across the official 3×3 size
    matrix (widths 400/610/900 dp × heights 400/500/1000 dp), plus font scale 1.5, for SC-009 and
    FR-026.
- **iOS:** Swift Testing unit tests and one XCUITest smoke test.

**Alternatives considered**
- JUnit5: JVM-only, so not usable in `commonTest`.
- The Compose Preview Screenshot Testing plugin: the official default, but its standalone plugin is
  deprecated below AGP 9.5.0-alpha03, and test suites need an alpha AGP. Revisit when AGP 9.5 is
  stable.
- Paparazzi: viable, but Robolectric is already needed for UI tests.

---

## ADR-011 — Quality gates, CI and updates

**Decision**
- **Formatting and lint:**
  - Spotless with ktlint 1.8.0 plus compose-rules 0.6.7 (Kotlin and `.kts`);
  - `xcrun swift-format lint --strict`;
  - Android Lint with `warningsAsErrors` for `androidApp`.
- **Coverage:** Kover 0.9.9 on `shared`, with `koverVerify` at ≥ 90% line coverage for the
  `game.domain` and `presentation` packages.
- **CI:** GitHub Actions, with two jobs.
  - `android` on ubuntu-latest, JDK 21:
    `spotlessCheck :androidApp:lintDebug :shared:testAndroidHostTest :androidApp:testDebugUnitTest koverVerify :androidApp:assembleDebug`.
  - `ios` on macos-latest with Xcode 26.4, which Kotlin tests:
    `:shared:iosSimulatorArm64Test`, `xcodegen`, then `xcodebuild test`.
- **Updates:** a Renovate config groups Kotlin, Compose compiler, SKIE and KGP updates together.
- **Compose stability without leaking types to Swift:**
  - Public UI models use `kotlin.collections.List`, which bridges to Swift `[T]`.
  - `ImmutableList` is avoided because it would export as an opaque protocol, violating constitution V.
  - The `androidApp` Compose compiler gets a `stability_config.conf` that marks
    `kotlin.collections.List` and `dev.epool.waay.**.presentation.**` as stable. Strong skipping
    remains on.
- **Kotlin 2.4 syntax caveat:** explicit backing fields are avoided until ktlint and compose-rules
  parse them reliably, so we use the classic `_state` / `state` pair (Lackner's MVI skill).

**Revisit**: ktlint or Detekt 2 stable releases with full Kotlin 2.4 support.

---

## Resolved unknowns

Every Technical Context item in plan.md is resolved, and no NEEDS CLARIFICATION remains. The open
risks below are handled by verification tasks, not by unknowns.

1. **Xcode 27 + Kotlin 2.4.20 + SKIE 0.10.15:** covered by the spike task.
2. **The `ViewModelStore` path from `iosMain`:** covered by the spike task.
3. **Kover with the Android-KMP library plugin's host tests:** covered by a verification task. The
   fallback is reporting coverage from the Android host tests only.
