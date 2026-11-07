package com.cataloguestudio.domain

data class Product(
    val id: Int,
    val title: String,
    val description: String,
    val price: Double,
    val category: String,
    val imageUrl: String,
    val rating: Double,
    val reviewCount: Int,
)

data class Catalogue(
    val categories: List<String>,
    val featured: List<Product>,
    val products: List<Product>,
)

data class Enquiry(val productId: Int, val name: String, val email: String, val message: String)

interface DomainError

enum class DataError : DomainError {
    NETWORK,
    UNAVAILABLE,
    NOT_FOUND,
    INVALID_INPUT,
    INVALID_RESPONSE,
}

sealed interface Result<out T, out E : DomainError> {
    data class Success<T>(val data: T) : Result<T, Nothing>

    data class Error<E : DomainError>(val error: E) : Result<Nothing, E>
}

interface CatalogueRemoteDataSource {
    suspend fun catalogue(
        category: String? = null,
        search: String? = null,
    ): Result<Catalogue, DataError>

    suspend fun product(id: Int): Result<Product, DataError>

    suspend fun enquire(enquiry: Enquiry): Result<Unit, DataError>
}

// The backend also validates. This gives immediate feedback without a round trip.
data class EnquiryValidation(
    val nameInvalid: Boolean,
    val emailInvalid: Boolean,
    val messageInvalid: Boolean,
) {
    val valid: Boolean
        get() = !nameInvalid && !emailInvalid && !messageInvalid
}

fun validateEnquiry(name: String, email: String, message: String) =
    EnquiryValidation(
        name.trim().length !in 2..100,
        email.trim().length > 254 ||
            !Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$").matches(email.trim()),
        message.length > 2000,
    )
