package dev.epool.waay.android.adaptive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import dev.epool.waay.game.domain.Answer

/** How the game screen is arranged for the current window and fold posture (ADR-008, FR-031, FR-032). */
enum class GameLayoutMode {
    /** Compact portrait: content above, controls below. */
    Stacked,

    /** Medium width and up (phone landscape, tablet, unfolded foldable, wide split screen). */
    SideBySide,

    /** Half-opened with a horizontal fold: content above the hinge, controls below it. */
    Tabletop,

    /** Half-opened with a vertical fold: content left of the hinge, controls right of it. */
    Book,
}

data class GameLayout(
    val mode: GameLayoutMode,
    /** Hinge bounds in window coordinates (px) for [GameLayoutMode.Tabletop]/[GameLayoutMode.Book]. */
    val hingeBounds: Rect? = null,
)

@Composable
fun rememberGameLayout(): GameLayout {
    val info = currentWindowAdaptiveInfoV2()
    val hinge = info.windowPosture.hingeList.firstOrNull { it.isSeparating || !it.isFlat }
    val sizeClass = info.windowSizeClass
    return when {
        hinge != null && !hinge.isVertical -> {
            GameLayout(GameLayoutMode.Tabletop, hinge.bounds)
        }

        hinge != null -> {
            GameLayout(GameLayoutMode.Book, hinge.bounds)
        }

        // Medium width and up: phones in landscape, tablets, unfolded foldables, wide split screens.
        // Narrow-but-short windows stay stacked; their grid scrolls.
        sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) -> {
            GameLayout(GameLayoutMode.SideBySide)
        }

        else -> {
            GameLayout(GameLayoutMode.Stacked)
        }
    }
}

/**
 * Places [primary] (what the player reads) and [secondary] (what the player taps) for [layout].
 * In folded postures the split follows the hinge, so nothing is placed on or across the fold.
 * With [keepTogether] (intro, result), unfolded layouts center both pieces as one group.
 */
@Composable
fun AdaptiveGameLayout(
    layout: GameLayout,
    primary: @Composable () -> Unit,
    secondary: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    keepTogether: Boolean = false,
) {
    var origin by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    // Movable, so the slots keep their state (e.g. grid scroll) when the posture or size changes.
    val primaryContent = remember(primary) { movableContentOf(primary) }
    val secondaryContent = remember(secondary) { movableContentOf(secondary) }
    val root = modifier.fillMaxSize().onGloballyPositioned { origin = it.positionInWindow() }
    val hinge = layout.hingeBounds

    when {
        layout.mode == GameLayoutMode.Tabletop && hinge != null -> {
            Column(modifier = root) {
                val above = with(density) { (hinge.top - origin.y).coerceAtLeast(0f).toDp() }
                Box(Modifier.fillMaxWidth().height(above).padding(bottom = HingeGutter), contentAlignment = Alignment.Center) {
                    primaryContent()
                }
                Spacer(Modifier.height(with(density) { hinge.height.toDp() }))
                Box(Modifier.fillMaxWidth().weight(1f).padding(top = HingeGutter), contentAlignment = Alignment.Center) {
                    secondaryContent()
                }
            }
        }

        layout.mode == GameLayoutMode.Book && hinge != null -> {
            Row(modifier = root) {
                val before = with(density) { (hinge.left - origin.x).coerceAtLeast(0f).toDp() }
                Box(Modifier.fillMaxHeight().width(before).padding(end = HingeGutter), contentAlignment = Alignment.Center) {
                    primaryContent()
                }
                Spacer(Modifier.width(with(density) { hinge.width.toDp() }))
                Box(Modifier.fillMaxHeight().weight(1f).padding(start = HingeGutter), contentAlignment = Alignment.Center) {
                    secondaryContent()
                }
            }
        }

        keepTogether -> {
            Column(
                modifier = root,
                verticalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                primaryContent()
                secondaryContent()
            }
        }

        layout.mode == GameLayoutMode.SideBySide -> {
            BoxWithConstraints(modifier = root) {
                val controlsWidth = (maxWidth * 0.4f).coerceIn(200.dp, 360.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Box(Modifier.weight(1f).fillMaxHeight()) { primaryContent() }
                    Box(Modifier.width(controlsWidth).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        secondaryContent()
                    }
                }
            }
        }

        else -> {
            Column(modifier = root, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f).fillMaxWidth()) { primaryContent() }
                secondaryContent()
            }
        }
    }
}

/** Clear space on each side of a fold, so nothing sits on or against it (FR-032). */
private val HingeGutter = 16.dp

/** How an answer control is drawn: a button under the card, or a tall panel beside it. */
enum class AnswerStyle {
    Button,
    Panel,
}

/**
 * Places the card stage and its two answers for [layout] (spec 002 FR-012a, FR-020, FR-021):
 * "No" is always left of "Yes". Compact: buttons under the card. Wide: panels flanking the card.
 * Folded: the card on one side of the hinge, the answers on the other, nothing on the fold.
 *
 * [answer] is a factory, not content to preserve: each layout asks it for a different control (a
 * button or a panel), and those hold no state, so reusing it across branches is intended.
 */
@Suppress("ktlint:compose:content-slot-reused")
@Composable
fun CardStageLayout(
    layout: GameLayout,
    card: @Composable () -> Unit,
    answer: @Composable (answer: Answer, style: AnswerStyle, modifier: Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    var origin by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    // Movable, so the card keeps its state (drag, scroll) when the posture or size changes.
    val cardContent = remember(card) { movableContentOf(card) }
    val root = modifier.fillMaxSize().onGloballyPositioned { origin = it.positionInWindow() }
    val hinge = layout.hingeBounds
    val answerRow: @Composable (Modifier) -> Unit = { rowModifier ->
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = rowModifier) {
            answer(Answer.No, AnswerStyle.Button, Modifier.weight(1f))
            answer(Answer.Yes, AnswerStyle.Button, Modifier.weight(1f))
        }
    }

    when {
        layout.mode == GameLayoutMode.Tabletop && hinge != null -> {
            Column(modifier = root) {
                val above = with(density) { (hinge.top - origin.y).coerceAtLeast(0f).toDp() }
                Box(Modifier.fillMaxWidth().height(above).padding(bottom = HingeGutter)) { cardContent() }
                Spacer(Modifier.height(with(density) { hinge.height.toDp() }))
                Box(Modifier.fillMaxWidth().weight(1f).padding(top = HingeGutter), contentAlignment = Alignment.Center) {
                    answerRow(Modifier.fillMaxWidth())
                }
            }
        }

        layout.mode == GameLayoutMode.Book && hinge != null -> {
            Row(modifier = root) {
                val before = with(density) { (hinge.left - origin.x).coerceAtLeast(0f).toDp() }
                Box(Modifier.fillMaxHeight().width(before).padding(end = HingeGutter)) { cardContent() }
                Spacer(Modifier.width(with(density) { hinge.width.toDp() }))
                Box(Modifier.fillMaxHeight().weight(1f).padding(start = HingeGutter), contentAlignment = Alignment.Center) {
                    answerRow(Modifier.fillMaxWidth())
                }
            }
        }

        layout.mode == GameLayoutMode.SideBySide -> {
            BoxWithConstraints(modifier = root) {
                val panelWidth = (maxWidth * 0.14f).coerceIn(88.dp, 160.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    answer(Answer.No, AnswerStyle.Panel, Modifier.width(panelWidth).fillMaxHeight())
                    Box(Modifier.weight(1f).fillMaxHeight()) { cardContent() }
                    answer(Answer.Yes, AnswerStyle.Panel, Modifier.width(panelWidth).fillMaxHeight())
                }
            }
        }

        else -> {
            Column(modifier = root, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.weight(1f).fillMaxWidth()) { cardContent() }
                answerRow(Modifier.fillMaxWidth())
            }
        }
    }
}
