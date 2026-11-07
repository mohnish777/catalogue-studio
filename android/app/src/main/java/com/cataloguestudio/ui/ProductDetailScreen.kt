package com.cataloguestudio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cataloguestudio.R
import com.cataloguestudio.presentation.ProductDetailAction
import com.cataloguestudio.presentation.ProductDetailEvent
import com.cataloguestudio.presentation.ProductDetailState
import com.cataloguestudio.presentation.ProductDetailViewModel
import com.cataloguestudio.presentation.UiText
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun ProductDetailRoot(
    onBack: () -> Unit,
    onNavigateToEnquiry: (Long) -> Unit,
    viewModel: ProductDetailViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            ProductDetailEvent.NavigateBack -> onBack()
            is ProductDetailEvent.NavigateToEnquiry -> onNavigateToEnquiry(event.productId)
        }
    }
    ProductDetailScreen(state, viewModel::onAction)
}

@Composable
internal fun ProductDetailScreen(state: ProductDetailState, onAction: (ProductDetailAction) -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(contentAlignment = Alignment.TopCenter) {
            Scaffold(
                modifier = Modifier.widthIn(max = 840.dp).fillMaxSize().safeDrawingPadding(),
                topBar = {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        TextButton(onClick = { onAction(ProductDetailAction.OnBack) }) {
                            Text(stringResource(R.string.navigation_back))
                        }
                        Text(stringResource(R.string.product_detail_title), style = MaterialTheme.typography.titleMedium)
                    }
                },
                bottomBar = {
                    if (!state.isLoading && state.error == null && state.product != null) {
                        Surface(tonalElevation = 3.dp) {
                            Button(
                                onClick = { onAction(ProductDetailAction.OnEnquire) },
                                modifier = Modifier.fillMaxWidth().padding(20.dp),
                            ) {
                                Text(stringResource(R.string.product_enquire))
                            }
                        }
                    }
                },
            ) { padding ->
                Column(
                    modifier = Modifier.padding(padding).fillMaxSize()
                        .verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    when {
                        state.isLoading -> LoadingState(UiText.StringResource(R.string.state_loading_product))
                        state.error != null -> ErrorState(
                            message = state.error,
                            onRetry = { onAction(ProductDetailAction.OnRetry) },
                        )
                        else -> state.product?.let { product ->
                            Surface(shape = RoundedCornerShape(24.dp)) {
                                ProductImage(
                                    imageUrl = product.imageUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxWidth().height(300.dp),
                                )
                            }
                            Text(
                                text = product.categoryLabel.asString(),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Text(
                                text = product.title,
                                modifier = Modifier.semantics { heading() },
                                style = MaterialTheme.typography.headlineLarge,
                            )
                            Text(
                                text = product.priceLabel,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.headlineMedium,
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(stringResource(R.string.product_rating, product.ratingLabel))
                                Text(pluralStringResource(R.plurals.product_reviews, product.reviewCount, product.reviewCount))
                            }
                            HorizontalDivider()
                            Text(
                                text = stringResource(R.string.product_about),
                                modifier = Modifier.semantics { heading() },
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(product.description, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}
