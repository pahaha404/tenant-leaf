package com.seipseip.feature.media.data

import com.seipseip.core.common.AppError
import com.seipseip.core.common.AppResult
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class DirectJpegUploaderTest {
    private lateinit var server: MockWebServer
    private lateinit var file: File

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
        file = File.createTempFile("media-upload", ".jpg").apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }
    }

    @After
    fun tearDown() {
        file.delete()
        server.shutdown()
    }

    @Test
    fun `JPEG를 서명 URL에 PUT한다`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200))

        val result = DirectJpegUploader(OkHttpClient()).put(server.url("/signed/object").toString(), file)

        assertTrue(result is AppResult.Success)
        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/signed/object", request.path)
        assertEquals("image/jpeg", request.getHeader("Content-Type"))
        assertEquals(4L, request.bodySize)
    }

    @Test
    fun `객체 저장소 오류를 서버 오류로 구분한다`() = runTest {
        server.enqueue(MockResponse().setResponseCode(403))

        val result = DirectJpegUploader(OkHttpClient()).put(server.url("/expired").toString(), file)

        assertTrue(result is AppResult.Failure)
        val error = (result as AppResult.Failure).error as AppError.Server
        assertEquals("UPLOAD_URL_EXPIRED", error.code)
        assertEquals(403, error.httpStatus)
    }
}
