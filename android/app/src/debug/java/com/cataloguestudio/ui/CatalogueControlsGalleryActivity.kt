package com.cataloguestudio.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cataloguestudio.R
import com.cataloguestudio.domain.CatalogueMode
import com.cataloguestudio.presentation.CatalogueAction
import com.cataloguestudio.presentation.CatalogueState
import com.cataloguestudio.presentation.CatalogueViewModel
import com.cataloguestudio.presentation.CategoryUi
import com.cataloguestudio.presentation.UiText
import org.koin.androidx.viewmodel.ext.android.viewModel

class CatalogueControlsGalleryActivity : ComponentActivity() {
    private val viewModel: CatalogueViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StudioTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                CatalogueControlsGallery(state, viewModel::onAction)
            }
        }
    }
}

@Composable
private fun CatalogueControlsGallery(state: CatalogueState, onAction: (CatalogueAction) -> Unit) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.safeDrawingPadding().imePadding()
                .verticalScroll(scrollState).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            CatalogueControls(
                state.query, state.selectedCategory, state.categories, onAction,
                onExplore = { scope.launch { scrollState.animateScrollTo(scrollState.maxValue) } },
            )
            Column {
                Text(stringResource(R.string.gallery_controls_description), style = MaterialTheme.typography.bodySmall)
                Text(
                    stringResource(R.string.gallery_submitted_query, state.submittedQuery),
                    style = MaterialTheme.typography.bodySmall,
                )
                when {
                    state.isLoading -> LoadingState(UiText.StringResource(R.string.state_loading_products))
                    state.error != null -> ErrorState(state.error, onRetry = { onAction(CatalogueAction.OnRetry) })
                    state.isEmpty -> EmptyState(
                        message = UiText.StringResource(
                            if (state.hasActiveFilters) R.string.state_empty_filtered else R.string.state_empty_catalogue,
                        ),
                        onClearFilters = if (state.hasActiveFilters) {
                            { onAction(CatalogueAction.OnClearFilters) }
                        } else null,
                    )
                    else -> state.mode?.let { mode ->
                        CatalogueSummary(
                            sourceLabel = UiText.StringResource(when (mode) {
                                CatalogueMode.DEMO -> R.string.catalogue_mode_demo
                                CatalogueMode.LIVE -> R.string.catalogue_mode_live
                            }),
                            productCount = state.productCount,
                            featuredCount = state.featured.size,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Catalogue controls", widthDp = 390, heightDp = 950, showBackground = true)
@Preview(name = "Narrow controls", widthDp = 320, heightDp = 1100, showBackground = true)
@Preview(name = "Large text controls", widthDp = 390, heightDp = 1400, fontScale = 1.5f, showBackground = true)
@Composable
private fun CatalogueControlsPreview() {
    StudioTheme {
        Surface {
            CatalogueControls(
                query = "wireless headphones",
                selectedCategory = "electronics",
                categories = listOf(
                    CategoryUi("electronics", UiText.StringResource(R.string.category_electronics)),
                    CategoryUi("jewelery", UiText.StringResource(R.string.category_jewellery)),
                    CategoryUi("Home & café essentials", UiText.DynamicString("Home & café essentials")),
                ),
                onAction = {},
                onExplore = {},
                modifier = Modifier.padding(20.dp),
            )
        }
    }
}
