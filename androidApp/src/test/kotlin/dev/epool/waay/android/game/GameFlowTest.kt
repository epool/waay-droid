package dev.epool.waay.android.game

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.epool.waay.android.MainActivity
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

/** US1 smoke flow on Android 8.0 (API 26, FR-030) and API 36. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [26, 36], qualifiers = "w411dp-h914dp")
class GameFlowTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @After
    fun tearDown() {
        // WaayApp starts Koin per Robolectric Application; stop it so the next test can start again.
        stopKoin()
    }

    @Test
    fun playsAFullRoundThenStartsANewGame() {
        GameRobot(rule)
            .assertIntro()
            .tapReady()
            .answerTruthfully(secret = 27)
            .assertRevealed(secret = 27)
            .tapNewGame()
            .assertIntro()
    }
}
