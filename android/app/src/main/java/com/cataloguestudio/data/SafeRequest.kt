package com.cataloguestudio.data

import com.cataloguestudio.domain.DataError
import com.cataloguestudio.domain.Result
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.statement.HttpResponse
import io.ktor.serialization.ContentConvertException
import io.ktor.util.network.UnresolvedAddressException
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.SerializationException

internal suspend fun <T> safeRequest(
    execute: suspend () -> HttpResponse,
    decode: suspend (HttpResponse) -> T,
): Result<T, DataError> = try {
    val response = execute()
    when (response.status.value) {
        in 200..299 -> Result.Success(decode(response))
        400 -> Result.Error(DataError.BAD_REQUEST)
        404 -> Result.Error(DataError.NOT_FOUND)
        408, 504 -> Result.Error(DataError.REQUEST_TIMEOUT)
        in 500..599 -> Result.Error(DataError.SERVER_ERROR)
        else -> Result.Error(DataError.UNKNOWN)
    }
} catch (error: Exception) {
    // Also preserves cancellation when an engine reports it as another exception.
    currentCoroutineContext().ensureActive()
    val failure = when (error) {
        is CancellationException -> throw error
        is HttpRequestTimeoutException, is ConnectTimeoutException,
        is SocketTimeoutException -> DataError.REQUEST_TIMEOUT
        is SerializationException, is ContentConvertException,
        is NoTransformationFoundException -> DataError.SERIALIZATION
        is UnresolvedAddressException, is IOException -> DataError.NO_INTERNET
        else -> DataError.UNKNOWN
    }
    Result.Error(failure)
}
