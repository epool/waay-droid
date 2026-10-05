package dev.epool.waay.android.screenshots

import androidx.compose.ui.geometry.Rect
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import dev.epool.waay.android.adaptive.GameLayout
import dev.epool.waay.android.adaptive.GameLayoutMode
import dev.epool.waay.android.game.GameScreen
import dev.epool.waay.android.ui.theme.WaayTheme
import dev.epool.waay.game.presentation.GameContentUi
import dev.epool.waay.game.presentation.GameState
import dev.epool.waay.game.presentation.NumberUi
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * SC-009 / FR-026 / FR-031 / FR-032: every game phase across the official size matrix (testing-setup
 * skill, step 8): widths 400/610/900 dp × heights 400/500/1000 dp, plus 400×500 at font scale 1.5
 * and 2.0, plus tabletop and book postures with a synthetic hinge. Record with
 * `./gradlew :androidApp:recordRoborazziDebug`; CI runs `verifyRoborazziDebug`.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class GameScreenScreenshotTest {
    @After
    fun tearDown() {
        stopKoin()
    }

    private val widths = listOf(400, 610, 900)
    private val heights = listOf(400, 500, 1000)

    private fun state(content: GameContentUi) =
        GameState(title = "Wáay", settingsLabel = "Settings", newGameLabel = "New game", content = content)

    private fun card(count: Int): GameContentUi.Card {
        val max = (1 shl count) - 1
        val numbers = (1..max).filter { it and 4 != 0 }.shuffled(kotlin.random.Random(7))
        return GameContentUi.Card(
            index = 1,
            progress = "Card 2 of $count",
            question = "Is your number on this card?",
            numbers = numbers.map { NumberUi(it, "$it") },
            yesLabel = "Yes",
            noLabel = "No",
        )
    }

    private val phases =
        mapOf(
            "intro" to state(GameContentUi.Intro("Think of a number from 1 to 31 and let me guess it…", "I'm ready")),
            "card5" to state(card(5)),
            "card7" to state(card(7)),
            "revealed" to state(GameContentUi.Revealed("The number you thought of is… 27!", 27, "New game")),
            "invalid" to
                state(
                    GameContentUi.Invalid(
                        "Hmm… no number from 1 to 31 matches those answers. One of them may have been mistaken. Let's try again!",
                        "New game",
                    ),
                ),
        )

    @Test
    fun sizeMatrix() {
        widths.forEach { width ->
            heights.forEach { height ->
                RuntimeEnvironment.setQualifiers("w${width}dp-h${height}dp")
                phases.forEach { (name, state) ->
                    captureRoboImage("src/test/screenshots/game_${name}_w${width}_h$height.png") {
                        WaayTheme { GameScreen(state = state, onAction = {}, canAnswer = { true }) }
                    }
                }
            }
        }
    }

    @Test
    fun largeFontScales() {
        listOf(1.5f, 2.0f).forEach { scale ->
            RuntimeEnvironment.setQualifiers("w400dp-h500dp")
            RuntimeEnvironment.setFontScale(scale)
            phases.forEach { (name, state) ->
                captureRoboImage("src/test/screenshots/game_${name}_font$scale.png") {
                    WaayTheme { GameScreen(state = state, onAction = {}, canAnswer = { true }) }
                }
            }
        }
        RuntimeEnvironment.setFontScale(1f)
    }

    @Test
    fun foldablePostures() {
        RuntimeEnvironment.setQualifiers("w673dp-h841dp")
        val density =
            RuntimeEnvironment
                .getApplication()
                .resources.displayMetrics.density
        // Hinges in window px: a horizontal fold at mid-height (tabletop) and a vertical one (book).
        val tabletop = GameLayout(GameLayoutMode.Tabletop, Rect(0f, 420f * density, 673f * density, 420f * density))
        val book = GameLayout(GameLayoutMode.Book, Rect(336f * density, 0f, 336f * density, 841f * density))
        listOf("tabletop" to tabletop, "book" to book).forEach { (posture, layout) ->
            listOf("intro", "card5", "revealed").forEach { name ->
                captureRoboImage("src/test/screenshots/game_${name}_$posture.png") {
                    WaayTheme { GameScreen(state = phases.getValue(name), onAction = {}, canAnswer = { true }, layout = layout) }
                }
            }
        }
    }

    // Spec 002 FR-022: the card screen in light and dark, with the generated fallback scheme
    // (API 30, no dynamic colour) and with dynamic colour (API 36).
    @Test
    @Config(sdk = [30], qualifiers = "w400dp-h900dp")
    fun fallbackSchemeLight() = captureCard("game_card5_fallback_light")

    @Test
    @Config(sdk = [30], qualifiers = "w400dp-h900dp-night")
    fun fallbackSchemeDark() = captureCard("game_card5_fallback_dark")

    @Test
    @Config(sdk = [36], qualifiers = "w400dp-h900dp")
    fun dynamicSchemeLight() = captureCard("game_card5_dynamic_light")

    @Test
    @Config(sdk = [36], qualifiers = "w400dp-h900dp-night")
    fun dynamicSchemeDark() = captureCard("game_card5_dynamic_dark")

    private fun captureCard(name: String) {
        captureRoboImage("src/test/screenshots/$name.png") {
            WaayTheme { GameScreen(state = phases.getValue("card5"), onAction = {}, canAnswer = { true }) }
        }
    }
}
