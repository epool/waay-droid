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
            Picker(state.cardCountLabel, selection: cardCountBinding) {
                ForEach(state.cardCountOptions, id: \.value) { option in
                    Text(option.label).tag(option.value)
                }
            }
            .pickerStyle(.inline)
            .accessibilityIdentifier("settings.cardCount")
            // Language (US5) options are added by its story.
        }
        .navigationTitle(state.title)
        .navigationBarTitleDisplayMode(.inline)
    }

    private var cardCountBinding: Binding<Int32> {
        Binding(
            get: { state.selectedCardCount },
            set: { onAction(SettingsActionOnCardCountSelect(value: $0)) }
        )
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
            state: SettingsState(
                title: "Settings",
                backLabel: "Back",
                voiceLabel: "Magician's voice",
                voiceEnabled: true,
                cardCountLabel: "Number of cards",
                cardCountOptions: (3...7).map { CardCountOptionUi(value: Int32($0), label: "\($0) cards (1–\((1 << $0) - 1))") },
                selectedCardCount: 5
            ),
            onAction: { _ in }
        )
    }
}
