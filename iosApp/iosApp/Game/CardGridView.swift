import Shared
import SwiftUI

/// Draws a card's numbers in the grid the shared `CardGridFit` picks for the space this view gets:
/// every number visible at once and as large as it fits (spec 002 FR-001 to FR-003). Numbers are
/// never smaller than the player's text size; only when that can't fit does the grid scroll (FR-004).
struct CardGridView: View {
    let numbers: [NumberUi]

    /// The player's text size, following Dynamic Type (ADR-020: passes the accessibility audit).
    @ScaledMetric(relativeTo: .subheadline) private var minimumNumberSize: CGFloat = 15
    private let spacing: CGFloat = 6
    /// Readability cap for very few numbers on a large screen (spec 002 edge case).
    private let maximumNumberSize: CGFloat = 72

    var body: some View {
        GeometryReader { proxy in
            let grid = fit(in: proxy.size)
            if grid.scrolls {
                ScrollView { rows(grid) }
                    .scrollBounceBehavior(.basedOnSize)
            } else {
                rows(grid)
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier("card.numbers")
    }

    private func fit(in size: CGSize) -> CardGrid {
        let maxDigits = numbers.map { String($0.value).count }.max() ?? 1
        return CardGridFit.shared.fit(
            count: Int32(numbers.count),
            maxDigits: Int32(maxDigits),
            width: size.width,
            height: size.height,
            spacing: spacing,
            minFontSize: minimumNumberSize,
            maxFontSize: maximumNumberSize
        )
    }

    private func rows(_ grid: CardGrid) -> some View {
        let columns = max(Int(grid.columns), 1)
        return VStack(alignment: .leading, spacing: spacing) {
            ForEach(Array(stride(from: 0, to: numbers.count, by: columns)), id: \.self) { start in
                HStack(spacing: spacing) {
                    ForEach(numbers[start..<min(start + columns, numbers.count)], id: \.value) { number in
                        cell(number, grid)
                    }
                }
            }
        }
    }

    private func cell(_ number: NumberUi, _ grid: CardGrid) -> some View {
        Text("\(number.value)")
            // Tabular digits, so every number on the card has the same width (FR-003).
            .font(.system(size: grid.fontSize).monospacedDigit())
            .lineLimit(1)
            .frame(width: grid.cellWidth, height: grid.cellHeight)
            .overlay(RoundedRectangle(cornerRadius: 8).stroke(.secondary))
            .accessibilityLabel(number.label)
            .accessibilityIdentifier("number.\(number.value)")
    }
}
