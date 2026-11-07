package com.cataloguestudio.data

import com.cataloguestudio.domain.Catalogue
import com.cataloguestudio.domain.CatalogueRemoteDataSource
import com.cataloguestudio.domain.DataError
import com.cataloguestudio.domain.Enquiry
import com.cataloguestudio.domain.Product
import com.cataloguestudio.domain.Result
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException

@Serializable
internal data class ProductDto(
    val id: Int,
    val title: String,
    val description: String,
    val price: Double,
    val category: String,
    val imageUrl: String,
    val rating: Double,
    val reviewCount: Int,
) {
    fun toProduct(baseUrl: String) =
        Product(
            id,
            title,
            description,
            price,
            category,
            if (imageUrl.startsWith("/")) "$baseUrl$imageUrl" else imageUrl,
            rating,
            reviewCount,
        )
}

@Serializable
internal data class CatalogueDto(
    val categories: List<String>,
    val featured: List<ProductDto>,
    val products: List<ProductDto>,
)

@Serializable
private data class EnquiryDto(
    val productId: Int,
    val name: String,
    val email: String,
    val message: String,
)

class KtorCatalogueDataSource(private val client: HttpClient, baseUrl: String) :
    CatalogueRemoteDataSource {
    private val baseUrl = baseUrl.trimEnd('/')

    override suspend fun catalogue(
        category: String?,
        search: String?,
    ): Result<Catalogue, DataError> = safeCall {
        val response =
            client.get("$baseUrl/api/catalogue") {
                category?.let { parameter("category", it) }
                search?.takeIf { it.isNotBlank() }?.let { parameter("search", it) }
            }
        when (val result = response.decode<CatalogueDto>()) {
            is Result.Error -> result
            is Result.Success ->
                Result.Success(
                    Catalogue(
                        result.data.categories,
                        result.data.featured.map { it.toProduct(baseUrl) },
                        result.data.products.map { it.toProduct(baseUrl) },
                    )
                )
        }
    }

    override suspend fun product(id: Int): Result<Product, DataError> = safeCall {
        when (val result = client.get("$baseUrl/api/products/$id").decode<ProductDto>()) {
            is Result.Error -> result
            is Result.Success -> Result.Success(result.data.toProduct(baseUrl))
        }
    }

    override suspend fun enquire(enquiry: Enquiry): Result<Unit, DataError> = safeCall {
        val response =
            client.post("$baseUrl/api/enquiries") {
                contentType(ContentType.Application.Json)
                setBody(
                    EnquiryDto(
                        enquiry.productId,
                        enquiry.name.trim(),
                        enquiry.email.trim(),
                        enquiry.message.trim(),
                    )
                )
            }
        if (response.status.value in 200..299) Result.Success(Unit)
        else Result.Error(response.error())
    }

    private suspend inline fun <reified T> HttpResponse.decode(): Result<T, DataError> =
        if (status.value in 200..299) Result.Success(body<T>()) else Result.Error(error())

    private fun HttpResponse.error() =
        when (status.value) {
            400 -> DataError.INVALID_INPUT
            404 -> DataError.NOT_FOUND
            else -> DataError.UNAVAILABLE
        }

    private suspend inline fun <T> safeCall(
        block: () -> Result<T, DataError>
    ): Result<T, DataError> =
        try {
            block()
        } catch (error: CancellationException) {
            throw error
        } catch (_: SerializationException) {
            Result.Error(DataError.INVALID_RESPONSE)
        } catch (_: IOException) {
            Result.Error(DataError.NETWORK)
        } catch (_: Exception) {
            Result.Error(DataError.UNAVAILABLE)
        }
}
