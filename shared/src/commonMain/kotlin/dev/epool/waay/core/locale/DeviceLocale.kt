package dev.epool.waay.core.locale

import dev.epool.waay.core.speech.SpeechLanguage

/** Reads the device's current locale (contracts/platform-services.md). */
internal interface DeviceLocale {
    /** The current device locale, e.g. `("es", "MX")`. Read on each resolution. */
    fun current(): SpeechLanguage
}
