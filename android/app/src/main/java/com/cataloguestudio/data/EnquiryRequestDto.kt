package com.cataloguestudio.data

import com.cataloguestudio.domain.Enquiry
import kotlinx.serialization.Serializable

@Serializable
internal data class EnquiryRequestDto(
    val productId: Long,
    val name: String,
    val email: String,
    val message: String,
)

internal fun Enquiry.toRequestDto(): EnquiryRequestDto = EnquiryRequestDto(
    productId = productId,
    name = name,
    email = email,
    message = message,
)
