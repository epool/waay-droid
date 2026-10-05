package dev.epool.waay.android.game

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.epool.waay.android.adaptive.AdaptiveGameLayout
import dev.epool.waay.android.adaptive.CardStageLayout
import dev.epool.waay.android.adaptive.GameLayout
import dev.epool.waay.android.adaptive.GameLayoutMode
import dev.epool.waay.android.adaptive.rememberGameLayout
import dev.epool.waay.android.ui.ObserveAsEvents
import dev.epool.waay.android.ui.theme.WaayTheme
import dev.epool.waay.game.domain.Answer
import dev.epool.waay.game.presentation.GameAction
import dev.epool.waay.game.presentation.GameContentUi
import dev.epool.waay.game.presentation.GameEvent
import dev.epool.waay.game.presentation.GameState
import dev.epool.waay.game.presentation.GameViewModel
import dev.epool.waay.game.presentation.NumberUi
import org.koin.androidx.compose.koinViewModel

/** Stateful entry: obtains the ViewModel, collects state and events, performs navigation. */
@Composable
fun GameRoot(
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GameViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            GameEvent.NavigateToSettings -> onNavigateToSettings()
        }
    }
    GameScreen(state = state, onAction = viewModel::onAction, canAnswer = viewModel::canAnswer, modifier = modifier)
}

/**
 * Stateless, previewable screen: renders [state] and forwards actions. [canAnswer] tells whether an
 * answer for a card would be recorded now (ADR-014); answers are only sent, and cards only animated
 * away, when it says yes (FR-011).
 *
 * Spec 002 layout (FR-019): a backdrop, a top bar with the progress centred, and each phase drawn as a
 * card in front of it. The card phase is a draggable [CardStage] with its answers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    state: GameState,
    onAction: (GameAction) -> Unit,
    canAnswer: (cardIndex: Int) -> Boolean,
    modifier: Modifier = Modifier,
    layout: GameLayout = rememberGameLayout(),
) {
    val content = state.content
    var exiting by remember { mutableStateOf<ExitingCard?>(null) }
    var origin by remember { mutableStateOf(Offset.Zero) }
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    if (content is GameContentUi.Card) {
                        Text(
                            text = content.progress,
                            // Announced on every new card for screen-reader users (FR-025).
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }.testTag("card.progress"),
                        )
                    } else {
                        Text(state.title)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { onAction(GameAction.OnNewGameClick) }, modifier = Modifier.testTag("toolbar.newGame")) {
                        Icon(Icons.Filled.Refresh, contentDescription = state.newGameLabel)
                    }
                },
                actions = {
                    IconButton(onClick = { onAction(GameAction.OnSettingsClick) }, modifier = Modifier.testTag("toolbar.settings")) {
                        Icon(Icons.Filled.Settings, contentDescription = state.settingsLabel)
                    }
                },
                colors =
                    TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
            )
        },
    ) { padding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .onGloballyPositioned { origin = it.positionInRoot() },
        ) {
            when (content) {
                is GameContentUi.Intro -> {
                    MessageCard(content.message, content.readyLabel, "intro", layout) { onAction(GameAction.OnReadyClick) }
                }

                is GameContentUi.Card -> {
                    CardPhase(content, layout, canAnswer) { exit ->
                        exiting = exit
                        onAction(GameAction.OnAnswerClick(exit.answer, exit.card.index))
                    }
                }

                is GameContentUi.Revealed -> {
                    MessageCard(content.message, content.newGameLabel, "result", layout) { onAction(GameAction.OnNewGameClick) }
                }

                is GameContentUi.Invalid -> {
                    MessageCard(content.message, content.newGameLabel, "result", layout) { onAction(GameAction.OnNewGameClick) }
                }
            }
            // Drawn over every phase, so the last card can finish flying off as the result appears.
            exiting?.let { ExitingCardOverlay(it, origin, onFinish = { exiting = null }) }
        }
    }
}

/**
 * The card phase: the draggable card and its "No"/"Yes" answers, placed for [layout]. A swipe and a
 * tap go through the same gate: only an answer [canAnswer] accepts throws the card (FR-011, FR-013).
 */
@Composable
private fun CardPhase(
    content: GameContentUi.Card,
    layout: GameLayout,
    canAnswer: (cardIndex: Int) -> Boolean,
    onAnswer: (ExitingCard) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    var cardBounds by remember { mutableStateOf(Rect.Zero) }
    val tryAnswer: (Answer, Float) -> Boolean = { answer, fromOffset ->
        canAnswer(content.index).also { accepted ->
            if (accepted) {
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                onAnswer(ExitingCard(content, answer, fromOffset, cardBounds))
            }
        }
    }
    CardStageLayout(
        layout = layout,
        card = { CardStage(content, tryAnswer, onCardBounds = { cardBounds = it }) },
        answer = { answer, style, answerModifier ->
            AnswerControl(
                answer = answer,
                label = if (answer == Answer.Yes) content.yesLabel else content.noLabel,
                style = style,
                onClick = { tryAnswer(answer, 0f) },
                modifier = answerModifier,
            )
        },
    )
}

/** The intro and result screens: their message and single action on a card over the backdrop. */
@Composable
private fun MessageCard(
    message: String,
    actionLabel: String,
    tagPrefix: String,
    layout: GameLayout,
    onClick: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxSize(),
    ) {
        AdaptiveGameLayout(
            layout = layout,
            keepTogether = true,
            modifier = Modifier.padding(16.dp),
            primary = {
                Text(
                    text = message,
                    style = if (tagPrefix == "intro") MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier
                            .widthIn(max = 480.dp)
                            .semantics {
                                heading()
                                if (tagPrefix == "result") liveRegion = LiveRegionMode.Polite
                            }.testTag("$tagPrefix.message"),
                )
            },
            secondary = {
                Button(onClick = onClick, modifier = Modifier.testTag(if (tagPrefix == "intro") "intro.ready" else "result.newGame")) {
                    Text(actionLabel)
                }
            },
        )
    }
}

private fun previewState(content: GameContentUi) =
    GameState(title = "Wáay", settingsLabel = "Settings", newGameLabel = "New game", content = content)

@Preview(showBackground = true)
@Composable
private fun IntroPreview() {
    WaayTheme {
        GameScreen(
            state = previewState(GameContentUi.Intro("Think of a number from 1 to 31 and let me guess it…", "I'm ready")),
            onAction = {},
            canAnswer = { true },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CardPreview() {
    WaayTheme {
        GameScreen(
            state =
                previewState(
                    GameContentUi.Card(
                        index = 1,
                        progress = "Card 2 of 5",
                        question = "Is your number on this card?",
                        numbers = listOf(19, 3, 27, 6, 15, 22, 7, 31, 2, 11, 30, 18, 14, 23, 10, 26).map { NumberUi(it, "$it") },
                        yesLabel = "Yes",
                        noLabel = "No",
                    ),
                ),
            onAction = {},
            canAnswer = { true },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RevealedPreview() {
    WaayTheme {
        GameScreen(
            state = previewState(GameContentUi.Revealed("The number you thought of is… 27!", 27, "New game")),
            onAction = {},
            canAnswer = { true },
        )
    }
}
