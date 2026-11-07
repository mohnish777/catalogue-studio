package com.cataloguestudio.data

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import com.cataloguestudio.domain.CatalogueMode
import com.cataloguestudio.domain.CatalogueRemoteDataSource
import com.cataloguestudio.domain.DataError
import com.cataloguestudio.domain.Enquiry
import com.cataloguestudio.domain.Result
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class KtorCatalogueDataSourceTest {
    private val baseUrl = "http://catalogue.test:3000"
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    private val productBody = """
        {"id":9,"title":"Portable drive","description":"Everyday storage","price":64.99,
         "category":"electronics","imageUrl":"/images/9.jpg","rating":4.7,"reviewCount":203}
    """.trimIndent()
    private val catalogueBody = """
        {"mode":"demo","categories":["electronics"],"featured":[],"remaining":[$productBody]}
    """.trimIndent()

    private suspend fun TestScope.withSource(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
        verify: suspend (CatalogueRemoteDataSource) -> Unit,
    ) {
        val engine = MockEngine(MockEngineConfig().apply {
            dispatcher = UnconfinedTestDispatcher(testScheduler)
            addHandler(handler)
        })
        val client = createCatalogueHttpClient(baseUrl, engine)
        try {
            verify(KtorCatalogueDataSource(client, baseUrl))
        } finally {
            client.close()
            engine.close()
        }
    }

    @Test
    fun `catalogue uses the correct GET route and omits absent filters`() = runTest {
        withSource({ request ->
            assertThat(request.method).isEqualTo(HttpMethod.Get)
            assertThat(request.url.toString()).isEqualTo("$baseUrl/api/catalogue")
            respond(catalogueBody, HttpStatusCode.OK, jsonHeaders)
        }) { source ->
            when (val result = source.getCatalogue()) {
                is Result.Success -> {
                    assertThat(result.data.mode).isEqualTo(CatalogueMode.DEMO)
                    assertThat(result.data.remaining.single().id).isEqualTo(9L)
                    assertThat(result.data.remaining.single().imageUrl).isEqualTo("$baseUrl/images/9.jpg")
                }
                is Result.Error -> error("Expected catalogue success, got ${result.error}")
            }
        }
    }

    @Test
    fun `category and query travel as encoded parameters without changing their values`() = runTest {
        val category = "men's clothing"
        val query = "canvas & cotton + café?"
        withSource({ request ->
            assertThat(request.url.encodedPath).isEqualTo("/api/catalogue")
            assertThat(request.url.parameters["category"]).isEqualTo(category)
            assertThat(request.url.parameters["q"]).isEqualTo(query)
            assertThat(request.url.parameters.names()).isEqualTo(setOf("category", "q"))
            respond(catalogueBody, HttpStatusCode.OK, jsonHeaders)
        }) { source ->
            assertThat(source.getCatalogue(category, query) is Result.Success).isEqualTo(true)
        }
    }

    @Test
    fun `each filter can be sent independently and blank search remains explicit`() = runTest {
        for ((category, query) in listOf("electronics" to null, null to "portable", null to "")) {
            withSource({ request ->
                assertThat(request.url.parameters["category"]).isEqualTo(category)
                assertThat(request.url.parameters["q"]).isEqualTo(query)
                respond(catalogueBody, HttpStatusCode.OK, jsonHeaders)
            }) { source ->
                source.getCatalogue(category, query)
            }
        }
    }

    @Test
    fun `detail uses the selected product ID and maps its response`() = runTest {
        withSource({ request ->
            assertThat(request.method).isEqualTo(HttpMethod.Get)
            assertThat(request.url.toString()).isEqualTo("$baseUrl/api/products/9")
            respond(productBody, HttpStatusCode.OK, jsonHeaders)
        }) { source ->
            when (val result = source.getProduct(9)) {
                is Result.Success -> {
                    assertThat(result.data.id).isEqualTo(9L)
                    assertThat(result.data.title).isEqualTo("Portable drive")
                    assertThat(result.data.imageUrl).isEqualTo("$baseUrl/images/9.jpg")
                }
                is Result.Error -> error("Expected product success, got ${result.error}")
            }
        }
    }

    @Test
    fun `enquiry sends exactly the planned JSON fields and returns Unit on server success`() = runTest {
        val enquiry = Enquiry(9, "Shraddha \"S\"", "shraddha+catalogue@example.test", "Hello\nIs this available?")
        var requests = 0
        withSource({ request ->
            requests++
            assertThat(request.method).isEqualTo(HttpMethod.Post)
            assertThat(request.url.toString()).isEqualTo("$baseUrl/api/enquiries")
            assertThat(request.body.contentType?.withoutParameters()).isEqualTo(ContentType.Application.Json)
            val body = catalogueJson.parseToJsonElement(request.body.toByteArray().decodeToString())
            assertThat(body).isEqualTo(buildJsonObject {
                put("productId", 9)
                put("name", enquiry.name)
                put("email", enquiry.email)
                put("message", enquiry.message)
            })
            respond("", HttpStatusCode.Created)
        }) { source ->
            assertThat(source.sendEnquiry(enquiry)).isEqualTo(Result.Success(Unit))
            assertThat(requests).isEqualTo(1)
        }
    }

    @Test
    fun `optional empty message is represented explicitly in the enquiry JSON`() = runTest {
        withSource({ request ->
            val body = catalogueJson.parseToJsonElement(request.body.toByteArray().decodeToString())
            assertThat(body).isEqualTo(buildJsonObject {
                put("productId", 9)
                put("name", "Alex")
                put("email", "alex@example.test")
                put("message", "")
            })
            respond("", HttpStatusCode.Created)
        }) { source ->
            assertThat(source.sendEnquiry(Enquiry(9, "Alex", "alex@example.test")))
                .isEqualTo(Result.Success(Unit))
        }
    }

    @Test
    fun `endpoint errors pass through typed handling including an enquiry endpoint not yet present`() = runTest {
        withSource({ request ->
            val status = when (request.url.encodedPath) {
                "/api/catalogue" -> HttpStatusCode.InternalServerError
                "/api/products/999" -> HttpStatusCode.NotFound
                "/api/enquiries" -> HttpStatusCode.NotFound
                else -> error("Unexpected request ${request.url}")
            }
            respond("not a success DTO", status)
        }) { source ->
            assertThat(source.getCatalogue()).isEqualTo(Result.Error(DataError.SERVER_ERROR))
            assertThat(source.getProduct(999)).isEqualTo(Result.Error(DataError.NOT_FOUND))
            assertThat(source.sendEnquiry(Enquiry(9, "Alex", "alex@example.test")))
                .isEqualTo(Result.Error(DataError.NOT_FOUND))
        }
    }

    @Test
    fun `an enquiry server failure is never treated as success and is not retried`() = runTest {
        var requests = 0
        withSource({
            requests++
            respond("Unavailable", HttpStatusCode.ServiceUnavailable)
        }) { source ->
            assertThat(source.sendEnquiry(Enquiry(9, "Alex", "alex@example.test")))
                .isEqualTo(Result.Error(DataError.SERVER_ERROR))
            assertThat(requests).isEqualTo(1)
        }
    }

    @Test
    fun `catalogue and detail mapping failures remain typed serialization errors`() = runTest {
        withSource({ request ->
            val body = when (request.url.encodedPath) {
                "/api/catalogue" -> catalogueBody.replace("demo", "preview")
                "/api/products/9" -> productBody.replace("/images/9.jpg", "https://outside.test/9.jpg")
                else -> error("Unexpected request ${request.url}")
            }
            respond(body, HttpStatusCode.OK, jsonHeaders)
        }) { source ->
            assertThat(source.getCatalogue()).isEqualTo(Result.Error(DataError.SERIALIZATION))
            assertThat(source.getProduct(9)).isEqualTo(Result.Error(DataError.SERIALIZATION))
        }
    }

    @Test
    fun `a caller can cancel a data-source request without receiving an error result`() = runTest {
        withSource({ awaitCancellation() }) { source ->
            var returned = false
            val job = launch {
                source.getCatalogue()
                returned = true
            }
            runCurrent()
            job.cancelAndJoin()
            assertThat(returned).isFalse()
        }
    }
}
