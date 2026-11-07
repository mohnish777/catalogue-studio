package com.cataloguestudio.presentation

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.cataloguestudio.domain.CatalogueMode
import org.junit.jupiter.api.Test

class CatalogueStateTest {
    private val product = ProductUi(
        id = 9,
        title = "Portable drive",
        description = "Everyday storage",
        priceLabel = "$64.99",
        category = "electronics",
        categoryLabel = UiText.DynamicString("Electronics"),
        imageUrl = "http://catalogue.test/images/9.jpg",
        ratingLabel = "4.7",
        reviewCount = 203,
    )

    @Test
    fun `initial loading and request failures are not empty results`() {
        assertThat(CatalogueState().isEmpty).isFalse()
        assertThat(CatalogueState(isLoading = false, error = UiText.DynamicString("Could not connect")).isEmpty)
            .isFalse()
        assertThat(CatalogueState(isLoading = false, mode = CatalogueMode.DEMO).isEmpty).isTrue()
    }

    @Test
    fun `a product in either section prevents an empty state`() {
        assertThat(CatalogueState(isLoading = false, featured = listOf(product)).isEmpty).isFalse()
        assertThat(CatalogueState(isLoading = false, remaining = listOf(product)).isEmpty).isFalse()
    }

    @Test
    fun `draft edits do not change active search filters`() {
        val draft = CatalogueState(query = "drive")
        assertThat(draft.hasActiveFilters).isFalse()
        val submitted = draft.copy(submittedQuery = "drive")
        assertThat(submitted.hasActiveFilters).isTrue()
        assertThat(submitted.copy(query = "").hasActiveFilters).isTrue()
        assertThat(submitted.copy(query = "headphones").hasActiveFilters).isTrue()
    }

    @Test
    fun `category and nonblank submitted query independently count as active filters`() {
        assertThat(CatalogueState(selectedCategory = "electronics").hasActiveFilters).isTrue()
        assertThat(CatalogueState(submittedQuery = "drive").hasActiveFilters).isTrue()
        assertThat(CatalogueState(submittedQuery = "  ").hasActiveFilters).isFalse()
    }

    @Test
    fun `product count follows both sections in each state snapshot`() {
        val loaded = CatalogueState(
            isLoading = false,
            featured = listOf(product),
            remaining = listOf(product.copy(id = 10), product.copy(id = 11)),
        )
        assertThat(loaded.productCount).isEqualTo(3)
        assertThat(loaded.copy(featured = emptyList()).productCount).isEqualTo(2)
        assertThat(loaded.copy(featured = emptyList(), remaining = emptyList()).productCount).isEqualTo(0)
        assertThat(loaded.productCount).isEqualTo(3)
    }
}
