package dev.epool.waay.core.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import dev.epool.waay.core.logging.log
import java.util.Locale

/**
 * Android voice (ADR-004) over the platform [TextToSpeech] engine.
 * - The engine is created lazily; a line requested before it is ready is kept (last one wins).
 * - Every line interrupts the previous one (`QUEUE_FLUSH`, FR-015).
 * - Failures and missing voices are logged and swallowed: the game never blocks on speech (FR-016).
 * Requires the `TTS_SERVICE` `<queries>` entry in the app manifest (Android 11+ package visibility).
 */
internal class TextToSpeechSpeaker(
    private val context: Context,
) : Speaker {
    private var engine: TextToSpeech? = null
    private var isReady = false
    private var pending: Pair<String, SpeechLanguage>? = null

    override fun speak(
        text: String,
        language: SpeechLanguage,
    ) {
        runCatching {
            if (isReady) speakNow(text, language) else pending = text to language
            ensureEngine()
        }.onFailure { log.w(it) { "Speech failed" } }
    }

    override fun stop() {
        pending = null
        runCatching { engine?.stop() }.onFailure { log.w(it) { "Stopping speech failed" } }
    }

    private fun ensureEngine() {
        if (engine != null) return
        engine =
            TextToSpeech(context.applicationContext) { status ->
                isReady = status == TextToSpeech.SUCCESS
                if (!isReady) log.w { "TextToSpeech unavailable (status=$status); continuing silently" }
                pending?.let { (text, language) ->
                    pending = null
                    if (isReady) speakNow(text, language)
                }
            }
    }

    private fun speakNow(
        text: String,
        language: SpeechLanguage,
    ) {
        val tts = engine ?: return
        val locale = Locale.forLanguageTag(language.tag)
        if (tts.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE) {
            tts.language = locale
        } else {
            log.w { "No voice for ${language.tag}; using the engine default" }
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    private companion object {
        const val UTTERANCE_ID = "waay"
    }
}
