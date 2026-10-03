# Wáay Constitution

Wáay is a "magic cards" mind-reading game rebuilt as a Kotlin Multiplatform (KMP) app. The UIs are
native: Jetpack Compose on Android and SwiftUI on iOS. Web, desktop and Compose Multiplatform come in
later specs. This document defines the non-negotiable rules for every spec, plan, task and change.

## Core Principles

### I. Spec-First Delivery

- Production code MUST NOT be written without an approved `spec.md`, `plan.md` and `tasks.md` for
  the feature.
- Specs describe *what* and *why*, and MUST NOT name technologies. Plans describe *how*.
- Specs are living documents. Any behaviour change MUST start by amending the spec.
- Human review gates are mandatory, and no agent may skip them, including when it resumes
  automatically. They are:
  1. constitution;
  2. spec plus clarifications;
  3. plan plus tasks;
  4. acceptance.
- Every technical decision that has real alternatives MUST be recorded as an ADR in the feature's
  `research.md`. Each ADR MUST include:
  - the options considered;
  - the criteria;
  - the decision;
  - revisit triggers;
  - primary-source citations.

*Rationale:* the specs stay the single source of truth, so humans steer and agents execute. ADRs
make decisions auditable when the ecosystem moves.

### II. Shared Brain, Thin Native UIs

- The following MUST live in the KMP `shared` code:
  - domain logic and state machines;
  - ViewModels;
  - user-facing strings (EN/ES);
  - text-to-speech orchestration;
  - persistence.
- Platform UIs MUST only render state and forward user actions. They MUST NOT contain business
  rules, string composition or decision logic.
- Both platforms MUST use the same screen split:
  - a stateful `ScreenRoot` obtains the ViewModel, collects state and events, and performs
    navigation;
  - a stateless `Screen` takes `state` plus `onAction` and MUST be previewable.

*Rationale:* one tested implementation of behaviour, and UIs that are cheap to replace. The pattern
follows Philipp Lackner's NativeKMPDemo and Touchlab's KaMPKit (strings and resources in KMP).

### III. Future-Proof Common Code

- `commonMain` MUST NOT reference platform APIs.
- Platform capabilities MUST sit behind common interfaces, with `androidMain` and `iosMain`
  implementations injected through DI. Examples are speech, locale and storage.
- Every dependency added to `commonMain` MUST support `android`, `ios`, `jvm` and `wasmJs`, even
  though v1 only declares the Android and iOS targets.
- v1 MUST NOT use Compose Multiplatform. The structure MUST still allow a later `sharedUI` module to
  reuse the same ViewModels unchanged.

*Rationale:* desktop, web and Compose Multiplatform are already on the roadmap. The JetBrains
KMP-App-Template-Native shows the growth path: `shared` + `androidApp` + `iosApp`, later adding
`sharedUI`, `desktopApp` and `webApp`.

### IV. Unidirectional Data Flow

- Every screen MUST expose these, in `presentation/<screen>/`:
  - an immutable `XState` (a data class with defaults);
  - a sealed `XAction`, handled by a single `onAction(action)`;
  - a sealed `XEvent` for one-off effects, delivered through a `Channel` and exposed as a `Flow`.
- Business rules MUST be plain, lifecycle-free Kotlin: pure reducers or use cases. Randomness,
  clocks and other non-determinism MUST be injected.
- ViewModels are thin adapters. They own coroutine scope, state exposure and side effects, and MUST
  NOT contain business rules.
- ViewModels extend AndroidX `ViewModel` from `org.jetbrains.androidx.lifecycle` (ADR-001 of spec
  001). Changing that requires a superseding ADR.

*Rationale:* predictable, testable state, following Lackner's State/Action/Event pattern. Keeping
the logic lifecycle-free answers the pure-Kotlin-ViewModel argument (Osuala). Keeping AndroidX
`ViewModel` follows Google's KMP ViewModel guidance and Touchlab's "new project + future CMP"
recommendation.

### V. Swift-Friendly Interop

- The `shared` module MUST compile in explicit API mode and keep its public surface minimal.
- Swift-facing APIs MUST NOT expose generics or Android lifecycle types such as `ViewModelStore` or
  `ViewModelStoreOwner`.
- Sealed hierarchies and Flows MUST be consumed in Swift through SKIE: exhaustive enums with
  `onEnum(of:)`, and `AsyncSequence`.
- iOS MUST obtain ViewModels only through a single Koin-backed `ViewModelProvider`, scoped to a
  non-generic `ScreenScope`. The scope MUST be closed deterministically when the screen goes away,
  which clears the ViewModel's store.

*Rationale:* this follows Touchlab's guidance that iOS code must stay idiomatic and must not inherit
Android lifecycle concepts. Swift export is still Alpha (Kotlin 2.4), so SKIE is today's stable
bridge.

### VI. Test-First Verification

- Tests MUST be written before the implementation, and MUST fail first. Use `commonTest` with
  `kotlin.test`, `kotlinx-coroutines-test` and Turbine.
- Prefer fakes over mocks.
- Every randomized behaviour MUST be tested with seeded randomness and with invariants that hold for
  all seeds.
- Every functional requirement in a spec MUST trace to at least one automated test.
- Each platform MUST have at least one UI smoke test covering a full happy path: a Compose UI test on
  Android, and XCUITest on iOS.

*Rationale:* game correctness, meaning decoding every number for every card count, is cheap to prove
exhaustively and expensive to debug on devices.

### VII. Current, Compatible Dependencies

- Dependencies MUST use the newest stable versions that are *mutually supported*. That means checked
  against the Kotlin compatibility guide for Gradle, AGP and Xcode, and against each plugin's
  supported Kotlin version.
- The pinned matrix MUST be recorded, with sources, in the feature's `research.md`.
- All versions MUST live in the single version catalog `gradle/libs.versions.toml`.
- The build MUST use the Gradle wrapper only; Gradle is never installed globally.
- Automated update tooling MUST propose upgrades. An upgrade outside the supported matrix requires an
  ADR.

*Rationale:* staying current without breaking the toolchain. The Kotlin, AGP and Xcode coupling is
the most common KMP failure mode.

### VIII. Quality Gates

- Before a feature branch merges, all of the following MUST pass:
  - Spotless + ktlint (+ compose-rules);
  - `swift format` lint;
  - Android Lint;
  - Kover coverage verification on `shared`;
  - all tests;
  - CI.
- Commits MUST follow Conventional Commits and stay small. Each completed task in `tasks.md` gets its
  own commit.
- Agents MUST NOT push to any remote unless a human explicitly asks.

*Rationale:* automated, objective gates keep agent-produced code at the same bar as human code, and
small commits make resuming and reviewing work reliable.

### IX. Accessible & Localized by Default

- Every interactive or informative element MUST have:
  - a TalkBack or VoiceOver label sourced from the shared strings;
  - support for system font scaling and Dynamic Type;
  - no information conveyed by colour alone.
- All user-facing text, including spoken text, MUST exist in English and Spanish. The language
  follows the system language unless the user overrides it in the app.
- Speech output MUST respect the user's voice setting and MUST use the active language's voice.

*Rationale:* the game is fundamentally conversational ("think of a number…"). It must work for screen
reader users and in both of the audience's languages.

### X. Evolve Structure When Earned

- Start with one KMP module, `shared`, using feature-first packages:
  `dev.epool.waay.<feature>.{data,domain,presentation}`, plus `core` and `di`.
- Splitting into multiple Gradle modules or adding `build-logic` convention plugins MUST be justified
  by a documented trigger. Triggers are:
  - adding the web, desktop or `sharedUI` targets/apps;
  - a second team or app consuming part of `shared`;
  - measured build-time pain.
- The split MUST be recorded as an ADR.

*Rationale:* Lackner's NativeKMPDemo, Touchlab's KaMPKit and JetBrains' templates all start with a
single shared module. Premature modularization adds iOS umbrella-export and build overhead without
benefit at this size.

## Technology Constraints

- **Kotlin:** 2.4.x, the latest stable that is mutually compatible.
- **Build:** Gradle wrapper and version catalog.
- **AGP 9 project structure:**
  - `androidApp` uses `com.android.application` only, with built-in Kotlin;
  - `shared` uses `org.jetbrains.kotlin.multiplatform` + `com.android.kotlin.multiplatform.library`.
- **iOS:**
  - static `Shared` framework with SKIE, via direct integration (`embedAndSignAppleFrameworkForXcode`);
  - Xcode project generated by XcodeGen from `iosApp/project.yml`, never hand-edited;
  - SwiftUI with the Observation framework, minimum iOS 17.
- **Android:** Jetpack Compose with Material 3; the Navigation 3 and edge-to-edge guidance from the
  official Android skills.
- **Shared libraries:**
  - `kotlinx-coroutines`;
  - `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel`;
  - Koin, for DI;
  - Kermit, for logging;
  - multiplatform-settings, for persistence.

  Alternatives require an ADR.
- **Identifiers:** `dev.epool.waay` for the Android `applicationId`, the iOS bundle id and the Kotlin
  root package.
- **Deferred:** these are rejected for v1 and may be revisited by spec:
  - Compose Multiplatform;
  - Swift export (Alpha);
  - Kotlin Toolchain (Alpha);
  - Metro DI;
  - Molecule.

## Development Workflow

- **Spec Kit loop per feature:**
  1. `/speckit-specify`
  2. `/speckit-clarify` ⛔
  3. `/speckit-plan`
  4. `/speckit-tasks` and `/speckit-analyze` ⛔
  5. `/speckit-implement`, then `/speckit-converge`, repeated until converged
  6. Verification ⛔
- **Branches:**
  - `kmp` is the trunk of the rewrite; `master` keeps the legacy app;
  - each feature uses a branch `NNN-slug` created off `kmp`;
  - after acceptance it merges back with `git merge --no-ff`.
- **Durable progress:** `tasks.md` checkboxes plus one commit per task. Any session resumes by
  reading the active `tasks.md` and `git log`, as described in `AGENTS.md` under "Resuming work".
- **Official guidance first:** before inventing a pattern, consult:
  - `android docs search/fetch` (Android Knowledge Base, architecture recommendations);
  - the official Android skills in `.claude/skills/`;
  - the JetBrains `kotlin-agent-skills` plugin;
  - the reference playbook in `AGENTS.md` §7.
- **Third-party skills,** such as Philipp Lackner's architecture skills, apply only where they are
  consistent with this constitution. Known divergences are listed in `AGENTS.md` §6.

## Governance

- This constitution supersedes `AGENTS.md` and every other guidance file. When they conflict, the
  constitution wins and the conflicting file MUST be updated.
- Amendments are made only through `/speckit-constitution`, with explicit human approval, and are
  committed as `docs: amend constitution to vX.Y.Z`.
- **Versioning (SemVer):**
  - MAJOR: a principle is removed or redefined incompatibly;
  - MINOR: a principle or section is added or materially expanded;
  - PATCH: clarifications and wording.
- **Compliance:**
  - every `plan.md` MUST pass the "Constitution Check" gate before research, and again after design;
  - `/speckit-analyze` flags violations;
  - any justified deviation MUST appear in the plan's Complexity Tracking table, with a linked ADR.

**Version**: 1.0.0 | **Ratified**: 2026-10-02 | **Last Amended**: 2026-10-02
