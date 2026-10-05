import Shared
import SwiftUI

/// Arranges the game screen for the current size classes (ADR-008, FR-031):
/// side by side on iPad (regular width) and iPhone landscape (compact height), stacked otherwise.
/// With `keepTogether` (intro, result) both pieces are centered as one group.
struct AdaptiveGameLayout<Primary: View, Secondary: View>: View {
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    @Environment(\.verticalSizeClass) private var verticalSizeClass

    private let keepTogether: Bool
    private let primary: Primary
    private let secondary: Secondary

    init(
        keepTogether: Bool = false,
        @ViewBuilder primary: () -> Primary,
        @ViewBuilder secondary: () -> Secondary
    ) {
        self.keepTogether = keepTogether
        self.primary = primary()
        self.secondary = secondary()
    }

    static func isSideBySide(horizontal: UserInterfaceSizeClass?, vertical: UserInterfaceSizeClass?) -> Bool {
        horizontal == .regular || vertical == .compact
    }

    var body: some View {
        if keepTogether {
            // Centered when it fits; scrolls instead of clipping at the largest text sizes (FR-026).
            GeometryReader { proxy in
                ScrollView {
                    VStack(spacing: 32) {
                        primary
                        secondary
                    }
                    .frame(maxWidth: .infinity, minHeight: proxy.size.height)
                }
                .scrollBounceBehavior(.basedOnSize)
            }
        } else if Self.isSideBySide(horizontal: horizontalSizeClass, vertical: verticalSizeClass) {
            HStack(spacing: 24) {
                primary.frame(maxWidth: .infinity, maxHeight: .infinity)
                secondary.frame(maxWidth: 360)
            }
        } else {
            VStack(spacing: 12) {
                primary.frame(maxHeight: .infinity)
                secondary
            }
        }
    }
}

/// How an answer control is drawn: a button under the card, or a tall panel beside it.
enum AnswerStyle {
    case button
    case panel
}

/// Places the card stage and its two answers for the current size classes (spec 002 FR-012a,
/// FR-020, FR-021): "No" is always left of "Yes". Compact: buttons side by side under the card.
/// Wide (iPad, iPhone landscape): panels flanking the card.
struct CardStageLayout<Card: View, Answers: View>: View {
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    @Environment(\.verticalSizeClass) private var verticalSizeClass

    private let card: Card
    private let answer: (Answer, AnswerStyle) -> Answers

    init(@ViewBuilder card: () -> Card, @ViewBuilder answer: @escaping (Answer, AnswerStyle) -> Answers) {
        self.card = card()
        self.answer = answer
    }

    var body: some View {
        if AdaptiveGameLayout<EmptyView, EmptyView>.isSideBySide(
            horizontal: horizontalSizeClass, vertical: verticalSizeClass)
        {
            GeometryReader { proxy in
                let panelWidth = min(max(proxy.size.width * 0.14, 88), 160)
                HStack(spacing: 16) {
                    answer(.no, .panel).frame(width: panelWidth)
                    card.frame(maxWidth: .infinity, maxHeight: .infinity)
                    answer(.yes, .panel).frame(width: panelWidth)
                }
            }
        } else {
            VStack(spacing: 16) {
                card.frame(maxHeight: .infinity)
                AnswerGroup {
                    HStack(spacing: 12) {
                        answer(.no, .button)
                        answer(.yes, .button)
                    }
                }
            }
        }
    }
}
