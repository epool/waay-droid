package dev.epool.waay.game.presentation

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isGreaterThan
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isLessThanOrEqualTo
import assertk.assertions.isTrue
import kotlin.math.ceil
import kotlin.math.min
import kotlin.test.Test

/**
 * Contract guarantees F1–F8 for [CardGridFit] (specs/002, ADR-013): every card's numbers fit the
 * card's area at the largest possible size, the text size wins when nothing fits (FR-004).
 */
class CardGridFitTest {
    private data class Area(
        val name: String,
        val width: Double,
        val height: Double,
    )

    // Card areas of spec 001's verification matrix (research ADR-013 model: chrome per layout removed).
    private val matrix =
        listOf(
            Area("phonePortraitSmall", 304.0, 408.0),
            Area("phonePortraitIphone", 337.0, 620.0),
            Area("phoneLandscapeSmall", 328.0, 208.0),
            Area("phoneLandscapeIphone", 540.0, 241.0),
            Area("tabletPortrait", 744.0, 1048.0),
            Area("tabletLandscape", 968.0, 648.0),
            Area("foldableUnfolded", 361.0, 689.0),
            Area("foldableFolded", 304.0, 548.0),
            Area("tabletopTopHalf", 785.0, 264.0),
            Area("bookLeftHalf", 280.0, 689.0),
            Area("splitHalfPhone", 304.0, 88.0),
            Area("splitHalfTablet", 584.0, 568.0),
        )

    // Numbers per card for 3–7 cards and the digits of each game's largest number.
    private val cards = listOf(4 to 1, 8 to 2, 16 to 2, 32 to 2, 64 to 3)
    private val minimumSizes = listOf(14.0, 15.0, 18.2, 21.0, 28.0)
    private val spacing = 6.0
    private val maxFont = 72.0
    private val epsilon = 1e-6

    private fun fit(
        count: Int,
        digits: Int,
        area: Area,
        minFont: Double,
    ) = CardGridFit.fit(count, digits, area.width, area.height, spacing, minFont, maxFont)

    /** The uncapped number size a grid's cells hold (the shared font model). */
    private fun sizeOf(
        cellWidth: Double,
        cellHeight: Double,
        digits: Int,
    ) = min(cellWidth / (0.6 * digits + 0.5), cellHeight / (1.2 + 0.4))

    private fun forEveryCase(block: (count: Int, digits: Int, area: Area, minFont: Double) -> Unit) {
        for ((count, digits) in cards) for (area in matrix) for (minFont in minimumSizes) block(count, digits, area, minFont)
    }

    // F1: when it fits, the whole grid lies inside the area and nothing scrolls.
    @Test
    fun aFittingGridLiesInsideTheArea() =
        forEveryCase { count, digits, area, minFont ->
            val grid = fit(count, digits, area, minFont)
            if (!grid.scrolls) {
                assertThat(grid.columns * grid.cellWidth + (grid.columns - 1) * spacing).isLessThanOrEqualTo(area.width + epsilon)
                assertThat(grid.rows * grid.cellHeight + (grid.rows - 1) * spacing).isLessThanOrEqualTo(area.height + epsilon)
            }
        }

    // F2: rows hold every number, columns are within 1..count.
    @Test
    fun rowsAndColumnsHoldEveryNumber() =
        forEveryCase { count, digits, area, minFont ->
            val grid = fit(count, digits, area, minFont)
            assertThat(grid.columns).isGreaterThanOrEqualTo(1)
            assertThat(grid.columns).isLessThanOrEqualTo(count)
            assertThat(grid.rows).isEqualTo(ceil(count.toDouble() / grid.columns).toInt())
        }

    // F3: no other column count gives larger numbers (brute force over 1..count).
    @Test
    fun theChosenGridMaximisesTheNumberSize() =
        forEveryCase { count, digits, area, minFont ->
            val grid = fit(count, digits, area, minFont)
            if (!grid.scrolls) {
                val chosen = sizeOf(grid.cellWidth, grid.cellHeight, digits)
                for (columns in 1..count) {
                    val rows = ceil(count.toDouble() / columns).toInt()
                    val cellWidth = (area.width - (columns - 1) * spacing) / columns
                    val cellHeight = (area.height - (rows - 1) * spacing) / rows
                    if (cellWidth > 0 && cellHeight > 0) {
                        assertThat(chosen).isGreaterThanOrEqualTo(sizeOf(cellWidth, cellHeight, digits) - epsilon)
                    }
                }
            }
        }

    // F4: the size respects the cap, and the minimum unless the card scrolls.
    @Test
    fun theSizeRespectsTheMinimumAndTheCap() =
        forEveryCase { count, digits, area, minFont ->
            val grid = fit(count, digits, area, minFont)
            assertThat(grid.fontSize).isLessThanOrEqualTo(maxFont)
            if (!grid.scrolls) assertThat(grid.fontSize).isGreaterThanOrEqualTo(minFont - epsilon)
        }

    // F5: when nothing fits at the minimum, the text size wins: scroll, as many columns as hold it.
    @Test
    fun whenNothingFitsTheTextSizeWinsAndTheCardScrolls() {
        val area = matrix.first { it.name == "splitHalfPhone" }
        val grid = fit(64, 3, area, 14.0)

        assertThat(grid.scrolls).isTrue()
        assertThat(grid.fontSize).isEqualTo(14.0)
        val widestCell = 14.0 * (0.6 * 3 + 0.5)
        assertThat(grid.cellWidth).isGreaterThanOrEqualTo(widestCell - epsilon)
        // One more column would no longer hold the minimum size.
        val tighter = (area.width - grid.columns * spacing) / (grid.columns + 1)
        assertThat(tighter < widestCell || grid.columns == 64).isTrue()
        assertThat(grid.rows * grid.cellHeight + (grid.rows - 1) * spacing).isGreaterThan(area.height)
    }

    // F6: the narrowed SC-001 at the platforms' default sizes (Android 14 sp, iOS 15 pt): only small
    // landscape phones (7 cards) and phone split-screen halves (6–7 cards) fall back to scrolling.
    @Test
    fun scrollingOnlyHappensInTheWindowsSc001Exempts() {
        val exempt = mapOf(64 to setOf("phoneLandscapeSmall", "splitHalfPhone"), 32 to setOf("splitHalfPhone"))
        for (minFont in listOf(14.0, 15.0)) {
            for ((count, digits) in cards) {
                for (area in matrix) {
                    val expectedToScroll = area.name in exempt[count].orEmpty()
                    assertThat(fit(count, digits, area, minFont).scrolls, "${area.name}, $count numbers, $minFont")
                        .isEqualTo(expectedToScroll)
                }
            }
        }
        // Larger standard text sizes still fit 64 numbers on phones in portrait, tablets and foldables.
        for (name in listOf("phonePortraitSmall", "phonePortraitIphone", "tabletPortrait", "foldableUnfolded", "foldableFolded")) {
            assertThat(fit(64, 3, matrix.first { it.name == name }, 18.2).scrolls, name).isFalse()
        }
    }

    // F7: degenerate input gives a defined result, never an exception.
    @Test
    fun degenerateInputGivesADefinedResult() {
        for (grid in listOf(
            CardGridFit.fit(0, 1, 300.0, 300.0, spacing, 14.0, maxFont),
            CardGridFit.fit(16, 2, 0.0, 300.0, spacing, 14.0, maxFont),
            CardGridFit.fit(16, 2, 300.0, -1.0, spacing, 14.0, maxFont),
        )) {
            assertThat(grid.columns).isEqualTo(1)
            assertThat(grid.scrolls).isTrue()
        }
    }

    // F8: pure and deterministic.
    @Test
    fun theSameInputGivesTheSameGrid() =
        forEveryCase { count, digits, area, minFont ->
            assertThat(fit(count, digits, area, minFont)).isEqualTo(fit(count, digits, area, minFont))
        }

    // FR-002 / spec edge case: few numbers on a big screen fill the area but stay readable (capped).
    @Test
    fun fewNumbersOnABigScreenAreCappedButFillTheArea() {
        val area = matrix.first { it.name == "tabletPortrait" }
        val grid = fit(4, 1, area, 14.0)

        assertThat(grid.scrolls).isFalse()
        assertThat(grid.fontSize).isEqualTo(maxFont)
        val usedWidth = grid.columns * grid.cellWidth + (grid.columns - 1) * spacing
        val usedHeight = grid.rows * grid.cellHeight + (grid.rows - 1) * spacing
        assertThat(usedWidth * usedHeight / (area.width * area.height)).isGreaterThanOrEqualTo(0.8)
    }
}
