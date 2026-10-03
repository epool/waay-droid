# Contract: iOS bridge (Swift-facing API of the `Shared` framework)

This contract implements ADR-001 and ADR-002. Its rules:
- Swift must not see any `ViewModelStore` or `ViewModelStoreOwner`;
- no generic types appear on the Swift surface;
- everything goes through SKIE.

```kotlin
// shared/src/iosMain — dev.epool.waay.di
public class ScreenScope {                 // non-generic; owns a private ViewModelStore
    public fun close()                     // = viewModelStore.clear() → onCleared() on owned VMs
}

public object ViewModelProvider {          // Swift: ViewModelProvider.shared
    public fun gameViewModel(scope: ScreenScope): GameViewModel
    public fun settingsViewModel(scope: ScreenScope): SettingsViewModel
}

// shared/src/commonMain — dev.epool.waay.di
public fun initKoin(config: KoinAppDeclaration? = null)   // Swift: KoinKt.doInitKoin(config: nil)
```

## Swift usage pattern (normative)

```swift
struct GameRoot: View {
    @State private var model = GameModel()          // @Observable @MainActor wrapper, inert init
    var body: some View {
        GameScreen(state: model.state, onAction: model.send)
            .task { await model.run() }               // creates ScreenScope + VM; closes scope on cancel
    }
}
```

- `GameModel.run()` does the following:
  1. Creates a `ScreenScope`.
  2. Gets the ViewModel through `ViewModelProvider.shared.gameViewModel(scope:)`.
  3. Iterates `vm.state` and `vm.events` concurrently with `for await`, using SKIE `AsyncSequence`.
  4. Calls `scope.close()` in a `defer` or cancellation handler when the task ends.
- Sealed types are switched with `onEnum(of:)`.
- The `Screen` views are stateless, `(state, onAction)`, and have `#Preview`s.

## Verification (spike task)

- `iosSimulatorArm64Test`: `ScreenScope.close()` triggers `onCleared` on a test ViewModel.
- An Xcode build of `iosApp` that only references `ScreenScope` and `ViewModelProvider` compiles
  under Xcode 27 with SKIE 0.10.15.
