package dev.epool.waay.di

import dev.epool.waay.core.i18n.StringsProvider
import dev.epool.waay.game.domain.DeckFactory
import dev.epool.waay.game.domain.MagicDeck
import dev.epool.waay.game.presentation.GameViewModel
import dev.epool.waay.settings.data.KeyValuePreferencesDataSource
import dev.epool.waay.settings.domain.PreferencesDataSource
import dev.epool.waay.settings.presentation.SettingsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import kotlin.random.Random

/** Platform-independent dependency graph. Platform services come from [platformModule]. */
internal val sharedModule =
    module {
        single<PreferencesDataSource> { KeyValuePreferencesDataSource(get()) }
        single { StringsProvider(get()) }
        // Randomness is injected (constitution IV); tests use seeded Random instances instead.
        single<Random> { Random.Default }
        single { DeckFactory { cardCount -> MagicDeck.create(cardCount, get()) } }
        viewModelOf(::SettingsViewModel)
        viewModelOf(::GameViewModel)
    }
