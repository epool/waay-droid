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
            // Section headers are visible and carry the header trait for VoiceOver's rotor (FR-025).
            // Custom headers: the system's grey, non-scaling header text fails the accessibility audit
            // (4.5:1 contrast, Dynamic Type).
            Section {
                Picker(state.cardCountLabel, selection: cardCountBinding) {
                    ForEach(state.cardCountOptions, id: \.value) { option in
                        Text(option.label).tag(option.value)
                    }
                }
                .pickerStyle(.inline)
                .labelsHidden()
                .accessibilityIdentifier("settings.cardCount")
            } header: {
                sectionHeader(state.cardCountLabel)
            }
            Section {
                Picker(state.languageLabel, selection: languageBinding) {
                    ForEach(state.languageOptions, id: \.choice) { option in
                        Text(option.label).tag(option.choice)
                    }
                }
                .pickerStyle(.inline)
                .labelsHidden()
                .accessibilityIdentifier("settings.language")
            } header: {
                sectionHeader(state.languageLabel)
            }
        }
        .navigationTitle(state.title)
        .navigationBarTitleDisplayMode(.inline)
    }

    private func sectionHeader(_ title: String) -> some View {
        Text(title)
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(Color.primary)
            .textCase(nil)  // sentence case on every iOS version, as on Android
    }

    private var languageBinding: Binding<LanguageChoiceUi> {
        Binding(
            get: { state.selectedLanguage },
            set: { onAction(SettingsActionOnLanguageSelect(choice: $0)) }
        )
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
                cardCountOptions: (3...7).map {
                    CardCountOptionUi(value: Int32($0), label: "\($0) cards (1–\((1 << $0) - 1))")
                },
                selectedCardCount: 5,
                languageLabel: "Language",
                languageOptions: [
                    LanguageOptionUi(choice: .device, label: "Device language"),
                    LanguageOptionUi(choice: .english, label: "English"),
                    LanguageOptionUi(choice: .spanish, label: "Español"),
                ],
                selectedLanguage: .device
            ),
            onAction: { _ in }
        )
    }
}
