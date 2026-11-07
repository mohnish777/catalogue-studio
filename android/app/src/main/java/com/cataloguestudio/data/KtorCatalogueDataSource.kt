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

internal class KtorCatalogueDataSource(
    private val client: HttpClient,
    private val baseUrl: String,
) : CatalogueRemoteDataSource {
    override suspend fun getCatalogue(
        category: String?,
        query: String?,
    ): Result<Catalogue, DataError> = safeRequest(
        execute = {
            client.get("/api/catalogue") {
                category?.let { parameter("category", it) }
                query?.let { parameter("q", it) }
            }
        },
        decode = { it.body<CatalogueDto>().toCatalogue(baseUrl) },
    )

    override suspend fun getProduct(id: Long): Result<Product, DataError> = safeRequest(
        execute = { client.get("/api/products/$id") },
        decode = { it.body<ProductDto>().toProduct(baseUrl) },
    )

    // The backend enquiry endpoint is introduced in Slice 12.
    override suspend fun sendEnquiry(enquiry: Enquiry): Result<Unit, DataError> = safeRequest(
        execute = {
            client.post("/api/enquiries") {
                setBody(enquiry.toRequestDto())
            }
        },
        decode = { Unit },
    )
}
