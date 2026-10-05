package dev.epool.waay.android.game

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isTrue
import dev.epool.waay.android.adaptive.GameLayout
import dev.epool.waay.android.adaptive.GameLayoutMode
import dev.epool.waay.android.ui.theme.WaayTheme
import dev.epool.waay.game.presentation.GameContentUi
import dev.epool.waay.game.presentation.GameState
import dev.epool.waay.game.presentation.NumberUi
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Spec 002 FR-012a / FR-020 / FR-021: "No" is always left of "Yes", mirroring the swipes; compact
 * layouts put them under the card, wide layouts flank it, folds keep them off the card's side.
 */
@RunWith(AndroidJUnit4::class)
class ButtonOrderTest {
    @get:Rule
    val rule = createComposeRule()

    private val state =
        GameState(
            title = "Wáay",
            settingsLabel = "Settings",
            newGameLabel = "New game",
            content =
                GameContentUi.Card(
                    index = 0,
                    progress = "Card 1 of 5",
                    question = "Is your number on this card?",
                    numbers = (16..31).map { NumberUi(it, "$it") },
                    yesLabel = "Yes",
                    noLabel = "No",
                ),
        )

    private fun bounds(tag: String): Rect = rule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot

    private fun show(layout: GameLayout? = null) {
        rule.setContent {
            WaayTheme {
                if (layout == null) {
                    GameScreen(state = state, onAction = {}, canAnswer = { true })
                } else {
                    GameScreen(state = state, onAction = {}, canAnswer = { true }, layout = layout)
                }
            }
        }
    }

    @Test
    @Config(sdk = [36], qualifiers = "w411dp-h914dp")
    fun compactPutsNoLeftOfYesUnderTheCard() {
        show()
        val no = bounds("card.no")
        val yes = bounds("card.yes")
        val card = bounds("card.surface")
        assertThat(no.right <= yes.left, "No left of Yes").isTrue()
        assertThat(no.top >= card.bottom && yes.top >= card.bottom, "both under the card").isTrue()
    }

    @Test
    @Config(sdk = [36], qualifiers = "w900dp-h600dp")
    fun widePanelsFlankTheCard() {
        show()
        val card = bounds("card.surface")
        assertThat(bounds("card.no").right <= card.left, "No left of the card").isTrue()
        assertThat(bounds("card.yes").left >= card.right, "Yes right of the card").isTrue()
    }

    @Test
    @Config(sdk = [36], qualifiers = "w673dp-h841dp")
    fun tabletopPutsTheAnswersBelowTheHinge() {
        val density =
            RuntimeEnvironment
                .getApplication()
                .resources.displayMetrics.density
        val hinge = Rect(0f, 420f * density, 673f * density, 420f * density)
        show(GameLayout(GameLayoutMode.Tabletop, hinge))
        val no = bounds("card.no")
        val yes = bounds("card.yes")
        assertThat(no.right <= yes.left, "No left of Yes").isTrue()
        assertThat(bounds("card.surface").bottom <= hinge.top && no.top >= hinge.bottom, "card above, answers below the hinge").isTrue()
    }

    @Test
    @Config(sdk = [36], qualifiers = "w673dp-h841dp")
    fun bookPutsTheAnswersRightOfTheHinge() {
        val density =
            RuntimeEnvironment
                .getApplication()
                .resources.displayMetrics.density
        val hinge = Rect(336f * density, 0f, 336f * density, 841f * density)
        show(GameLayout(GameLayoutMode.Book, hinge))
        val no = bounds("card.no")
        val yes = bounds("card.yes")
        assertThat(no.right <= yes.left, "No left of Yes").isTrue()
        assertThat(bounds("card.surface").right <= hinge.left && no.left >= hinge.right, "card left, answers right of the hinge").isTrue()
    }
}
