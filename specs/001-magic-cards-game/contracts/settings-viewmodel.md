# Contract: SettingsViewModel

**Package:** `dev.epool.waay.settings.presentation`.
**Consumers:** `androidApp` (`SettingsRoot`), `iosApp` (`SettingsRoot`).

```kotlin
public class SettingsViewModel internal constructor(/* deps via Koin */) : ViewModel() {
    public val state: StateFlow<SettingsState>
    public val events: Flow<SettingsEvent>
    public fun onAction(action: SettingsAction)
}

public data class SettingsState(
    val title: String,
    val backLabel: String,
    val cardCountLabel: String,
    val cardCountOptions: List<CardCountOptionUi>,   // 3..7
    val selectedCardCount: Int,
    val voiceLabel: String,
    val voiceEnabled: Boolean,
    val languageLabel: String,
    val languageOptions: List<LanguageOptionUi>,     // Device, English, Español
    val selectedLanguage: LanguageChoiceUi,
)

public data class CardCountOptionUi(val value: Int, val label: String)   // "5 cards (1–31)"
public data class LanguageOptionUi(val choice: LanguageChoiceUi, val label: String)
public enum class LanguageChoiceUi { Device, English, Spanish }

public sealed interface SettingsAction {
    public data class OnCardCountSelect(val value: Int) : SettingsAction
    public data class OnVoiceToggle(val enabled: Boolean) : SettingsAction
    public data class OnLanguageSelect(val choice: LanguageChoiceUi) : SettingsAction
    public data object OnBackClick : SettingsAction
}

public sealed interface SettingsEvent {
    public data object NavigateBack : SettingsEvent
}
```

## Behavioural guarantees

| # | Given / When | Then | Spec |
|---|---|---|---|
| S1 | First subscription | Reflects the persisted preferences, or the defaults on a fresh install: 5, on, Device. | FR-023, FR-024 |
| S2 | `OnCardCountSelect(n)` with n in 3..7 | Persisted. `selectedCardCount = n`. Values outside the range are ignored. | FR-017, FR-023 |
| S3 | `OnVoiceToggle(b)` | Persisted. If set to false, any speech in progress stops (through GameViewModel G9). | FR-014 |
| S4 | `OnLanguageSelect(c)` | Persisted. **All labels in this state switch language immediately.** | FR-021 |
| S5 | `OnBackClick` | Emits `NavigateBack` once. The platform's own system back gesture behaves the same. | FR-016b |
| S6 | Any change | Observed by GameViewModel through `PreferencesDataSource` (G7 and G8). | FR-018 |
