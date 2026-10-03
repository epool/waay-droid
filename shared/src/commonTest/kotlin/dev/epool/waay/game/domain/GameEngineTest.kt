package dev.epool.waay.game.domain

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isSameInstanceAs
import kotlin.random.Random
import kotlin.test.Test

class GameEngineTest {
    private val random = Random(7)
    private val newDeck: (CardCount) -> Deck = { MagicDeck.create(it, random) }
    private val start = GameEngine.start(MagicDeck.create(CardCount.DEFAULT, random))

    private fun GameSnapshot.reduce(vararg commands: GameCommand): GameSnapshot =
        commands.fold(this) { snapshot, command -> GameEngine.reduce(snapshot, command, newDeck) }

    private fun GameSnapshot.answerTruthfully(secret: Int): GameSnapshot =
        deck.cards.fold(this) { snapshot, card ->
            snapshot.reduce(GameCommand.AnswerCard(if (secret in card.numbers) Answer.Yes else Answer.No))
        }

    @Test
    fun startsInIntro() {
        assertThat(start.phase).isEqualTo(GamePhase.Intro)
        assertThat(start.answers).isEmpty()
    }

    @Test
    fun readyMovesFromIntroToTheFirstCard() {
        assertThat(start.reduce(GameCommand.Ready).phase).isEqualTo(GamePhase.Asking(0))
    }

    @Test
    fun answersAdvanceThroughTheCards() {
        val snapshot = start.reduce(GameCommand.Ready, GameCommand.AnswerCard(Answer.Yes), GameCommand.AnswerCard(Answer.No))

        assertThat(snapshot.phase).isEqualTo(GamePhase.Asking(2))
        assertThat(snapshot.answers).isEqualTo(listOf(Answer.Yes, Answer.No))
    }

    @Test
    fun lastTruthfulAnswerRevealsTheSecret() {
        val snapshot = start.reduce(GameCommand.Ready).answerTruthfully(27)

        assertThat(snapshot.phase).isEqualTo(GamePhase.Revealed(27))
    }

    @Test
    fun allNoEndsInvalid() {
        val snapshot = start.reduce(GameCommand.Ready).answerTruthfully(0)

        assertThat(snapshot.phase).isEqualTo(GamePhase.Invalid)
    }

    @Test
    fun newGameFromAnyPhaseReturnsToIntroWithAFreshDeck() {
        listOf(
            start,
            start.reduce(GameCommand.Ready, GameCommand.AnswerCard(Answer.Yes)),
            start.reduce(GameCommand.Ready).answerTruthfully(5),
            start.reduce(GameCommand.Ready).answerTruthfully(0),
        ).forEach { snapshot ->
            val next = snapshot.reduce(GameCommand.NewGame)
            assertThat(next.phase).isEqualTo(GamePhase.Intro)
            assertThat(next.answers).isEmpty()
        }
    }

    @Test
    fun cardCountChangeReturnsToIntroWithTheNewCount() {
        val seven = CardCount.all.last()

        val next = start.reduce(GameCommand.Ready).reduce(GameCommand.CardCountChanged(seven))

        assertThat(next.phase).isEqualTo(GamePhase.Intro)
        assertThat(next.deck.cardCount).isEqualTo(seven)
    }

    // FR-028: commands a phase doesn't allow are no-ops.
    @Test
    fun disallowedCommandsAreNoOps() {
        assertThat(start.reduce(GameCommand.AnswerCard(Answer.Yes))).isSameInstanceAs(start)

        val asking = start.reduce(GameCommand.Ready)
        assertThat(asking.reduce(GameCommand.Ready)).isSameInstanceAs(asking)

        val revealed = asking.answerTruthfully(9)
        assertThat(revealed.reduce(GameCommand.AnswerCard(Answer.Yes))).isSameInstanceAs(revealed)
        assertThat(revealed.reduce(GameCommand.Ready)).isSameInstanceAs(revealed)
    }
}
