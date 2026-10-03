package dev.epool.waay.di

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.ObservableSettings
import dev.epool.waay.core.locale.DeviceLocale
import dev.epool.waay.core.speech.AvSpeechSpeaker
import dev.epool.waay.core.speech.Speaker
import dev.epool.waay.core.speech.SpeechLanguage
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

internal actual val platformModule: Module =
    module {
        single<ObservableSettings> { NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults) }
        single<Speaker> { AvSpeechSpeaker() }
        // Placeholder until US5 (IosDeviceLocale).
        single<DeviceLocale> {
            object : DeviceLocale {
                override fun current() = SpeechLanguage("en", null)
            }
        }
    }
