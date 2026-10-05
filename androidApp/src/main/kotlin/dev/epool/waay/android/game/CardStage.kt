package dev.epool.waay.android.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.epool.waay.android.ui.rememberReduceMotion
import dev.epool.waay.game.domain.Answer
import dev.epool.waay.game.presentation.GameContentUi
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

/** Card physics (spec 002 ADR-015, FR-026): springs throughout, a tilt cap and the answer thresholds. */
internal object CardMotion {
    /** Back to rest after a short drag, with a little bounce. */
    val settle: SpringSpec<Float> = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)

    /** Thrown off-screen once the answer is recorded. */
    val exit: SpringSpec<Float> = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)

    /** The next card rising from the stack. */
    val enter: SpringSpec<Float> = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)

    /** Reduce motion (FR-014, U8): cards cross-fade, and a released card slides straight back. */
    val fade: TweenSpec<Float> = tween(durationMillis = 150)

    const val MAX_TILT_DEGREES = 12f
    const val THRESHOLD_FRACTION = 1f / 3f
    val FlickVelocity = 1_200.dp // per second

    fun tilt(
        offset: Float,
        width: Float,
    ): Float = if (width <= 0f) 0f else (offset / width * MAX_TILT_DEGREES).coerceIn(-MAX_TILT_DEGREES, MAX_TILT_DEGREES)
}

/** A card that has just been answered, flying off from where it was released (FR-010). */
@Immutable
data class ExitingCard(
    val card: GameContentUi.Card,
    val answer: Answer,
    val fromOffset: Float,
    val bounds: Rect,
)

/**
 * The current card, draggable left and right (spec 002 FR-006 to FR-011): it follows the finger,
 * tilts, and shows the answer it would give. On release past a third of its width, or on a flick,
 * it asks [tryAnswer]; if the answer counts, the card is handed to the exit animation, otherwise it
 * springs back. Interrupted drags answer nothing (FR-016). Two blank backs behind it hint at the
 * stack and never show numbers (FR-015). With reduce motion on (FR-014) it still follows the finger,
 * but doesn't tilt, rise or spring: cards fade in.
 */
@Composable
fun CardStage(
    card: GameContentUi.Card,
    tryAnswer: (answer: Answer, fromOffset: Float) -> Boolean,
    onCardBounds: (Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val flickVelocity = with(LocalDensity.current) { CardMotion.FlickVelocity.toPx() }
    val currentTryAnswer by rememberUpdatedState(tryAnswer)
    val reduceMotion = rememberReduceMotion()
    val currentReduceMotion by rememberUpdatedState(reduceMotion)
    val offset = remember(card.index) { Animatable(0f) }
    val entrance = remember(card.index) { Animatable(0f) }
    var width by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(card.index) { entrance.animateTo(1f, if (currentReduceMotion) CardMotion.fade else CardMotion.enter) }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .pointerInput(card.index) {
                    val tracker = VelocityTracker()
                    var travelled = 0f
                    var pastThreshold = false
                    detectHorizontalDragGestures(
                        onDragStart = {
                            tracker.resetTracking()
                            travelled = offset.value
                            pastThreshold = false
                        },
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            travelled += amount
                            tracker.addPosition(change.uptimeMillis, Offset(travelled, 0f))
                            scope.launch { offset.snapTo(travelled) }
                            val beyond = abs(travelled) >= size.width * CardMotion.THRESHOLD_FRACTION
                            if (beyond && !pastThreshold) haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                            pastThreshold = beyond
                        },
                        onDragEnd = {
                            val velocity = tracker.calculateVelocity().x
                            val threshold = size.width * CardMotion.THRESHOLD_FRACTION
                            val answer =
                                when {
                                    travelled >= threshold || velocity >= flickVelocity -> Answer.Yes
                                    travelled <= -threshold || velocity <= -flickVelocity -> Answer.No
                                    else -> null
                                }
                            if (answer == null || !currentTryAnswer(answer, travelled)) {
                                scope.launch {
                                    if (currentReduceMotion) {
                                        offset.animateTo(0f, CardMotion.fade)
                                    } else {
                                        offset.animateTo(0f, CardMotion.settle, initialVelocity = velocity)
                                    }
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch { offset.animateTo(0f, if (currentReduceMotion) CardMotion.fade else CardMotion.settle) }
                        },
                    )
                },
    ) {
        StackHint()
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(bottom = StackDepth * 2)
                    .onGloballyPositioned {
                        width = it.size.width.toFloat()
                        onCardBounds(it.boundsInRoot())
                    },
        ) {
            CardFace(
                card = card,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = offset.value
                            rotationZ = if (reduceMotion) 0f else CardMotion.tilt(offset.value, width)
                            val rise = if (reduceMotion) 1f else 0.94f + 0.06f * entrance.value
                            scaleX = rise
                            scaleY = rise
                            alpha = entrance.value
                        }.testTag("card.surface"),
            )
            SwipeHints(card, offset.value, width)
        }
    }
}

/** The "Yes"/"No" the card would give, fading in towards the threshold; text, never colour alone (FR-009). */
@Composable
private fun SwipeHints(
    card: GameContentUi.Card,
    offset: Float,
    width: Float,
) {
    if (width <= 0f || offset == 0f) return
    val strength = (abs(offset) / (width * CardMotion.THRESHOLD_FRACTION)).coerceIn(0f, 1f)
    val isYes = offset.sign > 0
    Box(
        contentAlignment = if (isYes) Alignment.TopStart else Alignment.TopEnd,
        modifier = Modifier.fillMaxSize().padding(24.dp),
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = if (isYes) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.inverseSurface,
            contentColor = if (isYes) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.inverseOnSurface,
            modifier =
                Modifier
                    .graphicsLayer {
                        alpha = strength
                        translationX = offset
                    }.testTag(if (isYes) "card.hint.yes" else "card.hint.no"),
        ) {
            Text(
                text = if (isYes) card.yesLabel else card.noLabel,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

/** Two blank card backs peeking out under the current card: a stack, with no numbers (FR-015). */
@Composable
private fun StackHint() {
    Box(Modifier.fillMaxSize()) {
        // The farther back first, so the nearer one is drawn over it.
        for (depth in 2 downTo 1) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp * depth, vertical = 0.dp)
                        .padding(top = StackDepth * depth, bottom = StackDepth * (2 - depth))
                        .clearAndSetSemantics {},
            ) {}
        }
    }
}

/** The card itself: the question as its header, every number as its body (FR-019, FR-001). */
@Composable
internal fun CardFace(
    card: GameContentUi.Card,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 6.dp,
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(16.dp)) {
            Text(card.question, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            // A fresh grid per card: in the scroll fallback (FR-004) each card starts at the top (FR-003a).
            key(card.index) {
                CardGridView(numbers = card.numbers, modifier = Modifier.fillMaxWidth().weight(1f))
            }
        }
    }
}

/**
 * The answered card flying off in its answer's direction, from where it was released, drawn over the
 * screen so it can finish even when the next phase has already replaced the card (FR-010). With reduce
 * motion on (FR-014) it fades out where it was released instead. Hidden from accessibility: it is
 * only motion.
 */
@Composable
fun ExitingCardOverlay(
    exiting: ExitingCard,
    origin: Offset,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val reduceMotion = rememberReduceMotion()
    val currentReduceMotion by rememberUpdatedState(reduceMotion)
    val position = remember(exiting) { Animatable(exiting.fromOffset) }
    val opacity = remember(exiting) { Animatable(1f) }
    val currentOnFinished by rememberUpdatedState(onFinish)
    LaunchedEffect(exiting) {
        if (currentReduceMotion) {
            opacity.animateTo(0f, CardMotion.fade)
        } else {
            val direction = if (exiting.answer == Answer.Yes) 1f else -1f
            position.animateTo(direction * exiting.bounds.width * 1.6f, CardMotion.exit)
        }
        currentOnFinished()
    }
    val size =
        with(density) {
            androidx.compose.ui.unit
                .DpSize(exiting.bounds.width.toDp(), exiting.bounds.height.toDp())
        }
    CardFace(
        card = exiting.card,
        modifier =
            modifier
                .offset { IntOffset((exiting.bounds.left - origin.x).roundToInt(), (exiting.bounds.top - origin.y).roundToInt()) }
                .size(size)
                .graphicsLayer {
                    translationX = position.value
                    rotationZ = if (reduceMotion) 0f else CardMotion.tilt(position.value, exiting.bounds.width)
                    alpha = opacity.value
                }.clearAndSetSemantics {},
    )
}

/** How far each blank back peeks out under the card. */
private val StackDepth = 8.dp
