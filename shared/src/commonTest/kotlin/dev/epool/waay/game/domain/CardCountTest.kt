package dev.epool.waay.game.domain

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import dev.epool.waay.core.domain.Result
import kotlin.test.Test

class CardCountTest {
    @Test
    fun acceptsThreeToSevenInclusiveWithDerivedRangeAndCardSize() {
        val expected = mapOf(3 to (7 to 4), 4 to (15 to 8), 5 to (31 to 16), 6 to (63 to 32), 7 to (127 to 64))
        expected.forEach { (value, derived) ->
            val count = (CardCount.of(value) as Result.Success).data
            assertThat(count.value).isEqualTo(value)
            assertThat(count.maxNumber).isEqualTo(derived.first)
            assertThat(count.numbersPerCard).isEqualTo(derived.second)
        }
    }

    @Test
    fun rejectsValuesOutsideThreeToSeven() {
        listOf(Int.MIN_VALUE, 0, 2, 8, 100).forEach { value ->
            val result = CardCount.of(value)
            assertThat(result).isInstanceOf(Result.Error::class)
            assertThat((result as Result.Error).error).isEqualTo(CardCountError.OutOfRange)
        }
    }

    @Test
    fun defaultIsFive() {
        assertThat(CardCount.DEFAULT.value).isEqualTo(5)
    }
}
