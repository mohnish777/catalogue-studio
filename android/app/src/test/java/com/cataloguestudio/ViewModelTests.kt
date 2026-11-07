package com.cataloguestudio

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.*
import com.cataloguestudio.domain.*
import com.cataloguestudio.domain.Result
import com.cataloguestudio.presentation.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private val sampleProduct =
    Product(
        1,
        "Canvas backpack",
        "Everyday canvas",
        109.95,
        "men's clothing",
        "http://localhost/image.jpg",
        4.7,
        120,
    )

private class FakeCatalogueSource : CatalogueRemoteDataSource {
    var catalogueResult: Result<Catalogue, DataError> =
        Result.Success(Catalogue(listOf("men's clothing"), listOf(sampleProduct), emptyList()))
    var productResult: Result<Product, DataError> = Result.Success(sampleProduct)
    var enquiryResult: Result<Unit, DataError> = Result.Success(Unit)
    var sendGate: CompletableDeferred<Unit>? = null
    var enquiryCount = 0
    val filters = mutableListOf<Pair<String?, String?>>()
    var lastEnquiry: Enquiry? = null

    override suspend fun catalogue(
        category: String?,
        search: String?,
    ): Result<Catalogue, DataError> {
        filters += category to search
        if (category == "slow") awaitCancellation()
        return catalogueResult
    }

    override suspend fun product(id: Int) = productResult

    override suspend fun enquire(enquiry: Enquiry): Result<Unit, DataError> {
        enquiryCount++
        lastEnquiry = enquiry
        sendGate?.await()
        return enquiryResult
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelTests {
    @BeforeEach
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `catalogue loads editorial picks and formats USD prices`() = runTest {
        val vm = CatalogueViewModel(FakeCatalogueSource(), SavedStateHandle())
        assertThat(vm.state.value.loading).isFalse()
        assertThat(vm.state.value.featured).hasSize(1)
        assertThat(vm.state.value.featured.single().price).isEqualTo("$109.95")
    }

    @Test
    fun `typing does not query until search is submitted`() = runTest {
        val source = FakeCatalogueSource()
        val vm = CatalogueViewModel(source, SavedStateHandle())
        vm.onAction(CatalogueAction.QueryChanged("  canvas  "))
        assertThat(source.filters).hasSize(1)
        vm.onAction(CatalogueAction.Search)
        assertThat(source.filters.last().second).isEqualTo("canvas")
        vm.onAction(CatalogueAction.SelectCategory("jewelery"))
        assertThat(source.filters.last()).isEqualTo("jewelery" to "")
        assertThat(vm.state.value.query).isEqualTo("")
    }

    @Test
    fun `changing filters cancels an older request and clears stale results`() = runTest {
        val source = FakeCatalogueSource()
        val vm = CatalogueViewModel(source, SavedStateHandle())
        vm.onAction(CatalogueAction.SelectCategory("slow"))
        assertThat(vm.state.value.loading).isTrue()
        assertThat(vm.state.value.featured).isEmpty()
        vm.onAction(CatalogueAction.SelectCategory("jewelery"))
        assertThat(vm.state.value.loading).isFalse()
        assertThat(vm.state.value.category).isEqualTo("jewelery")
    }

    @Test
    fun `catalogue failure is retryable and product tap emits navigation`() = runTest {
        val source =
            FakeCatalogueSource().apply { catalogueResult = Result.Error(DataError.NETWORK) }
        val vm = CatalogueViewModel(source, SavedStateHandle())
        assertThat(vm.state.value.error).isNotNull()
        source.catalogueResult =
            Result.Success(Catalogue(emptyList(), listOf(sampleProduct), emptyList()))
        vm.onAction(CatalogueAction.Retry)
        assertThat(vm.state.value.error).isNull()
        vm.events.test {
            vm.onAction(CatalogueAction.OpenProduct(1))
            assertThat(awaitItem()).isEqualTo(CatalogueEvent.OpenProduct(1))
        }
    }

    @Test
    fun `details handle unavailable product and allow recovery`() = runTest {
        val source =
            FakeCatalogueSource().apply { productResult = Result.Error(DataError.NOT_FOUND) }
        val vm = ProductDetailViewModel(source, SavedStateHandle(mapOf("productId" to 1)))
        assertThat(vm.state.value.product).isNull()
        assertThat(vm.state.value.error).isNotNull()
        vm.events.test {
            vm.onAction(ProductDetailAction.Enquire)
            expectNoEvents()
            source.productResult = Result.Success(sampleProduct)
            vm.onAction(ProductDetailAction.Retry)
            vm.onAction(ProductDetailAction.Enquire)
            assertThat(awaitItem()).isEqualTo(ProductDetailEvent.Enquire(1))
        }
    }

    @Test
    fun `invalid enquiry never reaches network`() = runTest {
        val source = FakeCatalogueSource()
        val vm = EnquiryViewModel(source, SavedStateHandle(mapOf("productId" to 1)))
        vm.onAction(EnquiryAction.Submit)
        assertThat(vm.state.value.nameError).isTrue()
        assertThat(vm.state.value.emailError).isTrue()
        assertThat(source.enquiryCount).isEqualTo(0)
    }

    @Test
    fun `failed email retains form values and never shows success`() = runTest {
        val source =
            FakeCatalogueSource().apply { enquiryResult = Result.Error(DataError.UNAVAILABLE) }
        val handle = SavedStateHandle(mapOf("productId" to 1))
        val vm = EnquiryViewModel(source, handle)
        vm.onAction(EnquiryAction.NameChanged("Ada Lovelace"))
        vm.onAction(EnquiryAction.EmailChanged("ada@example.test"))
        vm.onAction(EnquiryAction.MessageChanged("Please tell me more."))
        vm.onAction(EnquiryAction.Submit)
        assertThat(vm.state.value.sent).isFalse()
        assertThat(vm.state.value.sending).isFalse()
        assertThat(vm.state.value.sendError).isNotNull()
        assertThat(vm.state.value.message).isEqualTo("Please tell me more.")
        val restored = EnquiryViewModel(source, handle)
        assertThat(restored.state.value.email).isEqualTo("ada@example.test")
    }

    @Test
    fun `pending submission blocks duplicate taps and success clears contact details`() = runTest {
        val source = FakeCatalogueSource().apply { sendGate = CompletableDeferred() }
        val vm = EnquiryViewModel(source, SavedStateHandle(mapOf("productId" to 1)))
        vm.onAction(EnquiryAction.NameChanged("Ada Lovelace"))
        vm.onAction(EnquiryAction.EmailChanged("ada@example.test"))
        vm.onAction(EnquiryAction.Submit)
        vm.onAction(EnquiryAction.Submit)
        assertThat(source.enquiryCount).isEqualTo(1)
        assertThat(vm.state.value.sent).isFalse()
        assertThat(vm.state.value.sending).isTrue()
        source.sendGate!!.complete(Unit)
        assertThat(vm.state.value.sent).isTrue()
        assertThat(vm.state.value.email).isEqualTo("")
        vm.onAction(EnquiryAction.Submit)
        assertThat(source.enquiryCount).isEqualTo(1)
    }

    @Test
    fun `validation rejects newline email and overlong message`() {
        assertThat(validateEnquiry("Ada", "ada@example.test\nBcc: bad@example.test", "").valid)
            .isFalse()
        assertThat(validateEnquiry("Ada", "ada@example.test", "x".repeat(2001)).valid).isFalse()
        assertThat(validateEnquiry(" Ada ", " ada@example.test ", "Hello").valid).isTrue()
    }
}
