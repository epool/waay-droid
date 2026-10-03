package dev.epool.waay.game.presentation

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEqualTo
import com.russhwolf.settings.MapSettings
import dev.epool.waay.core.i18n.EnglishStrings
import dev.epool.waay.core.i18n.StringsProvider
import dev.epool.waay.fakes.MainDispatcherTest
import dev.epool.waay.game.domain.Answer
import dev.epool.waay.game.domain.Card
import dev.epool.waay.game.domain.CardCount
import dev.epool.waay.game.domain.Deck
import dev.epool.waay.game.domain.DeckFactory
import dev.epool.waay.game.domain.GameCommand
import dev.epool.waay.game.domain.GameEngine
import dev.epool.waay.game.domain.MagicDeck
import dev.epool.waay.settings.data.KeyValuePreferencesDataSource
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test

class GameViewModelTest : MainDispatcherTest() {
    private fun viewModel(random: Random = Random(1)) =
        GameViewModel(
            preferencesDataSource = KeyValuePreferencesDataSource(MapSettings()),
            stringsProvider = StringsProvider(),
            deckFactory = DeckFactory { MagicDeck.create(it, random) },
        )

    private suspend fun ReceiveTurbine<GameState>.answerTruthfully(
        viewModel: GameViewModel,
        firstCard: GameState,
        secret: Int,
        cards: Int = 5,
    ): GameState {
        var state = firstCard
        repeat(cards) {
            val card = state.content as GameContentUi.Card
            val answer = if (card.numbers.any { it.value == secret }) Answer.Yes else Answer.No
            viewModel.onAction(GameAction.OnAnswerClick(answer))
            state = awaitItem()
        }
        return state
    }

    // G1: first subscription shows the intro for the current card count.
    @Test
    fun firstSubscriptionShowsTheIntro() =
        runTest {
            viewModel().state.test {
                val state = awaitItem()
                val intro = state.content as GameContentUi.Intro
                assertThat(intro.message).contains("1 to 31")
                assertThat(intro.readyLabel).isEqualTo("I'm ready")
                assertThat(state.settingsLabel).isEqualTo("Settings")
                assertThat(state.newGameLabel).isEqualTo("New game")
            }
        }

    // G2: ready shows card 1 of N with numbersPerCard numbers.
    @Test
    fun readyShowsTheFirstCard() =
        runTest {
            val viewModel = viewModel()
            viewModel.state.test {
                awaitItem()
                viewModel.onAction(GameAction.OnReadyClick)

                val card = awaitItem().content as GameContentUi.Card
                assertThat(card.progress).isEqualTo("Card 1 of 5")
                assertThat(card.numbers).hasSize(16)
                assertThat(card.yesLabel).isEqualTo("Yes")
                assertThat(card.noLabel).isEqualTo("No")
            }
        }

    // G3: truthful answers reveal the secret as a statement (FR-004, FR-007).
    @Test
    fun truthfulAnswersRevealTheSecret() =
        runTest {
            val viewModel = viewModel()
            viewModel.state.test {
                awaitItem()
                viewModel.onAction(GameAction.OnReadyClick)

                val revealed = answerTruthfully(viewModel, awaitItem(), secret = 27).content as GameContentUi.Revealed
                assertThat(revealed.number).isEqualTo(27)
                assertThat(revealed.message).isEqualTo("The number you thought of is… 27!")
            }
        }

    // G4: all "No" ends invalid (FR-005).
    @Test
    fun allNoEndsInvalid() =
        runTest {
            val viewModel = viewModel()
            viewModel.state.test {
                awaitItem()
                viewModel.onAction(GameAction.OnReadyClick)

                val state = answerTruthfully(viewModel, awaitItem(), secret = 0)
                assertThat(state.content).isInstanceOf(GameContentUi.Invalid::class)
                assertThat((state.content as GameContentUi.Invalid).message).contains("1 to 31")
            }
        }

    // G5: new game from any phase returns to the intro (FR-006).
    @Test
    fun newGameReturnsToTheIntro() =
        runTest {
            val viewModel = viewModel()
            viewModel.state.test {
                awaitItem()
                viewModel.onAction(GameAction.OnReadyClick)
                awaitItem()
                viewModel.onAction(GameAction.OnAnswerClick(Answer.Yes))
                awaitItem()

                viewModel.onAction(GameAction.OnNewGameClick)

                assertThat(awaitItem().content).isInstanceOf(GameContentUi.Intro::class)
            }
        }

    // G6: answers outside the Card phase change nothing (FR-028).
    @Test
    fun answersOutsideTheCardPhaseAreIgnored() =
        runTest {
            val viewModel = viewModel()
            viewModel.state.test {
                awaitItem()
                viewModel.onAction(GameAction.OnAnswerClick(Answer.Yes))
                expectNoEvents()
            }
        }

    // G11: settings click emits NavigateToSettings exactly once (FR-016a).
    @Test
    fun settingsClickEmitsNavigateToSettingsOnce() =
        runTest {
            val viewModel = viewModel()
            viewModel.events.test {
                viewModel.onAction(GameAction.OnSettingsClick)
                assertThat(awaitItem()).isEqualTo(GameEvent.NavigateToSettings)
                expectNoEvents()
            }
        }

    // G13: the card UI carries only the displayed numbers and labels — no bit information (FR-011).
    @Test
    fun cardUiExposesOnlyDisplayedNumbers() =
        runTest {
            val viewModel = viewModel()
            viewModel.state.test {
                awaitItem()
                viewModel.onAction(GameAction.OnReadyClick)

                val card = awaitItem().content as GameContentUi.Card
                card.numbers.forEach { assertThat(it.label).isEqualTo(it.value.toString()) }
            }
        }

    // G5 (US2): a new game deals a freshly shuffled deck (FR-008, FR-009).
    @Test
    fun newGameDealsAFreshlyShuffledDeck() =
        runTest {
            val viewModel = viewModel(Random(99))
            viewModel.state.test {
                awaitItem()
                viewModel.onAction(GameAction.OnReadyClick)
                val firstGame = (awaitItem().content as GameContentUi.Card).numbers

                viewModel.onAction(GameAction.OnNewGameClick)
                awaitItem()
                viewModel.onAction(GameAction.OnReadyClick)
                val secondGame = (awaitItem().content as GameContentUi.Card).numbers

                assertThat(secondGame).isNotEqualTo(firstGame)
            }
        }

    // G13 (US2): the hidden mapping is invisible — same displayed numbers, different bit values,
    // identical UI state (FR-011).
    @Test
    fun hiddenBitMappingDoesNotReachTheUi() {
        val numbers = listOf(3, 1, 7, 5)

        fun snapshotWithFirstCardBit(bitValue: Int) =
            GameEngine.reduce(
                GameEngine.start(
                    Deck(
                        cardCount = CardCount.all.first(),
                        cards =
                            listOf(
                                Card(bitValue, numbers),
                                Card(2, listOf(2, 3, 6, 7)),
                                Card(4 xor bitValue xor 1, listOf(4, 5, 6, 7)),
                            ),
                    ),
                ),
                GameCommand.Ready,
                newDeck = { error("unused") },
            )

        assertThat(snapshotWithFirstCardBit(1).toGameState(EnglishStrings))
            .isEqualTo(snapshotWithFirstCardBit(4).toGameState(EnglishStrings))
    }
}
