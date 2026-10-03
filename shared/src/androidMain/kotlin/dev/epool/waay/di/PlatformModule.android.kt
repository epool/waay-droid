package dev.epool.waay.di

import android.content.Context
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.SharedPreferencesSettings
import dev.epool.waay.core.locale.AndroidDeviceLocale
import dev.epool.waay.core.locale.DeviceLocale
import dev.epool.waay.core.speech.Speaker
import dev.epool.waay.core.speech.TextToSpeechSpeaker
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val platformModule: Module =
    module {
        single<ObservableSettings> {
            SharedPreferencesSettings(androidContext().getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE))
        }
        single<Speaker> { TextToSpeechSpeaker(androidContext()) }
        single<DeviceLocale> { AndroidDeviceLocale() }
    }

private const val PREFERENCES_FILE = "waay_preferences"
