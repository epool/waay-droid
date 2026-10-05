package dev.epool.waay.game.presentation

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * How a card's numbers are laid out: one size for every number (FR-003), cells that fill the card's
 * area (FR-002), and [scrolls] only when even the player's text size can't fit (FR-004).
 * Sizes are in the caller's units (dp on Android, pt on iOS).
 */
public data class CardGrid(
    val columns: Int,
    val rows: Int,
    val cellWidth: Double,
    val cellHeight: Double,
    val fontSize: Double,
    val scrolls: Boolean,
)

/**
 * Picks the grid that shows all of a card's numbers at the largest size that fits (ADR-013). Shared
 * so both UIs lay cards out identically; pure, so it is tested exhaustively in commonTest.
 */
public object CardGridFit {
    // Font model, in em: tabular digit advance and cell padding across, line height and padding down.
    private const val DIGIT_ADVANCE = 0.6
    private const val HORIZONTAL_PADDING = 0.5
    private const val LINE_HEIGHT = 1.2
    private const val VERTICAL_PADDING = 0.4
    private const val TOLERANCE = 1e-9

    /**
     * @param count numbers on the card
     * @param maxDigits digits of the card's widest number
     * @param width the card body's width, [height] its height, [spacing] the gap between cells
     * @param minFontSize the player's text size: numbers are never smaller (FR-004)
     * @param maxFontSize the readability cap for very few numbers on a large screen
     */
    public fun fit(
        count: Int,
        maxDigits: Int,
        width: Double,
        height: Double,
        spacing: Double,
        minFontSize: Double,
        maxFontSize: Double,
    ): CardGrid {
        val digits = max(maxDigits, 1)
        val cap = max(maxFontSize, minFontSize)
        val emWide = DIGIT_ADVANCE * digits + HORIZONTAL_PADDING
        val emTall = LINE_HEIGHT + VERTICAL_PADDING
        if (count <= 0 || width <= 0.0 || height <= 0.0) {
            return CardGrid(1, max(count, 1), max(width, 0.0), minFontSize * emTall, minFontSize, scrolls = true)
        }

        var best: Candidate? = null
        for (columns in 1..count) {
            val candidate = Candidate.of(count, columns, width, height, spacing, emWide, emTall) ?: continue
            if (best == null || candidate.isBetterThan(best)) best = candidate
        }
        if (best != null && best.size >= minFontSize - TOLERANCE) {
            return CardGrid(best.columns, best.rows, best.cellWidth, best.cellHeight, min(best.size, cap), scrolls = false)
        }

        // Nothing fits at the player's text size: keep that size and scroll vertically (FR-004).
        val cellMinWidth = minFontSize * emWide
        val columns = floor((width + spacing) / (cellMinWidth + spacing)).toInt().coerceIn(1, count)
        val rows = ceilDiv(count, columns)
        val cellWidth = (width - (columns - 1) * spacing) / columns
        return CardGrid(columns, rows, cellWidth, minFontSize * emTall, minFontSize, scrolls = true)
    }

    private class Candidate(
        val columns: Int,
        val rows: Int,
        val cellWidth: Double,
        val cellHeight: Double,
        val size: Double,
        val emptyCells: Int,
    ) {
        /** Larger numbers first; then fewer empty cells; then the squarer grid. */
        fun isBetterThan(other: Candidate): Boolean =
            when {
                abs(size - other.size) > TOLERANCE -> size > other.size
                emptyCells != other.emptyCells -> emptyCells < other.emptyCells
                else -> abs(columns - rows) < abs(other.columns - other.rows)
            }

        companion object {
            fun of(
                count: Int,
                columns: Int,
                width: Double,
                height: Double,
                spacing: Double,
                emWide: Double,
                emTall: Double,
            ): Candidate? {
                val rows = ceilDiv(count, columns)
                val cellWidth = (width - (columns - 1) * spacing) / columns
                val cellHeight = (height - (rows - 1) * spacing) / rows
                if (cellWidth <= 0.0 || cellHeight <= 0.0) return null
                val size = min(cellWidth / emWide, cellHeight / emTall)
                return Candidate(columns, rows, cellWidth, cellHeight, size, columns * rows - count)
            }
        }
    }

    private fun ceilDiv(
        a: Int,
        b: Int,
    ): Int = ceil(a.toDouble() / b).toInt()
}
