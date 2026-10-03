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
import dev.epool.waay.core.domain.Result
import dev.epool.waay.core.i18n.EnglishStrings
import dev.epool.waay.core.i18n.StringsProvider
import dev.epool.waay.core.speech.Speaker
import dev.epool.waay.fakes.AdvancingTimeSource
import dev.epool.waay.fakes.BrokenSpeaker
import dev.epool.waay.fakes.FakeDeviceLocale
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
import dev.epool.waay.settings.domain.LanguageChoice
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TestTimeSource
import kotlin.time.TimeSource

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest : MainDispatcherTest() {
    private val speaker = FakeSpeaker()
    private val preferences = KeyValuePreferencesDataSource(MapSettings())

    private fun viewModel(
        random: Random = Random(1),
        timeSource: TimeSource = AdvancingTimeSource(),
        speaker: Speaker = this.speaker,
    ) = GameViewModel(
        preferencesDataSource = preferences,
        stringsProvider = StringsProvider(FakeDeviceLocale()),
        deckFactory = DeckFactory { MagicDeck.create(it, random) },
        speaker = speaker,
        timeSource = timeSource,
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
            viewModel.onAction(GameAction.OnAnswerClick(answer, card.index))
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
                viewModel.onAction(GameAction.OnAnswerClick(Answer.Yes, cardIndex = 0))
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
                viewModel.onAction(GameAction.OnAnswerClick(Answer.Yes, cardIndex = 0))
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

    // FR-016: a speech engine that fails on every call never blocks the game, from the intro to the
    // reveal, through turning the voice off, to leaving the screen.
    @Test
    fun speechFailuresNeverBlockTheGame() =
        runTest {
            val brokenSpeaker = BrokenSpeaker()
            val store = ViewModelStore()
            val viewModel =
                ViewModelProvider.create(
                    store,
                    viewModelFactory { initializer { viewModel(speaker = brokenSpeaker) } },
                )[GameViewModel::class]
            viewModel.state.test {
                awaitItem()
                viewModel.onAction(GameAction.OnReadyClick)

                val revealed = answerTruthfully(viewModel, awaitItem(), secret = 27).content as GameContentUi.Revealed
                assertThat(revealed.number).isEqualTo(27)

                preferences.setVoiceEnabled(false)
                runCurrent()
                cancelAndIgnoreRemainingEvents()
            }

            store.clear()

            assertThat(brokenSpeaker.calls).isGreaterThanOrEqualTo(8)
        }

    private fun cardCount(value: Int) = (CardCount.of(value) as Result.Success).data

    // US4 — G7: changing the card count while watching resets to the intro for the new range,
    // speaks it, and the cards then show numbersPerCard numbers (FR-018).
    @Test
    fun cardCountChangeResetsToTheIntroForTheNewRange() =
        runTest {
            val viewModel = viewModel()
            viewModel.state.test {
                awaitItem()
                viewModel.onAction(GameAction.OnReadyClick)
                awaitItem()

                preferences.setCardCount(cardCount(7))
                val intro = awaitItem().content as GameContentUi.Intro
                assertThat(intro.message).contains("1 to 127")
                assertThat(speaker.texts().last()).isEqualTo(intro.message)

                viewModel.onAction(GameAction.OnReadyClick)
                val card = awaitItem().content as GameContentUi.Card
                assertThat(card.progress).isEqualTo("Card 1 of 7")
                assertThat(card.numbers).hasSize(64)
            }
        }

    // US4 — G7 + finding U1: a change made while the game screen is not watched (player in Settings)
    // keeps the intro speech pending until the game screen subscribes again — never over Settings.
    @Test
    fun introSpeechIsDeferredWhileTheGameScreenIsAway() =
        runTest {
            val viewModel = viewModel()
            viewModel.state.test {
                awaitItem()
                viewModel.onAction(GameAction.OnReadyClick)
                awaitItem()
            }
            val spokenBefore = speaker.utterances.size

            preferences.setCardCount(cardCount(3))
            runCurrent()
            assertThat(speaker.utterances).hasSize(spokenBefore)

            viewModel.state.test {
                val intro = awaitItem().content as GameContentUi.Intro
                assertThat(intro.message).contains("1 to 7")
                assertThat(speaker.texts().last()).isEqualTo(intro.message)
            }
        }

    // US5 — G8: switching the language re-resolves every text in place; the game itself is unchanged,
    // and the next line is spoken with a voice for the new language (FR-021, FR-022).
    @Test
    fun languageSwitchReResolvesTextInPlace() =
        runTest {
            val viewModel = viewModel()
            viewModel.state.test {
                awaitItem()
                viewModel.onAction(GameAction.OnReadyClick)
                val english = awaitItem()
                val englishCard = english.content as GameContentUi.Card

                preferences.setLanguageChoice(LanguageChoice.Spanish)
                val spanish = awaitItem()
                val spanishCard = spanish.content as GameContentUi.Card

                assertThat(spanish.newGameLabel).isEqualTo("Nuevo juego")
                assertThat(spanishCard.progress).isEqualTo("Carta 1 de 5")
                assertThat(spanishCard.numbers).isEqualTo(englishCard.numbers)

                viewModel.onAction(GameAction.OnAnswerClick(Answer.No, spanishCard.index))
                awaitItem()
                assertThat(
                    speaker.utterances
                        .last()
                        .language.languageCode,
                ).isEqualTo("es")
            }
        }

    // FR-028: a rapid double tap records a single answer — both a same-frame duplicate (stale card
    // index) and a second tap landing on the next card within the 300 ms answer cooldown.
    @Test
    fun rapidDoubleTapRecordsASingleAnswer() =
        runTest {
            val clock = TestTimeSource()
            val viewModel = viewModel(timeSource = clock)
            viewModel.state.test {
                awaitItem()
                viewModel.onAction(GameAction.OnReadyClick)
                val first = awaitItem().content as GameContentUi.Card
                clock += 2_000.milliseconds

                viewModel.onAction(GameAction.OnAnswerClick(Answer.Yes, first.index))
                viewModel.onAction(GameAction.OnAnswerClick(Answer.Yes, first.index))
                val second = awaitItem().content as GameContentUi.Card
                assertThat(second.progress).isEqualTo("Card 2 of 5")

                clock += 150.milliseconds
                viewModel.onAction(GameAction.OnAnswerClick(Answer.Yes, second.index))
                expectNoEvents()

                clock += 200.milliseconds
                viewModel.onAction(GameAction.OnAnswerClick(Answer.Yes, second.index))
                assertThat((awaitItem().content as GameContentUi.Card).progress).isEqualTo("Card 3 of 5")
            }
        }
}
