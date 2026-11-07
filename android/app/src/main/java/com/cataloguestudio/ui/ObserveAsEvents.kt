package com.cataloguestudio.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow

@Composable
fun <T> ObserveAsEvents(events: Flow<T>, onEvent: (T) -> Unit) {
    val owner = LocalLifecycleOwner.current
    val latest = rememberUpdatedState(onEvent)
    LaunchedEffect(owner, events) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            events.collect { latest.value(it) }
        }
    }
}
