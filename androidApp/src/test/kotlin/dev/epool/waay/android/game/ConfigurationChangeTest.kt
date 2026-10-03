package dev.epool.waay.android.game

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.epool.waay.android.MainActivity
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

/** FR-029 / FR-032: recreating the Activity mid-game (rotation, fold, resize) keeps the card and answers. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "w411dp-h914dp")
class ConfigurationChangeTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun recreatingTheActivityKeepsTheGame() {
        GameRobot(rule)
            .tapReady()
            .answer("card.yes")
            .answer("card.no")
        rule.onNodeWithTag("card.progress").assertTextEquals("Card 3 of 5")

        rule.activityRule.scenario.recreate()

        rule.onNodeWithTag("card.progress").assertTextEquals("Card 3 of 5")
    }
}
