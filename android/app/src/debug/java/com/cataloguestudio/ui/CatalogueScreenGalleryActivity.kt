package com.cataloguestudio.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.cataloguestudio.presentation.CatalogueAction
import com.cataloguestudio.R

// Runs the production Root while destination navigation is reviewed in Slice 10.
class CatalogueScreenGalleryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StudioTheme {
                val reviewState = catalogueReviewState(intent.getStringExtra("scenario"))
                if (reviewState != null) {
                    CatalogueScreen(reviewState, onAction = { action ->
                        val message = when (action) {
                            CatalogueAction.OnRetry -> R.string.gallery_retry_received
                            CatalogueAction.OnClearFilters -> R.string.gallery_clear_received
                            else -> null
                        }
                        message?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
                    })
                } else CatalogueRoot(onNavigateToProduct = { productId ->
                    Toast.makeText(
                        this,
                        getString(R.string.gallery_selected_product_id, productId),
                        Toast.LENGTH_SHORT,
                    ).show()
                })
            }
        }
    }
}
