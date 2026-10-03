package dev.epool.waay.game.domain

import dev.epool.waay.core.domain.Error
import dev.epool.waay.core.domain.Result
import kotlin.jvm.JvmInline

/** Number of cards in a game: 3–7 inclusive (FR-017), default 5 (FR-024). */
@JvmInline
internal value class CardCount private constructor(
    val value: Int,
) {
    /** Highest secret number: 2^N − 1 (FR-001). */
    val maxNumber: Int get() = (1 shl value) - 1

    /** Numbers on each card: 2^(N−1) (FR-002). */
    val numbersPerCard: Int get() = 1 shl (value - 1)

    companion object {
        const val MIN: Int = 3
        const val MAX: Int = 7
        val DEFAULT: CardCount = CardCount(5)
        val all: List<CardCount> = (MIN..MAX).map(::CardCount)

        fun of(value: Int): Result<CardCount, CardCountError> =
            if (value in MIN..MAX) Result.Success(CardCount(value)) else Result.Error(CardCountError.OutOfRange)
    }
}

internal enum class CardCountError : Error { OutOfRange }
