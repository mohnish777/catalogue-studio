package com.cataloguestudio.domain

interface CatalogueRemoteDataSource {
    suspend fun getCatalogue(
        category: String? = null,
        query: String? = null,
    ): Result<Catalogue, DataError>

    suspend fun getProduct(id: Long): Result<Product, DataError>

    suspend fun sendEnquiry(enquiry: Enquiry): Result<Unit, DataError>
}
