package com.cataloguestudio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cataloguestudio.R
import com.cataloguestudio.presentation.*
import org.koin.androidx.compose.koinViewModel

@Composable
fun ProductDetailRoot(
    onBack: () -> Unit,
    onEnquire: (Int) -> Unit,
    viewModel: ProductDetailViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            ProductDetailEvent.Back -> onBack()
            is ProductDetailEvent.Enquire -> onEnquire(event.id)
        }
    }
    ProductDetailScreen(state, viewModel::onAction)
}

@Composable
fun ProductDetailScreen(state: ProductDetailState, onAction: (ProductDetailAction) -> Unit) {
    Scaffold(
        containerColor = Paper,
        topBar = {
            PageHeader(stringResource(R.string.details_title)) {
                onAction(ProductDetailAction.Back)
            }
        },
        bottomBar = {
            if (state.product != null && !state.loading && state.error == null)
                Surface(color = Paper, shadowElevation = 8.dp) {
                    Row(
                        Modifier.fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Eyebrow(stringResource(R.string.price_label))
                            Text(state.product.price, style = MaterialTheme.typography.titleLarge)
                        }
                        Spacer(Modifier.width(20.dp))
                        Button(
                            onClick = { onAction(ProductDetailAction.Enquire) },
                            modifier =
                                Modifier.weight(1f).heightIn(min = 54.dp).testTag("enquire_button"),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Text(stringResource(R.string.enquire), Modifier.weight(1f))
                            Spacer(Modifier.width(8.dp))
                            StudioIcon(Mark.ARROW, tint = Color.White)
                        }
                    }
                }
        },
    ) { padding ->
        when {
            state.loading -> Box(Modifier.padding(padding).padding(24.dp)) { LoadingState() }
            state.error != null ->
                Box(Modifier.padding(padding).padding(24.dp)) {
                    ErrorState(state.error) { onAction(ProductDetailAction.Retry) }
                }
            state.product != null -> {
                val product = state.product
                Column(
                    Modifier.fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Box(
                        Modifier.fillMaxWidth()
                            .height(310.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color.White),
                        contentAlignment = Alignment.Center,
                    ) {
                        ProductImage(product, Modifier.fillMaxSize().padding(36.dp))
                    }
                    Spacer(Modifier.height(28.dp))
                    Eyebrow(categoryLabel(product.category).asString())
                    Spacer(Modifier.height(10.dp))
                    Text(product.title, style = MaterialTheme.typography.headlineLarge)
                    Spacer(Modifier.height(14.dp))
                    Text(
                        pluralStringResource(
                            R.plurals.rating_reviews,
                            product.reviewCount,
                            product.rating,
                            product.reviewCount,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Gold,
                    )
                    Spacer(Modifier.height(24.dp))
                    HorizontalDivider(color = Line)
                    Spacer(Modifier.height(24.dp))
                    Eyebrow(stringResource(R.string.details_label))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        product.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Muted,
                    )
                    Spacer(Modifier.height(28.dp))
                    Column(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Sage)
                            .padding(20.dp)
                    ) {
                        Text(
                            stringResource(R.string.details_help),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            stringResource(R.string.details_help_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Muted,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 900)
@Composable
private fun ProductDetailPreview() = StudioTheme {
    ProductDetailScreen(ProductDetailState(loading = false, product = previewProduct), {})
}
