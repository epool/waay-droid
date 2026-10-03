package dev.epool.waay.settings.presentation

public sealed interface SettingsAction {
    public data object OnBackClick : SettingsAction

    public data class OnVoiceToggle(
        val enabled: Boolean,
    ) : SettingsAction
}
