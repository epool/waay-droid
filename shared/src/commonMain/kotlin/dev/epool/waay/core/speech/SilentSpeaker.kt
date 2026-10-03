package dev.epool.waay.core.speech

/** A [Speaker] that never makes a sound: the fallback when no voice engine is available. */
internal object SilentSpeaker : Speaker {
    override fun speak(
        text: String,
        language: SpeechLanguage,
    ) = Unit

    override fun stop() = Unit
}
