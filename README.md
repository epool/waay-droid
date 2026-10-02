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
| 001 | Magic cards game v1 (Android + iOS, randomized cards, TTS, EN/ES, settings) | in progress |

## Quick start

Build, test and run instructions are added as spec 001 is implemented. Each feature's `quickstart.md` has the details.
