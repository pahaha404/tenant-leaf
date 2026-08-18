package com.seipseip.core.common

data class FieldViolation(
    val field: String,
    val reason: String,
)

sealed interface AppError {
    val userMessage: String

    data class Server(
        val httpStatus: Int,
        val code: String,
        override val userMessage: String,
        val traceId: String?,
        val fieldViolations: List<FieldViolation> = emptyList(),
    ) : AppError

    data object Network : AppError {
        override val userMessage: String = "서버에 연결할 수 없습니다. 네트워크를 확인해 주세요."
    }

    data object InvalidResponse : AppError {
        override val userMessage: String = "서버 응답을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요."
    }

    data object Unexpected : AppError {
        override val userMessage: String = "예상하지 못한 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."
    }
}
