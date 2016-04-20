package mx.eduardopool.waaydroid

import java.util.*

/**
 * Cards generator.
 * Created by EduardoPool on 17/08/14.
 */
class MagicCardsGenerator(val cardsNumber: Int) {
    private var maxNumber: Int = 0
    val cards: MutableList<List<Int>> = ArrayList(cardsNumber)

    init {
        calculateCardsNumbers()
    }

    fun calculateCardsNumbers() {
        cards.clear()
        maxNumber = Math.pow(2.0, cardsNumber.toDouble()).toInt()
        for (cardIndex in 0..cardsNumber - 1) {
            cards.add(calculateNumbersForCardIndex(cardIndex))
        }
    }

    private fun calculateNumbersForCardIndex(cardIndex: Int): List<Int> {
        val card = ArrayList<Int>()
        val jumpNumber = Math.pow(2.0, cardIndex.toDouble()).toInt()
        var isAdd = true
        var numberToAdd = jumpNumber
        var count = 0
        while (numberToAdd < maxNumber) {
            if (count == jumpNumber) {
                count = 0
                isAdd = !isAdd
            }
            if (isAdd) {
                card.add(numberToAdd)
            }
            numberToAdd++
            count++
        }
        return card
    }

}
