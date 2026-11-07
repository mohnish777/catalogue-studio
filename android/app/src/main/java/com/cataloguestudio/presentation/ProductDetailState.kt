package com.cataloguestudio.presentation

internal data class ProductDetailState(
    val product: ProductUi? = null,
    val isLoading: Boolean = true,
    val error: UiText? = null,
)

internal sealed interface ProductDetailAction {
    data object OnRetry : ProductDetailAction
    data object OnBack : ProductDetailAction
    data object OnEnquire : ProductDetailAction
}

internal sealed interface ProductDetailEvent {
    data object NavigateBack : ProductDetailEvent
    data class NavigateToEnquiry(val productId: Long) : ProductDetailEvent
}
