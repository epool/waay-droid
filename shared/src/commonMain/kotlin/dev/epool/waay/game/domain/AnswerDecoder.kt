package dev.epool.waay.game.domain

import dev.epool.waay.core.domain.Error
import dev.epool.waay.core.domain.Result

/** Decodes the answers through the hidden card mapping (FR-004, FR-010). */
internal object AnswerDecoder {
    fun decode(
        deck: Deck,
        answers: List<Answer>,
    ): Result<Int, DecodeError> {
        if (answers.size != deck.cards.size) return Result.Error(DecodeError.IncompleteAnswers)
        val number = deck.cards.zip(answers).sumOf { (card, answer) -> if (answer == Answer.Yes) card.bitValue else 0 }
        return if (number == 0) Result.Error(DecodeError.OutOfRange) else Result.Success(number)
    }
}

internal enum class DecodeError : Error {
    /** All answers were "No": 0 is outside 1…2^N − 1 (FR-005). */
    OutOfRange,

    /** Programming error: one answer per card is required. */
    IncompleteAnswers,
}
