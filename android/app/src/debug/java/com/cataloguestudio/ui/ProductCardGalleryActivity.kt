package com.cataloguestudio.ui

import android.graphics.BitmapFactory
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.annotation.ExperimentalCoilApi
import coil3.asImage
import coil3.compose.AsyncImagePainter
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import coil3.request.ErrorResult
import com.cataloguestudio.BuildConfig
import com.cataloguestudio.R
import com.cataloguestudio.presentation.ProductUi
import com.cataloguestudio.presentation.UiText

// A debug-only review surface; the launcher and release app do not expose this gallery.
class ProductCardGalleryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StudioTheme {
                val context = LocalContext.current
                ProductCardGallery(
                    onProductClick = { product ->
                        Toast.makeText(
                            context,
                            context.getString(R.string.gallery_selected_product, product.title),
                            Toast.LENGTH_SHORT,
                        ).show()
                    },
                    onShowStates = {
                        startActivity(Intent(this@ProductCardGalleryActivity, StateGalleryActivity::class.java))
                    },
                )
            }
        }
    }
}

@Composable
private fun ProductCardGallery(onProductClick: (ProductUi) -> Unit, onShowStates: () -> Unit = {}) {
    val product = ProductUi(
        id = 9,
        title = "Portable everyday drive",
        description = "Keep your everyday files close.",
        priceLabel = "$64.99",
        category = "electronics",
        categoryLabel = UiText.StringResource(R.string.category_electronics),
        imageUrl = "${BuildConfig.BASE_URL.trimEnd('/')}/images/9.jpg",
        ratingLabel = "4.7",
        reviewCount = 203,
    )
    val missingImage = product.copy(
        id = 999999,
        title = "A deliberately long product title to check wrapping on a narrow phone",
        imageUrl = "${BuildConfig.BASE_URL.trimEnd('/')}/images/999999.jpg",
        priceLabel = "$1,234.50",
        reviewCount = 1,
    )
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.gallery_title), style = MaterialTheme.typography.headlineLarge)
            Text(stringResource(R.string.gallery_description), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onShowStates) { Text(stringResource(R.string.gallery_show_states)) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ProductCard(product, { onProductClick(product) }, Modifier.weight(1f))
                ProductCard(missingImage, { onProductClick(missingImage) }, Modifier.weight(1f))
            }
            Text(stringResource(R.string.gallery_loading), style = MaterialTheme.typography.titleMedium)
            ProductImageLoading(Modifier.fillMaxWidth().height(120.dp))
        }
    }
}

@OptIn(ExperimentalCoilApi::class)
@Preview(name = "Product cards", widthDp = 390, heightDp = 900, showBackground = true)
@Preview(name = "Narrow phone", widthDp = 320, heightDp = 900, showBackground = true)
@Preview(name = "Large text", widthDp = 390, heightDp = 1000, fontScale = 1.5f, showBackground = true)
@Composable
private fun ProductCardsPreview() {
    // Preview networking is disabled; use a bundled debug fixture for success and a forced error.
    val previewHandler = AsyncImagePreviewHandler { imageLoader, request ->
        if (request.data.toString().endsWith("/999999.jpg")) {
            AsyncImagePainter.State.Error(
                painter = null,
                result = ErrorResult(null, request, IllegalStateException("Preview missing image")),
            )
        } else {
            val image = BitmapFactory.decodeResource(request.context.resources, R.drawable.preview_product).asImage()
            AsyncImagePreviewHandler { image }.handle(imageLoader, request)
        }
    }
    StudioTheme {
        CompositionLocalProvider(LocalAsyncImagePreviewHandler provides previewHandler) {
            ProductCardGallery(onProductClick = {})
        }
    }
}
