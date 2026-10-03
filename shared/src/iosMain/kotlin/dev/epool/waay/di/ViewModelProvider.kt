package dev.epool.waay.di

import org.koin.core.component.KoinComponent

/**
 * The only way Swift obtains shared ViewModels (ADR-001, contracts/ios-bridge.md).
 * Each factory resolves through Koin and is owned by the given [ScreenScope], so Swift never sees
 * lifecycle types or generics. Swift: `ViewModelProvider.shared.…(scope:)`.
 * Factories are added per user story.
 */
public object ViewModelProvider : KoinComponent
