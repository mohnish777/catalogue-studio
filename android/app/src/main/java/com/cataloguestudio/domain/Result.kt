package com.cataloguestudio.domain

interface AppError

sealed interface Result<out D, out E : AppError> {
    data class Success<out D>(val data: D) : Result<D, Nothing>

    data class Error<out E : AppError>(val error: E) : Result<Nothing, E>
}

enum class DataError : AppError {
    NO_INTERNET,
    REQUEST_TIMEOUT,
    BAD_REQUEST,
    NOT_FOUND,
    SERVER_ERROR,
    SERIALIZATION,
    UNKNOWN,
}
