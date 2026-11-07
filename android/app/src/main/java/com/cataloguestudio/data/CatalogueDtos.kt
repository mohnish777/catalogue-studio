package com.cataloguestudio.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal val catalogueJson = Json { ignoreUnknownKeys = true }

@Serializable
internal data class ProductDto(
    val id: Long,
    val title: String,
    val description: String,
    val price: Double,
    val category: String,
    val imageUrl: String,
    val rating: Double,
    val reviewCount: Int,
)

@Serializable
internal data class CatalogueDto(
    val mode: String,
    val categories: List<String>,
    val featured: List<ProductDto>,
    val remaining: List<ProductDto>,
)
