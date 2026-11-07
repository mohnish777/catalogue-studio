package com.cataloguestudio.presentation

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.cataloguestudio.R
import com.cataloguestudio.domain.Product
import org.junit.jupiter.api.Test
import java.util.Locale

class ProductUiTest {
    private val product = Product(
        id = 9,
        title = "Portable drive — café edition",
        description = "Keep your files close.\nReady for travel.",
        price = 64.99,
        category = "electronics",
        imageUrl = "http://10.0.2.2:3000/images/9.jpg",
        rating = 4.7,
        reviewCount = 203,
    )

    @Test
    fun `mapping keeps product content and identity while preparing display values`() {
        val original = product.copy()

        assertThat(product.toProductUi()).isEqualTo(ProductUi(
            id = 9,
            title = product.title,
            description = product.description,
            priceLabel = "$64.99",
            category = "electronics",
            categoryLabel = UiText.StringResource(R.string.category_electronics),
            imageUrl = product.imageUrl,
            ratingLabel = "4.7",
            reviewCount = 203,
        ))
        assertThat(product).isEqualTo(original)
    }

    @Test
    fun `USD prices include cents grouping and display rounding`() {
        for ((price, expected) in listOf(
            0.0 to "$0.00",
            9.5 to "$9.50",
            64.999 to "$65.00",
            1234.5 to "$1,234.50",
        )) {
            assertThat(product.copy(price = price).toProductUi().priceLabel).isEqualTo(expected)
        }
    }

    @Test
    fun `ratings use exactly one decimal place`() {
        for ((rating, expected) in listOf(0.0 to "0.0", 4.74 to "4.7", 4.76 to "4.8", 5.0 to "5.0")) {
            assertThat(product.copy(rating = rating).toProductUi().ratingLabel).isEqualTo(expected)
        }
    }

    @Test
    fun `USD price and rating formatting do not depend on the device format locale`() {
        val original = Locale.getDefault(Locale.Category.FORMAT)
        try {
            for (locale in listOf(Locale.GERMANY, Locale.FRANCE)) {
                Locale.setDefault(Locale.Category.FORMAT, locale)
                val ui = product.copy(price = 1234.5).toProductUi()
                assertThat(ui.priceLabel).isEqualTo("$1,234.50")
                assertThat(ui.ratingLabel).isEqualTo("4.7")
            }
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, original)
        }
    }

    @Test
    fun `known categories use resources while retaining exact API values`() {
        val categories = mapOf(
            "men's clothing" to R.string.category_mens_clothing,
            "women's clothing" to R.string.category_womens_clothing,
            "jewelery" to R.string.category_jewellery,
            "electronics" to R.string.category_electronics,
        )
        for ((category, resource) in categories) {
            val ui = product.copy(category = category).toProductUi()
            assertThat(ui.category).isEqualTo(category)
            assertThat(ui.categoryLabel).isEqualTo(UiText.StringResource(resource))
            assertThat(category.toCategoryLabel()).isEqualTo(ui.categoryLabel)
        }
    }

    @Test
    fun `unknown category labels preserve backend text without guessing`() {
        val category = "Home & café"
        val ui = product.copy(category = category, reviewCount = 0).toProductUi()
        assertThat(ui.category).isEqualTo(category)
        assertThat(ui.categoryLabel).isEqualTo(UiText.DynamicString(category))
        assertThat(ui.reviewCount).isEqualTo(0)
    }
}
