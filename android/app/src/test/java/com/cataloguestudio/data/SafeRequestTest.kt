package com.cataloguestudio.data

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isSameInstanceAs
import com.cataloguestudio.domain.CatalogueMode
import com.cataloguestudio.domain.DataError
import com.cataloguestudio.domain.Result
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.util.network.UnresolvedAddressException
import java.net.ConnectException
import java.net.UnknownHostException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SafeRequestTest {
    private val baseUrl = "http://catalogue.test:3000"
    private val catalogueBody = """{"mode":"demo","categories":[],"featured":[],"remaining":[],"extra":true}"""
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private suspend fun TestScope.withClient(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
        verify: suspend (HttpClient) -> Unit,
    ) {
        val engine = MockEngine(MockEngineConfig().apply {
            dispatcher = UnconfinedTestDispatcher(testScheduler)
            addHandler(handler)
        })
        val client = createCatalogueHttpClient(baseUrl, engine)
        try {
            verify(client)
        } finally {
            client.close()
            engine.close()
        }
    }

    @Test
    fun `client uses backend URL and JSON negotiation to decode and map success`() = runTest {
        withClient({ request ->
            assertThat(request.url.toString()).isEqualTo("$baseUrl/api/catalogue")
            assertThat(request.headers[HttpHeaders.Accept]).isEqualTo("application/json")
            respond(catalogueBody, HttpStatusCode.OK, jsonHeaders)
        }) { client ->
            val result = safeRequest(
                execute = { client.get("/api/catalogue") },
                decode = { it.body<CatalogueDto>().toCatalogue(baseUrl) },
            )
            when (result) {
                is Result.Success -> assertThat(result.data.mode).isEqualTo(CatalogueMode.DEMO)
                is Result.Error -> error("Expected success, got ${result.error}")
            }
        }
    }

    @Test
    fun `success without a domain payload returns Unit`() = runTest {
        for (status in listOf(HttpStatusCode.Created, HttpStatusCode.NoContent)) {
            withClient({ respond("", status) }) { client ->
                val result = safeRequest(execute = { client.post("/example") }, decode = { Unit })
                assertThat(result).isEqualTo(Result.Success(Unit))
            }
        }
    }

    @Test
    fun `HTTP errors keep their status meaning without decoding error bodies`() = runTest {
        for ((status, expected) in listOf(
            400 to DataError.BAD_REQUEST, 404 to DataError.NOT_FOUND,
            408 to DataError.REQUEST_TIMEOUT, 504 to DataError.REQUEST_TIMEOUT,
            500 to DataError.SERVER_ERROR, 502 to DataError.SERVER_ERROR,
            503 to DataError.SERVER_ERROR, 401 to DataError.UNKNOWN, 429 to DataError.UNKNOWN,
        )) {
            var decoded = false
            withClient({ respond("not a success DTO", HttpStatusCode.fromValue(status)) }) { client ->
                val result = safeRequest(
                    execute = { client.get("/example") },
                    decode = { decoded = true; it.body<CatalogueDto>() },
                )
                assertThat(result, "HTTP $status").isEqualTo(Result.Error(expected))
                assertThat(decoded).isFalse()
            }
        }
    }

    @Test
    fun `redirects are surfaced instead of following another URL`() = runTest {
        var requests = 0
        withClient({
            requests++
            respond("", HttpStatusCode.Found, headersOf(HttpHeaders.Location, "https://other.test/"))
        }) { client ->
            assertThat(safeRequest(execute = { client.get("/example") }, decode = { Unit }))
                .isEqualTo(Result.Error(DataError.UNKNOWN))
            assertThat(requests).isEqualTo(1)
        }
    }

    @Test
    fun `malformed JSON missing fields and mapper failures become serialization errors`() = runTest {
        for (body in listOf("not JSON", "{}", catalogueBody.replace("demo", "preview"))) {
            withClient({ respond(body, HttpStatusCode.OK, jsonHeaders) }) { client ->
                val result = safeRequest(
                    execute = { client.get("/api/catalogue") },
                    decode = { it.body<CatalogueDto>().toCatalogue(baseUrl) },
                )
                assertThat(result).isEqualTo(Result.Error(DataError.SERIALIZATION))
            }
        }
    }

    @Test
    fun `unsupported success content types become serialization errors`() = runTest {
        withClient({ respond("<html/>", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/html")) }) { client ->
            val result = safeRequest(
                execute = { client.get("/example") },
                decode = { it.body<CatalogueDto>() },
            )
            assertThat(result).isEqualTo(Result.Error(DataError.SERIALIZATION))
        }
    }

    @Test
    fun `DNS and connection failures become connection errors`() = runTest {
        for (failure in listOf(UnknownHostException("Unavailable"), ConnectException("Refused"), UnresolvedAddressException())) {
            withClient({ throw failure }) { client ->
                assertThat(safeRequest(execute = { client.get("/example") }, decode = { Unit }))
                    .isEqualTo(Result.Error(DataError.NO_INTERNET))
            }
        }
    }

    @Test
    fun `connect and socket timeouts become timeout errors`() = runTest {
        for (failure in listOf(ConnectTimeoutException("Connect timed out", null), SocketTimeoutException("Read timed out"))) {
            withClient({ throw failure }) { client ->
                assertThat(safeRequest(execute = { client.get("/example") }, decode = { Unit }))
                    .isEqualTo(Result.Error(DataError.REQUEST_TIMEOUT))
            }
        }
    }

    @Test
    fun `configured request timeout stops a delayed response after 15 virtual seconds`() = runTest {
        withClient({ delay(20_000); respond(catalogueBody, HttpStatusCode.OK, jsonHeaders) }) { client ->
            val result = safeRequest(execute = { client.get("/example") }, decode = { Unit })
            assertThat(result).isEqualTo(Result.Error(DataError.REQUEST_TIMEOUT))
            assertThat(testScheduler.currentTime).isEqualTo(15_000L)
        }
    }

    @Test
    fun `explicit cancellation is rethrown unchanged`() = runTest {
        val cancellation = CancellationException("Screen left")
        try {
            safeRequest<Unit>(execute = { throw cancellation }, decode = { Unit })
            error("Cancellation was swallowed")
        } catch (error: CancellationException) {
            assertThat(error).isSameInstanceAs(cancellation)
        }
    }

    @Test
    fun `cancelling an in-flight request does not return an error result to its caller`() = runTest {
        withClient({ awaitCancellation() }) { client ->
            var returned = false
            val job = launch {
                safeRequest(execute = { client.get("/example") }, decode = { Unit })
                returned = true
            }
            runCurrent()
            job.cancelAndJoin()
            assertThat(returned).isFalse()
        }
    }

    @Test
    fun `unexpected failures become unknown errors`() = runTest {
        val result = safeRequest<Unit>(execute = { error("Unexpected failure") }, decode = { Unit })
        assertThat(result).isEqualTo(Result.Error(DataError.UNKNOWN))
    }
}
