package com.cataloguestudio.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cataloguestudio.domain.CatalogueRemoteDataSource
import com.cataloguestudio.domain.Result
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CatalogueState(
    val loading: Boolean = true,
    val error: UiText? = null,
    val categories: List<String> = emptyList(),
    val featured: List<ProductUi> = emptyList(),
    val products: List<ProductUi> = emptyList(),
    val query: String = "",
    val category: String? = null,
    val submittedSearch: String = "",
)

sealed interface CatalogueAction {
    data class QueryChanged(val query: String) : CatalogueAction

    data class SelectCategory(val category: String?) : CatalogueAction

    data class OpenProduct(val id: Int) : CatalogueAction

    data object Search : CatalogueAction

    data object ClearSearch : CatalogueAction

    data object Retry : CatalogueAction
}

sealed interface CatalogueEvent {
    data class OpenProduct(val id: Int) : CatalogueEvent
}

class CatalogueViewModel(
    private val source: CatalogueRemoteDataSource,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val _state =
        MutableStateFlow(
            CatalogueState(
                query = savedState["query"] ?: "",
                category = savedState["category"],
                submittedSearch = savedState["search"] ?: "",
            )
        )
    val state = _state.asStateFlow()
    private val _events = Channel<CatalogueEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()
    private var loadJob: Job? = null

    init {
        load()
    }

    fun onAction(action: CatalogueAction) {
        when (action) {
            is CatalogueAction.QueryChanged -> {
                val query = action.query.take(200)
                savedState["query"] = query
                _state.update { it.copy(query = query) }
            }
            CatalogueAction.Search -> {
                val query = state.value.query.trim()
                savedState["search"] = query
                _state.update { it.copy(submittedSearch = query) }
                load()
            }
            CatalogueAction.ClearSearch -> {
                savedState["query"] = ""
                savedState["search"] = ""
                _state.update { it.copy(query = "", submittedSearch = "") }
                load()
            }
            is CatalogueAction.SelectCategory -> {
                savedState["category"] = action.category
                savedState["query"] = ""
                savedState["search"] = ""
                _state.update {
                    it.copy(category = action.category, query = "", submittedSearch = "")
                }
                load()
            }
            is CatalogueAction.OpenProduct ->
                viewModelScope.launch { _events.send(CatalogueEvent.OpenProduct(action.id)) }
            CatalogueAction.Retry -> load()
        }
    }

    private fun load() {
        loadJob?.cancel()
        val filter = state.value
        loadJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    loading = true,
                    error = null,
                    featured = emptyList(),
                    products = emptyList(),
                )
            }
            when (val result = source.catalogue(filter.category, filter.submittedSearch)) {
                is Result.Success ->
                    _state.update {
                        it.copy(
                            loading = false,
                            categories = result.data.categories,
                            featured = result.data.featured.map { p -> p.toProductUi() },
                            products = result.data.products.map { p -> p.toProductUi() },
                        )
                    }
                is Result.Error ->
                    _state.update { it.copy(loading = false, error = result.error.toUiText()) }
            }
        }
    }
}
