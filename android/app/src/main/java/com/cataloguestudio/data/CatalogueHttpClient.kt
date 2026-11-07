package com.cataloguestudio.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.accept
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import java.io.IOException

internal fun createCatalogueHttpEngine(): HttpClientEngine = OkHttp.create {
    config {
        // A failed enquiry must be retried explicitly by the user.
        retryOnConnectionFailure(false)
    }
}

internal fun createCatalogueHttpClient(baseUrl: String, engine: HttpClientEngine): HttpClient =
    HttpClient(engine) {
        // safeRequest handles status codes explicitly, before decoding a success body.
        expectSuccess = false
        followRedirects = false
        install(ContentNegotiation) { json(catalogueJson) }
        install(HttpRequestRetry) {
            // Recover a read after a broken connection, without replaying an enquiry.
            retryOnExceptionIf(maxRetries = 1) { request, cause ->
                request.method == HttpMethod.Get && cause is IOException &&
                    cause !is ConnectTimeoutException && cause !is SocketTimeoutException
            }
            retryIf { _, _ -> false }
            delayMillis(respectRetryAfterHeader = false) { 0L }
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 5_000
            socketTimeoutMillis = 15_000
        }
        defaultRequest {
            url(baseUrl)
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
        }
    }
