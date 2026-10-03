package dev.epool.waay.android.settings

import android.app.Application
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.epool.waay.android.MainActivity
import dev.epool.waay.di.initKoin
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

/**
 * US6 / SC-005: preferences changed through the UI survive a restart, simulated here by a fresh Koin
 * graph plus Activity recreation over the same SharedPreferences.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "w411dp-h914dp")
class PreferencesPersistenceTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun settingsAreRestoredAfterARestart() {
        rule.onNodeWithTag("toolbar.settings").performClick()
        rule.onNodeWithTag("settings.cardCount.6").performClick()
        rule.onNodeWithTag("settings.voice").performClick()
        rule.onNodeWithTag("settings.language.Spanish").performClick()

        // "Restart": a brand-new dependency graph and Activity over the same persisted store.
        stopKoin()
        initKoin { androidContext(ApplicationProvider.getApplicationContext<Application>()) }
        rule.activityRule.scenario.recreate()

        rule.onNodeWithText("Ajustes").assertExists()
        rule.onNodeWithTag("settings.cardCount.6").assertIsSelected()
        rule.onNodeWithTag("settings.voice").assertIsOff()
        rule.onNodeWithTag("settings.language.Spanish").assertIsSelected()
    }
}
