package dev.epool.waay.core.speech

/** BCP-47 style language for speech and locale resolution, e.g. `("es", "MX")`. */
internal data class SpeechLanguage(
    val languageCode: String,
    val regionCode: String?,
) {
    val tag: String get() = if (regionCode.isNullOrBlank()) languageCode else "$languageCode-$regionCode"
}
