package com.cataloguestudio.data

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.cataloguestudio.domain.CatalogueMode
import com.cataloguestudio.domain.Product
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CatalogueMappingTest {
    private val baseUrl = "http://10.0.2.2:3000"
    private val productJson = """
        {
          "id": 9,
          "title": "Portable everyday drive",
          "description": "Keep your everyday files close.",
          "price": 64.99,
          "category": "electronics",
          "imageUrl": "/images/9.jpg",
          "rating": 4.7,
          "reviewCount": 203
        }
    """.trimIndent()

    @Test
    fun `product response decodes and maps every field including a usable image URL`() {
        val product = catalogueJson.decodeFromString<ProductDto>(productJson).toProduct(baseUrl)

        assertThat(product).isEqualTo(Product(
            id = 9,
            title = "Portable everyday drive",
            description = "Keep your everyday files close.",
            price = 64.99,
            category = "electronics",
            imageUrl = "http://10.0.2.2:3000/images/9.jpg",
            rating = 4.7,
            reviewCount = 203,
        ))
    }

    @Test
    fun `catalogue decodes both explicit modes and preserves product groups and order`() {
        val secondProductJson = productJson
            .replace("\"id\": 9", "\"id\": 10")
            .replace("/images/9.jpg", "/images/10.jpg")
        for ((mode, expected) in listOf("demo" to CatalogueMode.DEMO, "live" to CatalogueMode.LIVE)) {
            val json = """
                {
                  "mode": "$mode",
                  "categories": ["electronics", "jewelery"],
                  "featured": [$secondProductJson, $productJson],
                  "remaining": []
                }
            """.trimIndent()
            val catalogue = catalogueJson.decodeFromString<CatalogueDto>(json).toCatalogue(baseUrl)

            assertThat(catalogue.mode).isEqualTo(expected)
            assertThat(catalogue.categories).isEqualTo(listOf("electronics", "jewelery"))
            assertThat(catalogue.featured.map { it.id }).isEqualTo(listOf(10L, 9L))
            assertThat(catalogue.featured[0].imageUrl).isEqualTo("$baseUrl/images/10.jpg")
            assertThat(catalogue.remaining).isEqualTo(emptyList())
        }
    }

    @Test
    fun `remaining search results map without creating featured products`() {
        val json = """{"mode":"demo","categories":["electronics"],"featured":[],"remaining":[$productJson]}"""
        val catalogue = catalogueJson.decodeFromString<CatalogueDto>(json).toCatalogue(baseUrl)

        assertThat(catalogue.featured).isEqualTo(emptyList())
        assertThat(catalogue.remaining.single().imageUrl).isEqualTo("$baseUrl/images/9.jpg")
    }

    @Test
    fun `unknown additive fields do not break decoding`() {
        val json = productJson.replace("\"id\": 9", "\"id\": 9, \"futureField\": true")
        assertThat(catalogueJson.decodeFromString<ProductDto>(json).toProduct(baseUrl).id)
            .isEqualTo(9L)
    }

    @Test
    fun `missing required fields and explicit nulls fail instead of becoming defaults`() {
        for (json in listOf(
            """{"mode":"demo","categories":[],"featured":[]}""",
            """{"mode":"demo","categories":[],"featured":[],"remaining":null}""",
            productJson.replace("\"title\": \"Portable everyday drive\",", ""),
            productJson.replace("\"title\": \"Portable everyday drive\"", "\"title\": null"),
        )) {
            assertThrows<SerializationException> {
                if (json.contains("\"mode\"")) catalogueJson.decodeFromString<CatalogueDto>(json)
                else catalogueJson.decodeFromString<ProductDto>(json)
            }
        }
    }

    @Test
    fun `invalid number shapes and malformed JSON fail decoding`() {
        for (json in listOf(
            productJson.replace("\"id\": 9", "\"id\": 9.5"),
            productJson.replace("\"rating\": 4.7", "\"rating\": {}"),
            "not JSON",
        )) {
            assertThrows<SerializationException> { catalogueJson.decodeFromString<ProductDto>(json) }
        }
    }

    @Test
    fun `unknown modes fail rather than silently showing demo data`() {
        val dto = CatalogueDto("preview", emptyList(), emptyList(), emptyList())
        assertThrows<SerializationException> { dto.toCatalogue(baseUrl) }
    }

    @Test
    fun `image mapping preserves backend origin and port with either base URL slash style`() {
        val dto = catalogueJson.decodeFromString<ProductDto>(productJson)
        for (base in listOf("https://catalogue.example:8443", "https://catalogue.example:8443/")) {
            assertThat(dto.toProduct(base).imageUrl)
                .isEqualTo("https://catalogue.example:8443/images/9.jpg")
        }
        for (path in listOf("https://other.example/9.jpg", "//other.example/9.jpg", "", "/images/%zz.jpg")) {
            assertThrows<SerializationException> { dto.copy(imageUrl = path).toProduct(baseUrl) }
        }
    }

    @Test
    fun `product IDs above Int range are preserved as Long`() {
        val json = productJson.replace("\"id\": 9", "\"id\": 2147483648")
        assertThat(catalogueJson.decodeFromString<ProductDto>(json).toProduct(baseUrl).id)
            .isEqualTo(2_147_483_648L)
    }
}
