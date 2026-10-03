package dev.epool.waay.core.domain

/**
 * Typed success/failure for expected outcomes. Expected failures are values, never exceptions.
 * Internal on purpose: no generics on the Swift-facing API (constitution V).
 */
internal sealed interface Result<out D, out E : dev.epool.waay.core.domain.Error> {
    data class Success<out D>(
        val data: D,
    ) : Result<D, Nothing>

    data class Error<out E : dev.epool.waay.core.domain.Error>(
        val error: E,
    ) : Result<Nothing, E>
}

internal typealias EmptyResult<E> = Result<Unit, E>

internal inline fun <T, E : Error, R> Result<T, E>.map(transform: (T) -> R): Result<R, E> =
    when (this) {
        is Result.Error -> Result.Error(error)
        is Result.Success -> Result.Success(transform(data))
    }

internal inline fun <T, E : Error> Result<T, E>.onSuccess(action: (T) -> Unit): Result<T, E> {
    if (this is Result.Success) action(data)
    return this
}

internal inline fun <T, E : Error> Result<T, E>.onFailure(action: (E) -> Unit): Result<T, E> {
    if (this is Result.Error) action(error)
    return this
}

internal fun <T, E : Error> Result<T, E>.asEmptyResult(): EmptyResult<E> = map { }
