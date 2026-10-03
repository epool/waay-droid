package dev.epool.waay.core.domain

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test

class ResultTest {
    private enum class TestError : Error { Boom }

    private val success: Result<Int, TestError> = Result.Success(2)
    private val failure: Result<Int, TestError> = Result.Error(TestError.Boom)

    @Test
    fun mapTransformsOnlySuccess() {
        assertThat(success.map { it * 10 }).isEqualTo(Result.Success(20))
        assertThat(failure.map { it * 10 }).isEqualTo(Result.Error(TestError.Boom))
    }

    @Test
    fun onSuccessRunsOnlyForSuccessAndReturnsTheSameResult() {
        var seen: Int? = null
        assertThat(success.onSuccess { seen = it }).isEqualTo(success)
        assertThat(seen).isEqualTo(2)

        seen = null
        failure.onSuccess { seen = it }
        assertThat(seen).isNull()
    }

    @Test
    fun onFailureRunsOnlyForErrorAndReturnsTheSameResult() {
        var seen: TestError? = null
        assertThat(failure.onFailure { seen = it }).isEqualTo(failure)
        assertThat(seen).isEqualTo(TestError.Boom)

        seen = null
        success.onFailure { seen = it }
        assertThat(seen).isNull()
    }

    @Test
    fun asEmptyResultDropsTheDataButKeepsTheError() {
        assertThat(success.asEmptyResult()).isEqualTo(Result.Success(Unit))
        assertThat(failure.asEmptyResult()).isEqualTo(Result.Error(TestError.Boom))
    }
}
