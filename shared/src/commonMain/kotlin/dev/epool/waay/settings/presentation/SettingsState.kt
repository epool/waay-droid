package dev.epool.waay.settings.presentation

/** Settings screen UI state with resolved, localized text (contracts/settings-viewmodel.md). */
public data class SettingsState(
    val title: String,
    val backLabel: String,
    val voiceLabel: String,
    val voiceEnabled: Boolean,
    val cardCountLabel: String,
    /** 3–7 cards, each labelled with its range, e.g. "5 cards (1–31)" (FR-017). */
    val cardCountOptions: List<CardCountOptionUi>,
    val selectedCardCount: Int,
)

/** Non-generic option type for Swift (ADR-002). */
public data class CardCountOptionUi(
    val value: Int,
    val label: String,
)
