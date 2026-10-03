import Shared
import SwiftUI

/// Stateful entry: owns the model, collects state and events, performs navigation.
struct SettingsRoot: View {
    @State private var model = SettingsModel()
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        SettingsScreen(state: model.state, onAction: model.send)
            .task { await model.observeState() }
            .task {
                await model.observeEvents { event in
                    switch onEnum(of: event) {
                    case .navigateBack:
                        dismiss()
                    }
                }
            }
    }
}
