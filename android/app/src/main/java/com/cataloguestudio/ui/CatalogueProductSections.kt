package com.cataloguestudio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cataloguestudio.presentation.CatalogueAction
import com.cataloguestudio.presentation.ProductUi
import com.cataloguestudio.presentation.UiText

// The parent owns one vertical grid; this adds full-width sections and individual grid cards.
internal fun LazyGridScope.catalogueProductSections(
    featured: List<ProductUi>,
    remaining: List<ProductUi>,
    featuredTitle: UiText,
    remainingTitle: UiText,
    onAction: (CatalogueAction) -> Unit,
) {
    if (featured.isNotEmpty()) {
        item(key = "featured-heading", span = { GridItemSpan(maxLineSpan) }, contentType = "heading") {
            Text(
                text = featuredTitle.asString(),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        item(key = "featured-products", span = { GridItemSpan(maxLineSpan) }, contentType = "carousel") {
            BoxWithConstraints {
                val cardWidth = minOf(240.dp * LocalDensity.current.fontScale.coerceAtLeast(1f), maxWidth)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(featured, key = { it.id }, contentType = { "product" }) { product ->
                        ProductCard(
                            product = product,
                            onClick = { onAction(CatalogueAction.OnProductClick(product.id)) },
                            modifier = Modifier.width(cardWidth),
                        )
                    }
                }
            }
        }
    }
    if (remaining.isNotEmpty()) {
        item(key = "remaining-heading", span = { GridItemSpan(maxLineSpan) }, contentType = "heading") {
            Text(
                text = remainingTitle.asString(),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        items(remaining, key = { "product:${it.id}" }, contentType = { "product" }) { product ->
            ProductCard(
                product = product,
                onClick = { onAction(CatalogueAction.OnProductClick(product.id)) },
            )
        }
    }
}
