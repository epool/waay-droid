package dev.epool.waay.core.speech

import dev.epool.waay.core.logging.log
import platform.AVFAudio.AVSpeechBoundary
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechUtterance

/**
 * iOS voice (ADR-004) over [AVSpeechSynthesizer].
 * - Every line interrupts the previous one immediately (FR-015).
 * - Prefers the regional voice (e.g. `es-MX`), falling back to the plain language, then the default.
 * - Uses the default audio session, which respects the silent switch.
 */
internal class AvSpeechSpeaker : Speaker {
    private val synthesizer by lazy { AVSpeechSynthesizer() }

    override fun speak(
        text: String,
        language: SpeechLanguage,
    ) {
        runCatching {
            synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
            val utterance = AVSpeechUtterance.speechUtteranceWithString(text)
            utterance.voice = AVSpeechSynthesisVoice.voiceWithLanguage(language.tag)
                ?: AVSpeechSynthesisVoice.voiceWithLanguage(language.languageCode)
            synthesizer.speakUtterance(utterance)
        }.onFailure { log.w(it) { "Speech failed" } }
    }

    override fun stop() {
        runCatching { synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate) }
            .onFailure { log.w(it) { "Stopping speech failed" } }
    }
}
