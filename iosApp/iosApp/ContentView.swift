import SwiftUI

/// App navigation (ADR-007): a `NavigationStack` with Settings pushed on top of the game, so the
/// game's model stays alive while Settings is shown (FR-016b).
struct ContentView: View {
    enum Route: Hashable {
        case settings
    }

    @State private var path: [Route] = []

    var body: some View {
        NavigationStack(path: $path) {
            GameRoot(onNavigateToSettings: { path.append(.settings) })
                .navigationDestination(for: Route.self) { route in
                    switch route {
                    case .settings:
                        SettingsRoot()
                    }
                }
        }
    }
}

#Preview {
    ContentView()
}
