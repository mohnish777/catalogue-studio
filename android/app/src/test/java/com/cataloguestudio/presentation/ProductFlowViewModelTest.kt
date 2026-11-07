package com.cataloguestudio.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.cataloguestudio.R
import com.cataloguestudio.domain.Catalogue
import com.cataloguestudio.domain.CatalogueRemoteDataSource
import com.cataloguestudio.domain.DataError
import com.cataloguestudio.domain.Enquiry
import com.cataloguestudio.domain.Product
import com.cataloguestudio.domain.Result
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProductFlowViewModelTest {
    private val store = ViewModelStore()
    private val product = Product(9, "Portable drive", "Everyday storage", 64.99, "electronics", "http://test/images/9.jpg", 4.7, 203)
    private var nextKey = 0

    @BeforeEach fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @AfterEach fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private fun handle(vararg values: Pair<String, Any?>) = SavedStateHandle(mapOf("productId" to 9L, *values))
    private fun detail(source: FakeSource) = ProductDetailViewModel(source, handle()).also { store.put("detail${nextKey++}", it) }
    private fun enquiry(source: FakeSource, saved: SavedStateHandle = handle()) =
        EnquiryViewModel(source, saved).also { store.put("enquiry${nextKey++}", it) }
    private fun fill(vm: EnquiryViewModel) {
        vm.onAction(EnquiryAction.OnNameChange(" Alex "))
        vm.onAction(EnquiryAction.OnEmailChange(" alex@example.test "))
        vm.onAction(EnquiryAction.OnMessageChange(" More information please. "))
    }

    @Test fun `detail loads the route ID and publishes loading before mapped content`() = runTest {
        val gate = CompletableDeferred<Result<Product, DataError>>()
        val source = FakeSource().apply { load = { gate.await() } }
        val vm = detail(source)
        vm.state.test {
            assertThat(awaitItem().isLoading).isTrue()
            assertThat(source.ids).isEqualTo(listOf(9L))
            gate.complete(Result.Success(product))
            val loaded = awaitItem()
            assertThat(loaded.isLoading).isFalse()
            assertThat(loaded.product?.id).isEqualTo(9L)
            assertThat(loaded.product?.priceLabel).isEqualTo("$64.99")
        }
    }

    @Test fun `missing detail blocks enquiry and retry recovers the same ID`() = runTest {
        val source = FakeSource().apply { load = { Result.Error(DataError.NOT_FOUND) } }
        val vm = detail(source)
        assertThat(vm.state.value.error).isEqualTo(UiText.StringResource(R.string.error_not_found))
        vm.events.test {
            vm.onAction(ProductDetailAction.OnEnquire)
            expectNoEvents()
            source.load = { Result.Success(product) }
            vm.onAction(ProductDetailAction.OnRetry)
            assertThat(source.ids).isEqualTo(listOf(9L, 9L))
            vm.onAction(ProductDetailAction.OnEnquire)
            assertThat(awaitItem()).isEqualTo(ProductDetailEvent.NavigateToEnquiry(9))
            vm.onAction(ProductDetailAction.OnBack)
            assertThat(awaitItem()).isEqualTo(ProductDetailEvent.NavigateBack)
        }
    }

    @Test fun `restored fields survive product loading and do not submit themselves`() = runTest {
        val gate = CompletableDeferred<Result<Product, DataError>>()
        val source = FakeSource().apply { load = { gate.await() } }
        val vm = enquiry(source, handle("enquiry.name" to "Alex", "enquiry.email" to "alex@example.test", "enquiry.message" to "Saved draft"))
        vm.onAction(EnquiryAction.OnSubmit)
        assertThat(vm.state.value.canSubmit).isFalse()
        assertThat(vm.state.value.message).isEqualTo("Saved draft")
        gate.complete(Result.Success(product))
        assertThat(vm.state.value.canSubmit).isTrue()
        assertThat(source.submissions.size).isEqualTo(0)
    }

    @Test fun `invalid fields are explained together without a request and edits revalidate`() = runTest {
        val source = FakeSource()
        val vm = enquiry(source)
        vm.onAction(EnquiryAction.OnEmailChange("invalid"))
        vm.onAction(EnquiryAction.OnMessageChange("x".repeat(2001)))
        vm.onAction(EnquiryAction.OnSubmit)
        assertThat(vm.state.value.nameError).isNotNull()
        assertThat(vm.state.value.emailError).isNotNull()
        assertThat(vm.state.value.messageError).isNotNull()
        assertThat(source.submissions.size).isEqualTo(0)
        fill(vm)
        assertThat(vm.state.value.nameError).isNull()
        assertThat(vm.state.value.emailError).isNull()
        assertThat(vm.state.value.messageError).isNull()
        assertThat(source.submissions.size).isEqualTo(0)
    }

    @Test fun `sending freezes one normalized snapshot and success clears saved contact fields`() = runTest {
        val gate = CompletableDeferred<Result<Unit, DataError>>()
        val source = FakeSource().apply { send = { gate.await() } }
        val saved = handle()
        val vm = enquiry(source, saved)
        fill(vm)
        vm.onAction(EnquiryAction.OnSubmit)
        assertThat(vm.state.value.isSending).isTrue()
        vm.events.test {
            vm.onAction(EnquiryAction.OnSubmit)
            vm.onAction(EnquiryAction.OnNameChange("Changed"))
            vm.onAction(EnquiryAction.OnRetryProduct)
            vm.onAction(EnquiryAction.OnBack)
            expectNoEvents()
        }
        assertThat(source.submissions).isEqualTo(listOf(Enquiry(9, "Alex", "alex@example.test", "More information please.")))
        assertThat(source.ids).isEqualTo(listOf(9L))
        assertThat(vm.state.value.name).isEqualTo(" Alex ")
        gate.complete(Result.Success(Unit))
        assertThat(vm.state.value.isSent).isTrue()
        assertThat(vm.state.value.isSending).isFalse()
        assertThat(vm.state.value.name).isEqualTo("")
        assertThat(vm.state.value.email).isEqualTo("")
        assertThat(vm.state.value.message).isEqualTo("")
        assertThat(saved.get<String>("enquiry.name")).isNull()
        assertThat(saved.get<String>("enquiry.email")).isNull()
        assertThat(saved.get<String>("enquiry.message")).isNull()
        assertThat(saved.get<Boolean>("enquiry.sent")).isEqualTo(true)
        vm.onAction(EnquiryAction.OnSubmit)
        assertThat(source.submissions.size).isEqualTo(1)
    }

    @Test fun `failed send retains raw saved fields and retry needs an explicit action`() = runTest {
        val source = FakeSource().apply { send = { Result.Error(DataError.SERVER_ERROR) } }
        val saved = handle()
        val vm = enquiry(source, saved)
        fill(vm)
        vm.onAction(EnquiryAction.OnSubmit)
        assertThat(vm.state.value.isSent).isFalse()
        assertThat(vm.state.value.isSending).isFalse()
        assertThat(vm.state.value.sendError).isEqualTo(UiText.StringResource(R.string.error_server))
        assertThat(vm.state.value.email).isEqualTo(" alex@example.test ")
        assertThat(saved.get<String>("enquiry.message")).isEqualTo(" More information please. ")
        assertThat(source.submissions.size).isEqualTo(1)
        source.send = { Result.Success(Unit) }
        vm.onAction(EnquiryAction.OnSubmit)
        assertThat(source.submissions.size).isEqualTo(2)
        assertThat(vm.state.value.isSent).isTrue()
    }

    @Test fun `restored sent screen skips loading and submission but still permits Back`() = runTest {
        val source = FakeSource()
        val vm = enquiry(source, handle("enquiry.sent" to true, "enquiry.name" to "Old contact"))
        assertThat(vm.state.value.isSent).isTrue()
        assertThat(vm.state.value.isLoading).isFalse()
        assertThat(vm.state.value.name).isEqualTo("")
        vm.onAction(EnquiryAction.OnSubmit)
        vm.onAction(EnquiryAction.OnRetryProduct)
        vm.onAction(EnquiryAction.OnEmailChange("other@example.test"))
        assertThat(source.ids.size).isEqualTo(0)
        assertThat(source.submissions.size).isEqualTo(0)
        vm.events.test {
            vm.onAction(EnquiryAction.OnBack)
            assertThat(awaitItem()).isEqualTo(EnquiryEvent.NavigateBack)
        }
    }

    @Test fun `new instance restores an interrupted form without resending`() = runTest {
        val source = FakeSource().apply { send = { CompletableDeferred<Result<Unit, DataError>>().await() } }
        val saved = handle()
        val vm = enquiry(source, saved)
        fill(vm)
        vm.onAction(EnquiryAction.OnSubmit)
        val snapshot = saved.keys().associateWith { saved.get<Any?>(it) }
        store.clear()
        val restoredSource = FakeSource()
        val restored = enquiry(restoredSource, SavedStateHandle(snapshot))
        assertThat(restored.state.value.isSending).isFalse()
        assertThat(restored.state.value.isSent).isFalse()
        assertThat(restored.state.value.name).isEqualTo(" Alex ")
        assertThat(restoredSource.submissions.size).isEqualTo(0)
    }

    @Test fun `product failure blocks sending and product retry retains form inputs`() = runTest {
        val source = FakeSource().apply { load = { Result.Error(DataError.NO_INTERNET) } }
        val vm = enquiry(source)
        fill(vm)
        vm.onAction(EnquiryAction.OnSubmit)
        assertThat(vm.state.value.loadError).isNotNull()
        assertThat(vm.state.value.sendError).isNull()
        assertThat(source.submissions.size).isEqualTo(0)
        source.load = { Result.Success(product) }
        vm.onAction(EnquiryAction.OnRetryProduct)
        assertThat(vm.state.value.canSubmit).isTrue()
        assertThat(vm.state.value.name).isEqualTo(" Alex ")
        assertThat(source.ids).isEqualTo(listOf(9L, 9L))
    }

    private inner class FakeSource : CatalogueRemoteDataSource {
        val ids = mutableListOf<Long>()
        val submissions = mutableListOf<Enquiry>()
        var load: suspend () -> Result<Product, DataError> = { Result.Success(product) }
        var send: suspend () -> Result<Unit, DataError> = { Result.Success(Unit) }
        override suspend fun getProduct(id: Long): Result<Product, DataError> { ids += id; return load() }
        override suspend fun sendEnquiry(enquiry: Enquiry): Result<Unit, DataError> { submissions += enquiry; return send() }
        override suspend fun getCatalogue(category: String?, query: String?): Result<Catalogue, DataError> = error("Unexpected catalogue load")
    }
}
