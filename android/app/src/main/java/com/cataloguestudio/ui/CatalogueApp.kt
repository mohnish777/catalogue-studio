package com.cataloguestudio.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
    composable<EnquiryRoute> {
        EnquiryRoot(onBack = { navController.popBackStack() })
    }
}
