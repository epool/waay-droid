import Observation
import Shared

/// Owns the shared `GameViewModel` for the game screen (ADR-001, contracts/ios-bridge.md).
/// - `init` is inert: throwaway `@State` initial values create nothing.
/// - The `ScreenScope` + ViewModel are created lazily on first use and reused afterwards.
/// - `.task` only drives collection; cancelling it (e.g. Settings pushed) never clears the game.
/// - `deinit` closes the scope when the screen leaves the hierarchy for good.
@MainActor
@Observable
final class GameModel {
    private var latestState: GameState?

    @ObservationIgnored nonisolated(unsafe) private var scope: ScreenScope?
    @ObservationIgnored private var viewModel: GameViewModel?

    var state: GameState {
        latestState ?? obtainViewModel().state.value
    }

    func observeState() async {
        for await state in obtainViewModel().state {
            latestState = state
        }
    }

    func observeEvents(_ onEvent: (GameEvent) -> Void) async {
        for await event in obtainViewModel().events {
            onEvent(event)
        }
    }

    func send(_ action: GameAction) {
        obtainViewModel().onAction(action: action)
    }

    private func obtainViewModel() -> GameViewModel {
        if let viewModel { return viewModel }
        let scope = ScreenScope()
        let viewModel = ViewModelProvider.shared.gameViewModel(scope: scope)
        self.scope = scope
        self.viewModel = viewModel
        return viewModel
    }

    deinit {
        scope?.close()
    }
}
