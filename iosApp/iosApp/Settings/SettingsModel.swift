import Observation
import Shared

/// Owns the shared `SettingsViewModel` for one Settings screen (ADR-001, contracts/ios-bridge.md).
/// - `init` is inert: throwaway `@State` initial values create nothing.
/// - The `ScreenScope` + ViewModel are created lazily on first use and reused afterwards.
/// - `.task` only drives collection; cancelling it never clears the ViewModel.
/// - `deinit` closes the scope when the screen leaves the hierarchy for good.
@MainActor
@Observable
final class SettingsModel {
    private var latestState: SettingsState?

    @ObservationIgnored nonisolated(unsafe) private var scope: ScreenScope?
    @ObservationIgnored private var viewModel: SettingsViewModel?

    var state: SettingsState {
        latestState ?? obtainViewModel().state.value
    }

    func observeState() async {
        for await state in obtainViewModel().state {
            latestState = state
        }
    }

    func observeEvents(_ onEvent: (SettingsEvent) -> Void) async {
        for await event in obtainViewModel().events {
            onEvent(event)
        }
    }

    func send(_ action: SettingsAction) {
        obtainViewModel().onAction(action: action)
    }

    private func obtainViewModel() -> SettingsViewModel {
        if let viewModel { return viewModel }
        let scope = ScreenScope()
        let viewModel = ViewModelProvider.shared.settingsViewModel(scope: scope)
        self.scope = scope
        self.viewModel = viewModel
        return viewModel
    }

    deinit {
        scope?.close()
    }
}
