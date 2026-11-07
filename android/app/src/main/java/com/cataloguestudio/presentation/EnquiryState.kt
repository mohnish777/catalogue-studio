package com.cataloguestudio.presentation

internal data class EnquiryState(
    val product: ProductUi? = null,
    val name: String = "",
    val email: String = "",
    val message: String = "",
    val nameError: UiText? = null,
    val emailError: UiText? = null,
    val messageError: UiText? = null,
    val showValidation: Boolean = false,
    val isLoading: Boolean = true,
    val loadError: UiText? = null,
    val isSending: Boolean = false,
    val sendError: UiText? = null,
    val isSent: Boolean = false,
) {
    // Field validation runs on Submit, so the UI can explain missing/invalid values.
    val canSubmit: Boolean
        get() = product != null && !isLoading && loadError == null && !isSending && !isSent
}

internal sealed interface EnquiryAction {
    data class OnNameChange(val value: String) : EnquiryAction
    data class OnEmailChange(val value: String) : EnquiryAction
    data class OnMessageChange(val value: String) : EnquiryAction
    data object OnRetryProduct : EnquiryAction
    data object OnSubmit : EnquiryAction
    data object OnBack : EnquiryAction
}

internal sealed interface EnquiryEvent {
    data object NavigateBack : EnquiryEvent
}
