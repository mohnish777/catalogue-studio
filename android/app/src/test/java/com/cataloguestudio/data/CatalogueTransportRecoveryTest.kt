package com.cataloguestudio.data

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.cataloguestudio.domain.Catalogue
import com.cataloguestudio.domain.CatalogueMode
import com.cataloguestudio.domain.DataError
import com.cataloguestudio.domain.Enquiry
import com.cataloguestudio.domain.Result
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.SocketEffect
import org.junit.jupiter.api.Test

class CatalogueTransportRecoveryTest {
    private val catalogueBody = """{"mode":"demo","categories":[],"featured":[],"remaining":[]}"""
    private val expectedCatalogue = Result.Success(Catalogue(CatalogueMode.DEMO, emptyList(), emptyList(), emptyList()))

    @Test
    fun `GET recovers once when a reused connection ends before response headers`() = runBlocking {
        withServer { server, source ->
            server.enqueue(jsonResponse())
            server.enqueue(disconnectBeforeHeaders())
            server.enqueue(jsonResponse())
            assertThat(source.getCatalogue()).isEqualTo(expectedCatalogue)
            assertThat(source.getCatalogue("electronics", "drive")).isEqualTo(expectedCatalogue)
            assertThat(server.requestCount).isEqualTo(3)
            server.takeRequest()
            val failed = server.takeRequest()
            val recovered = server.takeRequest()
            assertThat(failed.connectionIndex).isEqualTo(0)
            assertThat(recovered.connectionIndex).isEqualTo(1)
            assertThat(failed.method).isEqualTo("GET")
            assertThat(recovered.target).isEqualTo(failed.target)
            assertThat(recovered.url.queryParameter("category")).isEqualTo("electronics")
            assertThat(recovered.url.queryParameter("q")).isEqualTo("drive")
        }
    }

    @Test
    fun `GET stops after one recovery attempt when the connection fails again`() = runBlocking {
        withServer { server, source ->
            server.enqueue(disconnectBeforeHeaders())
            server.enqueue(disconnectBeforeHeaders())
            server.enqueue(jsonResponse())
            assertThat(source.getCatalogue()).isEqualTo(Result.Error(DataError.NO_INTERNET))
            assertThat(server.requestCount).isEqualTo(2)
        }
    }

    @Test
    fun `enquiry is not replayed when server receives its body but closes before acknowledgement`() = runBlocking {
        withServer { server, source ->
            server.enqueue(disconnectBeforeHeaders())
            server.enqueue(MockResponse.Builder().code(201).build())
            val enquiry = Enquiry(9, "Alex", "alex@example.com", "Please share details")
            assertThat(source.sendEnquiry(enquiry)).isEqualTo(Result.Error(DataError.NO_INTERNET))
            assertThat(server.requestCount).isEqualTo(1)
            val request = server.takeRequest()
            assertThat(request.method).isEqualTo("POST")
            assertThat(request.target).isEqualTo("/api/enquiries")
            assertThat(request.body!!.utf8()).isEqualTo(
                """{"productId":9,"name":"Alex","email":"alex@example.com","message":"Please share details"}""",
            )
        }
    }

    @Test
    fun `server errors are returned without an automatic GET retry`() = runBlocking {
        withServer { server, source ->
            server.enqueue(MockResponse.Builder().code(503).body("{}").build())
            server.enqueue(jsonResponse())
            assertThat(source.getCatalogue()).isEqualTo(Result.Error(DataError.SERVER_ERROR))
            assertThat(server.requestCount).isEqualTo(1)
        }
    }

    private fun jsonResponse() = MockResponse.Builder()
        .code(200).addHeader("Content-Type", "application/json").body(catalogueBody).build()

    private fun disconnectBeforeHeaders() = MockResponse.Builder()
        .onResponseStart(SocketEffect.CloseSocket()).build()

    private suspend fun withServer(block: suspend (MockWebServer, KtorCatalogueDataSource) -> Unit) {
        val server = MockWebServer()
        server.start()
        val baseUrl = server.url("/").toString().trimEnd('/')
        val engine = createCatalogueHttpEngine()
        val client = createCatalogueHttpClient(baseUrl, engine)
        try {
            withTimeout(5_000) { block(server, KtorCatalogueDataSource(client, baseUrl)) }
        } finally {
            client.close()
            engine.close()
            server.close()
        }
    }
}
