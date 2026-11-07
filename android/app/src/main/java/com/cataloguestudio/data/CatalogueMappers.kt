package com.cataloguestudio.data

import com.cataloguestudio.domain.Catalogue
import com.cataloguestudio.domain.CatalogueMode
import com.cataloguestudio.domain.Product
import java.net.URI
import kotlinx.serialization.SerializationException

internal fun ProductDto.toProduct(baseUrl: String): Product {
    if (!imageUrl.startsWith("/images/")) {
        throw SerializationException("Expected a backend-relative product image path.")
    }
    val resolvedImageUrl = try {
        URI(baseUrl).resolve(imageUrl).toString()
    } catch (error: IllegalArgumentException) {
        throw SerializationException("Invalid product image path.", error)
    }
    return Product(
        id = id,
        title = title,
        description = description,
        price = price,
        category = category,
        imageUrl = resolvedImageUrl,
        rating = rating,
        reviewCount = reviewCount,
    )
}

internal fun CatalogueDto.toCatalogue(baseUrl: String): Catalogue {
    val catalogueMode = when (mode) {
        "demo" -> CatalogueMode.DEMO
        "live" -> CatalogueMode.LIVE
        else -> throw SerializationException("Unknown catalogue mode: $mode")
    }
    return Catalogue(
        mode = catalogueMode,
        categories = categories,
        featured = featured.map { it.toProduct(baseUrl) },
        remaining = remaining.map { it.toProduct(baseUrl) },
    )
}
