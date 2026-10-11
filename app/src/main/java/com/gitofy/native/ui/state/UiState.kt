package com.gitofy.native.ui.state

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Error(val message: String) : UiState<Nothing>
    data class Content<T>(val data: T) : UiState<T>
}

inline fun <T> UiState<T>.onContent(block: (T) -> Unit) {
    if (this is UiState.Content) block(data)
}
