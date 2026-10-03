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

    // US1 — game
    fun intro(max: Int): String

    val readyLabel: String

    fun progress(
        current: Int,
        total: Int,
    ): String

    val cardQuestion: String
    val yesLabel: String
    val noLabel: String

    /** The reveal is a statement, never a confirmation question (FR-007). */
    fun reveal(number: Int): String

    fun invalid(max: Int): String

    val newGameLabel: String

    fun numberLabel(number: Int): String
}
