package dev.epool.waay.android

import android.app.Application
import dev.epool.waay.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import kotlin.random.Random

/**
 * [WaayApp] with a seeded deal, for screenshots of a real game: the app deals with `Random.Default`,
 * so without a seed every run shows different numbers (constitution VI: seeded Random in tests).
 */
class SeededWaayApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin { androidContext(this@SeededWaayApp) }
        loadKoinModules(module { single<Random> { Random(7) } })
    }
}
