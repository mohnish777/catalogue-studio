package com.cataloguestudio.presentation

import com.cataloguestudio.R
import com.cataloguestudio.domain.DataError
import com.cataloguestudio.domain.EnquiryValidationError

internal fun DataError.toUiText(): UiText = UiText.StringResource(
    when (this) {
        DataError.NO_INTERNET -> R.string.error_connection
        DataError.REQUEST_TIMEOUT -> R.string.error_timeout
        DataError.BAD_REQUEST -> R.string.error_bad_request
        DataError.NOT_FOUND -> R.string.error_not_found
        DataError.SERVER_ERROR -> R.string.error_server
        DataError.SERIALIZATION -> R.string.error_response
        DataError.UNKNOWN -> R.string.error_unknown
    },
)

internal fun EnquiryValidationError.toUiText(): UiText = UiText.StringResource(
    when (this) {
        EnquiryValidationError.NAME_REQUIRED -> R.string.error_name_required
        EnquiryValidationError.NAME_TOO_LONG -> R.string.error_name_too_long
        EnquiryValidationError.NAME_INVALID -> R.string.error_name_invalid
        EnquiryValidationError.EMAIL_REQUIRED -> R.string.error_email_required
        EnquiryValidationError.EMAIL_TOO_LONG -> R.string.error_email_too_long
        EnquiryValidationError.EMAIL_INVALID -> R.string.error_email_invalid
        EnquiryValidationError.MESSAGE_TOO_LONG -> R.string.error_message_too_long
    },
)
