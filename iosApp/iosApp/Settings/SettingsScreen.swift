import Shared
import SwiftUI

/// Stateless, previewable screen: renders `state` and forwards actions.
/// Back navigation uses the system back button/gesture (S5).
struct SettingsScreen: View {
    let state: SettingsState
    let onAction: (SettingsAction) -> Void

    var body: some View {
        Form {
            // Options are added per user story (voice: US3, card count: US4, language: US5).
        }
        .navigationTitle(state.title)
        .navigationBarTitleDisplayMode(.inline)
    }
}

#Preview {
    NavigationStack {
        SettingsScreen(state: SettingsState(title: "Settings", backLabel: "Back"), onAction: { _ in })
    }
}
