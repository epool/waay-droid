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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(state.title) },
                actions = {
                    IconButton(
                        onClick = { onAction(GameAction.OnNewGameClick) },
                        modifier = Modifier.testTag("toolbar.newGame"),
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = state.newGameLabel)
                    }
                    IconButton(
                        onClick = { onAction(GameAction.OnSettingsClick) },
                        modifier = Modifier.testTag("toolbar.settings"),
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = state.settingsLabel)
                    }
                },
            )
        },
    ) { padding ->
        val contentModifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        when (val content = state.content) {
            is GameContentUi.Intro -> IntroContent(content, layout, onAction, contentModifier)
            is GameContentUi.Card -> CardContent(content, layout, onAction, canAnswer, contentModifier)
            is GameContentUi.Revealed -> ResultContent(content.message, content.newGameLabel, layout, onAction, contentModifier)
            is GameContentUi.Invalid -> ResultContent(content.message, content.newGameLabel, layout, onAction, contentModifier)
        }
    }
}

@Composable
private fun IntroContent(
    content: GameContentUi.Intro,
    layout: GameLayout,
    onAction: (GameAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    AdaptiveGameLayout(
        layout = layout,
        keepTogether = true,
        modifier = modifier,
        primary = {
            Text(
                text = content.message,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 480.dp).semantics { heading() }.testTag("intro.message"),
            )
        },
        secondary = {
            Button(onClick = { onAction(GameAction.OnReadyClick) }, modifier = Modifier.testTag("intro.ready")) {
                Text(content.readyLabel)
            }
        },
    )
}

@Composable
private fun CardContent(
    content: GameContentUi.Card,
    layout: GameLayout,
    onAction: (GameAction) -> Unit,
    canAnswer: (cardIndex: Int) -> Boolean,
    modifier: Modifier = Modifier,
) {
    AdaptiveGameLayout(
        layout = layout,
        modifier = modifier,
        primary = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = content.progress,
                    style = MaterialTheme.typography.labelLarge,
                    // Announced on every new card for screen-reader users (FR-025).
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }.testTag("card.progress"),
                )
                Text(content.question, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                // A fresh grid per card: in the scroll fallback (FR-004) each card starts at the top, so no
                // number stays hidden above the visible area by the previous card's scrolling (FR-003a).
                key(content.index) {
                    CardGridView(numbers = content.numbers, modifier = Modifier.fillMaxWidth().weight(1f))
                }
            }
        },
        secondary = {
            AnswerButtons(
                content,
                isVertical = layout.mode == GameLayoutMode.SideBySide,
                onAnswer = { answer -> if (canAnswer(content.index)) onAction(GameAction.OnAnswerClick(answer, content.index)) },
            )
        },
    )
}

@Composable
private fun AnswerButtons(
    content: GameContentUi.Card,
    isVertical: Boolean,
    onAnswer: (Answer) -> Unit,
    modifier: Modifier = Modifier,
) {
    val yes: @Composable (Modifier) -> Unit = { buttonModifier ->
        Button(
            onClick = { onAnswer(Answer.Yes) },
            modifier = buttonModifier.testTag("card.yes"),
        ) {
            Text(content.yesLabel)
        }
    }
    val no: @Composable (Modifier) -> Unit = { buttonModifier ->
        OutlinedButton(
            onClick = { onAnswer(Answer.No) },
            modifier = buttonModifier.testTag("card.no"),
        ) {
            Text(content.noLabel)
        }
    }
    if (isVertical) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = modifier.fillMaxWidth()) {
            yes(Modifier.fillMaxWidth())
            no(Modifier.fillMaxWidth())
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = modifier.fillMaxWidth()) {
            yes(Modifier.weight(1f))
            no(Modifier.weight(1f))
        }
    }
}

@Composable
private fun ResultContent(
    message: String,
    newGameLabel: String,
    layout: GameLayout,
    onAction: (GameAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    AdaptiveGameLayout(
        layout = layout,
        keepTogether = true,
        modifier = modifier,
        primary = {
            Text(
                text = message,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .widthIn(max = 480.dp)
                        .semantics {
                            heading()
                            liveRegion = LiveRegionMode.Polite
                        }.testTag("result.message"),
            )
        },
        secondary = {
            Button(onClick = { onAction(GameAction.OnNewGameClick) }, modifier = Modifier.testTag("result.newGame")) {
                Text(newGameLabel)
            }
        },
    )
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
