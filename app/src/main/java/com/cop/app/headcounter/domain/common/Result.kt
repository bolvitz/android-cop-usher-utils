package com.cop.app.headcounter.domain.common

/**
 * A generic class that represents the result of an operation
 * Can be either Success with data or Error with error details
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val error: AppError) : Result<Nothing>()

    val isSuccess: Boolean
        get() = this is Success

    val isError: Boolean
        get() = this is Error

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }

    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw Exception(error.message)
    }

    fun getOrDefault(defaultValue: @UnsafeVariance T): T = when (this) {
        is Success -> data
        is Error -> defaultValue
    }

    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (AppError) -> Unit): Result<T> {
        if (this is Error) action(error)
        return this
    }

    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> Error(error)
    }

    inline fun <R> flatMap(transform: (T) -> Result<R>): Result<R> = when (this) {
        is Success -> transform(data)
        is Error -> Error(error)
    }
}

/**
 * Extension function to convert nullable values to Result
 */
fun <T> T?.toResult(errorIfNull: () -> AppError): Result<T> {
    return if (this != null) {
        Result.Success(this)
    } else {
        Result.Error(errorIfNull())
    }
}

/**
 * Extension function to wrap try-catch blocks
 */
inline fun <T> resultOf(block: () -> T): Result<T> {
    return try {
        Result.Success(block())
    } catch (e: Exception) {
        Result.Error(AppError.UnknownError(e.message ?: "Unknown error occurred"))
    }
}
