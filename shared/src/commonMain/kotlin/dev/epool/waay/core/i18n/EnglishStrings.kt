package dev.epool.waay.core.i18n

internal object EnglishStrings : Strings {
    override val appTitle: String = "Wáay"
    override val settingsLabel: String = "Settings"
    override val settingsTitle: String = "Settings"
    override val backLabel: String = "Back"

    override fun intro(max: Int): String = "Think of a number from 1 to $max and let me guess it…"

    override val readyLabel: String = "I'm ready"

    override fun progress(
        current: Int,
        total: Int,
    ): String = "Card $current of $total"

    override val cardQuestion: String = "Is your number on this card?"
    override val yesLabel: String = "Yes"
    override val noLabel: String = "No"

    override fun reveal(number: Int): String = "The number you thought of is… $number!"

    override fun invalid(max: Int): String =
        "Hmm… no number from 1 to $max matches those answers. One of them may have been mistaken. Let's try again!"

    override val newGameLabel: String = "New game"

    override fun numberLabel(number: Int): String = number.toString()

    override val voiceLabel: String = "Magician's voice"
}
