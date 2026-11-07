package com.cataloguestudio.domain

enum class EnquiryValidationError : AppError {
    NAME_REQUIRED,
    NAME_TOO_LONG,
    NAME_INVALID,
    EMAIL_REQUIRED,
    EMAIL_TOO_LONG,
    EMAIL_INVALID,
    MESSAGE_TOO_LONG,
}

object EnquiryValidator {
    const val MAX_NAME_LENGTH = 100
    const val MAX_EMAIL_LENGTH = 254
    const val MAX_MESSAGE_LENGTH = 2_000

    // A practical address format check; it does not verify mailbox existence.
    private val emailPattern = Regex(
        """[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+""",
    )

    fun validateName(raw: String): Result<String, EnquiryValidationError> {
        val value = raw.trim()
        return when {
            value.isEmpty() -> Result.Error(EnquiryValidationError.NAME_REQUIRED)
            raw.any { it.isISOControl() } -> Result.Error(EnquiryValidationError.NAME_INVALID)
            value.length > MAX_NAME_LENGTH -> Result.Error(EnquiryValidationError.NAME_TOO_LONG)
            else -> Result.Success(value)
        }
    }

    fun validateEmail(raw: String): Result<String, EnquiryValidationError> {
        val value = raw.trim()
        val localPart = value.substringBefore('@')
        return when {
            value.isEmpty() -> Result.Error(EnquiryValidationError.EMAIL_REQUIRED)
            value.length > MAX_EMAIL_LENGTH -> Result.Error(EnquiryValidationError.EMAIL_TOO_LONG)
            raw.any { it.isISOControl() } || !emailPattern.matches(value) ||
                localPart.length > 64 || localPart.startsWith('.') || localPart.endsWith('.') ||
                localPart.contains("..") -> Result.Error(EnquiryValidationError.EMAIL_INVALID)
            else -> Result.Success(value)
        }
    }

    fun validateMessage(raw: String): Result<String, EnquiryValidationError> {
        val value = raw.trim()
        return if (value.length > MAX_MESSAGE_LENGTH) {
            Result.Error(EnquiryValidationError.MESSAGE_TOO_LONG)
        } else {
            Result.Success(value)
        }
    }
}
