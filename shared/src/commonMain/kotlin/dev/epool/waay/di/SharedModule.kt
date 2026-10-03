package dev.epool.waay.di

import dev.epool.waay.settings.data.KeyValuePreferencesDataSource
import dev.epool.waay.settings.domain.PreferencesDataSource
import org.koin.dsl.module

/** Platform-independent dependency graph. Platform services come from [platformModule]. */
internal val sharedModule =
    module {
        single<PreferencesDataSource> { KeyValuePreferencesDataSource(get()) }
    }
