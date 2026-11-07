package com.cataloguestudio.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cataloguestudio.domain.CatalogueRemoteDataSource
import com.cataloguestudio.domain.Enquiry
import com.cataloguestudio.domain.EnquiryValidationError
import com.cataloguestudio.domain.EnquiryValidator
import com.cataloguestudio.domain.Result
import com.cataloguestudio.presentation.navigation.EnquiryRoute
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class EnquiryViewModel(
    private val source: CatalogueRemoteDataSource,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val productId = requireNotNull(savedStateHandle.get<Long>(EnquiryRoute::productId.name))
    private val wasSent: Boolean = savedStateHandle[SENT] ?: false
    private val _state = MutableStateFlow(EnquiryState(
        name = if (wasSent) "" else savedStateHandle[NAME] ?: "",
        email = if (wasSent) "" else savedStateHandle[EMAIL] ?: "",
        message = if (wasSent) "" else savedStateHandle[MESSAGE] ?: "",
        isLoading = !wasSent,
        isSent = wasSent,
    ))
    val state = _state.asStateFlow()
    private val _events = Channel<EnquiryEvent>()
    val events = _events.receiveAsFlow()
    private var loadJob: Job? = null

    init {
        if (!wasSent) loadProduct()
    }

    fun onAction(action: EnquiryAction) {
        val current = state.value
        // Set synchronously before launching the POST; a second tap cannot start another send.
        if (current.isSending || (current.isSent && action != EnquiryAction.OnBack)) return
        when (action) {
            is EnquiryAction.OnNameChange -> {
                savedStateHandle[NAME] = action.value
                _state.update { it.copy(
                    name = action.value,
                    nameError = if (it.showValidation) EnquiryValidator.validateName(action.value).errorText() else null,
                    sendError = null,
                ) }
            }
            is EnquiryAction.OnEmailChange -> {
                savedStateHandle[EMAIL] = action.value
                _state.update { it.copy(
                    email = action.value,
                    emailError = if (it.showValidation) EnquiryValidator.validateEmail(action.value).errorText() else null,
                    sendError = null,
                ) }
            }
            is EnquiryAction.OnMessageChange -> {
                savedStateHandle[MESSAGE] = action.value
                _state.update { it.copy(
                    message = action.value,
                    messageError = if (it.showValidation) EnquiryValidator.validateMessage(action.value).errorText() else null,
                    sendError = null,
                ) }
            }
            EnquiryAction.OnRetryProduct -> loadProduct()
            EnquiryAction.OnSubmit -> submit()
            EnquiryAction.OnBack -> viewModelScope.launch { _events.send(EnquiryEvent.NavigateBack) }
        }
    }

    private fun loadProduct() {
        loadJob?.cancel()
        _state.update { it.copy(product = null, isLoading = true, loadError = null, sendError = null) }
        loadJob = viewModelScope.launch {
            val result = source.getProduct(productId)
            coroutineContext.ensureActive()
            when (result) {
                is Result.Success -> _state.update {
                    it.copy(product = result.data.toProductUi(), isLoading = false)
                }
                is Result.Error -> _state.update {
                    it.copy(isLoading = false, loadError = result.error.toUiText())
                }
            }
        }
    }

    private fun submit() {
        val current = state.value
        if (!current.canSubmit) return
        val name = EnquiryValidator.validateName(current.name)
        val email = EnquiryValidator.validateEmail(current.email)
        val message = EnquiryValidator.validateMessage(current.message)
        _state.update { it.copy(
            showValidation = true,
            nameError = name.errorText(),
            emailError = email.errorText(),
            messageError = message.errorText(),
            sendError = null,
        ) }
        if (name !is Result.Success || email !is Result.Success || message !is Result.Success) return

        // Submit one normalized snapshot; retain the raw form values if the request fails.
        val enquiry = Enquiry(productId, name.data, email.data, message.data)
        _state.update { it.copy(isSending = true) }
        viewModelScope.launch {
            val result = source.sendEnquiry(enquiry)
            coroutineContext.ensureActive()
            when (result) {
                is Result.Success -> {
                    savedStateHandle[SENT] = true
                    savedStateHandle.remove<String>(NAME)
                    savedStateHandle.remove<String>(EMAIL)
                    savedStateHandle.remove<String>(MESSAGE)
                    _state.update { it.copy(
                        name = "", email = "", message = "",
                        nameError = null, emailError = null, messageError = null,
                        showValidation = false, isSending = false, sendError = null, isSent = true,
                    ) }
                }
                is Result.Error -> _state.update {
                    it.copy(isSending = false, sendError = result.error.toUiText())
                }
            }
        }
    }

    private companion object {
        const val NAME = "enquiry.name"
        const val EMAIL = "enquiry.email"
        const val MESSAGE = "enquiry.message"
        const val SENT = "enquiry.sent"
    }
}

private fun Result<String, EnquiryValidationError>.errorText(): UiText? = when (this) {
    is Result.Success -> null
    is Result.Error -> error.toUiText()
}
