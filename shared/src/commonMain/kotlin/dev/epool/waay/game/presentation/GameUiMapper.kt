package dev.epool.waay.game.presentation

import dev.epool.waay.core.i18n.Strings
import dev.epool.waay.game.domain.GamePhase
import dev.epool.waay.game.domain.GameSnapshot

/** Pure mapping from the domain snapshot to UI state. Never maps `bitValue` (FR-011). */
internal fun GameSnapshot.toGameState(strings: Strings): GameState =
    GameState(
        title = strings.appTitle,
        settingsLabel = strings.settingsLabel,
        newGameLabel = strings.newGameLabel,
        content = toContent(strings),
    )

private fun GameSnapshot.toContent(strings: Strings): GameContentUi {
    val max = deck.cardCount.maxNumber
    return when (val phase = phase) {
        GamePhase.Intro -> {
            GameContentUi.Intro(message = strings.intro(max), readyLabel = strings.readyLabel)
        }

        is GamePhase.Asking -> {
            GameContentUi.Card(
                index = phase.index,
                progress = strings.progress(current = phase.index + 1, total = deck.cards.size),
                question = strings.cardQuestion,
                numbers = deck.cards[phase.index].numbers.map { NumberUi(value = it, label = strings.numberLabel(it)) },
                yesLabel = strings.yesLabel,
                noLabel = strings.noLabel,
            )
        }

        is GamePhase.Revealed -> {
            GameContentUi.Revealed(
                message = strings.reveal(phase.number),
                number = phase.number,
                newGameLabel = strings.newGameLabel,
            )
        }

        GamePhase.Invalid -> {
            GameContentUi.Invalid(message = strings.invalid(max), newGameLabel = strings.newGameLabel)
        }
    }
}
