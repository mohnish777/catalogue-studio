package com.cataloguestudio.presentation

import androidx.lifecycle.ViewModelStore
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.cataloguestudio.R
import com.cataloguestudio.domain.Catalogue
import com.cataloguestudio.domain.CatalogueMode
import com.cataloguestudio.domain.CatalogueRemoteDataSource
import com.cataloguestudio.domain.DataError
import com.cataloguestudio.domain.Enquiry
import com.cataloguestudio.domain.Product
import com.cataloguestudio.domain.Result
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogueViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val store = ViewModelStore()
    private val product = Product(
        id = 9,
        title = "Portable drive",
        description = "Everyday storage",
        price = 64.99,
        category = "electronics",
        imageUrl = "http://catalogue.test/images/9.jpg",
        rating = 4.7,
        reviewCount = 203,
    )
    private val catalogue = Catalogue(
        mode = CatalogueMode.DEMO,
        categories = listOf("electronics", "Home & café"),
        featured = listOf(product, product.copy(id = 1, price = 9.5)),
        remaining = listOf(product.copy(id = 2)),
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    private fun createViewModel(source: FakeCatalogueSource): CatalogueViewModel =
        CatalogueViewModel(source).also { store.put("catalogue", it) }

    @Test
    fun `initial load publishes loading then preserves backend order and maps display data`() = runTest {
        val source = FakeCatalogueSource()
        val viewModel = createViewModel(source)
        viewModel.state.test {
            assertThat(awaitItem()).isEqualTo(CatalogueState())
            assertThat(source.requests).isEqualTo(listOf(Request(null, null)))
            source.response.complete(Result.Success(catalogue))

            val loaded = awaitItem()
            assertThat(loaded.isLoading).isFalse()
            assertThat(loaded.error).isNull()
            assertThat(loaded.mode).isEqualTo(CatalogueMode.DEMO)
            assertThat(loaded.categories).isEqualTo(listOf(
                CategoryUi("electronics", UiText.StringResource(R.string.category_electronics)),
                CategoryUi("Home & café", UiText.DynamicString("Home & café")),
            ))
            assertThat(loaded.featured.map { it.id }).isEqualTo(listOf(9L, 1L))
            assertThat(loaded.remaining.map { it.id }).isEqualTo(listOf(2L))
            assertThat(loaded.featured.first().priceLabel).isEqualTo("$64.99")
            assertThat(loaded.featured.last().priceLabel).isEqualTo("$9.50")
            assertThat(loaded.featured.first().ratingLabel).isEqualTo("4.7")
            assertThat(loaded.featured.first().imageUrl).isEqualTo(product.imageUrl)
            assertThat(loaded.productCount).isEqualTo(3)
            assertThat(loaded.isEmpty).isFalse()
            assertThat(source.requests.size).isEqualTo(1)
        }
    }

    @Test
    fun `request failure publishes a typed message without pretending results are empty`() = runTest {
        val source = FakeCatalogueSource()
        val viewModel = createViewModel(source)
        viewModel.state.test {
            awaitItem()
            source.response.complete(Result.Error(DataError.SERVER_ERROR))
            val failed = awaitItem()
            assertThat(failed.isLoading).isFalse()
            assertThat(failed.error).isEqualTo(UiText.StringResource(R.string.error_server))
            assertThat(failed.mode).isNull()
            assertThat(failed.productCount).isEqualTo(0)
            assertThat(failed.isEmpty).isFalse()
            assertThat(source.requests.size).isEqualTo(1)
        }
    }

    @Test
    fun `a successful empty live catalogue is distinct from an error`() = runTest {
        val source = FakeCatalogueSource()
        val viewModel = createViewModel(source)
        viewModel.state.test {
            awaitItem()
            source.response.complete(Result.Success(Catalogue(CatalogueMode.LIVE, emptyList(), emptyList(), emptyList())))
            val empty = awaitItem()
            assertThat(empty.isLoading).isFalse()
            assertThat(empty.error).isNull()
            assertThat(empty.mode).isEqualTo(CatalogueMode.LIVE)
            assertThat(empty.isEmpty).isTrue()
        }
    }

    @Test
    fun `collecting state twice does not start a second request`() = runTest {
        val source = FakeCatalogueSource()
        val viewModel = createViewModel(source)
        viewModel.state.test {
            val first = this
            assertThat(first.awaitItem().isLoading).isTrue()
            viewModel.state.test {
                assertThat(awaitItem().isLoading).isTrue()
                source.response.complete(Result.Success(catalogue))
                assertThat(first.awaitItem().isLoading).isFalse()
                assertThat(awaitItem().isLoading).isFalse()
            }
        }
        assertThat(source.requests.size).isEqualTo(1)
    }

    @Test
    fun `clearing the ViewModel cancels the request without publishing an error or late result`() = runTest {
        val source = FakeCatalogueSource()
        val viewModel = createViewModel(source)
        viewModel.state.test {
            val initial = awaitItem()
            store.clear()
            runCurrent()
            assertThat(source.cancelledRequests).isEqualTo(1)
            source.response.complete(Result.Success(catalogue))
            runCurrent()
            expectNoEvents()
            assertThat(viewModel.state.value).isEqualTo(initial)
        }
    }

    private data class Request(val category: String?, val query: String?)

    private class FakeCatalogueSource : CatalogueRemoteDataSource {
        val requests = mutableListOf<Request>()
        val response = CompletableDeferred<Result<Catalogue, DataError>>()
        var cancelledRequests = 0
            private set

        override suspend fun getCatalogue(category: String?, query: String?): Result<Catalogue, DataError> {
            requests += Request(category, query)
            return try {
                response.await()
            } catch (error: CancellationException) {
                cancelledRequests++
                throw error
            }
        }

        override suspend fun getProduct(id: Long): Result<Product, DataError> =
            error("Unexpected product request in catalogue test")

        override suspend fun sendEnquiry(enquiry: Enquiry): Result<Unit, DataError> =
            error("Unexpected enquiry request in catalogue test")
    }
}
