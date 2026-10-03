package dev.epool.waay.core.i18n

/**
 * Every user-facing (shown or spoken) text, as a typed catalog (ADR-003).
 * Each language implements this interface, so the compiler guarantees completeness (SC-004).
 * Members are added per user story.
 */
internal interface Strings {
    val appTitle: String
    val settingsLabel: String
    val settingsTitle: String
    val backLabel: String
}
