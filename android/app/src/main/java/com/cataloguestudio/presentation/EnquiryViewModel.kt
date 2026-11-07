package com.cataloguestudio.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cataloguestudio.R
import com.cataloguestudio.domain.CatalogueRemoteDataSource
import com.cataloguestudio.domain.Enquiry
import com.cataloguestudio.domain.Result
import com.cataloguestudio.domain.validateEnquiry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EnquiryState(
    val product: ProductUi? = null,
    val loading: Boolean = true,
    val loadError: UiText? = null,
    val name: String = "",
    val email: String = "",
    val message: String = "",
    val nameError: Boolean = false,
    val emailError: Boolean = false,
    val messageError: Boolean = false,
    val sending: Boolean = false,
    val sent: Boolean = false,
    val sendError: UiText? = null,
)

sealed interface EnquiryAction {
    data class NameChanged(val value: String) : EnquiryAction

    data class EmailChanged(val value: String) : EnquiryAction

    data class MessageChanged(val value: String) : EnquiryAction

    data object Submit : EnquiryAction

    data object Retry : EnquiryAction
}

class EnquiryViewModel(
    private val source: CatalogueRemoteDataSource,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val productId: Int = checkNotNull(savedState["productId"])
    private val _state =
        MutableStateFlow(
            EnquiryState(
                name = savedState["name"] ?: "",
                email = savedState["email"] ?: "",
                message = savedState["message"] ?: "",
                sent = savedState["sent"] ?: false,
            )
        )
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun onAction(action: EnquiryAction) {
        // Edits and repeated taps must not change an in-flight request.
        if (state.value.sending || state.value.sent) return
        when (action) {
            is EnquiryAction.NameChanged -> {
                val value = action.value.take(100)
                savedState["name"] = value
                _state.update { it.copy(name = value, nameError = false, sendError = null) }
            }
            is EnquiryAction.EmailChanged -> {
                val value = action.value.take(254)
                savedState["email"] = value
                _state.update { it.copy(email = value, emailError = false, sendError = null) }
            }
            is EnquiryAction.MessageChanged -> {
                val value = action.value.take(2000)
                savedState["message"] = value
                _state.update { it.copy(message = value, messageError = false, sendError = null) }
            }
            EnquiryAction.Submit -> submit()
            EnquiryAction.Retry -> load()
        }
    }

    private fun load() = viewModelScope.launch {
        _state.update { it.copy(loading = true, loadError = null) }
        when (val result = source.product(productId)) {
            is Result.Success ->
                _state.update { it.copy(loading = false, product = result.data.toProductUi()) }
            is Result.Error ->
                _state.update { it.copy(loading = false, loadError = result.error.toUiText()) }
        }
    }

    private fun submit() {
        val form = state.value
        if (form.product == null || form.loading || form.sending || form.sent) return
        val validation = validateEnquiry(form.name, form.email, form.message)
        if (!validation.valid) {
            _state.update {
                it.copy(
                    nameError = validation.nameInvalid,
                    emailError = validation.emailInvalid,
                    messageError = validation.messageInvalid,
                )
            }
            return
        }
        _state.update { it.copy(sending = true, sendError = null) }
        viewModelScope.launch {
            when (source.enquire(Enquiry(productId, form.name, form.email, form.message))) {
                is Result.Success -> {
                    savedState["sent"] = true
                    savedState["name"] = ""
                    savedState["email"] = ""
                    savedState["message"] = ""
                    _state.update {
                        it.copy(sending = false, sent = true, name = "", email = "", message = "")
                    }
                }
                is Result.Error ->
                    _state.update {
                        it.copy(sending = false, sendError = UiText.Resource(R.string.error_send))
                    }
            }
        }
    }
}
