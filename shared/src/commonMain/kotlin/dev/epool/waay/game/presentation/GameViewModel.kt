package dev.epool.waay.game.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.epool.waay.core.i18n.StringsProvider
import dev.epool.waay.core.speech.Speaker
import dev.epool.waay.game.domain.CardCount
import dev.epool.waay.game.domain.DeckFactory
import dev.epool.waay.game.domain.GameCommand
import dev.epool.waay.game.domain.GameEngine
import dev.epool.waay.game.domain.GameSnapshot
import dev.epool.waay.settings.domain.LanguageChoice
import dev.epool.waay.settings.domain.Preferences
import dev.epool.waay.settings.domain.PreferencesDataSource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Thin MVI adapter over the pure [GameEngine] (ADR-001, contracts/game-viewmodel.md).
 *
 * The game screen counts as "visible" while it collects [state] (its `subscriptionCount`). The
 * game starts on the first subscription. Lines spoken while the screen is away, e.g. the new intro
 * after a card-count change made in Settings, stay pending until it returns (finding U1).
 */
public class GameViewModel internal constructor(
    private val preferencesDataSource: PreferencesDataSource,
    private val stringsProvider: StringsProvider,
    private val deckFactory: DeckFactory,
    private val speaker: Speaker,
) : ViewModel() {
    private var snapshot: GameSnapshot? = null
    private var preferences = Preferences()
    private var hasStarted = false
    private var isScreenVisible = false
    private var hasPendingSpeech = false

    private val _state =
        MutableStateFlow(
            GameEngine.start(deckFactory.create(CardCount.DEFAULT)).toGameState(stringsProvider.stringsFor(LanguageChoice.Device)),
        )
    public val state: StateFlow<GameState> = _state.asStateFlow()

    private val eventChannel = Channel<GameEvent>(Channel.BUFFERED)
    public val events: Flow<GameEvent> = eventChannel.receiveAsFlow()

    init {
        // Inert until someone watches: no game, speech or preference work happens before that.
        viewModelScope.launch {
            _state.subscriptionCount
                .map { it > 0 }
                .distinctUntilChanged()
                .collect { visible ->
                    isScreenVisible = visible
                    if (visible) onScreenVisible()
                }
        }
    }

    public fun onAction(action: GameAction) {
        when (action) {
            GameAction.OnReadyClick -> reduce(GameCommand.Ready)
            is GameAction.OnAnswerClick -> reduce(GameCommand.AnswerCard(action.answer))
            GameAction.OnNewGameClick -> reduce(GameCommand.NewGame)
            GameAction.OnSettingsClick -> eventChannel.trySend(GameEvent.NavigateToSettings)
        }
    }

    override fun onCleared() {
        speaker.stop()
    }

    private suspend fun onScreenVisible() {
        if (!hasStarted) {
            start()
        } else if (hasPendingSpeech) {
            hasPendingSpeech = false
            speakCurrentLine()
        }
    }

    private suspend fun start() {
        hasStarted = true
        preferences = preferencesDataSource.preferences.first()
        snapshot = GameEngine.start(deckFactory.create(preferences.cardCount))
        publish()
        speakCurrentLine()
        viewModelScope.launch { observePreferences() }
    }

    private suspend fun observePreferences() {
        preferencesDataSource.preferences.drop(1).collect { updated ->
            val previous = preferences
            preferences = updated
            if (previous.voiceEnabled && !updated.voiceEnabled) speaker.stop()
            if (updated.cardCount != previous.cardCount) {
                reduce(GameCommand.CardCountChanged(updated.cardCount))
            } else {
                publish()
            }
        }
    }

    private fun reduce(command: GameCommand) {
        val current = snapshot ?: return
        val next = GameEngine.reduce(current, command, deckFactory::create)
        if (next === current) return
        snapshot = next
        publish()
        speakCurrentLine()
    }

    private fun publish() {
        val current = snapshot ?: return
        _state.value = current.toGameState(stringsProvider.stringsFor(preferences.languageChoice))
    }

    /** Speaks exactly what the screen shows (FR-012, FR-013), only when the voice is on (FR-014). */
    private fun speakCurrentLine() {
        if (!preferences.voiceEnabled) return
        if (!isScreenVisible) {
            hasPendingSpeech = true
            return
        }
        speaker.speak(_state.value.content.spokenLine(), stringsProvider.speechLanguageFor(preferences.languageChoice))
    }
}

/** The line the magician says for each phase: always text that is also on screen (FR-013). */
internal fun GameContentUi.spokenLine(): String =
    when (this) {
        is GameContentUi.Intro -> message
        is GameContentUi.Card -> "$progress. $question"
        is GameContentUi.Revealed -> message
        is GameContentUi.Invalid -> message
    }
