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
                .modifier(AnswerLabelColor(isYes: isYes))
                .frame(maxWidth: .infinity, maxHeight: style == .panel ? .infinity : nil)
        }
        .modifier(AnswerButtonStyle(isYes: isYes))
        .controlSize(.large)
        .accessibilityIdentifier(isYes ? "card.yes" : "card.no")
    }
}

/// Which family of styles the answer controls use (spec 002 FR-023, FR-024).
enum AnswerControlStyle: Equatable {
    /// Liquid Glass, from iOS 26.
    case glass
    /// The system's standard controls and materials on iOS 17–25.
    case materials

    static func `for`(majorVersion: Int) -> AnswerControlStyle {
        majorVersion >= 26 ? .glass : .materials
    }

    static let current = AnswerControlStyle.for(
        majorVersion: ProcessInfo.processInfo.operatingSystemVersion.majorVersion)
}

/// Brand tint for "Yes": `#8257F1`, visible against the backdrop's `#4527A0`; white text on it passes AA.
let yesTint = Color(red: 0x82 / 255, green: 0x57 / 255, blue: 0xF1 / 255)

/// Liquid Glass on iOS 26+: "Yes" prominent and tinted, "No" glass tinted with the system background
/// (FR-023). Before iOS 26 (FR-024), over the dark backdrop (FR-025): a filled bright violet "Yes" and
/// a solid "No", as in Slack's Catch up. Translucent materials would take on the backdrop's colour and
/// lose contrast.
private struct AnswerButtonStyle: ViewModifier {
    let isYes: Bool

    func body(content: Content) -> some View {
        if #available(iOS 26, *), AnswerControlStyle.current == .glass {
            if isYes {
                // The deep brand violet in both modes: prominent glass mixes its tint with what's
                // behind it, and the brighter #8257F1 (the accent in dark mode) measured 4.0:1 under
                // white text.
                content.buttonStyle(.glassProminent).tint(Brand.violet)
            } else {
                // Glass tinted with the system background: light over the violet backdrop in light
                // mode, dark in dark mode, so the system label keeps its contrast (FR-025).
                content.buttonStyle(.glass(.regular.tint(Color(.systemBackground).opacity(0.85))))
            }
        } else {
            content.buttonStyle(.borderedProminent).tint(isYes ? yesTint : Color(.systemBackground))
        }
    }
}

/// On glass the system picks label colours for contrast; on the solid fallbacks they are set here.
private struct AnswerLabelColor: ViewModifier {
    let isYes: Bool

    func body(content: Content) -> some View {
        if AnswerControlStyle.current == .glass {
            content
        } else {
            content.foregroundStyle(isYes ? Color.white : Color(.label))
        }
    }
}

/// Groups the two answer buttons so their glass shapes blend and morph together on iOS 26+.
struct AnswerGroup<Content: View>: View {
    @ViewBuilder let content: Content

    var body: some View {
        if #available(iOS 26, *) {
            GlassEffectContainer(spacing: 12) { content }
        } else {
            content
        }
    }
}
