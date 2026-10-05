package dev.epool.waay.android.game

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.containsExactly
import dev.epool.waay.android.ui.theme.WaayTheme
import dev.epool.waay.game.presentation.GameAction
import dev.epool.waay.game.presentation.GameContentUi
import dev.epool.waay.game.presentation.GameState
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

/**
 * Spec 001 FR-026 inside spec 002's message card: at the largest text size the longest message (an
 * invalid answer set) no longer fits a small window, so the card scrolls and its action stays usable.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "w400dp-h500dp", fontScale = 2.0f)
class MessageCardTest {
    @get:Rule
    val rule = createComposeRule()

    @After
    fun tearDown() {
        // WaayApp starts Koin per Robolectric Application; stop it so the next test can start again.
        stopKoin()
    }

    @Test
    fun theResultActionStaysReachableAtTheLargestTextSize() {
        val actions = mutableListOf<GameAction>()
        val invalid =
            GameContentUi.Invalid(
                "Hmm… no number from 1 to 31 matches those answers. One of them may have been mistaken. Let's try again!",
                "New game",
            )
        rule.setContent {
            WaayTheme {
                GameScreen(
                    state = GameState(title = "Wáay", settingsLabel = "Settings", newGameLabel = "New game", content = invalid),
                    onAction = { actions += it },
                    canAnswer = { true },
                )
            }
        }

        rule
            .onNodeWithTag("result.newGame")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        assertThat(actions).containsExactly(GameAction.OnNewGameClick)
    }
}
