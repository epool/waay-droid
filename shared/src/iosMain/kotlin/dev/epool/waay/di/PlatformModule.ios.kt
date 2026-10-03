package dev.epool.waay.di

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.ObservableSettings
import dev.epool.waay.core.locale.DeviceLocale
import dev.epool.waay.core.locale.IosDeviceLocale
import dev.epool.waay.core.speech.AvSpeechSpeaker
import dev.epool.waay.core.speech.Speaker
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

internal actual val platformModule: Module =
    module {
        single<ObservableSettings> { NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults) }
        single<Speaker> { AvSpeechSpeaker() }
        single<DeviceLocale> { IosDeviceLocale() }
    }
