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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    GameScreen(state = state, onAction = viewModel::onAction, modifier = modifier)
}

/** Stateless, previewable screen: renders [state] and forwards actions. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    state: GameState,
    onAction: (GameAction) -> Unit,
    modifier: Modifier = Modifier,
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
            is GameContentUi.Intro -> IntroContent(content, onAction, contentModifier)
            is GameContentUi.Card -> CardContent(content, onAction, contentModifier)
            is GameContentUi.Revealed -> ResultContent(content.message, content.newGameLabel, onAction, contentModifier)
            is GameContentUi.Invalid -> ResultContent(content.message, content.newGameLabel, onAction, contentModifier)
        }
    }
}

@Composable
private fun IntroContent(
    content: GameContentUi.Intro,
    onAction: (GameAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = content.message,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 480.dp).testTag("intro.message"),
        )
        Spacer(Modifier.padding(16.dp))
        Button(onClick = { onAction(GameAction.OnReadyClick) }, modifier = Modifier.testTag("intro.ready")) {
            Text(content.readyLabel)
        }
    }
}

@Composable
private fun CardContent(
    content: GameContentUi.Card,
    onAction: (GameAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(content.progress, style = MaterialTheme.typography.labelLarge, modifier = Modifier.testTag("card.progress"))
        Text(content.question, style = MaterialTheme.typography.titleMedium)
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 64.dp),
            modifier = Modifier.weight(1f).fillMaxWidth().testTag("card.numbers"),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(content.numbers, key = { it.value }) { number -> NumberCell(number) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { onAction(GameAction.OnAnswerClick(Answer.Yes, content.index)) },
                modifier = Modifier.weight(1f).testTag("card.yes"),
            ) {
                Text(content.yesLabel)
            }
            OutlinedButton(
                onClick = { onAction(GameAction.OnAnswerClick(Answer.No, content.index)) },
                modifier = Modifier.weight(1f).testTag("card.no"),
            ) {
                Text(content.noLabel)
            }
        }
    }
}

@Composable
private fun NumberCell(
    number: NumberUi,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .heightIn(min = 48.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .semantics { contentDescription = number.label }
                .testTag("number.${number.value}"),
    ) {
        Text(text = number.value.toString(), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun ResultContent(
    message: String,
    newGameLabel: String,
    onAction: (GameAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 480.dp).testTag("result.message"),
        )
        Spacer(Modifier.padding(16.dp))
        Button(onClick = { onAction(GameAction.OnNewGameClick) }, modifier = Modifier.testTag("result.newGame")) {
            Text(newGameLabel)
        }
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
        )
    }
}
