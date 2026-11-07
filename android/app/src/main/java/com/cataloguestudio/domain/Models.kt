package com.cataloguestudio.domain

data class Product(
    val id: Long,
    val title: String,
    val description: String,
    val price: Double,
    val category: String,
    val imageUrl: String,
    val rating: Double,
    val reviewCount: Int,
)

enum class CatalogueMode {
    DEMO,
    LIVE,
}

data class Catalogue(
    val mode: CatalogueMode,
    val categories: List<String>,
    val featured: List<Product>,
    val remaining: List<Product>,
)
