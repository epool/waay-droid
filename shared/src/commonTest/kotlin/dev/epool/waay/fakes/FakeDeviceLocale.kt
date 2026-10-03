package dev.epool.waay.fakes

import dev.epool.waay.core.locale.DeviceLocale
import dev.epool.waay.core.speech.SpeechLanguage

internal class FakeDeviceLocale(
    var locale: SpeechLanguage = SpeechLanguage("en", "US"),
) : DeviceLocale {
    override fun current(): SpeechLanguage = locale
}
