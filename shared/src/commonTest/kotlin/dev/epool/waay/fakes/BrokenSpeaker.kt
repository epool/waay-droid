package dev.epool.waay.fakes

import dev.epool.waay.core.speech.Speaker
import dev.epool.waay.core.speech.SpeechLanguage

/** A speech engine that fails on every call, like a missing or crashing TTS service (FR-016). */
internal class BrokenSpeaker : Speaker {
    var calls = 0
        private set

    override fun speak(
        text: String,
        language: SpeechLanguage,
    ) {
        calls++
        error("Speech engine unavailable")
    }

    override fun stop() {
        calls++
        error("Speech engine unavailable")
    }
}
