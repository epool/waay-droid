package dev.epool.waay.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

/**
 * Starts the shared dependency graph. Called once from the Android `Application` and from the iOS
 * `App` initializer (Swift: `InitKoinKt.doInitKoin(config: nil)`).
 */
public fun initKoin(config: KoinAppDeclaration? = null) {
    startKoin {
        config?.invoke(this)
        modules(sharedModule, platformModule)
    }
}
