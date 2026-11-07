package com.cataloguestudio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cataloguestudio.R
import com.cataloguestudio.domain.CatalogueMode
import com.cataloguestudio.presentation.CatalogueAction
import com.cataloguestudio.presentation.CatalogueEvent
import com.cataloguestudio.presentation.CatalogueState
import com.cataloguestudio.presentation.CatalogueViewModel
import com.cataloguestudio.presentation.UiText
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun CatalogueRoot(
    onNavigateToProduct: (Long) -> Unit,
    viewModel: CatalogueViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is CatalogueEvent.NavigateToProduct -> onNavigateToProduct(event.productId)
        }
    }
    CatalogueScreen(state = state, onAction = viewModel::onAction)
}

@Composable
internal fun CatalogueScreen(state: CatalogueState, onAction: (CatalogueAction) -> Unit) {
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val minimumCardWidth = 156.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.safeDrawingPadding().imePadding(), contentAlignment = Alignment.TopCenter) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minimumCardWidth),
                state = gridState,
                modifier = Modifier.widthIn(max = 1100.dp).fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                item(key = "controls", span = { GridItemSpan(maxLineSpan) }, contentType = "controls") {
                    CatalogueControls(
                        query = state.query,
                        selectedCategory = state.selectedCategory,
                        categories = state.categories,
                        onAction = onAction,
                        // Item 1 always exists: feedback while loading, then the confirmed result summary.
                        onExplore = { scope.launch { gridState.animateScrollToItem(1) } },
                    )
                }
                item(key = "feedback", span = { GridItemSpan(maxLineSpan) }, contentType = "feedback") {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        when {
                            state.isLoading -> LoadingState(UiText.StringResource(R.string.state_loading_products))
                            state.error != null -> ErrorState(
                                message = state.error,
                                onRetry = { onAction(CatalogueAction.OnRetry) },
                            )
                            else -> {
                                state.mode?.let { mode ->
                                    CatalogueSummary(
                                        sourceLabel = UiText.StringResource(when (mode) {
                                            CatalogueMode.DEMO -> R.string.catalogue_mode_demo
                                            CatalogueMode.LIVE -> R.string.catalogue_mode_live
                                        }),
                                        productCount = state.productCount,
                                        featuredCount = state.featured.size,
                                    )
                                }
                                if (state.submittedQuery.isNotBlank()) {
                                    Text(
                                        text = stringResource(R.string.catalogue_applied_search, state.submittedQuery),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                                if (state.isEmpty) {
                                    EmptyState(
                                        message = UiText.StringResource(
                                            if (state.hasActiveFilters) R.string.state_empty_filtered
                                            else R.string.state_empty_catalogue,
                                        ),
                                        onClearFilters = if (state.hasActiveFilters) {
                                            { onAction(CatalogueAction.OnClearFilters) }
                                        } else null,
                                    )
                                }
                            }
                        }
                    }
                }
                if (!state.isLoading && state.error == null && !state.isEmpty) {
                    catalogueProductSections(
                        featured = state.featured,
                        remaining = state.remaining,
                        featuredTitle = UiText.StringResource(
                            if (state.selectedCategory == null) R.string.catalogue_featured_title
                            else R.string.catalogue_category_picks,
                        ),
                        remainingTitle = UiText.StringResource(
                            if (state.submittedQuery.isNotBlank()) R.string.catalogue_search_results
                            else R.string.catalogue_more_products,
                        ),
                        onAction = onAction,
                    )
                }
            }
        }
    }
}
