package com.cataloguestudio.data.di

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isSameInstanceAs
import com.cataloguestudio.data.KtorCatalogueDataSource
import com.cataloguestudio.domain.CatalogueRemoteDataSource
import com.cataloguestudio.domain.DataError
import com.cataloguestudio.domain.Result
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.jupiter.api.Test
import org.koin.dsl.koinApplication

class CatalogueDataModuleTest {
    private val catalogueBody = """
        {"mode":"demo","categories":["electronics"],"featured":[],"remaining":[
          {"id":9,"title":"Portable drive","description":"Everyday storage","price":64.99,
           "category":"electronics","imageUrl":"/images/9.jpg","rating":4.7,"reviewCount":203}
        ]}
    """.trimIndent()

    @Test
    fun `registering and closing an unused module does not create an engine`() {
        var engineCreations = 0
        val application = koinApplication {
            modules(catalogueDataModule("http://unused.test") {
                engineCreations++
                error("Dependencies should be lazy")
            })
        }
        application.close()
        assertThat(engineCreations).isEqualTo(0)
    }

    @Test
    fun `bindings share one engine client and source and close their resources`() = runBlocking {
        val baseUrl = "http://catalogue.test:4321"
        var engineCreations = 0
        var requests = 0
        val application = koinApplication {
            modules(catalogueDataModule(baseUrl) {
                engineCreations++
                MockEngine { request ->
                    requests++
                    assertThat(request.url.toString()).isEqualTo("$baseUrl/api/catalogue")
                    respond(catalogueBody, headers = headersOf(HttpHeaders.ContentType, "application/json"))
                }
            })
        }
        val koin = application.koin
        val source = koin.get<CatalogueRemoteDataSource>()
        val client = koin.get<HttpClient>()
        val engine = koin.get<HttpClientEngine>()
        try {
            assertThat(source).isInstanceOf(KtorCatalogueDataSource::class)
            assertThat(koin.get<CatalogueRemoteDataSource>()).isSameInstanceAs(source)
            assertThat(koin.get<HttpClient>()).isSameInstanceAs(client)
            assertThat(koin.get<HttpClientEngine>()).isSameInstanceAs(engine)
            assertThat(engineCreations).isEqualTo(1)
            assertThat(requests).isEqualTo(0)
            when (val result = source.getCatalogue()) {
                is Result.Success -> assertThat(result.data.remaining.single().imageUrl)
                    .isEqualTo("$baseUrl/images/9.jpg")
                is Result.Error -> error("Expected catalogue success, got ${result.error}")
            }
            assertThat(requests).isEqualTo(1)
        } finally {
            application.close()
        }
        withTimeout(5_000) {
            client.coroutineContext[Job]!!.join()
            engine.coroutineContext[Job]!!.join()
        }
    }

    @Test
    fun `production engine resolves through Koin and talks to a local HTTP server`() = runBlocking {
        val server = MockWebServer()
        server.enqueue(MockResponse.Builder()
            .code(200)
            .addHeader("Content-Type", "application/json")
            .body(catalogueBody)
            .build())
        server.enqueue(MockResponse.Builder().code(404).body("{}").build())
        server.start()
        val baseUrl = server.url("/").toString().trimEnd('/')
        val application = koinApplication { modules(catalogueDataModule(baseUrl)) }
        try {
            val source = application.koin.get<CatalogueRemoteDataSource>()
            withTimeout(5_000) {
                when (val result = source.getCatalogue()) {
                    is Result.Success -> assertThat(result.data.remaining.single().imageUrl)
                        .isEqualTo("$baseUrl/images/9.jpg")
                    is Result.Error -> error("Expected catalogue success, got ${result.error}")
                }
                assertThat(source.getProduct(999)).isEqualTo(Result.Error(DataError.NOT_FOUND))
            }
            assertThat(server.requestCount).isEqualTo(2)
        } finally {
            application.close()
            server.close()
        }
    }
}
