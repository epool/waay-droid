package dev.epool.waay.settings.domain

/** The player's language preference (FR-021). [key] is the stable persisted value. */
internal enum class LanguageChoice(
    val key: String,
) {
    Device("device"),
    English("en"),
    Spanish("es"),
    ;

    companion object {
        /** Unknown or missing keys fall back to [Device] (FR-024). */
        fun fromKey(key: String?): LanguageChoice = entries.firstOrNull { it.key == key } ?: Device
    }
}
