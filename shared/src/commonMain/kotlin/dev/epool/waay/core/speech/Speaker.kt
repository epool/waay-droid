package dev.epool.waay.core.speech

/**
 * The magician's voice (ADR-004, contracts/platform-services.md).
 * Implementations live in androidMain (TextToSpeech) and iosMain (AVSpeechSynthesizer).
 */
internal interface Speaker {
    /** Interrupts any utterance in progress, then speaks [text] with a voice for [language]. Never throws (FR-016). */
    fun speak(
        text: String,
        language: SpeechLanguage,
    )

    /** Stops immediately. Idempotent. */
    fun stop()
}
