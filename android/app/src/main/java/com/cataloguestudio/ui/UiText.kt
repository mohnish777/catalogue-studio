package com.cataloguestudio.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.cataloguestudio.presentation.UiText

@Composable
internal fun UiText.asString(): String = when (this) {
    is UiText.DynamicString -> value
    is UiText.StringResource -> stringResource(id)
}
