package com.tenantleaf.api.error

import com.tenantleaf.api.generated.model.ErrorResponse
import com.tenantleaf.api.generated.model.FieldError
import com.tenantleaf.api.property.PropertyNotFoundException
import com.tenantleaf.api.property.PropertyValidationException
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.util.UUID

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(PropertyNotFoundException::class)
    fun handleNotFound(): ResponseEntity<ErrorResponse> =
        response(HttpStatus.NOT_FOUND, "PROPERTY_NOT_FOUND", "요청한 매물을 찾을 수 없습니다.")

    @ExceptionHandler(PropertyValidationException::class)
    fun handlePropertyValidation(exception: PropertyValidationException): ResponseEntity<ErrorResponse> =
        response(
            HttpStatus.BAD_REQUEST,
            "VALIDATION_ERROR",
            "요청값을 확인해 주세요.",
            listOf(FieldError(exception.field, exception.reason)),
        )

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(exception: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> =
        response(
            HttpStatus.BAD_REQUEST,
            "VALIDATION_ERROR",
            "요청값을 확인해 주세요.",
            exception.bindingResult.fieldErrors.map { FieldError(it.field, it.defaultMessage ?: "올바르지 않은 값입니다.") },
        )

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(exception: ConstraintViolationException): ResponseEntity<ErrorResponse> =
        response(
            HttpStatus.BAD_REQUEST,
            "VALIDATION_ERROR",
            "요청값을 확인해 주세요.",
            exception.constraintViolations.map {
                FieldError(it.propertyPath.toString().substringAfterLast('.'), it.message)
            },
        )

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableBody(): ResponseEntity<ErrorResponse> =
        response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "요청 본문 형식을 확인해 주세요.")

    @ExceptionHandler(Exception::class)
    fun handleUnexpectedException(): ResponseEntity<ErrorResponse> =
        response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버에서 요청을 처리하지 못했습니다.")

    private fun response(
        status: HttpStatus,
        code: String,
        message: String,
        fieldErrors: List<FieldError>? = null,
    ): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(status).body(
            ErrorResponse(
                code = code,
                message = message,
                traceId = UUID.randomUUID().toString(),
                fieldErrors = fieldErrors,
            ),
        )
}
