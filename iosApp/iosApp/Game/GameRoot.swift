import Shared
import SwiftUI

/// Stateful entry: owns the model, collects state and events, performs navigation.
struct GameRoot: View {
    let onNavigateToSettings: () -> Void

    @State private var model = GameModel()

    var body: some View {
        GameScreen(state: model.state, onAction: model.send, canAnswer: model.canAnswer)
            .task { await model.observeState() }
            .task {
                await model.observeEvents { event in
                    switch onEnum(of: event) {
                    case .navigateToSettings:
                        onNavigateToSettings()
                    }
                }
            }
    }
}
