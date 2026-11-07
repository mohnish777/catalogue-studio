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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
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
    fun `clearing the ViewModel cancels a cooperative request without an error`() =
        verifyLifecycleCancellation(returnAfterCancellation = false)

    @Test
    fun `clearing the ViewModel prevents a source returning despite cancellation from publishing`() =
        verifyLifecycleCancellation(returnAfterCancellation = true)

    private fun verifyLifecycleCancellation(returnAfterCancellation: Boolean) = runTest {
        val source = FakeCatalogueSource(returnAfterCancellation)
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

    @Test
    fun `typing is local and explicit search submits trimmed text within the selected category`() = runTest {
        val source = FakeCatalogueSource()
        val viewModel = createViewModel(source)
        source.response.complete(Result.Success(catalogue))
        viewModel.onAction(CatalogueAction.OnCategorySelect("electronics"))
        source.responses[1].complete(Result.Success(catalogue))
        val beforeTyping = viewModel.state.value

        viewModel.onAction(CatalogueAction.OnQueryChange("  drive  "))
        assertThat(source.requests.size).isEqualTo(2)
        assertThat(viewModel.state.value).isEqualTo(beforeTyping.copy(query = "  drive  "))

        viewModel.onAction(CatalogueAction.OnSearch)
        assertThat(source.requests.last()).isEqualTo(Request("electronics", "drive"))
        val loading = viewModel.state.value
        assertThat(loading.query).isEqualTo("  drive  ")
        assertThat(loading.submittedQuery).isEqualTo("drive")
        assertThat(loading.isLoading).isTrue()
        assertThat(loading.productCount).isEqualTo(0)
        assertThat(loading.error).isNull()
        assertThat(loading.categories).isEqualTo(beforeTyping.categories)

        // Edits made while the submitted request is pending remain an unsent draft.
        viewModel.onAction(CatalogueAction.OnQueryChange("headphones"))
        source.responses[2].complete(Result.Success(catalogue.copy(
            featured = emptyList(), remaining = listOf(product),
        )))
        assertThat(viewModel.state.value.query).isEqualTo("headphones")
        assertThat(viewModel.state.value.submittedQuery).isEqualTo("drive")
        assertThat(viewModel.state.value.featured).isEqualTo(emptyList())
        assertThat(viewModel.state.value.remaining.map { it.id }).isEqualTo(listOf(9L))
        assertThat(source.requests.size).isEqualTo(3)
    }

    @Test
    fun `category selection clears submitted search and draft so category picks can return`() = runTest {
        val source = FakeCatalogueSource()
        val viewModel = createViewModel(source)
        source.response.complete(Result.Success(catalogue))
        viewModel.onAction(CatalogueAction.OnQueryChange("drive"))
        viewModel.onAction(CatalogueAction.OnSearch)
        viewModel.onAction(CatalogueAction.OnQueryChange("unsent draft"))
        viewModel.onAction(CatalogueAction.OnCategorySelect("electronics"))
        assertThat(source.requests.last()).isEqualTo(Request("electronics", null))
        assertThat(viewModel.state.value.query).isEqualTo("")
        assertThat(viewModel.state.value.submittedQuery).isEqualTo("")
        assertThat(viewModel.state.value.selectedCategory).isEqualTo("electronics")
        source.responses[2].complete(Result.Success(catalogue))
        assertThat(viewModel.state.value.featured.map { it.id }).isEqualTo(listOf(9L, 1L))
        assertThat(viewModel.state.value.remaining.map { it.id }).isEqualTo(listOf(2L))

        viewModel.onAction(CatalogueAction.OnCategorySelect(null))
        assertThat(source.requests.last()).isEqualTo(Request(null, null))
        assertThat(viewModel.state.value.selectedCategory).isNull()
    }

    @Test
    fun `clear search keeps the category while clear filters returns to unfiltered home`() = runTest {
        val source = FakeCatalogueSource()
        val viewModel = createViewModel(source)
        viewModel.onAction(CatalogueAction.OnCategorySelect("electronics"))
        viewModel.onAction(CatalogueAction.OnQueryChange("drive"))
        viewModel.onAction(CatalogueAction.OnSearch)
        viewModel.onAction(CatalogueAction.OnClearSearch)
        assertThat(source.requests.last()).isEqualTo(Request("electronics", null))
        assertThat(viewModel.state.value.query).isEqualTo("")
        assertThat(viewModel.state.value.submittedQuery).isEqualTo("")
        assertThat(viewModel.state.value.selectedCategory).isEqualTo("electronics")

        viewModel.onAction(CatalogueAction.OnQueryChange("drive"))
        viewModel.onAction(CatalogueAction.OnSearch)
        viewModel.onAction(CatalogueAction.OnClearFilters)
        assertThat(source.requests.last()).isEqualTo(Request(null, null))
        assertThat(viewModel.state.value.query).isEqualTo("")
        assertThat(viewModel.state.value.submittedQuery).isEqualTo("")
        assertThat(viewModel.state.value.selectedCategory).isNull()
        assertThat(viewModel.state.value.hasActiveFilters).isFalse()
    }

    @Test
    fun `submitting whitespace removes search instead of sending a blank query`() = runTest {
        val source = FakeCatalogueSource()
        val viewModel = createViewModel(source)
        viewModel.onAction(CatalogueAction.OnQueryChange("drive"))
        viewModel.onAction(CatalogueAction.OnSearch)
        viewModel.onAction(CatalogueAction.OnQueryChange(" \t "))
        viewModel.onAction(CatalogueAction.OnSearch)
        assertThat(source.requests.last()).isEqualTo(Request(null, null))
        assertThat(viewModel.state.value.submittedQuery).isEqualTo("")
        assertThat(viewModel.state.value.hasActiveFilters).isFalse()
    }

    @Test
    fun `retry clears the error and repeats submitted filters without applying a newer draft`() = runTest {
        val source = FakeCatalogueSource()
        val viewModel = createViewModel(source)
        viewModel.onAction(CatalogueAction.OnCategorySelect("electronics"))
        viewModel.onAction(CatalogueAction.OnQueryChange("drive"))
        viewModel.onAction(CatalogueAction.OnSearch)
        source.responses[2].complete(Result.Error(DataError.SERVER_ERROR))
        assertThat(viewModel.state.value.error).isEqualTo(UiText.StringResource(R.string.error_server))
        assertThat(viewModel.state.value.isLoading).isFalse()
        assertThat(source.requests.size).isEqualTo(3) // Failure does not retry automatically.
        viewModel.onAction(CatalogueAction.OnQueryChange("headphones"))

        viewModel.onAction(CatalogueAction.OnRetry)
        assertThat(source.requests.last()).isEqualTo(Request("electronics", "drive"))
        assertThat(viewModel.state.value.query).isEqualTo("headphones")
        assertThat(viewModel.state.value.submittedQuery).isEqualTo("drive")
        assertThat(viewModel.state.value.error).isNull()
        assertThat(viewModel.state.value.isLoading).isTrue()
        source.responses[3].complete(Result.Success(catalogue.copy(featured = emptyList(), remaining = emptyList())))
        assertThat(viewModel.state.value.isEmpty).isTrue()
    }

    @Test
    fun `starting a newer request cancels its predecessor without publishing an error`() = runTest {
        val source = FakeCatalogueSource()
        val viewModel = createViewModel(source)
        viewModel.onAction(CatalogueAction.OnQueryChange("drive"))
        viewModel.onAction(CatalogueAction.OnSearch)
        runCurrent()
        assertThat(source.cancelledRequests).isEqualTo(1)
        assertThat(source.requests).isEqualTo(listOf(Request(null, null), Request(null, "drive")))
        assertThat(viewModel.state.value.error).isNull()
        source.responses[1].complete(Result.Success(catalogue))
        assertThat(viewModel.state.value.isLoading).isFalse()
    }

    @Test
    fun `a cancelled source returning late success cannot replace newer results`() =
        verifyLateResponse(oldRequestFails = false)

    @Test
    fun `a cancelled source returning late error cannot replace newer results`() =
        verifyLateResponse(oldRequestFails = true)

    private fun verifyLateResponse(oldRequestFails: Boolean) = runTest {
        val source = FakeCatalogueSource(returnAfterCancellation = true)
        val viewModel = createViewModel(source)
        viewModel.onAction(CatalogueAction.OnQueryChange("drive"))
        viewModel.onAction(CatalogueAction.OnSearch)
        runCurrent()
        assertThat(source.cancelledRequests).isEqualTo(1)
        source.responses[1].complete(Result.Success(catalogue.copy(
            mode = CatalogueMode.LIVE, featured = emptyList(), remaining = listOf(product),
        )))
        val newest = viewModel.state.value
        assertThat(newest.mode).isEqualTo(CatalogueMode.LIVE)
        assertThat(newest.remaining.map { it.id }).isEqualTo(listOf(9L))
        viewModel.state.test {
            awaitItem()
            source.response.complete(if (oldRequestFails) Result.Error(DataError.SERVER_ERROR) else Result.Success(catalogue))
            runCurrent()
            expectNoEvents()
            assertThat(viewModel.state.value).isEqualTo(newest)
        }
    }

    @Test
    fun `product click emits one navigation event without changing catalogue state or reloading`() = runTest {
        val source = FakeCatalogueSource()
        val viewModel = createViewModel(source)
        source.response.complete(Result.Success(catalogue))
        val before = viewModel.state.value
        viewModel.events.test {
            viewModel.onAction(CatalogueAction.OnProductClick(9))
            assertThat(awaitItem()).isEqualTo(CatalogueEvent.NavigateToProduct(9))
            expectNoEvents()
        }
        viewModel.events.test { expectNoEvents() }
        assertThat(viewModel.state.value).isEqualTo(before)
        assertThat(source.requests.size).isEqualTo(1)
    }

    private data class Request(val category: String?, val query: String?)

    private class FakeCatalogueSource(private val returnAfterCancellation: Boolean = false) : CatalogueRemoteDataSource {
        val requests = mutableListOf<Request>()
        val responses = mutableListOf<CompletableDeferred<Result<Catalogue, DataError>>>()
        val response get() = responses.first()
        var cancelledRequests = 0
            private set

        override suspend fun getCatalogue(category: String?, query: String?): Result<Catalogue, DataError> {
            requests += Request(category, query)
            val response = CompletableDeferred<Result<Catalogue, DataError>>().also { responses += it }
            return try {
                response.await()
            } catch (error: CancellationException) {
                cancelledRequests++
                if (returnAfterCancellation) withContext(NonCancellable) { response.await() } else throw error
            }
        }

        override suspend fun getProduct(id: Long): Result<Product, DataError> =
            error("Unexpected product request in catalogue test")

        override suspend fun sendEnquiry(enquiry: Enquiry): Result<Unit, DataError> =
            error("Unexpected enquiry request in catalogue test")
    }
}
