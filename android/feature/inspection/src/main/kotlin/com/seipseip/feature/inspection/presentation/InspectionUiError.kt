package com.seipseip.feature.inspection.presentation

import com.seipseip.core.common.AppError
import com.seipseip.core.ui.ContentState

internal fun <T> AppError.toInspectionContentState(): ContentState<T> = when (this) {
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
