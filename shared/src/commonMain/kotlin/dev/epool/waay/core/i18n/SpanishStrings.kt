package dev.epool.waay.core.i18n

internal object SpanishStrings : Strings {
    override val appTitle: String = "Wáay"
    override val settingsLabel: String = "Ajustes"
    override val settingsTitle: String = "Ajustes"
    override val backLabel: String = "Atrás"

    override fun intro(max: Int): String = "Piensa en un número del 1 al $max y déjame adivinarlo…"

    override val readyLabel: String = "Estoy listo"

    override fun progress(
        current: Int,
        total: Int,
    ): String = "Carta $current de $total"

    override val cardQuestion: String = "¿Está tu número en esta carta?"
    override val yesLabel: String = "Sí"
    override val noLabel: String = "No"

    override fun reveal(number: Int): String = "El número que pensaste es… ¡$number!"

    override fun invalid(max: Int): String =
        "Mmm… ningún número del 1 al $max coincide con esas respuestas. Quizás alguna estuvo equivocada. ¡Intentémoslo de nuevo!"

    override val newGameLabel: String = "Nuevo juego"

    override fun numberLabel(number: Int): String = number.toString()

    override val voiceLabel: String = "Voz del mago"

    override val cardCountLabel: String = "Número de cartas"

    override fun cardCountOption(
        count: Int,
        max: Int,
    ): String = "$count cartas (1–$max)"

    override val languageLabel: String = "Idioma"
    override val languageDevice: String = "Idioma del dispositivo"
    override val languageEnglish: String = "English"
    override val languageSpanish: String = "Español"
}
