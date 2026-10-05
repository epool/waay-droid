import Shared
import SwiftUI

/// One answer control (spec 002 FR-012, FR-020, FR-021): "Yes" in the prominent style, "No" in the
/// secondary style. `.button` sits under the card, `.panel` flanks it in wide layouts. Tapping throws
/// the card the same way a swipe does (FR-013).
struct AnswerControl: View {
    let answer: Answer
    let label: String
    let style: AnswerStyle
    let action: () -> Void

    private var isYes: Bool { answer == .yes }

    var body: some View {
        Button(action: action) {
            Text(label)
                .font(style == .panel ? .title2.weight(.semibold) : .headline)
                .multilineTextAlignment(.center)
                .foregroundStyle(isYes ? Color.white : Color(.label))
                .frame(maxWidth: .infinity, maxHeight: style == .panel ? .infinity : nil)
        }
        .modifier(AnswerButtonStyle(isYes: isYes))
        .controlSize(.large)
        .accessibilityIdentifier(isYes ? "card.yes" : "card.no")
    }
}

/// Over the dark backdrop (FR-025): "Yes" is a filled bright violet with white text (4.58:1), "No" a
/// solid light surface with label-coloured text, as in Slack's Catch up. Translucent fills would take
/// on the backdrop's colour and lose contrast. US4 swaps these for Liquid Glass on iOS 26+.
private struct AnswerButtonStyle: ViewModifier {
    let isYes: Bool

    /// `#8257F1`: visible against the backdrop's `#4527A0`, and white text on it passes AA.
    static let yesTint = Color(red: 0x82 / 255, green: 0x57 / 255, blue: 0xF1 / 255)

    func body(content: Content) -> some View {
        content
            .buttonStyle(.borderedProminent)
            .tint(isYes ? Self.yesTint : Color(.systemBackground))
    }
}
