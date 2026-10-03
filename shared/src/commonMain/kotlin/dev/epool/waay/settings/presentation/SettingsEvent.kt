package dev.epool.waay.settings.presentation

public sealed interface SettingsEvent {
    public data object NavigateBack : SettingsEvent
}
