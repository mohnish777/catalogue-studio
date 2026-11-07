package com.cataloguestudio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cataloguestudio.R
import com.cataloguestudio.presentation.*
import org.koin.androidx.compose.koinViewModel

@Composable
fun EnquiryRoot(onBack: () -> Unit, viewModel: EnquiryViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    EnquiryScreen(state, viewModel::onAction, onBack)
}

@Composable
fun EnquiryScreen(state: EnquiryState, onAction: (EnquiryAction) -> Unit, onBack: () -> Unit) {
    Scaffold(
        containerColor = Paper,
        topBar = { PageHeader(stringResource(R.string.enquiry_title), onBack) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 32.dp)
        ) {
            when {
                state.sent -> {
                    Spacer(Modifier.height(32.dp))
                    Box(
                        Modifier.size(72.dp).clip(CircleShape).background(Sage),
                        contentAlignment = Alignment.Center,
                    ) {
                        StudioIcon(Mark.CHECK, Modifier.size(32.dp), tint = Forest)
                    }
                    Spacer(Modifier.height(28.dp))
                    Eyebrow(stringResource(R.string.success_label))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.success_title),
                        style = MaterialTheme.typography.displayLarge,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.success_body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Muted,
                    )
                    Spacer(Modifier.height(32.dp))
                    state.product?.let { ProductSummary(it) }
                    Spacer(Modifier.height(32.dp))
                    Button(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(stringResource(R.string.back_to_product))
                    }
                }
                state.loading -> LoadingState()
                state.loadError != null ->
                    ErrorState(state.loadError) { onAction(EnquiryAction.Retry) }
                else -> {
                    Eyebrow(stringResource(R.string.enquiry_eyebrow))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.enquiry_heading),
                        style = MaterialTheme.typography.displayLarge,
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        stringResource(R.string.enquiry_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Muted,
                    )
                    Spacer(Modifier.height(24.dp))
                    state.product?.let { ProductSummary(it) }
                    Spacer(Modifier.height(28.dp))
                    FormField(
                        stringResource(R.string.your_name),
                        state.name,
                        { onAction(EnquiryAction.NameChanged(it)) },
                        stringResource(R.string.name_hint),
                        state.nameError,
                        stringResource(R.string.name_error),
                        "name_input",
                        enabled = !state.sending,
                    )
                    Spacer(Modifier.height(16.dp))
                    FormField(
                        stringResource(R.string.email_address),
                        state.email,
                        { onAction(EnquiryAction.EmailChanged(it)) },
                        stringResource(R.string.email_hint),
                        state.emailError,
                        stringResource(R.string.email_error),
                        "email_input",
                        KeyboardType.Email,
                        enabled = !state.sending,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.message_label),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.message,
                        onValueChange = { onAction(EnquiryAction.MessageChanged(it)) },
                        enabled = !state.sending,
                        modifier = Modifier.fillMaxWidth().testTag("message_input"),
                        minLines = 3,
                        maxLines = 6,
                        placeholder = {
                            Text(
                                stringResource(R.string.message_hint),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        isError = state.messageError,
                        supportingText = {
                            Text(
                                if (state.messageError) stringResource(R.string.message_error)
                                else stringResource(R.string.message_count, state.message.length)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = fieldColors(),
                    )
                    state.sendError?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            it.asString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag("send_error"),
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { onAction(EnquiryAction.Submit) },
                        enabled = !state.sending,
                        modifier =
                            Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("send_enquiry"),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        if (state.sending) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(12.dp))
                        }
                        Text(
                            stringResource(
                                if (state.sending) R.string.sending else R.string.send_enquiry
                            ),
                            Modifier.weight(1f),
                        )
                        if (!state.sending) StudioIcon(Mark.ARROW, tint = Color.White)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.privacy_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                    )
                }
            }
        }
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    isError: Boolean,
    errorText: String,
    tag: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean,
) {
    Text(label, style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag(tag),
        placeholder = { Text(hint, style = MaterialTheme.typography.bodyMedium) },
        isError = isError,
        supportingText =
            if (isError) {
                { Text(errorText) }
            } else null,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
        shape = RoundedCornerShape(12.dp),
        colors = fieldColors(),
    )
}

@Composable
private fun fieldColors() =
    OutlinedTextFieldDefaults.colors(
        unfocusedContainerColor = Color.White,
        focusedContainerColor = Color.White,
        disabledContainerColor = Color.White,
        unfocusedBorderColor = Line,
    )

@Preview(showBackground = true, widthDp = 412, heightDp = 1000)
@Composable
private fun EnquiryPreview() = StudioTheme {
    EnquiryScreen(EnquiryState(loading = false, product = previewProduct), {}, {})
}

@Preview(showBackground = true, widthDp = 412, heightDp = 900)
@Composable
private fun SuccessPreview() = StudioTheme {
    EnquiryScreen(EnquiryState(loading = false, product = previewProduct, sent = true), {}, {})
}
