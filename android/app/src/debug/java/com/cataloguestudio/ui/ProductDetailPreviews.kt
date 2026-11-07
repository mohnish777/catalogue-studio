package com.cataloguestudio.ui

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import coil3.annotation.ExperimentalCoilApi
import coil3.asImage
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import com.cataloguestudio.R
import com.cataloguestudio.presentation.ProductDetailState
import com.cataloguestudio.presentation.ProductUi
import com.cataloguestudio.presentation.UiText

@OptIn(ExperimentalCoilApi::class)
@Preview(name = "Product detail", widthDp = 390, heightDp = 950)
@Preview(name = "Narrow product detail", widthDp = 320, heightDp = 950)
@Preview(name = "Larger detail text", widthDp = 390, heightDp = 1100, fontScale = 1.5f)
@Composable
private fun ProductDetailPreview() {
    val handler = AsyncImagePreviewHandler { request ->
        BitmapFactory.decodeResource(request.context.resources, R.drawable.preview_product).asImage()
    }
    StudioTheme {
        CompositionLocalProvider(LocalAsyncImagePreviewHandler provides handler) {
            ProductDetailScreen(
                state = ProductDetailState(
                    isLoading = false,
                    product = ProductUi(
                        id = 9,
                        title = "A considered everyday essential with a longer product title",
                        description = "A compact companion for everyday work and travel. Made to keep your essentials close, wherever the day takes you.",
                        priceLabel = "$64.99",
                        category = "electronics",
                        categoryLabel = UiText.StringResource(R.string.category_electronics),
                        imageUrl = "preview://product",
                        ratingLabel = "4.7",
                        reviewCount = 203,
                    ),
                ),
                onAction = {},
            )
        }
    }
}
