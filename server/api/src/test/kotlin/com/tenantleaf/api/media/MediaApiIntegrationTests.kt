package com.tenantleaf.api.media

import com.tenantleaf.api.inspection.InspectionAggregateStatus
import com.tenantleaf.api.inspection.InspectionEntity
import com.tenantleaf.api.inspection.InspectionLifecycleStatus
import com.tenantleaf.api.inspection.InspectionRepository
import com.tenantleaf.api.property.DemoUserContext
import com.tenantleaf.api.property.PropertyEntity
import com.tenantleaf.api.property.PropertyRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.net.URI
import java.time.OffsetDateTime
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@Import(MediaApiIntegrationTests.StorageTestConfiguration::class)
class MediaApiIntegrationTests(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val mediaRepository: MediaRepository,
    @Autowired private val analysisJobRepository: MediaAnalysisJobRepository,
    @Autowired private val idempotencyRepository: ApiIdempotencyRecordRepository,
    @Autowired private val inspectionRepository: InspectionRepository,
    @Autowired private val propertyRepository: PropertyRepository,
) {
    @BeforeEach
    fun cleanDatabase() {
        idempotencyRepository.deleteAll()
        analysisJobRepository.deleteAll()
        mediaRepository.deleteAll()
        inspectionRepository.deleteAll()
        propertyRepository.deleteAll()
    }

    @Test
    fun `종료된 임장에 JPEG를 등록하고 완료한 뒤 조회한다`() {
        val inspectionId = createEndedInspection()
        val clientMediaId = UUID.randomUUID()
        val sourceVideoId = UUID.randomUUID()
        val idempotencyKey = UUID.randomUUID()
        val body = requestBody(clientMediaId, sourceVideoId)

        val result = mockMvc.perform(
            post("/api/v1/inspections/{inspectionId}/media/upload-requests", inspectionId)
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.items[0].clientMediaId").value(clientMediaId.toString()))
            .andExpect(jsonPath("$.items[0].uploadStatus").value("PENDING"))
            .andReturn()

        val mediaId = objectMapper.readTree(result.response.contentAsByteArray)["items"][0]["mediaId"].stringValue()

        mockMvc.perform(
            post("/api/v1/inspections/{inspectionId}/media/upload-requests", inspectionId)
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
        ).andExpect(status().isCreated)

        mockMvc.perform(
            post("/api/v1/media/{mediaId}/upload-complete", mediaId)
                .header("Idempotency-Key", UUID.randomUUID()),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.uploadStatus").value("UPLOADED"))
            .andExpect(jsonPath("$.analysisStatus").value("QUEUED"))

        kotlin.test.assertEquals(1, analysisJobRepository.count())
        kotlin.test.assertEquals(
            MediaAnalysisJobState.QUEUED,
            analysisJobRepository.findByMediaId(UUID.fromString(mediaId))?.status,
        )

        mockMvc.perform(
            post("/api/v1/media/{mediaId}/upload-complete", mediaId)
                .header("Idempotency-Key", UUID.randomUUID()),
        ).andExpect(status().isOk)
        kotlin.test.assertEquals(1, analysisJobRepository.count())

        val mediaUuid = UUID.fromString(mediaId)
        kotlin.test.assertNotNull(analysisJobRepository.findByMediaId(mediaUuid)).also {
            it.status = MediaAnalysisJobState.COMPLETED
            it.completedAt = OffsetDateTime.now()
            it.modelVersion = "test-model"
            analysisJobRepository.save(it)
        }
        mediaRepository.findById(mediaUuid).orElseThrow().also {
            it.analysisStatus = MediaAnalysisState.COMPLETED
            mediaRepository.save(it)
        }
        mockMvc.perform(
            post("/api/v1/media/{mediaId}/upload-complete", mediaId)
                .header("Idempotency-Key", UUID.randomUUID()),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.analysisStatus").value("COMPLETED"))

        val finalizeKey = UUID.randomUUID()
        val finalizeBody = """{"expectedMediaCount":1}"""
        mockMvc.perform(
            post("/api/v1/inspections/{inspectionId}/media/finalize", inspectionId)
                .header("Idempotency-Key", finalizeKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(finalizeBody),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.expectedMediaCount").value(1))
            .andExpect(jsonPath("$.registeredMediaCount").value(1))
            .andExpect(jsonPath("$.analysisStatus").value("COMPLETED"))

        mockMvc.perform(
            post("/api/v1/inspections/{inspectionId}/media/finalize", inspectionId)
                .header("Idempotency-Key", finalizeKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(finalizeBody),
        ).andExpect(status().isOk)

        mockMvc.perform(
            post("/api/v1/inspections/{inspectionId}/media/upload-requests", inspectionId)
                .header("Idempotency-Key", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody(UUID.randomUUID(), sourceVideoId)),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("MEDIA_SET_FINALIZED"))

        mockMvc.perform(get("/api/v1/inspections/{inspectionId}/media", inspectionId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.items[0].id").value(mediaId))
    }

    @Test
    fun `진행 중 임장과 다른 본문의 멱등성 키 재사용을 거부한다`() {
        val inspectionId = createInspection(InspectionLifecycleStatus.IN_PROGRESS)
        mockMvc.perform(
            post("/api/v1/inspections/{inspectionId}/media/upload-requests", inspectionId)
                .header("Idempotency-Key", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody(UUID.randomUUID(), UUID.randomUUID())),
        ).andExpect(status().isConflict)

        val endedId = createInspection(InspectionLifecycleStatus.ENDED)
        val key = UUID.randomUUID()
        mockMvc.perform(
            post("/api/v1/inspections/{inspectionId}/media/upload-requests", endedId)
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody(UUID.randomUUID(), UUID.randomUUID())),
        ).andExpect(status().isCreated)

        mockMvc.perform(
            post("/api/v1/inspections/{inspectionId}/media/upload-requests", endedId)
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody(UUID.randomUUID(), UUID.randomUUID())),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_CONFLICT"))
    }

    private fun requestBody(clientMediaId: UUID, sourceVideoId: UUID) =
        """{"items":[{"clientMediaId":"$clientMediaId","contentType":"image/jpeg","fileSize":123,"width":640,"height":480,"sourceVideoId":"$sourceVideoId","sourceVideoOffsetMs":3000,"frameOrigin":"POST_RECORDING_EXTRACTION","captureSource":"META_GLASS","capturedAt":"2026-08-19T10:00:00+09:00"}]}"""

    private fun createEndedInspection() = createInspection(InspectionLifecycleStatus.ENDED)

    private fun createInspection(status: InspectionLifecycleStatus): UUID {
        val now = OffsetDateTime.now()
        val propertyId = propertyRepository.save(
            PropertyEntity(UUID.randomUUID(), DemoUserContext.DEMO_USER_ID, "미디어 테스트 매물", createdAt = now, updatedAt = now),
        ).id
        return inspectionRepository.save(
            InspectionEntity(
                id = UUID.randomUUID(),
                propertyId = propertyId,
                ownerId = DemoUserContext.DEMO_USER_ID,
                status = status,
                analysisStatus = InspectionAggregateStatus.NOT_STARTED,
                startedAt = now,
                endedAt = if (status == InspectionLifecycleStatus.ENDED) now else null,
                createdAt = now,
            ),
        ).id
    }

    @TestConfiguration
    class StorageTestConfiguration {
        @Bean
        @Primary
        fun fakeObjectStorage(): ObjectStorageGateway = object : ObjectStorageGateway {
            override fun createUploadUrl(key: String) =
                PresignedUpload(URI.create("http://storage.test/$key?signature=test"), OffsetDateTime.now().plusMinutes(15))

            override fun inspectJpeg(key: String, maximumBytes: Int) = StoredJpeg(123, 640, 480, "image/jpeg")
        }
    }
}
