package com.cataloguestudio.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.cataloguestudio.R
import com.cataloguestudio.presentation.navigation.CatalogueRoute
import com.cataloguestudio.presentation.navigation.EnquiryRoute
import com.cataloguestudio.presentation.navigation.ProductDetailRoute

@Composable
internal fun CatalogueApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = CatalogueRoute) {
        catalogueGraph(navController)
    }
}

// One feature in this PoC: the graph owns navigation; Roots expose callbacks only.
private fun NavGraphBuilder.catalogueGraph(navController: NavHostController) {
    composable<CatalogueRoute> {
        CatalogueRoot(onNavigateToProduct = { productId ->
            navController.navigate(ProductDetailRoute(productId)) { launchSingleTop = true }
        })
    }
    composable<ProductDetailRoute> {
        ProductDetailRoot(
            onBack = { navController.popBackStack() },
            onNavigateToEnquiry = { productId ->
                navController.navigate(EnquiryRoute(productId)) { launchSingleTop = true }
            },
        )
    }
    composable<EnquiryRoute> { entry ->
        val route = entry.toRoute<EnquiryRoute>()
        NavigationPlaceholder(
            title = R.string.enquiry_title,
            productId = route.productId,
            onBack = { navController.popBackStack() },
        )
    }
}

// Temporary enquiry shell; Slice 14 adds the form.
@Composable
private fun NavigationPlaceholder(
    @StringRes title: Int,
    productId: Long,
    onBack: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.safeDrawingPadding().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(title), style = MaterialTheme.typography.headlineLarge)
            Text(stringResource(R.string.navigation_product_reference, productId))
            TextButton(onClick = onBack) { Text(stringResource(R.string.navigation_back)) }
        }
    }
}
