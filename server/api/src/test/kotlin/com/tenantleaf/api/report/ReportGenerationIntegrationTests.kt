package com.tenantleaf.api.report

import com.tenantleaf.api.inspection.InspectionAggregateStatus
import com.tenantleaf.api.inspection.InspectionEntity
import com.tenantleaf.api.inspection.InspectionLifecycleStatus
import com.tenantleaf.api.inspection.InspectionRepository
import com.tenantleaf.api.media.MediaAnalysisDetectionEntity
import com.tenantleaf.api.media.MediaAnalysisDetectionRepository
import com.tenantleaf.api.media.MediaAnalysisJobEntity
import com.tenantleaf.api.media.MediaAnalysisJobRepository
import com.tenantleaf.api.media.MediaAnalysisJobState
import com.tenantleaf.api.media.MediaAnalysisState
import com.tenantleaf.api.media.MediaCaptureSource
import com.tenantleaf.api.media.MediaEntity
import com.tenantleaf.api.media.MediaRepository
import com.tenantleaf.api.media.MediaUploadState
import com.tenantleaf.api.property.DemoUserContext
import com.tenantleaf.api.property.PropertyEntity
import com.tenantleaf.api.property.PropertyRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@SpringBootTest
@Transactional
class ReportGenerationIntegrationTests(
    @Autowired private val coordinator: ReportGenerationCoordinator,
    @Autowired private val propertyRepository: PropertyRepository,
    @Autowired private val inspectionRepository: InspectionRepository,
    @Autowired private val mediaRepository: MediaRepository,
    @Autowired private val jobRepository: MediaAnalysisJobRepository,
    @Autowired private val detectionRepository: MediaAnalysisDetectionRepository,
    @Autowired private val observationRepository: ObservationRepository,
    @Autowired private val evidenceRepository: ObservationEvidenceRepository,
) {
    @Test
    fun `분석 완료 탐지를 관찰과 bbox 근거가 있는 완료 리포트로 만든다`() {
        val now = OffsetDateTime.now()
        val propertyId = propertyRepository.save(
            PropertyEntity(UUID.randomUUID(), DemoUserContext.DEMO_USER_ID, "리포트 테스트 매물", createdAt = now, updatedAt = now),
        ).id
        val inspectionId = inspectionRepository.save(
            InspectionEntity(
                id = UUID.randomUUID(),
                propertyId = propertyId,
                ownerId = DemoUserContext.DEMO_USER_ID,
                status = InspectionLifecycleStatus.ENDED,
                analysisStatus = InspectionAggregateStatus.COMPLETED,
                startedAt = now.minusMinutes(10),
                endedAt = now,
                mediaFinalizedAt = now,
                expectedMediaCount = 1,
                createdAt = now.minusMinutes(10),
            ),
        ).id
        val media = mediaRepository.save(
            MediaEntity(
                id = UUID.randomUUID(),
                inspectionId = inspectionId,
                ownerId = DemoUserContext.DEMO_USER_ID,
                clientMediaId = UUID.randomUUID(),
                declaredFileSize = 100_000,
                actualFileSize = 100_000,
                width = 1080,
                height = 1440,
                sourceVideoId = UUID.randomUUID(),
                sourceVideoOffsetMs = 3_000,
                captureSource = MediaCaptureSource.META_GLASS,
                capturedAt = now,
                storageKey = "reports/test.jpg",
                uploadStatus = MediaUploadState.UPLOADED,
                analysisStatus = MediaAnalysisState.COMPLETED,
                uploadedAt = now,
                createdAt = now,
                updatedAt = now,
            ),
        )
        val job = jobRepository.save(
            MediaAnalysisJobEntity(
                id = UUID.randomUUID(),
                mediaId = media.id,
                status = MediaAnalysisJobState.COMPLETED,
                attemptCount = 1,
                availableAt = now,
                startedAt = now,
                completedAt = now,
                modelVersion = "two_stage_negative_rot4",
                createdAt = now,
                updatedAt = now,
            ),
        )
        detectionRepository.save(
            MediaAnalysisDetectionEntity(
                id = UUID.randomUUID(),
                jobId = job.id,
                mediaId = media.id,
                classId = 1,
                label = "mold",
                confidence = 0.76,
                boxLeft = 120.0,
                boxTop = 80.0,
                boxRight = 640.0,
                boxBottom = 410.0,
                modelVersion = "two_stage_negative_rot4",
                createdAt = now,
            ),
        )

        val report = coordinator.evaluate(inspectionId)

        assertNotNull(report)
        assertEquals(ReportState.COMPLETED, report.status)
        assertEquals(1, report.observationCount)
        assertEquals(95, report.referenceScore)
        val observation = observationRepository.findAllByInspectionIdAndStatusNot(inspectionId, ObservationState.DISMISSED).single()
        val evidence = evidenceRepository.findAllByIdObservationIdIn(listOf(observation.id)).single()
        assertEquals("MOLD_CHECK_NEEDED", observation.type)
        assertEquals("PIXEL_XYXY", evidence.coordinateSystem)
        assertEquals(640.0, evidence.boxRight)
    }
}
