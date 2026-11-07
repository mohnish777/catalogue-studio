package com.cataloguestudio.presentation

import com.cataloguestudio.R
import com.cataloguestudio.domain.Product
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

internal data class ProductUi(
    val id: Long,
    val title: String,
    val description: String,
    val priceLabel: String,
    val category: String,
    val categoryLabel: UiText,
    val imageUrl: String,
    val ratingLabel: String,
    val reviewCount: Int,
)

internal fun Product.toProductUi(): ProductUi = ProductUi(
    id = id,
    title = title,
    description = description,
    priceLabel = NumberFormat.getCurrencyInstance(Locale.US).apply {
        currency = Currency.getInstance("USD")
    }.format(price),
    category = category,
    categoryLabel = category.toCategoryLabel(),
    imageUrl = imageUrl,
    ratingLabel = String.format(Locale.US, "%.1f", rating),
    reviewCount = reviewCount,
)

// Keep the API value for filtering; only its display label changes.
internal fun String.toCategoryLabel(): UiText = when (this) {
    "men's clothing" -> UiText.StringResource(R.string.category_mens_clothing)
    "women's clothing" -> UiText.StringResource(R.string.category_womens_clothing)
    "jewelery" -> UiText.StringResource(R.string.category_jewellery)
    "electronics" -> UiText.StringResource(R.string.category_electronics)
    else -> UiText.DynamicString(this)
}
