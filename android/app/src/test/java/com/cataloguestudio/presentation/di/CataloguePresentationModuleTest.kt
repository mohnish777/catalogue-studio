package com.cataloguestudio.presentation.di

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotSameInstanceAs
import com.cataloguestudio.domain.Catalogue
import com.cataloguestudio.domain.CatalogueMode
import com.cataloguestudio.domain.CatalogueRemoteDataSource
import com.cataloguestudio.domain.DataError
import com.cataloguestudio.domain.Enquiry
import com.cataloguestudio.domain.Product
import com.cataloguestudio.domain.Result
import com.cataloguestudio.presentation.CatalogueAction
import com.cataloguestudio.presentation.CatalogueViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
class CataloguePresentationModuleTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val store = ViewModelStore()

    @BeforeEach
    fun setUp() { Dispatchers.setMain(dispatcher) }

    @AfterEach
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun `registering presentation module does not resolve a source or start loading`() {
        var sourceCreations = 0
        val application = koinApplication {
            modules(cataloguePresentationModule, module {
                single<CatalogueRemoteDataSource> {
                    sourceCreations++
                    error("Source should stay lazy")
                }
            })
        }
        try {
            assertThat(sourceCreations).isEqualTo(0)
        } finally {
            application.close()
        }
    }

    @Test
    fun `module constructs distinct ViewModels with their own handles and the shared source`() = runTest {
        val source = RecordingCatalogueSource()
        val application = koinApplication {
            modules(cataloguePresentationModule, module {
                single<CatalogueRemoteDataSource> { source }
            })
        }
        try {
            // Explicit handles test constructor wiring without an Android lifecycle owner.
            val firstHandle = SavedStateHandle(mapOf(
                "catalogue.query" to "draft", "catalogue.submittedQuery" to "drive",
                "catalogue.selectedCategory" to "electronics",
            ))
            val secondHandle = SavedStateHandle()
            val first = application.koin.get<CatalogueViewModel> { parametersOf(firstHandle) }
                .also { store.put("first", it) }
            val second = application.koin.get<CatalogueViewModel> { parametersOf(secondHandle) }
                .also { store.put("second", it) }
            assertThat(first).isNotSameInstanceAs(second)
            assertThat(source.requests).isEqualTo(listOf("electronics" to "drive", null to null))
            assertThat(first.state.value.mode).isEqualTo(CatalogueMode.DEMO)
            first.onAction(CatalogueAction.OnQueryChange("new draft"))
            assertThat(firstHandle.get<String>("catalogue.query")).isEqualTo("new draft")
            assertThat(secondHandle.get<String>("catalogue.query")).isEqualTo("")
            assertThat(second.state.value.query).isEqualTo("")
        } finally {
            store.clear()
            application.close()
        }
    }

    private class RecordingCatalogueSource : CatalogueRemoteDataSource {
        val requests = mutableListOf<Pair<String?, String?>>()

        override suspend fun getCatalogue(category: String?, query: String?): Result<Catalogue, DataError> {
            requests += category to query
            return Result.Success(Catalogue(CatalogueMode.DEMO, emptyList(), emptyList(), emptyList()))
        }

        override suspend fun getProduct(id: Long): Result<Product, DataError> =
            error("Unexpected product request")

        override suspend fun sendEnquiry(enquiry: Enquiry): Result<Unit, DataError> =
            error("Unexpected enquiry request")
    }
}
