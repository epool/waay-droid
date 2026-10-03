import Shared
import SwiftUI

/// Stateless, previewable game screen: renders `state` and forwards actions.
struct GameScreen: View {
    let state: GameState
    let onAction: (GameAction) -> Void

    var body: some View {
        content
            .padding()
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .navigationTitle(state.title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItemGroup(placement: .topBarTrailing) {
                    Button { onAction(GameActionOnNewGameClick.shared) } label: {
                        Image(systemName: "arrow.clockwise")
                    }
                    .accessibilityLabel(state.newGameLabel)
                    .accessibilityIdentifier("toolbar.newGame")

                    Button { onAction(GameActionOnSettingsClick.shared) } label: {
                        Image(systemName: "gearshape")
                    }
                    .accessibilityLabel(state.settingsLabel)
                    .accessibilityIdentifier("toolbar.settings")
                }
            }
    }

    @ViewBuilder
    private var content: some View {
        switch onEnum(of: state.content) {
        case .intro(let intro):
            IntroView(intro: intro, onReady: { onAction(GameActionOnReadyClick.shared) })
        case .card(let card):
            CardView(card: card, onAnswer: { onAction(GameActionOnAnswerClick(answer: $0, cardIndex: card.index)) })
        case .revealed(let revealed):
            ResultView(message: revealed.message, newGameLabel: revealed.newGameLabel) {
                onAction(GameActionOnNewGameClick.shared)
            }
        case .invalid(let invalid):
            ResultView(message: invalid.message, newGameLabel: invalid.newGameLabel) {
                onAction(GameActionOnNewGameClick.shared)
            }
        }
    }
}

private struct IntroView: View {
    let intro: GameContentUiIntro
    let onReady: () -> Void

    var body: some View {
        VStack(spacing: 32) {
            Text(intro.message)
                .font(.title2)
                .multilineTextAlignment(.center)
                .frame(maxWidth: 480)
                .accessibilityIdentifier("intro.message")
            Button(intro.readyLabel, action: onReady)
                .buttonStyle(.borderedProminent)
                .controlSize(.large)
                .accessibilityIdentifier("intro.ready")
        }
    }
}

private struct CardView: View {
    let card: GameContentUiCard
    let onAnswer: (Answer) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(card.progress)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .accessibilityIdentifier("card.progress")
            Text(card.question)
                .font(.headline)
            ScrollView {
                LazyVGrid(columns: [GridItem(.adaptive(minimum: 56), spacing: 8)], spacing: 8) {
                    ForEach(card.numbers, id: \.value) { number in
                        Text("\(number.value)")
                            .font(.title3.monospacedDigit())
                            .frame(maxWidth: .infinity, minHeight: 44)
                            .overlay(RoundedRectangle(cornerRadius: 8).stroke(.secondary))
                            .accessibilityLabel(number.label)
                            .accessibilityIdentifier("number.\(number.value)")
                    }
                }
            }
            HStack(spacing: 12) {
                Button { onAnswer(.yes) } label: {
                    Text(card.yesLabel).frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .accessibilityIdentifier("card.yes")

                Button { onAnswer(.no) } label: {
                    Text(card.noLabel).frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
                .accessibilityIdentifier("card.no")
            }
            .controlSize(.large)
        }
    }
}

private struct ResultView: View {
    let message: String
    let newGameLabel: String
    let onNewGame: () -> Void

    var body: some View {
        VStack(spacing: 32) {
            Text(message)
                .font(.title)
                .multilineTextAlignment(.center)
                .frame(maxWidth: 480)
                .accessibilityIdentifier("result.message")
            Button(newGameLabel, action: onNewGame)
                .buttonStyle(.borderedProminent)
                .controlSize(.large)
                .accessibilityIdentifier("result.newGame")
        }
    }
}

private func previewState(_ content: GameContentUi) -> GameState {
    GameState(title: "Wáay", settingsLabel: "Settings", newGameLabel: "New game", content: content)
}

#Preview("Intro") {
    NavigationStack {
        GameScreen(
            state: previewState(GameContentUiIntro(message: "Think of a number from 1 to 31 and let me guess it…", readyLabel: "I'm ready")),
            onAction: { _ in }
        )
    }
}

#Preview("Card") {
    NavigationStack {
        GameScreen(
            state: previewState(
                GameContentUiCard(
                    index: 1,
                    progress: "Card 2 of 5",
                    question: "Is your number on this card?",
                    numbers: [19, 3, 27, 6, 15, 22, 7, 31, 2, 11, 30, 18, 14, 23, 10, 26].map { NumberUi(value: $0, label: "\($0)") },
                    yesLabel: "Yes",
                    noLabel: "No"
                )
            ),
            onAction: { _ in }
        )
    }
}

#Preview("Revealed") {
    NavigationStack {
        GameScreen(
            state: previewState(GameContentUiRevealed(message: "The number you thought of is… 27!", number: 27, newGameLabel: "New game")),
            onAction: { _ in }
        )
    }
}
