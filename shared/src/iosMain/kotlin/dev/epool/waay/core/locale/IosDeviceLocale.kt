package dev.epool.waay.core.locale

import dev.epool.waay.core.speech.SpeechLanguage
import platform.Foundation.NSLocale
import platform.Foundation.preferredLanguages

/** Reads the user's first preferred language (e.g. `es-MX`) on every call. */
internal class IosDeviceLocale : DeviceLocale {
    override fun current(): SpeechLanguage {
        val tag = NSLocale.preferredLanguages.firstOrNull() as? String ?: return SpeechLanguage("en", null)
        val parts = tag.split('-', '_')
        // Tags may carry a script (zh-Hans-CN); the region is the last 2-letter, upper-case part.
        val region = parts.drop(1).lastOrNull { it.length == 2 && it.all(Char::isUpperCase) }
        return SpeechLanguage(languageCode = parts.first().lowercase(), regionCode = region)
    }
}
