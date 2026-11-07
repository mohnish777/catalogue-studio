package com.cataloguestudio.ui

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil3.annotation.ExperimentalCoilApi
import coil3.asImage
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import com.cataloguestudio.R
import com.cataloguestudio.domain.CatalogueMode
import com.cataloguestudio.presentation.CatalogueAction
import com.cataloguestudio.presentation.CatalogueEvent
import com.cataloguestudio.presentation.CatalogueState
import com.cataloguestudio.presentation.CatalogueViewModel
import com.cataloguestudio.presentation.ProductUi
import com.cataloguestudio.presentation.UiText
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class CatalogueProductsGalleryActivity : ComponentActivity() {
    private val viewModel: CatalogueViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is CatalogueEvent.NavigateToProduct -> Toast.makeText(
                            this@CatalogueProductsGalleryActivity,
                            getString(R.string.gallery_selected_product_id, event.productId),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
            }
        }
        setContent {
            StudioTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                CatalogueProductsGallery(state, viewModel::onAction)
            }
        }
    }
}

@Composable
private fun CatalogueProductsGallery(state: CatalogueState, onAction: (CatalogueAction) -> Unit) {
    val minimumCardWidth = 156.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
    Surface(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minimumCardWidth),
            modifier = Modifier.safeDrawingPadding(),
            contentPadding = PaddingValues(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item(key = "review-header", span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.gallery_products_title), style = MaterialTheme.typography.headlineLarge)
                    Text(stringResource(R.string.gallery_products_description), style = MaterialTheme.typography.bodyMedium)
                    LazyRow {
                        item { TextButton(onClick = { onAction(CatalogueAction.OnCategorySelect(null)) }) {
                            Text(stringResource(R.string.catalogue_all_categories))
                        } }
                        item { TextButton(onClick = { onAction(CatalogueAction.OnCategorySelect("electronics")) }) {
                            Text(stringResource(R.string.category_electronics))
                        } }
                        item { TextButton(onClick = {
                            onAction(CatalogueAction.OnQueryChange("drive"))
                            onAction(CatalogueAction.OnSearch)
                        }) { Text(stringResource(R.string.gallery_search_drives)) } }
                    }
                }
            }
            when {
                state.isLoading -> item(key = "loading", span = { GridItemSpan(maxLineSpan) }) {
                    LoadingState(UiText.StringResource(R.string.state_loading_products))
                }
                state.error != null -> item(key = "error", span = { GridItemSpan(maxLineSpan) }) {
                    ErrorState(state.error, onRetry = { onAction(CatalogueAction.OnRetry) })
                }
                state.isEmpty -> item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(
                        message = UiText.StringResource(
                            if (state.hasActiveFilters) R.string.state_empty_filtered else R.string.state_empty_catalogue,
                        ),
                        onClearFilters = if (state.hasActiveFilters) {
                            { onAction(CatalogueAction.OnClearFilters) }
                        } else null,
                    )
                }
                else -> {
                    item(key = "summary", span = { GridItemSpan(maxLineSpan) }) {
                        state.mode?.let { mode -> CatalogueSummary(
                            sourceLabel = UiText.StringResource(when (mode) {
                                CatalogueMode.DEMO -> R.string.catalogue_mode_demo
                                CatalogueMode.LIVE -> R.string.catalogue_mode_live
                            }),
                            productCount = state.productCount,
                            featuredCount = state.featured.size,
                        ) }
                    }
                    catalogueProductSections(
                        featured = state.featured,
                        remaining = state.remaining,
                        featuredTitle = UiText.StringResource(
                            if (state.selectedCategory == null) R.string.catalogue_featured_title else R.string.catalogue_category_picks,
                        ),
                        remainingTitle = UiText.StringResource(
                            if (state.submittedQuery.isNotBlank()) R.string.catalogue_search_results else R.string.catalogue_more_products,
                        ),
                        onAction = onAction,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalCoilApi::class)
@Preview(name = "Product sections", widthDp = 390, heightDp = 1200, showBackground = true)
@Preview(name = "Narrow product sections", widthDp = 320, heightDp = 1400, showBackground = true)
@Preview(name = "Large product sections", widthDp = 390, heightDp = 1800, fontScale = 1.5f, showBackground = true)
@Composable
private fun CatalogueProductsPreview() {
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
            CatalogueProductsGallery(
                state = CatalogueState(
                    isLoading = false, mode = CatalogueMode.DEMO,
                    featured = listOf(product, product.copy(id = 2)),
                    remaining = listOf(product.copy(id = 9), product.copy(id = 10), product.copy(id = 11)),
                ),
                onAction = {},
            )
        }
    }
}
