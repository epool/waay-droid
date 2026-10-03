package dev.epool.waay.android

import android.app.Application
import dev.epool.waay.di.initKoin
import org.koin.android.ext.koin.androidContext

class WaayApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin { androidContext(this@WaayApp) }
    }
}
