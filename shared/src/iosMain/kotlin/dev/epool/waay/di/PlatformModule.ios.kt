package dev.epool.waay.di

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.ObservableSettings
import dev.epool.waay.core.locale.DeviceLocale
import dev.epool.waay.core.speech.SilentSpeaker
import dev.epool.waay.core.speech.Speaker
import dev.epool.waay.core.speech.SpeechLanguage
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

internal actual val platformModule: Module =
    module {
        single<ObservableSettings> { NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults) }
        // Placeholders until US3 (AvSpeechSpeaker) and US5 (IosDeviceLocale).
        single<Speaker> { SilentSpeaker }
        single<DeviceLocale> {
            object : DeviceLocale {
                override fun current() = SpeechLanguage("en", null)
            }
        }
    }
