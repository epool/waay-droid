package dev.epool.waay.game.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.epool.waay.core.i18n.StringsProvider
import dev.epool.waay.game.domain.CardCount
import dev.epool.waay.game.domain.DeckFactory
import dev.epool.waay.game.domain.GameCommand
import dev.epool.waay.game.domain.GameEngine
import dev.epool.waay.game.domain.GameSnapshot
import dev.epool.waay.settings.domain.LanguageChoice
import dev.epool.waay.settings.domain.PreferencesDataSource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * Thin MVI adapter over the pure [GameEngine] (ADR-001, contracts/game-viewmodel.md).
 * The constructor is inert: the game starts on the first subscription to [state].
 */
public class GameViewModel internal constructor(
    private val preferencesDataSource: PreferencesDataSource,
    private val stringsProvider: StringsProvider,
    private val deckFactory: DeckFactory,
) : ViewModel() {
    private val snapshot = MutableStateFlow<GameSnapshot?>(null)
    private var hasStarted = false

    public val state: StateFlow<GameState> =
        combine(snapshot.filterNotNull(), preferencesDataSource.preferences) { snapshot, preferences ->
            snapshot.toGameState(stringsProvider.stringsFor(preferences.languageChoice))
        }.onStart { startIfNeeded() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue =
                    GameEngine
                        .start(deckFactory.create(CardCount.DEFAULT))
                        .toGameState(stringsProvider.stringsFor(LanguageChoice.Device)),
            )

    private val eventChannel = Channel<GameEvent>(Channel.BUFFERED)
    public val events: Flow<GameEvent> = eventChannel.receiveAsFlow()

    public fun onAction(action: GameAction) {
        when (action) {
            GameAction.OnReadyClick -> reduce(GameCommand.Ready)
            is GameAction.OnAnswerClick -> reduce(GameCommand.AnswerCard(action.answer))
            GameAction.OnNewGameClick -> reduce(GameCommand.NewGame)
            GameAction.OnSettingsClick -> eventChannel.trySend(GameEvent.NavigateToSettings)
        }
    }

    private suspend fun startIfNeeded() {
        if (hasStarted) return
        hasStarted = true
        val cardCount = preferencesDataSource.preferences.first().cardCount
        snapshot.value = GameEngine.start(deckFactory.create(cardCount))
    }

    private fun reduce(command: GameCommand) {
        snapshot.update { current -> current?.let { GameEngine.reduce(it, command, deckFactory::create) } }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
