package com.cataloguestudio.domain

data class Enquiry(
    val productId: Long,
    val name: String,
    val email: String,
    val message: String = "",
)
