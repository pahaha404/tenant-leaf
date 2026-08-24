package com.tenantleaf.api.error

import com.tenantleaf.api.generated.model.ErrorResponse
import com.tenantleaf.api.generated.model.FieldError
import com.tenantleaf.api.inspection.InspectionNotFoundException
import com.tenantleaf.api.inspection.InspectionStateTransitionException
import com.tenantleaf.api.inspection.PropertyHasInspectionsException
import com.tenantleaf.api.media.ClientMediaIdConflictException
import com.tenantleaf.api.media.IdempotencyKeyConflictException
import com.tenantleaf.api.media.MediaNotFoundException
import com.tenantleaf.api.media.MediaFileTooLargeException
import com.tenantleaf.api.media.MediaStateException
import com.tenantleaf.api.media.MediaSetCountMismatchException
import com.tenantleaf.api.media.MediaSetFinalizedException
import com.tenantleaf.api.media.MediaValidationException
import com.tenantleaf.api.media.ObjectStorageUnavailableException
import com.tenantleaf.api.media.UnsupportedMediaTypeException
import com.tenantleaf.api.property.PropertyNotFoundException
import com.tenantleaf.api.property.PropertyValidationException
import com.tenantleaf.api.report.ObservationNotFoundException
import com.tenantleaf.api.report.ReportNotFoundException
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

    @ExceptionHandler(InspectionNotFoundException::class)
    fun handleInspectionNotFound(): ResponseEntity<ErrorResponse> =
        response(HttpStatus.NOT_FOUND, "INSPECTION_NOT_FOUND", "요청한 임장 기록을 찾을 수 없습니다.")

    @ExceptionHandler(MediaNotFoundException::class)
    fun handleMediaNotFound(): ResponseEntity<ErrorResponse> =
        response(HttpStatus.NOT_FOUND, "MEDIA_NOT_FOUND", "요청한 미디어를 찾을 수 없습니다.")

    @ExceptionHandler(ObservationNotFoundException::class)
    fun handleObservationNotFound(): ResponseEntity<ErrorResponse> =
        response(HttpStatus.NOT_FOUND, "OBSERVATION_NOT_FOUND", "요청한 확인 필요 관찰을 찾을 수 없습니다.")

    @ExceptionHandler(ReportNotFoundException::class)
    fun handleReportNotFound(): ResponseEntity<ErrorResponse> =
        response(HttpStatus.NOT_FOUND, "REPORT_NOT_FOUND", "아직 생성된 리포트가 없습니다.")

    @ExceptionHandler(InspectionStateTransitionException::class)
    fun handleInspectionStateTransition(): ResponseEntity<ErrorResponse> =
        response(HttpStatus.CONFLICT, "INVALID_STATE_TRANSITION", "현재 임장 상태에서는 요청을 수행할 수 없습니다.")

    @ExceptionHandler(PropertyHasInspectionsException::class)
    fun handlePropertyHasInspections(): ResponseEntity<ErrorResponse> =
        response(HttpStatus.CONFLICT, "INVALID_STATE_TRANSITION", "임장 기록이 있는 매물은 현재 삭제할 수 없습니다.")

    @ExceptionHandler(MediaStateException::class)
    fun handleMediaState(): ResponseEntity<ErrorResponse> =
        response(HttpStatus.CONFLICT, "INVALID_STATE_TRANSITION", "현재 미디어 상태에서는 요청을 수행할 수 없습니다.")

    @ExceptionHandler(MediaSetFinalizedException::class)
    fun handleMediaSetFinalized(): ResponseEntity<ErrorResponse> =
        response(HttpStatus.CONFLICT, "MEDIA_SET_FINALIZED", "이미 분석 대상 사진 등록이 확정되었습니다.")

    @ExceptionHandler(MediaSetCountMismatchException::class)
    fun handleMediaSetCountMismatch(): ResponseEntity<ErrorResponse> =
        response(HttpStatus.CONFLICT, "MEDIA_SET_COUNT_MISMATCH", "등록된 사진 수와 확정 요청의 사진 수가 다릅니다.")

    @ExceptionHandler(ClientMediaIdConflictException::class)
    fun handleClientMediaIdConflict(): ResponseEntity<ErrorResponse> =
        response(HttpStatus.CONFLICT, "CLIENT_MEDIA_ID_CONFLICT", "같은 사진 ID에 서로 다른 정보가 요청되었습니다.")

    @ExceptionHandler(IdempotencyKeyConflictException::class)
    fun handleIdempotencyKeyConflict(): ResponseEntity<ErrorResponse> =
        response(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_CONFLICT", "같은 멱등성 키에 서로 다른 요청이 전송되었습니다.")

    @ExceptionHandler(MediaValidationException::class)
    fun handleMediaValidation(exception: MediaValidationException): ResponseEntity<ErrorResponse> =
        response(
            HttpStatus.BAD_REQUEST,
            "VALIDATION_ERROR",
            "업로드할 JPEG 정보를 확인해 주세요.",
            listOf(FieldError(exception.field, exception.reason)),
        )

    @ExceptionHandler(MediaFileTooLargeException::class)
    fun handleMediaFileTooLarge(): ResponseEntity<ErrorResponse> =
        response(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "JPEG 파일은 2MiB 이하여야 합니다.")

    @ExceptionHandler(UnsupportedMediaTypeException::class)
    fun handleUnsupportedMediaType(): ResponseEntity<ErrorResponse> =
        response(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE", "실제 JPEG 파일만 업로드할 수 있습니다.")

    @ExceptionHandler(ObjectStorageUnavailableException::class)
    fun handleObjectStorageUnavailable(): ResponseEntity<ErrorResponse> =
        response(HttpStatus.SERVICE_UNAVAILABLE, "OBJECT_STORAGE_UNAVAILABLE", "사진 저장소에 잠시 연결할 수 없습니다.")

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
