package com.seipseip.feature.media.data

import com.seipseip.core.common.AppResult
import com.seipseip.core.network.OffsetDateTimeJsonAdapter
import com.seipseip.core.network.UuidJsonAdapter
import com.seipseip.core.network.UriJsonAdapter
import com.seipseip.core.network.generated.api.MediaApi
import com.seipseip.feature.media.domain.ExtractedJpeg
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.time.OffsetDateTime
import java.util.UUID

class MediaBatchUploaderTest {
    private lateinit var server: MockWebServer
    private lateinit var file: File
    private lateinit var uploader: MediaBatchUploader
    private val inspectionId = UUID.fromString("10000000-0000-0000-0000-000000000001")
    private val mediaId = UUID.fromString("20000000-0000-0000-0000-000000000001")
    private val clientMediaId = UUID.fromString("30000000-0000-0000-0000-000000000001")
    private val sourceVideoId = UUID.fromString("40000000-0000-0000-0000-000000000001")

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
        file = File.createTempFile("batch-upload", ".jpg").apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }
        val moshi = Moshi.Builder()
            .add(UuidJsonAdapter())
            .add(OffsetDateTimeJsonAdapter())
            .add(UriJsonAdapter())
            .addLast(KotlinJsonAdapterFactory())
            .build()
        val client = OkHttpClient()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/api/v1/"))
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(MediaApi::class.java)
        uploader = MediaBatchUploader(api, DirectJpegUploader(client), moshi)
    }

    @After
    fun tearDown() {
        file.delete()
        server.shutdown()
    }

    @Test
    fun `등록 PUT 완료 API를 순서대로 호출한다`() = runTest {
        server.enqueue(jsonResponse(registrationJson(server.url("/signed/first").toString()), 201))
        server.enqueue(MockResponse().setResponseCode(200))
        server.enqueue(jsonResponse(mediaJson(), 200))
        server.enqueue(jsonResponse(finalizeJson(), 200))

        val result = uploader.upload(inspectionId, listOf(photo())) {}

        assertTrue(result is AppResult.Success)
        val registration = server.takeRequest()
        assertEquals("/api/v1/inspections/$inspectionId/media/upload-requests", registration.path)
        assertTrue(registration.body.readUtf8().let { body ->
            body.contains(clientMediaId.toString()) && !body.contains("content://") && !body.contains(file.absolutePath)
        })
        assertEquals("PUT", server.takeRequest().method)
        assertEquals("/api/v1/media/$mediaId/upload-complete", server.takeRequest().path)
        val finalize = server.takeRequest()
        assertEquals("/api/v1/inspections/$inspectionId/media/finalize", finalize.path)
        assertTrue(finalize.body.readUtf8().contains("\"expectedMediaCount\":1"))
    }

    @Test
    fun `서명 URL 만료 시 재발급 후 같은 사진을 완료한다`() = runTest {
        server.enqueue(jsonResponse(registrationJson(server.url("/signed/expired").toString()), 201))
        server.enqueue(MockResponse().setResponseCode(403))
        server.enqueue(jsonResponse(instructionJson(server.url("/signed/fresh").toString()), 200))
        server.enqueue(MockResponse().setResponseCode(200))
        server.enqueue(jsonResponse(mediaJson(), 200))
        server.enqueue(jsonResponse(finalizeJson(), 200))

        val result = uploader.upload(inspectionId, listOf(photo())) {}

        assertTrue(result is AppResult.Success)
        server.takeRequest()
        server.takeRequest()
        val retry = server.takeRequest()
        assertEquals("/api/v1/media/$mediaId/upload-retry", retry.path)
        assertEquals("POST", retry.method)
        assertEquals("/signed/fresh", server.takeRequest().path)
        assertEquals("/api/v1/media/$mediaId/upload-complete", server.takeRequest().path)
        assertEquals("/api/v1/inspections/$inspectionId/media/finalize", server.takeRequest().path)
    }

    private fun photo() = ExtractedJpeg(
        file = file,
        clientMediaId = clientMediaId,
        sourceVideoId = sourceVideoId,
        sourceVideoOffsetMs = 3_000,
        capturedAt = OffsetDateTime.parse("2026-08-19T10:00:03+09:00"),
        width = 1,
        height = 1,
        needsQualityReview = false,
    )

    private fun registrationJson(uploadUrl: String) = """{"items":[${instructionJson(uploadUrl)}]}"""

    private fun instructionJson(uploadUrl: String) = """{
        "mediaId":"$mediaId",
        "clientMediaId":"$clientMediaId",
        "uploadUrl":"$uploadUrl",
        "expiresAt":"2026-08-19T10:15:00+09:00",
        "uploadStatus":"PENDING"
    }""".trimIndent()

    private fun mediaJson() = """{
        "id":"$mediaId",
        "clientMediaId":"$clientMediaId",
        "inspectionId":"$inspectionId",
        "mediaType":"PHOTO",
        "contentType":"image/jpeg",
        "fileSize":4,
        "width":1,
        "height":1,
        "sourceVideoId":"$sourceVideoId",
        "sourceVideoOffsetMs":3000,
        "frameOrigin":"POST_RECORDING_EXTRACTION",
        "captureSource":"META_GLASS",
        "capturedAt":"2026-08-19T10:00:03+09:00",
        "uploadStatus":"UPLOADED",
        "analysisStatus":"NOT_REQUESTED",
        "createdAt":"2026-08-19T10:00:04+09:00"
    }""".trimIndent()

    private fun finalizeJson() = """{
        "inspectionId":"$inspectionId",
        "expectedMediaCount":1,
        "registeredMediaCount":1,
        "mediaFinalizedAt":"2026-08-19T10:00:05+09:00",
        "analysisStatus":"QUEUED"
    }""".trimIndent()

    private fun jsonResponse(body: String, code: Int) = MockResponse()
        .setResponseCode(code)
        .setHeader("Content-Type", "application/json")
        .setBody(body)
}
