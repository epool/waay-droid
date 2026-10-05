package dev.epool.waay.android.game

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.epool.waay.game.presentation.CardGrid
import dev.epool.waay.game.presentation.CardGridFit
import dev.epool.waay.game.presentation.NumberUi

/** Gap between number cells; the shared fit accounts for it (ADR-013). */
private val CellSpacing = 6.dp

/** Readability cap for very few numbers on a large screen (spec 002 edge case). */
private val MaxNumberSize = 72.dp

/**
 * Draws a card's numbers in the grid [CardGridFit] picks for the space this view gets: every number
 * visible at once and as large as it fits (spec 002 FR-001 to FR-003). Numbers are never smaller than
 * the player's text size; only when that can't fit does the grid scroll (FR-004).
 */
@Composable
fun CardGridView(
    numbers: List<NumberUi>,
    modifier: Modifier = Modifier,
) {
    val minNumberSize =
        with(LocalDensity.current) {
            MaterialTheme.typography.bodyMedium.fontSize
                .toDp()
        }
    BoxWithConstraints(modifier = modifier.testTag("card.numbers")) {
        val maxDigits = numbers.maxOfOrNull { it.value.toString().length } ?: 1
        val grid =
            remember(numbers.size, maxDigits, maxWidth, maxHeight, minNumberSize) {
                CardGridFit.fit(
                    count = numbers.size,
                    maxDigits = maxDigits,
                    width = maxWidth.value.toDouble(),
                    height = maxHeight.value.toDouble(),
                    spacing = CellSpacing.value.toDouble(),
                    minFontSize = minNumberSize.value.toDouble(),
                    maxFontSize = MaxNumberSize.value.toDouble(),
                )
            }
        val rows = @Composable { NumberRows(numbers, grid) }
        if (grid.scrolls) {
            Box(Modifier.verticalScroll(rememberScrollState())) { rows() }
        } else {
            rows()
        }
    }
}

@Composable
private fun NumberRows(
    numbers: List<NumberUi>,
    grid: CardGrid,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CellSpacing)) {
        numbers.chunked(grid.columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(CellSpacing)) {
                row.forEach { number ->
                    NumberCell(number, width = grid.cellWidth.dp, height = grid.cellHeight.dp, size = grid.fontSize.dp)
                }
            }
        }
    }
}

@Composable
private fun NumberCell(
    number: NumberUi,
    width: Dp,
    height: Dp,
    size: Dp,
) {
    val fontSize = with(LocalDensity.current) { size.toSp() }
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(width, height)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .semantics { contentDescription = number.label }
                .testTag("number.${number.value}"),
    ) {
        Text(
            text = number.value.toString(),
            // Tabular digits, so every number on the card has the same width (FR-003).
            style =
                MaterialTheme.typography.titleMedium.copy(
                    fontSize = fontSize,
                    lineHeight = fontSize * 1.2,
                    fontFeatureSettings = "tnum",
                ),
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
        )
    }
}
