package com.seipseip.core.network

import com.seipseip.core.common.AppError
import com.seipseip.core.common.AppResult
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class ApiResponseTest {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun `successful response returns body`() = runTest {
        val result = executeApiCall(moshi) { Response.success("ok") }

        assertEquals(AppResult.Success("ok"), result)
    }

    @Test
    fun `error response is converted to stable server error`() = runTest {
        val body = """
            {
              "code": "VALIDATION_ERROR",
              "message": "입력값을 확인해 주세요.",
              "traceId": "trace-1",
              "fieldErrors": [{"field": "name", "reason": "필수입니다."}]
            }
        """.trimIndent().toResponseBody()

        val result = executeApiCall(moshi) { Response.error<String>(400, body) }

        assertTrue(result is AppResult.Failure)
        val error = (result as AppResult.Failure).error as AppError.Server
        assertEquals(400, error.httpStatus)
        assertEquals("VALIDATION_ERROR", error.code)
        assertEquals("trace-1", error.traceId)
        assertEquals("name", error.fieldViolations.single().field)
    }

    @Test
    fun `coroutine cancellation is not converted to an app error`() = runTest {
        var cancellationWasRethrown = false

        try {
            executeApiCall<String>(moshi) { throw CancellationException("screen left") }
        } catch (_: CancellationException) {
            cancellationWasRethrown = true
        }

        assertTrue(cancellationWasRethrown)
    }
}
