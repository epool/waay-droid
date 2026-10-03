package dev.epool.waay.core.locale

import dev.epool.waay.core.speech.SpeechLanguage
import java.util.Locale

/** Reads the current default locale on every call (Android updates it on configuration changes). */
internal class AndroidDeviceLocale : DeviceLocale {
    override fun current(): SpeechLanguage {
        val locale = Locale.getDefault()
        return SpeechLanguage(languageCode = locale.language, regionCode = locale.country.ifBlank { null })
    }
}
