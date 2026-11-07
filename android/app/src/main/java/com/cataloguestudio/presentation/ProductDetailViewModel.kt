package com.cataloguestudio.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cataloguestudio.domain.CatalogueRemoteDataSource
import com.cataloguestudio.domain.Result
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductDetailState(
    val loading: Boolean = true,
    val product: ProductUi? = null,
    val error: UiText? = null,
)

sealed interface ProductDetailAction {
    data object Back : ProductDetailAction

    data object Retry : ProductDetailAction

    data object Enquire : ProductDetailAction
}

sealed interface ProductDetailEvent {
    data object Back : ProductDetailEvent

    data class Enquire(val id: Int) : ProductDetailEvent
}

class ProductDetailViewModel(
    private val source: CatalogueRemoteDataSource,
    savedState: SavedStateHandle,
) : ViewModel() {
    private val productId: Int = checkNotNull(savedState["productId"])
    private val _state = MutableStateFlow(ProductDetailState())
    val state = _state.asStateFlow()
    private val _events = Channel<ProductDetailEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        load()
    }

    fun onAction(action: ProductDetailAction) {
        when (action) {
            ProductDetailAction.Retry -> load()
            ProductDetailAction.Back ->
                viewModelScope.launch { _events.send(ProductDetailEvent.Back) }
            ProductDetailAction.Enquire ->
                state.value.product?.let { product ->
                    viewModelScope.launch { _events.send(ProductDetailEvent.Enquire(product.id)) }
                }
        }
    }

    private fun load() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        when (val result = source.product(productId)) {
            is Result.Success ->
                _state.update { it.copy(loading = false, product = result.data.toProductUi()) }
            is Result.Error ->
                _state.update { it.copy(loading = false, error = result.error.toUiText()) }
        }
    }
}
