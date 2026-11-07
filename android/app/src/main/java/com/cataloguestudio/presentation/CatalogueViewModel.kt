package com.cataloguestudio.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cataloguestudio.domain.CatalogueRemoteDataSource
import com.cataloguestudio.domain.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class CatalogueViewModel(
    private val source: CatalogueRemoteDataSource,
) : ViewModel() {
    private val _state = MutableStateFlow(CatalogueState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            when (val result = source.getCatalogue()) {
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
