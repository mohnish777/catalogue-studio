package com.cataloguestudio.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.cataloguestudio.domain.CatalogueRemoteDataSource
import com.cataloguestudio.domain.Result
import com.cataloguestudio.presentation.navigation.ProductDetailRoute
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class ProductDetailViewModel(
    private val source: CatalogueRemoteDataSource,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    // Navigation supplies/restores this argument; product data is fetched, not stored in the route.
    private val productId = savedStateHandle.toRoute<ProductDetailRoute>().productId
    private val _state = MutableStateFlow(ProductDetailState())
    val state = _state.asStateFlow()
    private val _events = Channel<ProductDetailEvent>()
    val events = _events.receiveAsFlow()
    private var loadJob: Job? = null

    init {
        loadProduct()
    }

    fun onAction(action: ProductDetailAction) {
        when (action) {
            ProductDetailAction.OnRetry -> loadProduct()
            ProductDetailAction.OnBack -> sendEvent(ProductDetailEvent.NavigateBack)
            ProductDetailAction.OnEnquire -> state.value.product?.let {
                sendEvent(ProductDetailEvent.NavigateToEnquiry(it.id))
            }
        }
    }

    private fun loadProduct() {
        loadJob?.cancel()
        _state.update { it.copy(product = null, isLoading = true, error = null) }
        loadJob = viewModelScope.launch {
            val result = source.getProduct(productId)
            coroutineContext.ensureActive()
            when (result) {
                is Result.Success -> _state.update {
                    it.copy(product = result.data.toProductUi(), isLoading = false)
                }
                is Result.Error -> _state.update {
                    it.copy(isLoading = false, error = result.error.toUiText())
                }
            }
        }
    }

    private fun sendEvent(event: ProductDetailEvent) {
        viewModelScope.launch { _events.send(event) }
    }
}
