package com.seipseip.core.ui

sealed interface ContentState<out T> {
    data object Idle : ContentState<Nothing>

    data object Loading : ContentState<Nothing>

    data class Success<T>(val value: T) : ContentState<T>

    data object Empty : ContentState<Nothing>

    data class ValidationError(val message: String) : ContentState<Nothing>

    data class NetworkError(val message: String) : ContentState<Nothing>

    data class ServerError(
        val code: String,
        val message: String,
    ) : ContentState<Nothing>
}
