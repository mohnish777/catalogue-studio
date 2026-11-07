package com.cataloguestudio.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
internal data object CatalogueRoute

@Serializable
internal data class ProductDetailRoute(val productId: Long)

@Serializable
internal data class EnquiryRoute(val productId: Long)
