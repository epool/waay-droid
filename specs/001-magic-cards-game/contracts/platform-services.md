# Contract: Platform services (common interfaces with platform implementations)

These are the interfaces `commonMain` depends on. Implementations live in `androidMain` and
`iosMain`, are wired through Koin's `platformModule`, and are faked in tests.

```kotlin
// dev.epool.waay.core.speech
public interface Speaker {
    /** Interrupts any utterance in progress, then speaks [text] with a voice for [language]. Never throws. */
    public fun speak(text: String, language: SpeechLanguage)
    /** Stops immediately. Idempotent. */
    public fun stop()
}
public data class SpeechLanguage(val languageCode: String, val regionCode: String?) // e.g. ("es", "MX")

// dev.epool.waay.core.locale
public interface DeviceLocale {
    /** The current device locale, e.g. ("es", "MX"). Read on each resolution. */
    public fun current(): SpeechLanguage
}

// dev.epool.waay.settings.domain
public interface PreferencesDataSource {
    public val preferences: Flow<Preferences>          // emits current value first
    public suspend fun setCardCount(value: CardCount)
    public suspend fun setVoiceEnabled(enabled: Boolean)
    public suspend fun setLanguageChoice(choice: LanguageChoice)
}
```

| Service | Android | iOS | Test fake |
|---|---|---|---|
| `Speaker` | `TextToSpeechSpeaker`. `TextToSpeech` with `QUEUE_FLUSH`, a pending utterance until init, and the `TTS_SERVICE` `<queries>` entry in the manifest. | `AvSpeechSpeaker`. `AVSpeechSynthesizer`, with `stopSpeaking(.immediate)` before speaking and the regional voice preferred. | `FakeSpeaker`, which records calls. |
| `DeviceLocale` | `AndroidDeviceLocale` (`Locale.getDefault()`) | `IosDeviceLocale` (`NSLocale.preferredLanguages`) | `FakeDeviceLocale` |
| `PreferencesDataSource` | `KeyValuePreferencesDataSource` over `SharedPreferencesSettings` | the same class over `NSUserDefaultsSettings` | the same class over `MapSettings` |

**Failure semantics** (FR-016): speech failures are logged with Kermit and swallowed. The game never
shows a blocking error because of speech.
