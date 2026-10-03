package dev.epool.waay.di

import dev.epool.waay.settings.presentation.SettingsViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

/**
 * The only way Swift obtains shared ViewModels (ADR-001, contracts/ios-bridge.md).
 * Each factory resolves through Koin and is owned by the given [ScreenScope], so Swift never sees
 * lifecycle types or generics. Swift: `ViewModelProvider.shared.…(scope:)`.
 * Factories are added per user story.
 */
public object ViewModelProvider : KoinComponent {
    public fun settingsViewModel(scope: ScreenScope): SettingsViewModel = scope.obtain { get<SettingsViewModel>() }
}
