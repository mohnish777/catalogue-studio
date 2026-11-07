package com.cataloguestudio.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cataloguestudio.R
import com.cataloguestudio.domain.EnquiryValidator
import com.cataloguestudio.presentation.EnquiryAction
import com.cataloguestudio.presentation.EnquiryEvent
import com.cataloguestudio.presentation.EnquiryState
import com.cataloguestudio.presentation.EnquiryViewModel
import com.cataloguestudio.presentation.UiText
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun EnquiryRoot(onBack: () -> Unit, viewModel: EnquiryViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Leaving during the POST could hide an accepted enquiry before its acknowledgement arrives.
    BackHandler(enabled = state.isSending) { }
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            EnquiryEvent.NavigateBack -> onBack()
        }
    }
    EnquiryScreen(state, viewModel::onAction)
}

@Composable
internal fun EnquiryScreen(state: EnquiryState, onAction: (EnquiryAction) -> Unit) {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(contentAlignment = Alignment.TopCenter) {
            Scaffold(
                modifier = Modifier.widthIn(max = 720.dp).fillMaxSize().safeDrawingPadding().imePadding(),
                topBar = {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        TextButton(onClick = { onAction(EnquiryAction.OnBack) }, enabled = !state.isSending) {
                            Text(stringResource(R.string.navigation_back))
                        }
                        Text(stringResource(R.string.enquiry_title), style = MaterialTheme.typography.titleMedium)
                    }
                },
                bottomBar = {
                    if (!state.isSent && !state.isLoading && state.loadError == null && state.product != null) {
                        Surface(tonalElevation = 3.dp) {
                            Button(
                                onClick = {
                                    focus.clearFocus()
                                    keyboard?.hide()
                                    onAction(EnquiryAction.OnSubmit)
                                },
                                enabled = state.canSubmit,
                                modifier = Modifier.fillMaxWidth().padding(20.dp),
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (state.isSending) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    }
                                    Text(
                                        text = stringResource(if (state.isSending) R.string.enquiry_sending else R.string.enquiry_submit),
                                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                                    )
                                }
                            }
                        }
                    }
                },
            ) { padding ->
                if (state.isSent) {
                    EnquiryConfirmation(
                        onBack = { onAction(EnquiryAction.OnBack) },
                        modifier = Modifier.padding(padding).fillMaxSize()
                            .verticalScroll(rememberScrollState()).padding(24.dp),
                    )
                } else {
                    Column(
                        modifier = Modifier.padding(padding).fillMaxSize()
                            .verticalScroll(rememberScrollState()).padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        when {
                            state.isLoading -> LoadingState(UiText.StringResource(R.string.state_loading_product))
                            state.loadError != null -> ErrorState(
                                message = state.loadError,
                                onRetry = { onAction(EnquiryAction.OnRetryProduct) },
                            )
                            else -> EnquiryFields(state, onAction)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EnquiryFields(state: EnquiryState, onAction: (EnquiryAction) -> Unit) {
    val focus = LocalFocusManager.current
    val next = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) })
    val product = state.product ?: return
    Text(
        text = stringResource(R.string.enquiry_headline),
        modifier = Modifier.semantics { heading() },
        style = MaterialTheme.typography.headlineLarge,
    )
    Text(stringResource(R.string.enquiry_intro), style = MaterialTheme.typography.bodyLarge)
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(product.categoryLabel.asString(), style = MaterialTheme.typography.labelLarge)
            Text(product.title, style = MaterialTheme.typography.titleLarge)
            Text(product.priceLabel, style = MaterialTheme.typography.titleMedium)
        }
    }
    OutlinedTextField(
        value = state.name,
        onValueChange = { onAction(EnquiryAction.OnNameChange(it)) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.enquiry_name)) },
        singleLine = true,
        enabled = !state.isSending,
        isError = state.nameError != null,
        supportingText = { Text(state.nameError?.asString() ?: stringResource(R.string.enquiry_required)) },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
        keyboardActions = next,
        shape = RoundedCornerShape(16.dp),
    )
    OutlinedTextField(
        value = state.email,
        onValueChange = { onAction(EnquiryAction.OnEmailChange(it)) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.enquiry_email)) },
        singleLine = true,
        enabled = !state.isSending,
        isError = state.emailError != null,
        supportingText = { Text(state.emailError?.asString() ?: stringResource(R.string.enquiry_reply_note)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, autoCorrectEnabled = false, imeAction = ImeAction.Next),
        keyboardActions = next,
        shape = RoundedCornerShape(16.dp),
    )
    OutlinedTextField(
        value = state.message,
        onValueChange = { onAction(EnquiryAction.OnMessageChange(it)) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.enquiry_message)) },
        minLines = 4,
        maxLines = 6,
        enabled = !state.isSending,
        isError = state.messageError != null,
        supportingText = {
            Text(state.messageError?.asString() ?: stringResource(
                R.string.enquiry_message_count, state.message.length, EnquiryValidator.MAX_MESSAGE_LENGTH,
            ))
        },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        shape = RoundedCornerShape(16.dp),
    )
    state.sendError?.let { error ->
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ) {
            Text(
                text = error.asString(),
                modifier = Modifier.fillMaxWidth().padding(16.dp).semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

@Composable
private fun EnquiryConfirmation(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(20.dp).size(40.dp),
            )
        }
        Text(
            text = stringResource(R.string.enquiry_success_title),
            modifier = Modifier.semantics { heading(); liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
        )
        Text(stringResource(R.string.enquiry_success_message), textAlign = TextAlign.Center)
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.enquiry_back_to_product))
        }
    }
}
