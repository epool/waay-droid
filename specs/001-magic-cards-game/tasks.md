---

description: "Task list for 001 Magic Cards Game v1 (Wáay)"
---

# Tasks: Magic Cards Game v1 ("Wáay")

**Input**: Design documents from `specs/001-magic-cards-game/`

**Prerequisites**:
- [plan.md](./plan.md)
- [spec.md](./spec.md)
- [research.md](./research.md) (ADR-000…011)
- [data-model.md](./data-model.md)
- [contracts/](./contracts/)
- [quickstart.md](./quickstart.md)

**Tests**: tests are **mandatory** (constitution VI, Test-First). In every story, the test tasks come
first and MUST fail before the implementation tasks begin.

**Organization**: tasks are grouped by user story, so each story can be implemented, tested and
demoed on its own.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: can run in parallel (different files, no dependency on an incomplete task).
- **[Story]**: US1–US6 from spec.md. The Setup, Foundational, Cross-cutting and Polish phases have no
  story label.
- Shared code paths are relative to `shared/src/<sourceSet>/kotlin/dev/epool/waay/`. They are written
  in full below.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: project skeleton, build configuration and tooling. Versions come only from ADR-000.

- [X] T001 Create `settings.gradle.kts`:
  - `rootProject.name = "waay"`;
  - `include(":shared", ":androidApp")`;
  - `enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")`;
  - `pluginManagement` and `dependencyResolutionManagement` repositories (google, mavenCentral);
  - the `org.gradle.toolchains.foojay-resolver-convention` plugin, version 1.0.0.
- [X] T002 Create `gradle/libs.versions.toml` with the ADR-000 matrix **verbatim**:
  - Toolchain: kotlin 2.4.20, agp 9.3.3 (later 9.4.1, owner decision 2026-10-03), skie 0.10.15.
  - Shared libraries: coroutines 1.11.0, serialization 1.11.0, jetbrains-lifecycle 2.11.0, koin 4.2.2,
    multiplatform-settings 1.3.0, kermit 2.2.0.
  - Testing: turbine 1.2.1, assertk 0.28.1, robolectric 4.17, roborazzi 1.76.0, androidx-test-runner
    1.7.0, androidx-test-ext-junit 1.3.0.
  - Android: compose-bom 2026.09.00, material3-adaptive 1.3.0, activity-compose 1.13.0,
    androidx-lifecycle 2.11.0, navigation3 1.2.0.
  - Quality: spotless 8.10.3, ktlint 1.8.0, compose-rules 0.6.7, kover 0.9.9.
  - SDK levels: android-minSdk 26, android-compileSdk 36, android-targetSdk 36.
- [X] T003 Create the root `build.gradle.kts`:
  - every plugin with `apply false`;
  - Spotless: ktlint 1.8.0 plus `io.nlopez.compose.rules:ktlint` 0.6.7 for `**/*.kt` and `**/*.kts`,
    excluding `**/build/**`.

  Also create `.editorconfig`, with `ktlint_code_style = ktlint_official` and the compose-rules
  settings.
- [X] T004 [P] Create `gradle.properties`:
  - `org.gradle.configuration-cache=true`;
  - `org.gradle.caching=true`;
  - `org.gradle.parallel=true`;
  - `kotlin.code.style=official`;
  - `android.useAndroidX=true`;
  - JVM args `-Xmx4g`.
- [X] T005 Create `shared/build.gradle.kts`:
  - Plugins: `kotlin.multiplatform`, `com.android.kotlin.multiplatform.library` and `co.touchlab.skie`.
    Kover is applied in T095, where it is first used.
  - `kotlin { explicitApi() }`.
  - `androidLibrary { namespace = "dev.epool.waay.shared"; compileSdk = 36; minSdk = 26; withHostTest {} }`.
  - `iosArm64()` and `iosSimulatorArm64()`, with `binaries.framework { baseName = "Shared"; isStatic = true }`.
  - commonMain dependencies: coroutines-core, jetbrains lifecycle-viewmodel, koin-core,
    koin-core-viewmodel, multiplatform-settings (+ coroutines), kermit.
  - commonTest dependencies: kotlin-test, assertk, coroutines-test, turbine,
    multiplatform-settings-test.
  - androidMain dependencies: koin-android.
- [X] T006 Create `androidApp/build.gradle.kts`:
  - Plugins: `com.android.application`, `org.jetbrains.kotlin.plugin.compose`,
    `org.jetbrains.kotlin.plugin.serialization`. Roborazzi is applied in T088. No `kotlin.android`, because AGP 9
    has built-in Kotlin.
  - `namespace = "dev.epool.waay.android"`, `applicationId = "dev.epool.waay"`, min 26,
    compile/target 36.
  - `composeCompiler { stabilityConfigurationFiles.add(...stability_config.conf) }`.
  - `testOptions.unitTests.isIncludeAndroidResources = true`.
  - Dependencies:
    - `implementation(projects.shared)`;
    - Compose BOM, material3, material3-adaptive, activity-compose, lifecycle-runtime-compose,
      lifecycle-viewmodel-navigation3, navigation3-runtime/ui, koin-androidx-compose,
      serialization-core;
    - test: junit4, robolectric, compose ui-test-junit4, roborazzi(-compose, -junit-rule),
      androidx-test-ext-junit.
- [X] T007 [P] Create `androidApp/stability_config.conf`, marking `kotlin.collections.List` and
  `dev.epool.waay.**.presentation.**` as stable (ADR-011).
- [X] T008 Create `iosApp/project.yml` (XcodeGen):
  - app target `Waay`, bundle id `dev.epool.waay`, deployment target iOS 17.0, Swift 6 language mode;
  - synced folder `iosApp/`;
  - a pre-build script `cd "$SRCROOT/.." && ./gradlew :shared:embedAndSignAppleFrameworkForXcode`;
  - `ENABLE_USER_SCRIPT_SANDBOXING: NO`;
  - `FRAMEWORK_SEARCH_PATHS` and `OTHER_LDFLAGS` per the KMP direct-integration docs;
  - targets `WaayTests` (Swift Testing) and `WaayUITests` (XCUITest);
  - scheme `Waay`.
- [X] T009 Add minimal compile-only entry points:
  - `shared/src/commonMain/kotlin/dev/epool/waay/core/AppInfo.kt` (`public object AppInfo { public const val NAME: String = "Wáay" }`);
  - `androidApp/src/main/AndroidManifest.xml`;
  - `androidApp/src/main/kotlin/dev/epool/waay/android/MainActivity.kt`, with `enableEdgeToEdge()`
    and a placeholder `Text`;
  - `iosApp/iosApp/WaayApp.swift`, a placeholder `Text(AppInfo.shared.NAME)`.

  Then run `xcodegen --spec iosApp/project.yml`.
- [X] T010 Verify the skeleton builds:
  - `./gradlew spotlessApply :shared:compileKotlinIosSimulatorArm64 :androidApp:assembleDebug`;
  - `xcodebuild build -project iosApp/iosApp.xcodeproj -scheme Waay -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`.

  Record any Kotlin/Xcode compatibility warnings in `research.md` under ADR-000.

**Checkpoint**: both apps build and launch an empty screen.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: the risk spike, core types, DI, the preferences store, the settings scaffold and
navigation. **No user-story work starts until this phase is complete.**

### 2a. Risk spike (ADR-001 + Xcode 27). STOP AND ESCALATE if it fails.

- [X] T011 Write a failing test, `shared/src/iosTest/kotlin/dev/epool/waay/di/ScreenScopeTest.kt`. A
  test-only `ProbeViewModel` registers an `onCleared` flag; asserting that `ScreenScope.close()` sets
  it must fail at this stage.
- [X] T012 Implement `shared/src/iosMain/kotlin/dev/epool/waay/di/ScreenScope.kt`:
  - a non-generic `public class ScreenScope`;
  - a private `ViewModelStore`;
  - `public fun close()`, which calls `viewModelStore.clear()`. It is idempotent and thread-safe,
    hopping to the main thread before clearing, because Swift calls it from `deinit`;
  - an internal `inline fun <reified VM : ViewModel> obtain(factory)` that uses
    `ViewModelProvider.create(store, factory)`.

  Make T011 pass with `./gradlew :shared:iosSimulatorArm64Test`.
- [X] T013 Verify from Swift:
  1. Add `iosApp/WaayTests/ScreenScopeTests.swift` (Swift Testing). It creates a `ScreenScope()`,
     calls `close()` twice (idempotent), and asserts there is no crash.
  2. Add `iosApp/WaayUITests/NavigationLifecycleUITests.swift`. A probe screen's model must survive
     a `NavigationStack` push and pop without `onCleared`: the probe shows a counter that has to
     keep its value after the pop (analyze finding D1).
  3. Build and test with Xcode 27: `xcodebuild test -scheme Waay`.
  4. **If the build fails:** stop. Report to the human with the logs, and propose the fallbacks
     (Xcode 26.4 through `DEVELOPER_DIR`, or the Touchlab expect/actual base) per ADR-001.
  5. Record the outcome in the "Verification" section of ADR-001 in `research.md`.

### 2b. Core types and services

- [X] T014 [P] Write a failing test, `shared/src/commonTest/kotlin/dev/epool/waay/core/domain/ResultTest.kt`,
  covering `map`, `onSuccess`, `onFailure` and `asEmptyResult`.
- [X] T015 [P] Implement `shared/src/commonMain/kotlin/dev/epool/waay/core/domain/Result.kt` and
  `Error.kt`, using Lackner's error-handling skill: `Result<out D, out E : Error>`, `EmptyResult`,
  and the extensions. Visibility: `internal` (plan visibility rule, C1).
- [X] T016 [P] Implement `shared/src/commonMain/kotlin/dev/epool/waay/core/logging/Log.kt`: a Kermit
  `Logger` with the tag `Waay`.
- [X] T017 [P] Define `shared/src/commonMain/kotlin/dev/epool/waay/core/speech/Speaker.kt` and
  `SpeechLanguage.kt`, per [platform-services](./contracts/platform-services.md). Speech must never
  throw (FR-016).
- [X] T018 [P] Define `shared/src/commonMain/kotlin/dev/epool/waay/core/locale/DeviceLocale.kt`
  (interface), per [platform-services](./contracts/platform-services.md).
- [X] T019 [P] Define `shared/src/commonMain/kotlin/dev/epool/waay/core/i18n/AppLanguage.kt`
  (`enum AppLanguage { English, Spanish }`) and `Strings.kt`.
  - `Strings` is the interface with every user-facing text.
  - Its members are added per story. Here, add `appTitle`, `settingsLabel`, `settingsTitle` and
    `backLabel`.
- [X] T020 [P] Implement `shared/src/commonMain/kotlin/dev/epool/waay/core/i18n/EnglishStrings.kt`
  for the members defined so far.
- [X] T021 Create the test fakes:
  - `shared/src/commonTest/kotlin/dev/epool/waay/fakes/FakeSpeaker.kt`, which records
    `speak(text, language)` and `stop()`;
  - `FakeDeviceLocale.kt`;
  - `MainDispatcherTest.kt`, a base class that sets `Dispatchers.setMain(UnconfinedTestDispatcher())`.

### 2c. Preferences store (domain and data)

- [X] T022 [P] Write a failing test,
  `shared/src/commonTest/kotlin/dev/epool/waay/settings/data/KeyValuePreferencesDataSourceTest.kt`,
  using `MapSettings`. It covers these rules:
  - the defaults are "cardCount 5, voiceEnabled true, languageChoice Device";
  - "Invalid stored values fall back to the default" for a card count outside 3–7;
  - "Unknown keys fall back to `Device`";
  - the `preferences` Flow "emits current value first" and then emits on every setter.
- [X] T023 [P] Implement `shared/src/commonMain/kotlin/dev/epool/waay/game/domain/CardCount.kt`:
  - `CardCount.of(value): Result<CardCount, CardCountError>`;
  - the rule "3–7 inclusive";
  - derived `maxNumber = 2^value − 1` and `numbersPerCard = 2^(value−1)`;
  - `DEFAULT = 5`.

  Visibility: `internal` (plan visibility rule, C1). Write the test first, in `shared/src/commonTest/kotlin/dev/epool/waay/game/domain/CardCountTest.kt`.
- [X] T024 Implement the settings domain:
  - `shared/src/commonMain/kotlin/dev/epool/waay/settings/domain/LanguageChoice.kt`
    (`Device, English, Spanish`, each with a stable string key);
  - `Preferences.kt`;
  - `PreferencesDataSource.kt`.

  Visibility: `internal` (plan visibility rule, C1).
- [X] T025 Implement `shared/src/commonMain/kotlin/dev/epool/waay/settings/data/KeyValuePreferencesDataSource.kt`
  over `ObservableSettings` and the multiplatform-settings coroutines extensions. This makes T022 pass.
  Visibility: `internal` (plan visibility rule, C1).

### 2d. DI and app shells

- [X] T026 Implement the DI wiring:
  - `shared/src/commonMain/kotlin/dev/epool/waay/di/SharedModule.kt`;
  - `PlatformModule.kt` (`expect val platformModule: Module`);
  - `InitKoin.kt` (`public fun initKoin(config: KoinAppDeclaration? = null)`).
- [X] T027 [P] Implement `shared/src/androidMain/kotlin/dev/epool/waay/di/PlatformModule.android.kt`.
  It provides `ObservableSettings` as `SharedPreferencesSettings` (file "waay_preferences") from
  `androidContext()`, a placeholder `DeviceLocale` and a no-op `Speaker`. The real ones come in
  US3 and US5.
- [X] T028 [P] Implement `shared/src/iosMain/kotlin/dev/epool/waay/di/PlatformModule.ios.kt`, with
  `NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)` and the same placeholder speaker and
  locale as T027.
- [X] T029 Implement `shared/src/iosMain/kotlin/dev/epool/waay/di/ViewModelProvider.kt`
  (`public object ViewModelProvider : KoinComponent`). Its `…ViewModel(scope: ScreenScope)`
  factories are added per story.
- [X] T030 [P] Implement `androidApp/src/main/kotlin/dev/epool/waay/android/WaayApp.kt` (an
  `Application` that calls `initKoin { androidContext(this) }`) and register it in the manifest.
- [X] T031 [P] Update `iosApp/iosApp/WaayApp.swift` so its `init()` calls `KoinKt.doInitKoin(config: nil)`.

### 2e. Settings scaffold and navigation (screens get filled in by the stories)

- [X] T032 Write a failing test,
  `shared/src/commonTest/kotlin/dev/epool/waay/settings/presentation/SettingsViewModelTest.kt`, for
  guarantees S1 (reflects the persisted preferences or the defaults) and S5 (`OnBackClick` emits
  `NavigateBack` once), per [settings contract](./contracts/settings-viewmodel.md).
- [X] T033 Implement the settings presentation layer:
  - `shared/src/commonMain/kotlin/dev/epool/waay/settings/presentation/SettingsState.kt` (title and
    back label for now);
  - `SettingsAction.kt`;
  - `SettingsEvent.kt`;
  - `SettingsViewModel.kt`;
  - its Koin registration (`viewModelOf`);
  - `ViewModelProvider.settingsViewModel(scope)`.
- [X] T034 [P] Implement Android navigation:
  - `androidApp/src/main/kotlin/dev/epool/waay/android/navigation/NavKeys.kt`, with `@Serializable`
    `GameKey` and `SettingsKey`;
  - `WaayNavDisplay.kt`, using `NavDisplay` and `rememberNavBackStack(GameKey)` with
    `rememberSaveableStateHolderNavEntryDecorator` and `rememberViewModelStoreNavEntryDecorator`;
  - `ui/ObserveAsEvents.kt`;
  - `ui/theme/Theme.kt` (Material 3, dynamic color).
- [X] T035 [P] Implement `androidApp/src/main/kotlin/dev/epool/waay/android/settings/SettingsScreen.kt`:
  - `SettingsRoot`, which uses `koinViewModel()` and `ObserveAsEvents` → `onBack`;
  - a stateless `SettingsScreen(state, onAction)`, with a top app bar and a back button;
  - `@Preview`.
- [X] T036 [P] Implement the iOS settings screen:
  - `iosApp/iosApp/Settings/SettingsModel.swift` (`@Observable @MainActor`, per
    [ios-bridge](./contracts/ios-bridge.md)):
    - an inert `init`;
    - `run()` lazily creates the `ScreenScope` and the ViewModel on the first call, reuses them
      afterwards, and only drives collection;
    - cancelling `.task` does **not** close the scope;
    - `deinit` calls `scope?.close()`;
  - `SettingsRoot.swift`;
  - `SettingsScreen.swift` (stateless, with `#Preview`).
- [X] T037 Wire the iOS root: `iosApp/iosApp/ContentView.swift`, a `NavigationStack` with
  `navigationDestination` for Settings, and a placeholder game view.

**Checkpoint**:
- the spike passed;
- preferences persist;
- Settings opens and closes on both platforms;
- `./gradlew :shared:allTests` is green.

---

## Phase 3: User Story 1 - The magician reads my mind (Priority: P1) 🎯 MVP

**Goal**: the parity game with the default 5 cards. Cards come in **ascending, unshuffled order**
here, like the original; US2 adds randomization. It has an intro, "I'm ready", Yes/No per card, the
reveal statement, the invalid result, and "New game".

**Independent Test**: start a game, think of 27, and answer truthfully: the reveal says 27. Repeat
for 1 and 31. Answering "No" to everything gives the invalid message.

### Tests for User Story 1 (write first, they must FAIL) ⚠️

- [X] T038 [P] [US1] Write `shared/src/commonTest/kotlin/dev/epool/waay/game/domain/MagicDeckTest.kt`.
  It checks "Card membership is bit-exact" for N = 3…7: each card holds exactly the
  `n ∈ 1..maxNumber` with `n and bitValue ≠ 0`, with size `numbersPerCard`. It also checks that the
  set of `bitValue`s is exactly {1, 2, …, 2^(N−1)}.
- [X] T039 [P] [US1] Write `shared/src/commonTest/kotlin/dev/epool/waay/game/domain/AnswerDecoderTest.kt`:
  - exhaustive: all 243 cases (every n, every N = 3…7) decode to n (SC-001);
  - all "No" gives `DecodeError.OutOfRange` (FR-005);
  - `answers.size ≠ N` gives `IncompleteAnswers`.
- [X] T040 [P] [US1] Write `shared/src/commonTest/kotlin/dev/epool/waay/game/domain/GameEngineTest.kt`.
  It covers the phase table in data-model.md:
  - `Intro --Ready--> Asking(0)`;
  - answers advance through the cards;
  - the last answer gives `Revealed(n)` or `Invalid`;
  - `NewGame` from any phase gives Intro with a fresh deck;
  - "Commands that a phase doesn't allow are no-ops", including extra answers (FR-028).
- [X] T041 [P] [US1] Write `shared/src/commonTest/kotlin/dev/epool/waay/game/presentation/GameViewModelTest.kt`,
  covering guarantees G1–G6, G11 and G13 of [game contract](./contracts/game-viewmodel.md), using
  Turbine and `FakeSpeaker` (speech assertions come in US3).
- [X] T042 [P] [US1] Write `androidApp/src/test/kotlin/dev/epool/waay/android/game/GameRobot.kt` and
  `GameFlowTest.kt` (Robolectric, robot pattern). The flow is Intro → "I'm ready" → 5 answers for 27
  → reveal shows 27 → New game → Intro. Run it with Robolectric `@Config(sdk = [26, 36])` so the
  Android 8.0 minimum is exercised (FR-030, analyze finding G2).
- [X] T043 [P] [US1] Write `iosApp/WaayUITests/GameFlowUITests.swift` (XCUITest). It runs the same
  flow using accessibility identifiers `intro.ready`, `card.yes`, `card.no`, `result.message` and
  `toolbar.newGame`. Mid-game, it also:
  - rotates with `XCUIDevice.shared.orientation = .landscapeLeft` and back;
  - backgrounds and reactivates the app;
  - opens Settings and returns.

  After each step, it asserts that the same card and progress are shown (FR-029, FR-016b, findings
  G3 and D1).

### Implementation for User Story 1

- [X] T044 [P] [US1] Implement the domain types in `shared/src/commonMain/kotlin/dev/epool/waay/game/domain/`:
  - `Answer.kt` (`public enum Answer { Yes, No }`, the only public domain type, because the UI
    actions use it);
  - `Card.kt` (`internal`; `bitValue` hidden; `numbers: List<Int>`);
  - `Deck.kt` (`internal`).
- [X] T045 [US1] Implement `shared/src/commonMain/kotlin/dev/epool/waay/game/domain/MagicDeck.kt`:
  `create(cardCount)` builds the bit-exact cards in ascending order (makes T038 pass).
- [X] T046 [US1] Implement `shared/src/commonMain/kotlin/dev/epool/waay/game/domain/AnswerDecoder.kt`.
  It returns `Result<Int, DecodeError>` (makes T039 pass).
- [X] T047 [US1] Implement `shared/src/commonMain/kotlin/dev/epool/waay/game/domain/GameEngine.kt`
  (the pure reducer), `GameSnapshot`, `GamePhase` and `GameCommand` (makes T040 pass). Visibility: `internal` (plan visibility rule, C1).
- [X] T048 [US1] Add the Story 1 members to `shared/src/commonMain/kotlin/dev/epool/waay/core/i18n/Strings.kt` and `EnglishStrings.kt`:
  - `intro(max: Int)`, `readyLabel`;
  - `progress(current: Int, total: Int)`, `cardQuestion`;
  - `yesLabel`, `noLabel`;
  - `reveal(number: Int)`, which must be a statement: "The number you thought of is… 27!";
  - `invalid(max: Int)`, `newGameLabel`;
  - `numberLabel(n)`.
- [X] T049 [US1] Implement the game presentation layer in
  `shared/src/commonMain/kotlin/dev/epool/waay/game/presentation/`:
  - `GameState.kt`, including the top-level `newGameLabel` for every phase (FR-006, SC-008),
    `GameAction.kt` and `GameEvent.kt`, per the contract;
  - `GameUiMapper.kt`, a pure `(GameSnapshot, Strings) -> GameState` that never maps `bitValue`.
- [X] T050 [US1] Implement `shared/src/commonMain/kotlin/dev/epool/waay/game/presentation/GameViewModel.kt`:
  - the `_state`/`state` pair with `stateIn(WhileSubscribed(5_000))`;
  - a `Channel` for events;
  - an inert init, with a `hasStarted` flag so re-subscribing never repeats start-up effects;
  - an internal `StringsProvider` returning `EnglishStrings` until T079 replaces it with the
    language-aware resolver (finding U2).

  Register it in `SharedModule.kt`, and add `ViewModelProvider.gameViewModel(scope)`. This makes
  T041 pass.
- [X] T051 [P] [US1] Implement `androidApp/src/main/kotlin/dev/epool/waay/android/game/GameScreen.kt`:
  - `GameRoot`, which uses `koinViewModel()`, `collectAsStateWithLifecycle` and `ObserveAsEvents`
    for `NavigateToSettings`;
  - a stateless `GameScreen`, which renders Intro, Card (`LazyVerticalGrid(GridCells.Adaptive(...))`
    of numbers plus the Yes/No buttons), Revealed and Invalid;
  - a gear action and a "New game" action in the top app bar, both visible in every phase (FR-006,
    FR-016a);
  - previews for each phase.

  Wire it into `WaayNavDisplay`. This makes T042 pass.
- [X] T052 [P] [US1] Implement the iOS game screen:
  - `iosApp/iosApp/Game/GameModel.swift`, which follows the same ownership as T036: lazy scope,
    `.task` only collects, close in `deinit`;
  - `GameRoot.swift`;
  - `GameScreen.swift`, rendering the phases with `onEnum(of:)` and `LazyVGrid(.adaptive)`, with a
    gear item and a "New game" item (`toolbar.newGame`) in the toolbar for every phase, and
    `#Preview`s.

  Replace the placeholder in `ContentView.swift`. This makes T043 pass.
- [ ] T053 [US1] Run `./gradlew :shared:allTests :androidApp:testDebugUnitTest` and `xcodebuild test`.
  Demo US1 on the Android emulator (`android run`) and on the iOS simulator, following `specs/001-magic-cards-game/quickstart.md`
  section 1. Note any deviations in that file.

**Checkpoint**: the MVP. The trick works end-to-end on both platforms, with ascending cards.

---

## Phase 4: User Story 2 - I can't figure out the trick (Priority: P2)

**Goal**: shuffle the card order and the numbers within each card on every new game, decoding
through the hidden mapping.

**Independent Test**: two consecutive games show different card orders and number arrangements, and
the reveal is still correct.

### Tests for User Story 2 ⚠️

- [X] T054 [P] [US2] Extend `shared/src/commonTest/kotlin/dev/epool/waay/game/domain/MagicDeckTest.kt`:
  - with a seeded `Random`, the deck is deterministic;
  - the cards are a permutation of the bit set;
  - each card's numbers are a permutation of its ascending set;
  - decoding is correct for all 243 cases across 20 seeds.
- [X] T055 [P] [US2] Write `shared/src/commonTest/kotlin/dev/epool/waay/game/domain/DeckUniformityTest.kt`
  (SC-002). Over 10,000 seeded decks per N:
  - each card appears in each position with a frequency within ±5% of 1/N;
  - the first-shown number is the card's smallest with a frequency within ±5% of `1/numbersPerCard`.
- [X] T056 [P] [US2] Extend `shared/src/commonTest/kotlin/dev/epool/waay/game/presentation/GameViewModelTest.kt`
  with two FR-011 checks:
  - **G5:** `OnNewGameClick` produces a deck that differs from the previous one (seeded).
  - **G13:** two decks with the same displayed numbers and different bit mappings produce *equal*
    `GameState`s.

### Implementation for User Story 2

- [X] T057 [US2] Update `shared/src/commonMain/kotlin/dev/epool/waay/game/domain/MagicDeck.kt` to
  `create(cardCount, random: Random)`. It uses `shuffled(random)` for the card order (FR-008) and
  for each card's numbers (FR-009). This makes T054 and T055 pass.
- [X] T058 [US2] Provide `Random.Default` through Koin in `shared/src/commonMain/kotlin/dev/epool/waay/di/SharedModule.kt`,
  and inject it into `GameViewModel` (this makes T056 pass). Re-run T042 and T043; the robot and
  XCUITest helpers must answer from the displayed numbers, not by position.

**Checkpoint**: US1 and US2 pass. Repeated play reveals nothing about the trick.

---

## Phase 5: User Story 3 - The magician speaks (Priority: P3)

**Goal**: speak the intro, a prompt for each card, the reveal and the invalid message, with a voice
toggle in Settings. It degrades gracefully when no voice is available.

**Independent Test**: with the voice on, play a full game and hear the intro, every card prompt and
the reveal. With the voice off, nothing is spoken, and the same text appears on screen.

### Tests for User Story 3 ⚠️

- [X] T059 [P] [US3] Extend `shared/src/commonTest/kotlin/dev/epool/waay/game/presentation/GameViewModelTest.kt`
  with the speech guarantees:
  - **G1:** the intro is spoken once, and is not repeated when re-subscribing. Every `speak(text)`
    equals the message currently shown in `state` (FR-013, finding G1);
  - **G2:** the card prompt is spoken, and it never contains card numbers;
  - **G3:** the reveal is spoken;
  - **G4:** the invalid message is spoken;
  - **G9:** with voice off, nothing is spoken, and `stop()` is called when voice is turned off
    mid-speech;
  - **G10:** every new line calls `speak`;
  - **G12:** `onCleared` calls `stop()`.
- [X] T060 [P] [US3] Extend `shared/src/commonTest/kotlin/dev/epool/waay/settings/presentation/SettingsViewModelTest.kt`
  with **S3**: `OnVoiceToggle` persists the setting and is reflected in the state.

### Implementation for User Story 3

- [X] T061 [US3] Add `voiceLabel` to `shared/src/commonMain/kotlin/dev/epool/waay/core/i18n/Strings.kt`
  and `EnglishStrings.kt`. *Implementation note (FR-013):* no separate `cardPrompt` string. The
  spoken card line is the on-screen progress plus the question, for example "Card 3 of 5. Is your
  number on this card?". It names the position, as clarified, and is always identical to text on
  screen.
- [X] T062 [US3] Add speech orchestration to `shared/src/commonMain/kotlin/dev/epool/waay/game/presentation/GameViewModel.kt`:
  - on each phase change, speak through `Speaker`, only when `voiceEnabled` is true;
  - observe `voiceEnabled` and call `stop()` when it turns false;
  - call `stop()` in `onCleared`.

  This makes T059 pass.
- [X] T063 [P] [US3] Implement `shared/src/androidMain/kotlin/dev/epool/waay/core/speech/TextToSpeechSpeaker.kt`
  (ADR-004):
  - lazy `TextToSpeech` initialisation;
  - a single pending utterance until the engine is ready;
  - `QUEUE_FLUSH`;
  - `setLanguage` with an `isLanguageAvailable` guard;
  - Kermit logging, with no exceptions thrown.

  Add `<queries><intent><action android:name="android.intent.action.TTS_SERVICE"/></intent></queries>`
  to `androidApp/src/main/AndroidManifest.xml`, and bind the speaker in `PlatformModule.android.kt`.
- [X] T064 [P] [US3] Implement `shared/src/iosMain/kotlin/dev/epool/waay/core/speech/AvSpeechSpeaker.kt`
  (ADR-004):
  - `stopSpeakingAtBoundary(AVSpeechBoundaryImmediate)` before each speak;
  - the regional voice preferred, with the `AVSpeechSynthesisVoice` language falling back to the
    plain language;
  - the default audio session.

  Bind it in `PlatformModule.ios.kt`.
- [X] T065 [US3] Add `voiceEnabled` and `voiceLabel` to `SettingsState` and handle `OnVoiceToggle` in
  `SettingsViewModel.kt` (this makes T060 pass). Add the switch row to
  `androidApp/.../settings/SettingsScreen.kt` and `iosApp/iosApp/Settings/SettingsScreen.swift`
  (`Toggle`).
- [ ] T066 [US3] Verify manually on the Android emulator and the iOS simulator, using `specs/001-magic-cards-game/quickstart.md`
  scenarios A1, A5 and I1, I5. Disable the TTS engine on the emulator to verify FR-016, the fallback
  with no blocking error.

**Checkpoint**: US1–US3 pass. The voice is optional and never blocks play.

---

## Phase 6: User Story 4 - Choose how hard the trick is (Priority: P3)

**Goal**: pick a card count from 3 to 7 in Settings. Changing it starts a new game on Intro.

**Independent Test**: choose 3 cards, and the range is 1–7 with 3 cards. Choose 7, and the range is
1–127, with 64 numbers per card that can be scrolled.

### Tests for User Story 4 ⚠️

- [X] T067 [P] [US4] Extend `SettingsViewModelTest.kt` with **S2**: `OnCardCountSelect(n)` persists
  for "3–7 inclusive", values outside the range are ignored, and the option labels read like
  "5 cards (1–31)".
- [X] T068 [P] [US4] Extend `GameViewModelTest.kt` with **G7**: changing the card count resets to
  Intro with the new N. The invitation states the new range, and Card shows `numbersPerCard`
  numbers. When the change happens with **no subscriber** (the player is in Settings), the intro
  speech stays pending and is spoken only on the next subscription (finding U1).

### Implementation for User Story 4

- [X] T069 [US4] Add `cardCountLabel` and `cardCountOption(count, max)` to `Strings` and
  `EnglishStrings`. Add `cardCountOptions` and `selectedCardCount` to `SettingsState`, and handle
  `OnCardCountSelect` in `SettingsViewModel.kt` (makes T067 pass).
- [X] T070 [US4] Make `GameViewModel.kt` observe `cardCount` from `PreferencesDataSource`. A change
  resets the game to Intro with a fresh deck (FR-018). Intro speech is deferred while `state` has
  no subscribers, using `subscriptionCount`. This makes T068 pass.
- [X] T071 [P] [US4] Add the card-count picker to the Android settings screen,
  `androidApp/.../settings/SettingsScreen.kt` (single-choice rows, accessible).
- [X] T072 [P] [US4] Add the card-count picker to the iOS settings screen,
  `iosApp/iosApp/Settings/SettingsScreen.swift` (`Picker` with an inline style).
- [ ] T073 [US4] Verify `specs/001-magic-cards-game/quickstart.md` scenarios A4 and I4: 7 cards gives 64 numbers, and they scroll at the
  largest font size.

**Checkpoint**: US1–US4 pass.

---

## Phase 7: User Story 5 - Play in my language (Priority: P3)

**Goal**: English and Spanish for every text and speech. The language follows the device by default
and can be overridden in Settings, switching immediately.

**Independent Test**: with the device in Spanish, everything is in Spanish. Switch to English in the
app, and everything, including speech, is in English, without restarting the game.

### Tests for User Story 5 ⚠️

- [X] T074 [P] [US5] Write `shared/src/commonTest/kotlin/dev/epool/waay/core/i18n/StringsTest.kt`
  (SC-004). Every `Strings` member is non-blank in EN and ES, the formatted messages include their
  arguments, and the ES reveal is a statement.
- [X] T075 [P] [US5] Write `shared/src/commonTest/kotlin/dev/epool/waay/core/i18n/LanguageResolverTest.kt`
  (FR-020):
  - `Device` + es-MX resolves to Spanish;
  - `Device` + fr-FR resolves to English;
  - English and Spanish win over any device locale;
  - the speech language prefers the device region when the language matches (es-MX), and falls back
    to the default region otherwise.
- [X] T076 [P] [US5] Extend `GameViewModelTest.kt` with **G8** (switching the language re-resolves
  all text in place, with the phase, deck and answers unchanged, and the next speech uses the new
  language). Extend `SettingsViewModelTest.kt` with **S4** (all labels switch immediately).

### Implementation for User Story 5

- [X] T077 [US5] Implement `shared/src/commonMain/kotlin/dev/epool/waay/core/i18n/SpanishStrings.kt`
  for every `Strings` member, and `LanguageResolver.kt`. This makes T074 and T075 pass.
- [X] T078 [P] [US5] Implement the device locales:
  - `shared/src/androidMain/kotlin/dev/epool/waay/core/locale/AndroidDeviceLocale.kt`
    (`Locale.getDefault()`);
  - `shared/src/iosMain/kotlin/dev/epool/waay/core/locale/IosDeviceLocale.kt`
    (`NSLocale.preferredLanguages.first`).

  Bind both in the platform modules, replacing the placeholders.
- [X] T079 [US5] Update `GameViewModel.kt` and `SettingsViewModel.kt` to combine `languageChoice`
  with `DeviceLocale` into the active `Strings` and `SpeechLanguage` (FR-021, FR-022). This makes
  T076 pass.
- [X] T080 [US5] Add `languageLabel`, `languageOptions` and `selectedLanguage` to `SettingsState`,
  and handle `OnLanguageSelect` in `SettingsViewModel.kt`. Add the language pickers to the Android
  `SettingsScreen.kt` and the iOS `SettingsScreen.swift`.
- [X] T081 [P] [US5] Localize the app display name:
  - Android: `androidApp/src/main/res/values/strings.xml` and `values-es/strings.xml`, with
    `app_name` "Wáay";
  - iOS: `iosApp/iosApp/InfoPlist.xcstrings`, with `CFBundleDisplayName` in EN and ES.
- [ ] T082 [US5] Verify `specs/001-magic-cards-game/quickstart.md` scenarios A1, A6, I1 and I6: Spanish device, then switch to English
  mid-game.

**Checkpoint**: US1–US5 pass.

---

## Phase 8: User Story 6 - The game remembers my preferences (Priority: P3)

**Goal**: the card count, voice and language survive an app restart, and a fresh install uses the
defaults.

**Independent Test**: set 6 cards, voice off and Spanish, then kill and reopen the app. All three
are still in effect.

### Tests for User Story 6 ⚠️

- [X] T083 [P] [US6] Extend `KeyValuePreferencesDataSourceTest.kt`: a value written through one
  instance is read back by a **new** instance over the same `Settings` (SC-005).
- [X] T084 [P] [US6] Write `androidApp/src/test/kotlin/dev/epool/waay/android/settings/PreferencesPersistenceTest.kt`
  (Robolectric). It sets the values through the UI, recreates the Activity and a fresh Koin graph
  over the same SharedPreferences, and asserts that the values are restored.
- [X] T085 [P] [US6] Write `iosApp/WaayUITests/PreferencesPersistenceUITests.swift`. It changes the
  settings, then calls `app.terminate()` and `app.launch()` and asserts that the settings were kept.
  For the defaults case it uses the launch argument `-resetPreferences`.

### Implementation for User Story 6

- [X] T086 [US6] Support the `-resetPreferences` launch argument, for test-only resets. Handle it in
  `iosApp/iosApp/WaayApp.swift` by clearing the `NSUserDefaults` keys before `initKoin`. Document it
  in `quickstart.md`.
- [X] T087 [US6] Make T083, T084 and T085 pass. Fix any gaps in `KeyValuePreferencesDataSource.kt`
  or the platform modules.

**Checkpoint**: all six stories pass.

---

## Phase 9: Cross-cutting — Adaptive layout, foldables, accessibility and robustness

**Purpose**: FR-025–FR-029, FR-031, FR-032, SC-006 and SC-009, applied to every screen built above.
These tasks follow the official `adaptive` and `edge-to-edge` skills (ADR-008).

- [X] T088 Write `androidApp/src/test/kotlin/dev/epool/waay/android/screenshots/GameScreenScreenshotTest.kt`
  (Roborazzi), for each Game phase (Intro, Card with N = 5 and N = 7, Revealed, Invalid):
  - every combination of widths 400, 610 and 900 dp with heights 400, 500 and 1000 dp;
  - plus 400×500 runs at font scale 1.5 and **2.0**, Android's largest nonlinear scale (FR-026,
    finding G4).

  Record the baselines only after the layouts are implemented, and ask the human to review them.
- [X] T089 Implement `androidApp/src/main/kotlin/dev/epool/waay/android/adaptive/AdaptiveGameLayout.kt`:
  - `currentWindowAdaptiveInfo()`: compact width gives one column with the answer bar pinned at the
    bottom; medium or expanded width, or landscape, gives two panes;
  - `windowPosture.isTabletop`: numbers above the hinge, answers below;
  - book posture: split left and right at the hinge bounds;
  - nothing ever placed across the hinge (FR-031, FR-032).

  Use it in `GameScreen.kt`.
- [ ] T090 [P] Implement `iosApp/iosApp/Game/AdaptiveGameLayout.swift`: size classes choose a stacked
  or side-by-side layout, `LazyVGrid(.adaptive)` holds the numbers, and safe areas are respected.
- [X] T091 Do an accessibility pass on Android (`GameScreen.kt`, `SettingsScreen.kt`):
  - `contentDescription` and semantics from the shared labels;
  - progress announced as a live region;
  - headings;
  - the gear labelled;
  - no colour-only cues;
  - the font-scale screenshot reviewed.

  Extend `GameFlowTest.kt` with semantic matchers and a state-restoration check (official
  testing-setup step 9).
- [ ] T092 [P] Do an accessibility pass on iOS (`GameScreen.swift`, `SettingsScreen.swift`):
  - `accessibilityLabel` and `accessibilityAddTraits(.isHeader)`;
  - an announcement for each new card;
  - Dynamic Type up to AX5 without clipping;
  - identifiers matching XCUITest.
- [X] T093 Write `androidApp/src/test/kotlin/dev/epool/waay/android/game/RapidInputTest.kt` (FR-028:
  a double tap records one answer) and `ConfigurationChangeTest.kt` (FR-029: recreating the Activity
  mid-game keeps the card and the answers).
- [ ] T094 Verify manually on the foldable and tablet emulators (`android emulator create`) and on
  an iPad simulator. Cover `specs/001-magic-cards-game/quickstart.md` scenarios A8–A10 and I8–I10, including the **iOS 17.x simulator**
  (FR-030). Also run a smoke game on an **API 26 (Android 8.0)** emulator (finding G2).

**Checkpoint**: SC-006 and SC-009 are verified, along with the screenshot baselines.

---

## Phase 10: Polish & Quality Gates

- [ ] T095 [P] Configure Kover in `shared/build.gradle.kts`: `koverVerify` requires a minimum of 90%
  line coverage, filtered to the packages `dev.epool.waay.game.domain`, `*.presentation` and
  `core.i18n`. If the Android-KMP host tests aren't supported, report coverage from host tests only
  and note it in ADR-011.
- [ ] T096 [P] Add Swift formatting: `.swift-format` at the repository root (indent 4, line length
  120), and a Gradle-independent check script, `scripts/swift-format-lint.sh`
  (`xcrun swift-format lint --strict --recursive iosApp/iosApp iosApp/WaayTests iosApp/WaayUITests`).
- [ ] T097 [P] Configure Android Lint in `androidApp/build.gradle.kts` with `warningsAsErrors = true`
  and `abortOnError = true`. Fix every finding.
- [ ] T098 [P] Create `.github/workflows/ci.yml` with two jobs:
  - `android` (ubuntu-latest, Temurin 21):
    `./gradlew spotlessCheck :androidApp:lintDebug :shared:testAndroidHostTest :androidApp:testDebugUnitTest :shared:koverVerify :androidApp:assembleDebug`.
    Roborazzi runs in verify mode.
  - `ios` (macos-latest with Xcode 26.4 selected): `./gradlew :shared:iosSimulatorArm64Test`, then
    `brew install xcodegen && xcodegen`, then `xcodebuild test`, then the swift-format lint script.
- [ ] T099 [P] Create `renovate.json`:
  - the `config:recommended` preset;
  - a group for Kotlin, KGP, Compose compiler and SKIE;
  - a group for the AGP;
  - the Gradle wrapper manager enabled;
  - `prConcurrentLimit` 5.
- [ ] T100 [P] Update the documentation:
  - `AGENTS.md` §6, replacing "expected" with the real build, test and lint commands;
  - `README.md`, with a quick start and a status table;
  - `specs/001-magic-cards-game/quickstart.md`, with any corrections found during the work.
- [ ] T101 Run the full [quickstart.md](./quickstart.md) validation, sections 1–3, on both platforms,
  and fix any failures. Then run `/speckit-converge` until it reports converged.

---

## Dependencies & Execution Order

### Phase Dependencies

1. **Setup (Phase 1):** starts immediately.
2. **Foundational (Phase 2):** depends on Setup. The **2a spike (T011–T013) gates everything**: if it
   fails, stop and escalate (ADR-001).
3. **US1 (Phase 3):** depends on Foundational. This is the MVP.
4. **US2 (Phase 4):** depends on US1 (it changes `MagicDeck` and the `GameViewModel` deck source).
5. **US3, US4 and US5 (Phases 5–7):** each depends on US1 and the settings scaffold (T032–T037).
   They are independent of each other, but all touch `SettingsState`, `SettingsScreen` and
   `GameViewModel`, so run them **sequentially** to avoid file conflicts.
6. **US6 (Phase 8):** depends on the settings options existing (US3–US5).
7. **Cross-cutting (Phase 9):** depends on the screens (US1–US5).
8. **Polish (Phase 10):** comes last.

### Within each story

- Tests, then domain, then presentation (ViewModel), then the platform UIs, then verification.
- Commit after each task, using Conventional Commits, and check the task off here.

### Parallel Opportunities

- **Setup:** T004 and T007.
- **Foundational:**
  - T014–T020 (different files);
  - T022 and T023;
  - T027, T028, T030 and T031;
  - T034, T035 and T036.
- **US1:**
  - tests T038–T043 in parallel;
  - the Android UI (T051) and iOS UI (T052) in parallel, once T050 is done.
- **US2:** tests T054–T056.
- **US3:** T063 (Android speaker) and T064 (iOS speaker).
- **US4:** T071 and T072.
- **US5:** T074–T076 and T078.
- **Phase 9:** T090 and T092 (iOS) in parallel with the Android tasks.
- **Polish:** T095–T100.

## Parallel Example: User Story 1

```text
Task: "T038 MagicDeckTest (bit-exact membership, N=3…7)"
Task: "T039 AnswerDecoderTest (243 exhaustive cases + invalid)"
Task: "T040 GameEngineTest (phase table + no-op commands)"
Task: "T041 GameViewModelTest (G1–G6, G11, G13)"
Task: "T042 Android GameRobot + GameFlowTest (Robolectric)"
Task: "T043 iOS GameFlowUITests (XCUITest)"
# after T050:
Task: "T051 Android GameRoot/GameScreen"
Task: "T052 iOS GameModel/GameRoot/GameScreen"
```

## Implementation Strategy

### MVP first (User Story 1)

1. Phase 1, Setup.
2. Phase 2, Foundational, including the **spike**.
3. Phase 3, US1.
4. **Stop and validate**: demo the trick on both platforms.

### Incremental delivery

1. US2: randomization.
2. US3: voice.
3. US4: card count.
4. US5: languages.
5. US6: persistence.
6. Phase 9: adaptive layout and accessibility.
7. Phase 10: Polish.

Each increment keeps the earlier stories green.

## Notes

- Constitution gates apply at every step: test-first, no business logic in the UIs, no platform
  APIs in commonMain, no generics or lifecycle types on the Swift API.
- Use the official skills where they apply:
  - `navigation-3` for T034;
  - `adaptive` for T089;
  - `edge-to-edge` for T009 and T051;
  - `testing-setup` for T042, T088 and T091;
  - `android-cli` for running and inspecting the app.
- Never hand-edit `project.pbxproj`. Change `iosApp/project.yml` and run `xcodegen`.
- Never `git push`.
