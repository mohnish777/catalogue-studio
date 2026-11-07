package com.cataloguestudio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.Serializable

@Serializable data object CatalogueRoute

@Serializable data class ProductDetailRoute(val productId: Int)

@Serializable data class EnquiryRoute(val productId: Int)

@Composable
fun CatalogueApp() {
    val nav = rememberNavController()
    Box(Modifier.fillMaxSize().background(Paper), contentAlignment = Alignment.TopCenter) {
        NavHost(
            nav,
            startDestination = CatalogueRoute,
            modifier =
                Modifier.widthIn(max = 1000.dp).fillMaxWidth().semantics {
                    testTagsAsResourceId = true
                },
        ) {
            composable<CatalogueRoute> {
                CatalogueRoot(onProduct = { nav.navigate(ProductDetailRoute(it)) })
            }
            composable<ProductDetailRoute> {
                ProductDetailRoot(
                    onBack = { nav.popBackStack() },
                    onEnquire = { nav.navigate(EnquiryRoute(it)) },
                )
            }
            composable<EnquiryRoute> { EnquiryRoot(onBack = { nav.popBackStack() }) }
        }
    }
}
