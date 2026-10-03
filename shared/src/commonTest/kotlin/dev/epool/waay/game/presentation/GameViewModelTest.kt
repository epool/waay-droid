package dev.epool.waay.game.presentation

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEqualTo
import com.russhwolf.settings.MapSettings
import dev.epool.waay.core.i18n.EnglishStrings
import dev.epool.waay.core.i18n.StringsProvider
import dev.epool.waay.fakes.FakeSpeaker
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest : MainDispatcherTest() {
    private val speaker = FakeSpeaker()
    private val preferences = KeyValuePreferencesDataSource(MapSettings())

    private fun viewModel(random: Random = Random(1)) =
        GameViewModel(
            preferencesDataSource = preferences,
            stringsProvider = StringsProvider(),
            deckFactory = DeckFactory { MagicDeck.create(it, random) },
            speaker = speaker,
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

    // US3 — G1 + FR-013: the intro is spoken once, exactly as shown, and not repeated on resubscription.
    @Test
    fun introIsSpokenOnceExactlyAsShown() =
        runTest {
            val viewModel = viewModel()
            viewModel.state.test {
                val intro = awaitItem().content as GameContentUi.Intro
                assertThat(speaker.texts()).isEqualTo(listOf(intro.message))
            }
            advanceTimeBy(10_000)
            viewModel.state.test { awaitItem() }

            assertThat(speaker.utterances).hasSize(1)
        }

    // US3 — G2 + FR-013: each card is announced with exactly its on-screen progress and question.
    @Test
    fun eachCardIsAnnouncedWithItsOnScreenProgressAndQuestion() =
        runTest {
            val viewModel = viewModel()
            viewModel.state.test {
                awaitItem()
                viewModel.onAction(GameAction.OnReadyClick)
                val card = awaitItem().content as GameContentUi.Card

                assertThat(speaker.texts().last()).isEqualTo("${card.progress}. ${card.question}")
            }
        }

    // US3 — G3, G10: the reveal is spoken as shown; every new line is spoken (intro + 5 cards + reveal).
    @Test
    fun revealIsSpokenAndEveryLineIsSpokenOnce() =
        runTest {
            val viewModel = viewModel()
            viewModel.state.test {
                awaitItem()
                viewModel.onAction(GameAction.OnReadyClick)
                val revealed = answerTruthfully(viewModel, awaitItem(), secret = 27).content as GameContentUi.Revealed

                assertThat(speaker.texts().last()).isEqualTo(revealed.message)
                assertThat(speaker.utterances).hasSize(7)
            }
        }

    // US3 — G4: the invalid-result message is spoken as shown.
    @Test
    fun invalidMessageIsSpoken() =
        runTest {
            val viewModel = viewModel()
            viewModel.state.test {
                awaitItem()
                viewModel.onAction(GameAction.OnReadyClick)
                val invalid = answerTruthfully(viewModel, awaitItem(), secret = 0).content as GameContentUi.Invalid

                assertThat(speaker.texts().last()).isEqualTo(invalid.message)
            }
        }

    // US3 — G9: with voice off nothing is spoken (FR-014).
    @Test
    fun nothingIsSpokenWhenVoiceIsOff() =
        runTest {
            preferences.setVoiceEnabled(false)
            val viewModel = viewModel()
            viewModel.state.test {
                awaitItem()
                viewModel.onAction(GameAction.OnReadyClick)
                awaitItem()
            }

            assertThat(speaker.utterances).isEmpty()
        }

    // US3 — G9: turning the voice off mid-game stops any speech in progress.
    @Test
    fun turningVoiceOffStopsSpeech() =
        runTest {
            val viewModel = viewModel()
            viewModel.state.test {
                awaitItem()
                preferences.setVoiceEnabled(false)
                runCurrent()

                assertThat(speaker.stopCount).isGreaterThanOrEqualTo(1)
            }
        }

    // US3 — G12: clearing the ViewModel stops speech (ADR-001).
    @Test
    fun clearingTheViewModelStopsSpeech() =
        runTest {
            val store = ViewModelStore()
            val viewModel =
                ViewModelProvider.create(store, viewModelFactory { initializer { viewModel() } })[GameViewModel::class]
            viewModel.state.test { awaitItem() }

            store.clear()

            assertThat(speaker.stopCount).isGreaterThanOrEqualTo(1)
        }
}
