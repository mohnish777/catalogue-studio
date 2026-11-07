package com.cataloguestudio.presentation

internal sealed interface CatalogueEvent {
    data class NavigateToProduct(val productId: Long) : CatalogueEvent
}
