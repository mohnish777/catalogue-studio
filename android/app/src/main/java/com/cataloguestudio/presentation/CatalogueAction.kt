package com.cataloguestudio.presentation

internal sealed interface CatalogueAction {
    data class OnQueryChange(val query: String) : CatalogueAction
    data object OnSearch : CatalogueAction
    data object OnClearSearch : CatalogueAction
    // null selects all categories; other values use the raw API category.
    data class OnCategorySelect(val category: String?) : CatalogueAction
    data object OnClearFilters : CatalogueAction
    data object OnRetry : CatalogueAction
    data class OnProductClick(val productId: Long) : CatalogueAction
}
