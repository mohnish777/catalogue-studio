package com.cataloguestudio.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.accept
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json

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
