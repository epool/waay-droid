package dev.epool.waay.core.speech

import android.speech.tts.TextToSpeech
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowTextToSpeech

/** FR-015 and FR-016 for the Android voice, over Robolectric's TextToSpeech shadow. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TextToSpeechSpeakerTest {
    private val speaker = TextToSpeechSpeaker(RuntimeEnvironment.getApplication())
    private val english = SpeechLanguage(languageCode = "en", regionCode = "US")

    /** The engine is created on the first line; the platform then reports [status]. */
    private fun engineAfterInit(status: Int): ShadowTextToSpeech {
        speaker.speak("Think of a number from 1 to 31 and let me guess it…", english)
        val engine = shadowOf(ShadowTextToSpeech.getLastTextToSpeechInstance())
        engine.onInitListener.onInit(status)
        return engine
    }

    @Test
    fun everyLineInterruptsThePreviousOne() {
        val engine = engineAfterInit(TextToSpeech.SUCCESS)

        speaker.speak("Card 1 of 5. Is your number on this card?", english)
        assertThat(engine.queueMode).isEqualTo(TextToSpeech.QUEUE_FLUSH)
        speaker.speak("Card 2 of 5. Is your number on this card?", english)

        assertThat(engine.queueMode).isEqualTo(TextToSpeech.QUEUE_FLUSH)
        assertThat(engine.lastSpokenText).isEqualTo("Card 2 of 5. Is your number on this card?")
    }

    @Test
    fun aLineRequestedBeforeTheEngineIsReadyIsSpokenOnceItIs() {
        val engine = engineAfterInit(TextToSpeech.SUCCESS)

        assertThat(engine.spokenTextList).containsExactly("Think of a number from 1 to 31 and let me guess it…")
    }

    @Test
    fun stopSilencesTheEngine() {
        val engine = engineAfterInit(TextToSpeech.SUCCESS)

        speaker.stop()

        assertThat(engine.isStopped).isTrue()
    }

    @Test
    fun anUnavailableEngineStaysSilentWithoutFailing() {
        val engine = engineAfterInit(TextToSpeech.ERROR)

        speaker.speak("Card 1 of 5. Is your number on this card?", english)
        speaker.stop()

        assertThat(engine.spokenTextList).containsExactly()
    }
}
