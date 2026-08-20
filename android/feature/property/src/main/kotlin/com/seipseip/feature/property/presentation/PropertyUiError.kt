package com.seipseip.feature.property.presentation

import com.seipseip.core.common.AppError
import com.seipseip.core.ui.ContentState

internal fun <T> AppError.toContentState(): ContentState<T> = when (this) {
    AppError.Network -> ContentState.NetworkError(userMessage)
    AppError.InvalidResponse,
    AppError.Unexpected,
    -> ContentState.ServerError("UNEXPECTED", userMessage)
    is AppError.Server -> if (code == "VALIDATION_ERROR") {
        ContentState.ValidationError(userMessage)
    } else {
        ContentState.ServerError(code, userMessage)
    }
}

