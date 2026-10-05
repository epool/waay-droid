import Shared
import SwiftUI

/// Card physics (spec 002 ADR-015, FR-026): system springs, a tilt cap and the answer thresholds.
/// With Reduce Motion (FR-014, ADR-018) cards don't tilt, rise or spring: they fade.
enum CardMotion {
    static let maxTiltDegrees: CGFloat = 12
    static let thresholdFraction: CGFloat = 1.0 / 3.0
    /// Points per second.
    static let flickVelocity: CGFloat = 1_200
    /// The Reduce Motion cross-fade, and the slide back after a short drag.
    static let fade = Animation.easeInOut(duration: 0.15)

    static func tilt(offset: CGFloat, width: CGFloat, reduceMotion: Bool) -> Double {
        guard width > 0, !reduceMotion else { return 0 }
        return Double(min(max(offset / width * maxTiltDegrees, -maxTiltDegrees), maxTiltDegrees))
    }
}

/// A card that has just been answered, flying off from where it was released (FR-010).
struct ExitingCard: Equatable {
    let card: GameContentUiCard
    let answer: Answer
    let fromOffset: CGFloat
    let frame: CGRect

    static func == (lhs: ExitingCard, rhs: ExitingCard) -> Bool {
        lhs.card.index == rhs.card.index && lhs.answer == rhs.answer && lhs.frame == rhs.frame
    }
}

/// The current card, draggable left and right (spec 002 FR-006 to FR-011): it follows the finger,
/// tilts, and shows the answer it would give. On release past a third of its width, or on a flick, it
/// asks `tryAnswer`; if the answer counts the card is handed to the exit animation, otherwise it
/// springs back. Mostly vertical drags never move it. Two blank backs behind it hint at the stack and
/// never show numbers (FR-015). With Reduce Motion it still follows the finger, without tilting.
struct CardStage: View {
    let card: GameContentUiCard
    let tryAnswer: (Answer, CGFloat) -> Bool

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var offset: CGFloat = 0
    @State private var entered = false
    @State private var pastThreshold = false
    /// The answer was accepted, and the card now belongs to the exit animation.
    @State private var handedOff = false
    /// True while a drag is in progress. Unlike `onEnded`, it also resets when the system cancels the
    /// drag (an app switch, a call), so the card can go back to rest (FR-016, contract U9).
    @GestureState private var dragging = false

    var body: some View {
        GeometryReader { proxy in
            let width = proxy.size.width
            ZStack(alignment: .top) {
                StackHint()
                CardFace(card: card)
                    .padding(.bottom, StackHint.depth * 2)
                    .overlay(alignment: offset > 0 ? .topLeading : .topTrailing) {
                        SwipeHint(card: card, offset: offset, width: width).padding(.bottom, StackHint.depth * 2)
                    }
                    .offset(x: offset)
                    .rotationEffect(
                        .degrees(CardMotion.tilt(offset: offset, width: width, reduceMotion: reduceMotion)),
                        anchor: .bottom
                    )
                    .scaleEffect(entered || reduceMotion ? 1 : 0.94)
                    .opacity(entered ? 1 : 0)
                    .accessibilityElement(children: .contain)
                    .accessibilityIdentifier("card.surface")
            }
            .contentShape(Rectangle())
            // Simultaneous, so a scrolling card (FR-004 fallback) still scrolls vertically.
            .simultaneousGesture(drag(width: width))
        }
        .sensoryFeedback(.impact(weight: .light), trigger: pastThreshold) { _, isPast in isPast }
        .onAppear { withAnimation(reduceMotion ? CardMotion.fade : .snappy) { entered = true } }
        .onChange(of: dragging) { _, isDragging in
            if !isDragging { settleIfAbandoned() }
        }
    }

    /// When a drag stops, a card that wasn't answered goes back to rest. `onEnded` already does this
    /// after a release; this also covers a drag the system cancelled, which never reaches `onEnded`.
    /// It runs after the current update, once `onEnded` (if any) has decided.
    private func settleIfAbandoned() {
        Task { @MainActor in
            guard !handedOff, offset != 0 else { return }
            pastThreshold = false
            withAnimation(reduceMotion ? CardMotion.fade : .bouncy) { offset = 0 }
        }
    }

    private func drag(width: CGFloat) -> some Gesture {
        DragGesture(minimumDistance: 12)
            .updating($dragging) { _, isDragging, _ in isDragging = true }
            .onChanged { value in
                // Only a mostly horizontal drag moves the card (FR-008).
                guard offset != 0 || abs(value.translation.width) > abs(value.translation.height) else { return }
                offset = value.translation.width
                pastThreshold = abs(offset) >= width * CardMotion.thresholdFraction
            }
            .onEnded { value in
                let threshold = width * CardMotion.thresholdFraction
                let velocity = value.velocity.width
                let answer: Answer? =
                    if offset >= threshold || (offset > 0 && velocity >= CardMotion.flickVelocity) {
                        .yes
                    } else if offset <= -threshold || (offset < 0 && velocity <= -CardMotion.flickVelocity) {
                        .no
                    } else {
                        nil
                    }
                pastThreshold = false
                if let answer, tryAnswer(answer, offset) {
                    handedOff = true
                    return
                }
                withAnimation(reduceMotion ? CardMotion.fade : .bouncy) { offset = 0 }
            }
    }
}

/// The "Yes"/"No" the card would give, fading in towards the threshold: text, never colour alone.
private struct SwipeHint: View {
    let card: GameContentUiCard
    let offset: CGFloat
    let width: CGFloat

    var body: some View {
        if offset != 0, width > 0 {
            let isYes = offset > 0
            Text(isYes ? card.yesLabel : card.noLabel)
                .font(.title2.weight(.bold))
                .foregroundStyle(.white)
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                .background(isYes ? Brand.violet : Color(white: 0.2), in: Capsule())
                .opacity(min(abs(offset) / (width * CardMotion.thresholdFraction), 1))
                .padding(24)
                .accessibilityIdentifier(isYes ? "card.hint.yes" : "card.hint.no")
        }
    }
}

/// Two blank card backs peeking out under the current card: a stack, with no numbers (FR-015).
private struct StackHint: View {
    static let depth: CGFloat = 8

    var body: some View {
        ZStack(alignment: .top) {
            ForEach([2, 1], id: \.self) { level in
                RoundedRectangle(cornerRadius: 28)
                    .fill(Color(.secondarySystemBackground))
                    .padding(.horizontal, 12 * CGFloat(level))
                    .padding(.top, Self.depth * CGFloat(level))
                    .padding(.bottom, Self.depth * CGFloat(2 - level))
            }
        }
        .accessibilityHidden(true)
    }
}

/// The card itself: the question as its header, every number as its body (FR-019, FR-001). Always an
/// opaque surface, never glass (FR-025).
struct CardFace: View {
    let card: GameContentUiCard

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(card.question)
                .font(.headline)
                .accessibilityAddTraits(.isHeader)
            CardGridView(numbers: card.numbers)
                // A fresh grid per card: in the scroll fallback (FR-004) each card starts at the top,
                // so no number stays hidden by the previous card's scrolling (FR-003a).
                .id(card.index)
        }
        .padding(16)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .background(Color(.systemBackground), in: RoundedRectangle(cornerRadius: 28))
        .shadow(color: .black.opacity(0.18), radius: 10, y: 4)
    }
}

/// The answered card flying off in its answer's direction, drawn over the screen so it can finish even
/// when the result has already replaced the card (FR-010). With Reduce Motion it fades out where it
/// was released instead (FR-014). Hidden from accessibility: it is only motion.
struct ExitingCardView: View {
    let exiting: ExitingCard
    let onFinish: () -> Void

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var offset: CGFloat
    @State private var opacity: Double = 1

    init(exiting: ExitingCard, onFinish: @escaping () -> Void) {
        self.exiting = exiting
        self.onFinish = onFinish
        _offset = State(initialValue: exiting.fromOffset)
    }

    var body: some View {
        CardFace(card: exiting.card)
            .padding(.bottom, StackHint.depth * 2)
            .frame(width: exiting.frame.width, height: exiting.frame.height)
            .offset(x: offset)
            .rotationEffect(
                .degrees(CardMotion.tilt(offset: offset, width: exiting.frame.width, reduceMotion: reduceMotion)),
                anchor: .bottom
            )
            .opacity(opacity)
            .position(x: exiting.frame.midX, y: exiting.frame.midY)
            .allowsHitTesting(false)
            .accessibilityHidden(true)
            .onAppear {
                if reduceMotion {
                    withAnimation(CardMotion.fade) {
                        opacity = 0
                    } completion: {
                        onFinish()
                    }
                } else {
                    let direction: CGFloat = exiting.answer == .yes ? 1 : -1
                    withAnimation(.snappy) {
                        offset = direction * exiting.frame.width * 1.6
                    } completion: {
                        onFinish()
                    }
                }
            }
    }
}
