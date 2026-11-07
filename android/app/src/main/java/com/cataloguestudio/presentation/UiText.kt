package com.cataloguestudio.presentation

import androidx.annotation.StringRes

internal sealed interface UiText {
    data class DynamicString(val value: String) : UiText
    data class StringResource(@param:StringRes val id: Int) : UiText
}
