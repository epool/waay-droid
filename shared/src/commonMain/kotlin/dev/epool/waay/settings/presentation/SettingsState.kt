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
    val languageLabel: String,
    /** Device language, English, Español (FR-021). */
    val languageOptions: List<LanguageOptionUi>,
    val selectedLanguage: LanguageChoiceUi,
)

public enum class LanguageChoiceUi { Device, English, Spanish }

/** Non-generic option type for Swift (ADR-002). */
public data class LanguageOptionUi(
    val choice: LanguageChoiceUi,
    val label: String,
)

/** Non-generic option type for Swift (ADR-002). */
public data class CardCountOptionUi(
    val value: Int,
    val label: String,
)
