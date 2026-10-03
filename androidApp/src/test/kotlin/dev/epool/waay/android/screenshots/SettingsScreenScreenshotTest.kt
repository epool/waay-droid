package dev.epool.waay.android.screenshots

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import dev.epool.waay.android.settings.SettingsScreen
import dev.epool.waay.android.ui.theme.WaayTheme
import dev.epool.waay.settings.presentation.CardCountOptionUi
import dev.epool.waay.settings.presentation.LanguageChoiceUi
import dev.epool.waay.settings.presentation.LanguageOptionUi
import dev.epool.waay.settings.presentation.SettingsState
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * FR-027 / FR-026 / FR-019: the Settings screen in both languages, with voice on and off. Reviewed for
 * non-colour cues: selection shows as a filled radio dot and a switch thumb position, never by colour
 * alone. Record with `./gradlew :androidApp:recordRoborazziDebug`.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w400dp-h900dp")
class SettingsScreenScreenshotTest {
    @After
    fun tearDown() {
        stopKoin()
    }

    private val english =
        SettingsState(
            title = "Settings",
            backLabel = "Back",
            voiceLabel = "Magician's voice",
            voiceEnabled = true,
            cardCountLabel = "Number of cards",
            cardCountOptions = (3..7).map { CardCountOptionUi(it, "$it cards (1–${(1 shl it) - 1})") },
            selectedCardCount = 5,
            languageLabel = "Language",
            languageOptions =
                listOf(
                    LanguageOptionUi(LanguageChoiceUi.Device, "Device language"),
                    LanguageOptionUi(LanguageChoiceUi.English, "English"),
                    LanguageOptionUi(LanguageChoiceUi.Spanish, "Español"),
                ),
            selectedLanguage = LanguageChoiceUi.Device,
        )

    private val spanish =
        SettingsState(
            title = "Ajustes",
            backLabel = "Atrás",
            voiceLabel = "Voz del mago",
            voiceEnabled = false,
            cardCountLabel = "Número de cartas",
            cardCountOptions = (3..7).map { CardCountOptionUi(it, "$it cartas (1–${(1 shl it) - 1})") },
            selectedCardCount = 7,
            languageLabel = "Idioma",
            languageOptions =
                listOf(
                    LanguageOptionUi(LanguageChoiceUi.Device, "Idioma del dispositivo"),
                    LanguageOptionUi(LanguageChoiceUi.English, "English"),
                    LanguageOptionUi(LanguageChoiceUi.Spanish, "Español"),
                ),
            selectedLanguage = LanguageChoiceUi.Spanish,
        )

    @Test
    fun settingsInBothLanguages() {
        listOf("en_voiceOn" to english, "es_voiceOff" to spanish).forEach { (name, state) ->
            captureRoboImage("src/test/screenshots/settings_$name.png") {
                WaayTheme { SettingsScreen(state = state, onAction = {}) }
            }
        }
    }

    @Test
    fun settingsAtTheLargestFontScale() {
        RuntimeEnvironment.setFontScale(2.0f)
        captureRoboImage("src/test/screenshots/settings_en_font2.0.png") {
            WaayTheme { SettingsScreen(state = english, onAction = {}) }
        }
        RuntimeEnvironment.setFontScale(1f)
    }
}
