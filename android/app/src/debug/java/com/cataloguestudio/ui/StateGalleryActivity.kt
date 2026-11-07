package com.cataloguestudio.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cataloguestudio.R
import com.cataloguestudio.domain.DataError
import com.cataloguestudio.presentation.UiText
import com.cataloguestudio.presentation.toUiText

class StateGalleryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StudioTheme {
                val context = LocalContext.current
                StateGallery(
                    onRetry = {
                        Toast.makeText(context, R.string.gallery_retry_received, Toast.LENGTH_SHORT).show()
                    },
                    onClearFilters = {
                        Toast.makeText(context, R.string.gallery_clear_received, Toast.LENGTH_SHORT).show()
                    },
                )
            }
        }
    }
}

@Composable
private fun StateGallery(onRetry: () -> Unit, onClearFilters: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(stringResource(R.string.gallery_states_title), style = MaterialTheme.typography.headlineLarge)
            Text(stringResource(R.string.gallery_states_description), style = MaterialTheme.typography.bodyMedium)
            CatalogueSummary(UiText.StringResource(R.string.catalogue_mode_demo), productCount = 13, featuredCount = 5)
            HorizontalDivider()
            LoadingState(UiText.StringResource(R.string.state_loading_products))
            HorizontalDivider()
            ErrorState(message = DataError.SERVER_ERROR.toUiText(), onRetry = onRetry)
            HorizontalDivider()
            EmptyState(
                message = UiText.StringResource(R.string.state_empty_filtered),
                onClearFilters = onClearFilters,
            )
            HorizontalDivider()
            EmptyState(message = UiText.StringResource(R.string.state_empty_catalogue))
            CatalogueSummary(UiText.StringResource(R.string.catalogue_mode_live), productCount = 0, featuredCount = 0)
        }
    }
}

@Preview(name = "Catalogue states", widthDp = 390, heightDp = 1800, showBackground = true)
@Preview(name = "Narrow states", widthDp = 320, heightDp = 1800, showBackground = true)
@Preview(name = "Large text states", widthDp = 390, heightDp = 2400, fontScale = 1.5f, showBackground = true)
@Composable
private fun StateGalleryPreview() {
    StudioTheme { StateGallery(onRetry = {}, onClearFilters = {}) }
}
