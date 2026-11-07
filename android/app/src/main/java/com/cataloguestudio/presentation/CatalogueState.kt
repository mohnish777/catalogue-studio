package com.cataloguestudio.presentation

import com.cataloguestudio.domain.CatalogueMode

internal data class CategoryUi(
    val value: String,
    val label: UiText,
)

internal data class CatalogueState(
    val query: String = "",
    val submittedQuery: String = "",
    val selectedCategory: String? = null,
    val categories: List<CategoryUi> = emptyList(),
    val featured: List<ProductUi> = emptyList(),
    val remaining: List<ProductUi> = emptyList(),
    val mode: CatalogueMode? = null,
    val isLoading: Boolean = true,
    val error: UiText? = null,
) {
    val productCount: Int
        get() = featured.size + remaining.size

    // Unsubmitted edits do not change the filters that produced the current results.
    val hasActiveFilters: Boolean
        get() = selectedCategory != null || submittedQuery.isNotBlank()

    val isEmpty: Boolean
        get() = !isLoading && error == null && featured.isEmpty() && remaining.isEmpty()
}
