package com.seipseip.core.network

import com.seipseip.core.common.AppError
import com.seipseip.core.common.AppResult
import com.seipseip.core.common.FieldViolation
import com.seipseip.core.network.generated.model.ErrorResponse
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import java.io.IOException
import java.time.format.DateTimeParseException

suspend fun <T : Any> executeApiCall(
    moshi: Moshi,
    request: suspend () -> Response<T>,
): AppResult<T> = try {
    val response = request()
    if (response.isSuccessful) {
        response.body()?.let { AppResult.Success(it) } ?: AppResult.Failure(AppError.InvalidResponse)
    } else {
        AppResult.Failure(response.toAppError(moshi))
    }
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: JsonEncodingException) {
    AppResult.Failure(AppError.InvalidResponse)
} catch (_: JsonDataException) {
    AppResult.Failure(AppError.InvalidResponse)
} catch (_: DateTimeParseException) {
    AppResult.Failure(AppError.InvalidResponse)
} catch (_: IllegalArgumentException) {
    AppResult.Failure(AppError.InvalidResponse)
} catch (_: IOException) {
    AppResult.Failure(AppError.Network)
} catch (_: Exception) {
    AppResult.Failure(AppError.Unexpected)
}

suspend fun executeEmptyApiCall(
    moshi: Moshi,
    request: suspend () -> Response<Unit>,
): AppResult<Unit> = try {
    val response = request()
    if (response.isSuccessful) {
        AppResult.Success(Unit)
    } else {
        AppResult.Failure(response.toAppError(moshi))
    }
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: JsonEncodingException) {
    AppResult.Failure(AppError.InvalidResponse)
} catch (_: JsonDataException) {
    AppResult.Failure(AppError.InvalidResponse)
} catch (_: DateTimeParseException) {
    AppResult.Failure(AppError.InvalidResponse)
} catch (_: IllegalArgumentException) {
    AppResult.Failure(AppError.InvalidResponse)
} catch (_: IOException) {
    AppResult.Failure(AppError.Network)
} catch (_: Exception) {
    AppResult.Failure(AppError.Unexpected)
}

private fun Response<*>.toAppError(moshi: Moshi): AppError {
    val parsed = runCatching {
        errorBody()?.string()?.takeIf(String::isNotBlank)?.let {
            moshi.adapter(ErrorResponse::class.java).fromJson(it)
        }
    }.getOrNull()

    return AppError.Server(
        httpStatus = code(),
        code = parsed?.code ?: "HTTP_${code()}",
        userMessage = parsed?.message ?: "요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.",
        traceId = parsed?.traceId,
        fieldViolations = parsed?.fieldErrors.orEmpty().map { FieldViolation(it.`field`, it.reason) },
    )
}
