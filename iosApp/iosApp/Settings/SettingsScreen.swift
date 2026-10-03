import Shared
import SwiftUI

/// Stateless, previewable screen: renders `state` and forwards actions.
/// Back navigation uses the system back button/gesture (S5).
struct SettingsScreen: View {
    let state: SettingsState
    let onAction: (SettingsAction) -> Void

    var body: some View {
        Form {
            Toggle(state.voiceLabel, isOn: voiceBinding)
                .accessibilityIdentifier("settings.voice")
            // Card count (US4) and language (US5) options are added by their stories.
        }
        .navigationTitle(state.title)
        .navigationBarTitleDisplayMode(.inline)
    }

    private var voiceBinding: Binding<Bool> {
        Binding(
            get: { state.voiceEnabled },
            set: { onAction(SettingsActionOnVoiceToggle(enabled: $0)) }
        )
    }
}

#Preview {
    NavigationStack {
        SettingsScreen(
            state: SettingsState(title: "Settings", backLabel: "Back", voiceLabel: "Magician's voice", voiceEnabled: true),
            onAction: { _ in }
        )
    }
}
