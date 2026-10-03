# Wáay

A "magic cards" mind-reading game:

1. Think of a number.
2. Answer *yes* or *no* for a handful of cards.
3. Wáay tells you which number you picked.

This branch (`kmp`) is a from-scratch rewrite as a **Kotlin Multiplatform** app.
- All game logic, ViewModels, strings and text-to-speech orchestration are shared in Kotlin.
- The UIs are native: **Jetpack Compose** on Android and **SwiftUI** on iOS.
- The original 2014 Android app (`waay-droid`) is preserved on `master`.

## How this project is built

The project follows **spec-driven development** with [GitHub Spec Kit](https://github.com/github/spec-kit). Every feature moves through these stages:

1. Specification
2. Clarification
3. Technical plan
4. Tasks
5. Implementation
6. Convergence

There are human review gates between the stages.

- Project principles live in [`.specify/memory/constitution.md`](.specify/memory/constitution.md).
- Feature specs live in [`specs/`](specs/).
- Contributor and agent instructions are in [`AGENTS.md`](AGENTS.md).

## Status

| Spec | Scope | State |
|---|---|---|
| 001 | Magic cards game v1 (Android + iOS, randomized cards, TTS, EN/ES, settings, adaptive layouts and foldables) | implemented, awaiting acceptance |

## Quick start

You need:
- JDK 21. Gradle downloads everything else, including the JDK 25 it runs on.
- The Android SDK, with platform 37.1.
- For iOS: Xcode 26.4 or later, and [XcodeGen](https://github.com/yonaskolb/XcodeGen).

```sh
# Android: build, then install on a running emulator or device
./gradlew :androidApp:assembleDebug
adb install androidApp/build/outputs/apk/debug/androidApp-debug.apk

# iOS: generate the project, then open it and run the "Waay" scheme
xcodegen --spec iosApp/project.yml
open iosApp/iosApp.xcodeproj

# Tests and quality gates (the same ones CI runs)
./gradlew spotlessCheck :androidApp:lintDebug :shared:allTests :shared:koverVerify :androidApp:testDebugUnitTest
xcodebuild test -project iosApp/iosApp.xcodeproj -scheme Waay \
  -destination 'platform=iOS Simulator,name=iPhone 17'
```

The full validation guide, including manual scenarios on phones, foldables and tablets, is
[`specs/001-magic-cards-game/quickstart.md`](specs/001-magic-cards-game/quickstart.md).
