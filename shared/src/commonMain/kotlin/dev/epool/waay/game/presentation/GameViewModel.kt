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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Thin MVI adapter over the pure [GameEngine] (ADR-001, contracts/game-viewmodel.md).
 * The constructor is inert: the game, speech and preference observation start on the first
 * subscription to [state], once (re-subscribing never repeats them).
 */
public class GameViewModel internal constructor(
    private val preferencesDataSource: PreferencesDataSource,
    private val stringsProvider: StringsProvider,
    private val deckFactory: DeckFactory,
    private val speaker: Speaker,
) : ViewModel() {
    private val snapshot = MutableStateFlow<GameSnapshot?>(null)
    private var preferences = Preferences()
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

    override fun onCleared() {
        speaker.stop()
    }

    private suspend fun startIfNeeded() {
        if (hasStarted) return
        hasStarted = true
        preferences = preferencesDataSource.preferences.first()
        snapshot.value = GameEngine.start(deckFactory.create(preferences.cardCount))
        speakCurrentLine()
        viewModelScope.launch { observePreferences() }
    }

    private suspend fun observePreferences() {
        preferencesDataSource.preferences.drop(1).collect { updated ->
            val voiceTurnedOff = preferences.voiceEnabled && !updated.voiceEnabled
            preferences = updated
            if (voiceTurnedOff) speaker.stop()
        }
    }

    private fun reduce(command: GameCommand) {
        val current = snapshot.value ?: return
        val next = GameEngine.reduce(current, command, deckFactory::create)
        if (next === current) return
        snapshot.value = next
        speakCurrentLine()
    }

    /** Speaks exactly what the screen shows (FR-012, FR-013), only when the voice is on (FR-014). */
    private fun speakCurrentLine() {
        if (!preferences.voiceEnabled) return
        val current = snapshot.value ?: return
        val choice = preferences.languageChoice
        val line = current.toGameState(stringsProvider.stringsFor(choice)).content.spokenLine()
        speaker.speak(line, stringsProvider.speechLanguageFor(choice))
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
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
