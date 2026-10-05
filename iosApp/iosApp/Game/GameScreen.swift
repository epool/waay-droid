import Shared
import SwiftUI

/// Stateless, previewable game screen: renders `state` and forwards actions. `canAnswer` tells whether
/// an answer for a card would be recorded now (ADR-014); cards only fly away when it says yes (FR-011).
///
/// Spec 002 layout (FR-019): the backdrop, a toolbar with the progress centred, and each phase drawn as
/// a card in front of it. The card phase is a draggable `CardStage` with its answers.
struct GameScreen: View {
    let state: GameState
    let onAction: (GameAction) -> Void
    /// Whether an answer for a card would be recorded now (ADR-014, FR-011).
    let canAnswer: (Int32) -> Bool

    static let space = "game"
    @State private var exiting: ExitingCard?

    var body: some View {
        ZStack {
            Backdrop()
            content
                .padding()
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            // Drawn over every phase, so the last card can finish flying off as the result appears.
            if let exiting {
                ExitingCardView(exiting: exiting) { self.exiting = nil }
                    .id(exiting.card.index)
            }
        }
        .coordinateSpace(.named(Self.space))
        .navigationTitle(state.title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbarColorScheme(.dark, for: .navigationBar)
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button {
                    onAction(GameActionOnNewGameClick.shared)
                } label: {
                    Image(systemName: "arrow.clockwise")
                        .foregroundStyle(.white)
                }
                .accessibilityLabel(state.newGameLabel)
                .accessibilityIdentifier("toolbar.newGame")
            }
            ToolbarItem(placement: .principal) { title }
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    onAction(GameActionOnSettingsClick.shared)
                } label: {
                    Image(systemName: "gearshape")
                        .foregroundStyle(.white)
                }
                .accessibilityLabel(state.settingsLabel)
                .accessibilityIdentifier("toolbar.settings")
            }
        }
    }

    /// The progress during cards (FR-017), the app's name otherwise. The title and the toolbar icons
    /// are white: the toolbar sits on the backdrop, which is dark in both modes (FR-025).
    @ViewBuilder
    private var title: some View {
        if case .card(let card) = onEnum(of: state.content) {
            Text(card.progress)
                .font(.headline)
                .foregroundStyle(.white)
                .accessibilityIdentifier("card.progress")
        } else {
            Text(state.title)
                .font(.headline)
                .foregroundStyle(.white)
        }
    }

    @ViewBuilder
    private var content: some View {
        switch onEnum(of: state.content) {
        case .intro(let intro):
            IntroView(intro: intro, onReady: { onAction(GameActionOnReadyClick.shared) })
        case .card(let card):
            CardPhase(card: card, canAnswer: canAnswer) { exit in
                exiting = exit
                onAction(GameActionOnAnswerClick(answer: exit.answer, cardIndex: exit.card.index))
            }
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

/// Where the card sits in the game screen, so the exit animation starts exactly there.
private struct CardFrameKey: PreferenceKey {
    static let defaultValue: CGRect = .zero
    static func reduce(value: inout CGRect, nextValue: () -> CGRect) { value = nextValue() }
}

/// The card phase: the draggable card and its "No"/"Yes" answers, placed for the size classes. A swipe
/// and a tap go through the same gate: only an answer `canAnswer` accepts throws the card (FR-011, FR-013).
private struct CardPhase: View {
    let card: GameContentUiCard
    let canAnswer: (Int32) -> Bool
    let onAnswer: (ExitingCard) -> Void

    @State private var frame: CGRect = .zero
    @State private var answered = 0

    var body: some View {
        CardStageLayout {
            CardStage(card: card, tryAnswer: tryAnswer)
                .id(card.index)
                .background(
                    GeometryReader { proxy in
                        Color.clear.preference(key: CardFrameKey.self, value: proxy.frame(in: .named(GameScreen.space)))
                    })
        } answer: { answer, style in
            AnswerControl(answer: answer, label: answer == .yes ? card.yesLabel : card.noLabel, style: style) {
                _ = tryAnswer(answer, 0)
            }
        }
        .onPreferenceChange(CardFrameKey.self) { frame = $0 }
        .sensoryFeedback(.success, trigger: answered)
        // Screen-reader users hear each new card's position as it appears (FR-025).
        .onChange(of: card.progress, initial: true) { _, progress in
            AccessibilityNotification.Announcement(progress).post()
        }
    }

    private func tryAnswer(_ answer: Answer, _ fromOffset: CGFloat) -> Bool {
        guard canAnswer(card.index) else { return false }
        answered += 1
        onAnswer(ExitingCard(card: card, answer: answer, fromOffset: fromOffset, frame: frame))
        return true
    }
}

/// The intro and result share the card look: an opaque card over the backdrop.
private struct MessageCard<Content: View>: View {
    @ViewBuilder let content: Content

    var body: some View {
        content
            .padding()
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color(.systemBackground), in: RoundedRectangle(cornerRadius: 28))
    }
}

private struct IntroView: View {
    let intro: GameContentUiIntro
    let onReady: () -> Void

    var body: some View {
        MessageCard {
            AdaptiveGameLayout(keepTogether: true) {
                Text(intro.message)
                    .font(.title2)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: 480)
                    .accessibilityAddTraits(.isHeader)
                    .accessibilityIdentifier("intro.message")
            } secondary: {
                Button(intro.readyLabel, action: onReady)
                    .buttonStyle(.borderedProminent)
                    .tint(Brand.violet)
                    .controlSize(.large)
                    .accessibilityIdentifier("intro.ready")
            }
        }
    }
}

private struct ResultView: View {
    let message: String
    let newGameLabel: String
    let onNewGame: () -> Void

    var body: some View {
        MessageCard {
            AdaptiveGameLayout(keepTogether: true) {
                Text(message)
                    .font(.title)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: 480)
                    .accessibilityAddTraits(.isHeader)
                    .accessibilityIdentifier("result.message")
                    .onAppear { AccessibilityNotification.Announcement(message).post() }
            } secondary: {
                Button(newGameLabel, action: onNewGame)
                    .buttonStyle(.borderedProminent)
                    .tint(Brand.violet)
                    .controlSize(.large)
                    .accessibilityIdentifier("result.newGame")
            }
        }
    }
}

private func previewState(_ content: GameContentUi) -> GameState {
    GameState(title: "Wáay", settingsLabel: "Settings", newGameLabel: "New game", content: content)
}

#Preview("Intro") {
    NavigationStack {
        GameScreen(
            state: previewState(
                GameContentUiIntro(
                    message: "Think of a number from 1 to 31 and let me guess it…", readyLabel: "I'm ready")),
            onAction: { _ in },
            canAnswer: { _ in true }
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
                    numbers: [19, 3, 27, 6, 15, 22, 7, 31, 2, 11, 30, 18, 14, 23, 10, 26].map {
                        NumberUi(value: $0, label: "\($0)")
                    },
                    yesLabel: "Yes",
                    noLabel: "No"
                )
            ),
            onAction: { _ in },
            canAnswer: { _ in true }
        )
    }
}

#Preview("Revealed") {
    NavigationStack {
        GameScreen(
            state: previewState(
                GameContentUiRevealed(
                    message: "The number you thought of is… 27!", number: 27, newGameLabel: "New game")),
            onAction: { _ in },
            canAnswer: { _ in true }
        )
    }
}
