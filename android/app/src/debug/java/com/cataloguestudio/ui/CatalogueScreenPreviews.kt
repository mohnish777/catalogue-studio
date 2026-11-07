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
import com.cataloguestudio.domain.CatalogueMode
import com.cataloguestudio.presentation.CatalogueState
import com.cataloguestudio.presentation.CategoryUi
import com.cataloguestudio.presentation.ProductUi
import com.cataloguestudio.presentation.UiText

// Fixed screen states for layout review only; normal debug launches use the real Root.
internal fun catalogueReviewState(scenario: String?): CatalogueState? = when (scenario) {
    "loading" -> CatalogueState()
    "error" -> CatalogueState(isLoading = false, error = UiText.StringResource(R.string.error_connection))
    "empty" -> CatalogueState(isLoading = false, mode = CatalogueMode.DEMO)
    "filtered-empty" -> CatalogueState(
        query = "unmatched", submittedQuery = "unmatched", isLoading = false, mode = CatalogueMode.DEMO,
    )
    else -> null
}

@OptIn(ExperimentalCoilApi::class)
@Preview(name = "Catalogue", widthDp = 390, heightDp = 1500)
@Preview(name = "Narrow catalogue", widthDp = 320, heightDp = 1500)
@Preview(name = "Large text catalogue", widthDp = 390, heightDp = 1800, fontScale = 1.5f)
@Composable
private fun CatalogueScreenPreview() {
    val product = ProductUi(
        id = 1, title = "A considered everyday essential with a longer product title",
        description = "A product description.", priceLabel = "$64.99", category = "electronics",
        categoryLabel = UiText.StringResource(R.string.category_electronics),
        imageUrl = "preview://product", ratingLabel = "4.7", reviewCount = 203,
    )
    val previewHandler = AsyncImagePreviewHandler { request ->
        BitmapFactory.decodeResource(request.context.resources, R.drawable.preview_product).asImage()
    }
    StudioTheme {
        CompositionLocalProvider(LocalAsyncImagePreviewHandler provides previewHandler) {
            CatalogueScreen(
                state = CatalogueState(
                    isLoading = false, mode = CatalogueMode.DEMO,
                    categories = listOf(CategoryUi("electronics", product.categoryLabel)),
                    featured = listOf(product),
                    remaining = listOf(product.copy(id = 2), product.copy(id = 3)),
                ),
                onAction = {},
            )
        }
    }
}

@Preview(name = "Loading", widthDp = 390, heightDp = 1300)
@Composable
private fun CatalogueLoadingPreview() {
    StudioTheme { CatalogueScreen(catalogueReviewState("loading")!!, onAction = {}) }
}

@Preview(name = "Error", widthDp = 390, heightDp = 1300)
@Composable
private fun CatalogueErrorPreview() {
    StudioTheme { CatalogueScreen(catalogueReviewState("error")!!, onAction = {}) }
}

@Preview(name = "Filtered empty", widthDp = 390, heightDp = 1300)
@Composable
private fun CatalogueEmptyPreview() {
    StudioTheme { CatalogueScreen(catalogueReviewState("filtered-empty")!!, onAction = {}) }
}
