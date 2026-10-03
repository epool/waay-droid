package dev.epool.waay.fakes

import dev.epool.waay.core.speech.Speaker
import dev.epool.waay.core.speech.SpeechLanguage

/** Records every call instead of producing audio. */
internal class FakeSpeaker : Speaker {
    data class Utterance(
        val text: String,
        val language: SpeechLanguage,
    )

    val utterances = mutableListOf<Utterance>()

    fun texts(): List<String> = utterances.map { it.text }

    var stopCount = 0
        private set

    override fun speak(
        text: String,
        language: SpeechLanguage,
    ) {
        utterances += Utterance(text, language)
    }

    override fun stop() {
        stopCount++
    }
}
