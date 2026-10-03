#if DEBUG
import Observation
import Shared
import SwiftUI

/// Spike-only (T013): the ADR-001 ownership pattern. Removed in T043.
/// - `init` is inert, so throwaway `@State` initial values create nothing.
/// - `run()` lazily creates the `ScreenScope` + ViewModel once and only drives collection.
/// - `.task` cancellation stops collecting but keeps the ViewModel; `deinit` closes the scope.
@MainActor
@Observable
final class LifecycleProbeModel {
    private(set) var count: Int = 0

    @ObservationIgnored nonisolated(unsafe) private var scope: ScreenScope?
    @ObservationIgnored private var viewModel: LifecycleProbeViewModel?

    func run() async {
        for await value in obtainViewModel().count {
            count = value.intValue
        }
    }

    func increment() {
        obtainViewModel().increment()
    }

    private func obtainViewModel() -> LifecycleProbeViewModel {
        if let viewModel { return viewModel }
        let scope = ScreenScope()
        let viewModel = SpikeProbes.shared.lifecycleProbe(scope: scope)
        self.scope = scope
        self.viewModel = viewModel
        return viewModel
    }

    deinit {
        scope?.close()
    }
}

struct LifecycleProbeScreen: View {
    @State private var model = LifecycleProbeModel()

    var body: some View {
        NavigationStack {
            VStack(spacing: 16) {
                Text("\(model.count)")
                    .accessibilityIdentifier("probe.count")
                Button("Increment") { model.increment() }
                    .accessibilityIdentifier("probe.increment")
                NavigationLink("Push") {
                    Text("Pushed").accessibilityIdentifier("probe.pushed")
                }
                .accessibilityIdentifier("probe.push")
            }
            .task { await model.run() }
        }
    }
}
#endif
