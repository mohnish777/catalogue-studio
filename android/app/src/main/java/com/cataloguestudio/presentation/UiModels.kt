package com.cataloguestudio.presentation

import com.cataloguestudio.R
import com.cataloguestudio.domain.DataError
import com.cataloguestudio.domain.Product
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

sealed interface UiText {
    data class Resource(val id: Int) : UiText

    data class Dynamic(val text: String) : UiText
}

fun DataError.toUiText() =
    UiText.Resource(
        when (this) {
            DataError.NETWORK -> R.string.error_network
            DataError.NOT_FOUND -> R.string.error_not_found
            DataError.INVALID_INPUT -> R.string.error_invalid_input
            DataError.UNAVAILABLE,
            DataError.INVALID_RESPONSE -> R.string.error_unavailable
        }
    )

data class ProductUi(
    val id: Int,
    val title: String,
    val description: String,
    val price: String,
    val category: String,
    val imageUrl: String,
    val rating: String,
    val reviewCount: Int,
)

fun Product.toProductUi(): ProductUi {
    val formatter =
        NumberFormat.getCurrencyInstance(Locale.US).apply { currency = Currency.getInstance("USD") }
    return ProductUi(
        id,
        title,
        description,
        formatter.format(price),
        category,
        imageUrl,
        String.format(Locale.US, "%.1f", rating),
        reviewCount,
    )
}

fun categoryLabel(value: String): UiText =
    when (value) {
        "men's clothing" -> UiText.Resource(R.string.category_men)
        "women's clothing" -> UiText.Resource(R.string.category_women)
        "jewelery" -> UiText.Resource(R.string.category_jewellery)
        "electronics" -> UiText.Resource(R.string.category_tech)
        else -> UiText.Dynamic(value)
    }
