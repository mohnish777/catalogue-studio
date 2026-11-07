package com.cataloguestudio.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cataloguestudio.domain.CatalogueRemoteDataSource
import com.cataloguestudio.domain.Result
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class CatalogueViewModel(
    private val source: CatalogueRemoteDataSource,
) : ViewModel() {
    private val _state = MutableStateFlow(CatalogueState())
    val state = _state.asStateFlow()

    private val _events = Channel<CatalogueEvent>()
    val events = _events.receiveAsFlow()

    private var loadJob: Job? = null

    init {
        loadCatalogue()
    }

    fun onAction(action: CatalogueAction) {
        when (action) {
            is CatalogueAction.OnQueryChange -> _state.update { it.copy(query = action.query) }
            CatalogueAction.OnSearch -> loadCatalogue(submittedQuery = state.value.query.trim())
            CatalogueAction.OnClearSearch -> loadCatalogue(query = "", submittedQuery = "")
            is CatalogueAction.OnCategorySelect -> loadCatalogue(
                category = action.category,
                query = "",
                submittedQuery = "",
            )
            CatalogueAction.OnClearFilters -> loadCatalogue(category = null, query = "", submittedQuery = "")
            CatalogueAction.OnRetry -> loadCatalogue()
            is CatalogueAction.OnProductClick -> viewModelScope.launch {
                _events.send(CatalogueEvent.NavigateToProduct(action.productId))
            }
        }
    }

    private fun loadCatalogue(
        category: String? = state.value.selectedCategory,
        query: String = state.value.query,
        submittedQuery: String = state.value.submittedQuery,
    ) {
        loadJob?.cancel()
        // Filters and cleared cards change together, so old cards never represent a new request.
        _state.update {
            it.copy(
                query = query,
                submittedQuery = submittedQuery,
                selectedCategory = category,
                featured = emptyList(),
                remaining = emptyList(),
                isLoading = true,
                error = null,
            )
        }
        loadJob = viewModelScope.launch {
            val result = source.getCatalogue(category = category, query = submittedQuery.ifBlank { null })
            // A source that returns despite cancellation must not overwrite a newer request.
            coroutineContext.ensureActive()
            when (result) {
                is Result.Success -> {
                    val catalogue = result.data
                    val categories = catalogue.categories.map {
                        CategoryUi(value = it, label = it.toCategoryLabel())
                    }
                    val featured = catalogue.featured.map { it.toProductUi() }
                    val remaining = catalogue.remaining.map { it.toProductUi() }
                    _state.update {
                        it.copy(
                            categories = categories,
                            featured = featured,
                            remaining = remaining,
                            mode = catalogue.mode,
                            isLoading = false,
                            error = null,
                        )
                    }
                }
                is Result.Error -> _state.update {
                    it.copy(isLoading = false, error = result.error.toUiText())
                }
            }
        }
    }
}
