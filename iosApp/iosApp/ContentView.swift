import Shared
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
            GamePlaceholderView(onSettingsClick: { path.append(.settings) })
                .navigationDestination(for: Route.self) { route in
                    switch route {
                    case .settings:
                        SettingsRoot()
                    }
                }
        }
    }
}

/// Temporary game entry (Phase 2) so Settings navigation can be exercised. Replaced by GameRoot in T052.
private struct GamePlaceholderView: View {
    let onSettingsClick: () -> Void

    var body: some View {
        Text(AppInfo.shared.NAME)
            .navigationTitle(AppInfo.shared.NAME)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button(action: onSettingsClick) {
                        Image(systemName: "gearshape")
                    }
                    .accessibilityLabel("Settings")
                    .accessibilityIdentifier("toolbar.settings")
                }
            }
    }
}

#Preview {
    ContentView()
}
