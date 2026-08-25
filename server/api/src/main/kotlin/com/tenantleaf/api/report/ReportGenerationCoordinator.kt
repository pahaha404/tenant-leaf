package com.tenantleaf.api.report

import com.tenantleaf.api.inspection.InspectionLifecycleStatus
import com.tenantleaf.api.inspection.InspectionRepository
import com.tenantleaf.api.media.MediaAnalysisDetectionEntity
import com.tenantleaf.api.media.MediaAnalysisDetectionRepository
import com.tenantleaf.api.media.MediaAnalysisState
import com.tenantleaf.api.media.MediaRepository
import com.tenantleaf.api.media.MediaUploadState
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.OffsetDateTime
import java.util.UUID

@Service
class ReportGenerationCoordinator(
    private val inspectionRepository: InspectionRepository,
    private val mediaRepository: MediaRepository,
    private val detectionRepository: MediaAnalysisDetectionRepository,
    private val thresholdRepository: ObservationThresholdRepository,
    private val observationRepository: ObservationRepository,
    private val evidenceRepository: ObservationEvidenceRepository,
    private val reportRepository: ReportRepository,
    private val clock: Clock = Clock.systemUTC(),
) {
    @Scheduled(
        fixedDelayString = "\${app.report.reconcile-delay-ms:2000}",
        initialDelayString = "\${app.report.reconcile-initial-delay-ms:2000}",
    )
    fun reconcileFinalizedInspections() {
        inspectionRepository.findAllByStatusAndMediaFinalizedAtIsNotNull(InspectionLifecycleStatus.ENDED)
            .forEach { runCatching { evaluate(it.id) } }
    }

    @Transactional
    fun evaluate(inspectionId: UUID): ReportEntity? {
        val inspection = inspectionRepository.findById(inspectionId).orElse(null) ?: return null
        if (inspection.status != InspectionLifecycleStatus.ENDED || inspection.mediaFinalizedAt == null) return null
        val now = OffsetDateTime.now(clock)
        val media = mediaRepository.findAllByInspectionIdAndDeletedAtIsNull(inspectionId)
        val expected = inspection.expectedMediaCount ?: return null
        val uploadsComplete = media.size == expected && media.all { it.uploadStatus == MediaUploadState.UPLOADED }
        val analysesFinal = uploadsComplete && media.all {
            it.analysisStatus == MediaAnalysisState.COMPLETED || it.analysisStatus == MediaAnalysisState.FAILED
        }

        val report = reportRepository.findByInspectionId(inspectionId) ?: ReportEntity(
            id = UUID.randomUUID(),
            propertyId = inspection.propertyId,
            inspectionId = inspection.id,
            status = ReportState.WAITING_FOR_ANALYSIS,
            successfulMediaCount = 0,
            failedMediaCount = 0,
            observationCount = 0,
            failureCode = null,
            templateVersion = TEMPLATE_VERSION,
            generatedAt = null,
            createdAt = now,
            updatedAt = now,
        )

        if (!analysesFinal) {
            report.status = ReportState.WAITING_FOR_ANALYSIS
            report.successfulMediaCount = media.count { it.analysisStatus == MediaAnalysisState.COMPLETED }
            report.failedMediaCount = media.count { it.analysisStatus == MediaAnalysisState.FAILED }
            report.updatedAt = now
            return reportRepository.save(report)
        }

        report.status = ReportState.GENERATING
        report.updatedAt = now
        reportRepository.save(report)
        projectObservations(inspectionId, media.associateBy { it.id }, now)

        val successCount = media.count { it.analysisStatus == MediaAnalysisState.COMPLETED }
        val failedCount = media.count { it.analysisStatus == MediaAnalysisState.FAILED }
        val observationCount = observationRepository
            .findAllByInspectionIdAndStatusNot(inspectionId, ObservationState.DISMISSED)
            .size
        report.successfulMediaCount = successCount
        report.failedMediaCount = failedCount
        report.observationCount = observationCount
        report.failureCode = if (successCount == 0) "NO_ANALYZABLE_MEDIA" else null
        report.status = when {
            successCount == 0 -> ReportState.FAILED
            failedCount > 0 -> ReportState.PARTIAL_COMPLETED
            else -> ReportState.COMPLETED
        }
        report.generatedAt = now
        report.updatedAt = now
        return reportRepository.save(report)
    }

    private fun projectObservations(
        inspectionId: UUID,
        mediaById: Map<UUID, com.tenantleaf.api.media.MediaEntity>,
        now: OffsetDateTime,
    ) {
        val detections = detectionRepository.findAllByMediaIdIn(mediaById.keys)
        detections.forEach { detection ->
            if (observationRepository.existsBySourceDetectionId(detection.id)) return@forEach
            val media = mediaById[detection.mediaId] ?: return@forEach
            val mapping = ObservationCatalog.forClassId(detection.classId) ?: return@forEach
            if (normalizeLabel(detection.label) != mapping.label) return@forEach
            if (!validBox(detection, media.width, media.height)) return@forEach
            val threshold = thresholdRepository.findById(ObservationThresholdId(detection.modelVersion, detection.classId))
                .orElse(null)?.minimumConfidence ?: return@forEach
            if (detection.confidence < threshold) return@forEach

            val zone = media.userCorrectedZone ?: media.aiZone ?: media.zone
            val observation = ObservationEntity(
                id = UUID.randomUUID(),
                inspectionId = inspectionId,
                sourceDetectionId = detection.id,
                type = mapping.type,
                status = ObservationState.ACTIVE,
                aiZone = (media.aiZone ?: media.zone ?: com.tenantleaf.api.media.MediaZone.UNKNOWN).name,
                zoneConfidence = media.zoneConfidence,
                zoneUncertain = media.zoneUncertain ?: (zone == null || zone == com.tenantleaf.api.media.MediaZone.UNKNOWN),
                zoneModelVersion = media.zoneModelVersion,
                userCorrectedZone = media.userCorrectedZone?.name,
                classId = detection.classId,
                aiLabel = mapping.label,
                confidence = detection.confidence,
                observationMinConfidence = threshold,
                modelVersion = detection.modelVersion,
                templateVersion = TEMPLATE_VERSION,
                createdAt = now,
                updatedAt = now,
            )
            observationRepository.save(observation)
            evidenceRepository.save(
                ObservationEvidenceEntity(
                    id = ObservationEvidenceId(observation.id, media.id),
                    detectionId = detection.id,
                    isRepresentative = true,
                    coordinateSystem = "PIXEL_XYXY",
                    imageWidth = media.width,
                    imageHeight = media.height,
                    boxLeft = detection.boxLeft,
                    boxTop = detection.boxTop,
                    boxRight = detection.boxRight,
                    boxBottom = detection.boxBottom,
                    confidence = detection.confidence,
                    createdAt = now,
                ),
            )
        }
    }

    private fun validBox(detection: MediaAnalysisDetectionEntity, width: Int, height: Int) =
        detection.boxLeft >= 0 && detection.boxTop >= 0 &&
            detection.boxRight > detection.boxLeft && detection.boxBottom > detection.boxTop &&
            detection.boxRight <= width && detection.boxBottom <= height

    private fun normalizeLabel(label: String): String = when (label.lowercase().replace("_", "")) {
        "unknowndefect" -> "other"
        else -> label.lowercase().replace("_", "")
    }

    companion object {
        const val TEMPLATE_VERSION = "observation-ko-v1"
    }
}

data class ObservationDefinition(
    val type: String,
    val label: String,
    val displayLabel: String,
    val displayColor: String,
    val title: String,
    val description: String,
)

object ObservationCatalog {
    private val definitions = listOf(
        ObservationDefinition("CRACK_CHECK_NEEDED", "crack", "균열", "#E53935", "균열로 추정되는 흔적 확인 필요", "표시된 부분의 선형 흔적을 직접 확인해 주세요."),
        ObservationDefinition("MOLD_CHECK_NEEDED", "mold", "곰팡이", "#2E7D32", "곰팡이로 추정되는 흔적 확인 필요", "습기나 변색이 있는지 표시된 부분을 직접 확인해 주세요."),
        ObservationDefinition("PEELING_CHECK_NEEDED", "peeling", "들뜸·박리", "#FB8C00", "들뜸·박리로 추정되는 흔적 확인 필요", "마감재가 들뜨거나 벗겨졌는지 직접 확인해 주세요."),
        ObservationDefinition("WATER_DAMAGE_CHECK_NEEDED", "waterdamage", "누수", "#1E88E5", "누수로 추정되는 흔적 확인 필요", "물 번짐이나 변색 여부를 표시된 부분에서 확인해 주세요."),
        ObservationDefinition("TILE_DAMAGE_CHECK_NEEDED", "tiledamage", "타일 손상", "#8E24AA", "타일 손상으로 추정되는 흔적 확인 필요", "타일 표면의 손상 여부를 직접 확인해 주세요."),
        ObservationDefinition("HOLE_CHECK_NEEDED", "hole", "구멍", "#6D4C41", "구멍으로 추정되는 흔적 확인 필요", "표시된 타공 또는 구멍 흔적을 직접 확인해 주세요."),
        ObservationDefinition("TILE_CRACK_CHECK_NEEDED", "tilecrack", "타일 균열", "#D81B60", "타일 균열로 추정되는 흔적 확인 필요", "타일의 선형 균열 여부를 직접 확인해 주세요."),
        ObservationDefinition("PAINT_DRIPS_CHECK_NEEDED", "paintdrips", "페인트 흘러내림", "#5E35B1", "페인트 흘러내림으로 추정되는 흔적 확인 필요", "도장면의 흘러내림 흔적을 직접 확인해 주세요."),
        ObservationDefinition("PINHOLE_CHECK_NEEDED", "pinhole", "미세 구멍", "#00897B", "미세 구멍으로 추정되는 흔적 확인 필요", "표면의 작은 구멍 흔적을 직접 확인해 주세요."),
        ObservationDefinition("SURFACE_DEFECT_CHECK_NEEDED", "surfacedefect", "표면 하자", "#F9A825", "표면 하자로 추정되는 흔적 확인 필요", "표면의 고르지 않은 흔적을 직접 확인해 주세요."),
        ObservationDefinition("STAIN_CHECK_NEEDED", "stain", "오염", "#558B2F", "오염으로 추정되는 흔적 확인 필요", "표시된 변색이나 오염 흔적을 직접 확인해 주세요."),
        ObservationDefinition("TROWEL_MARK_CHECK_NEEDED", "trowelmark", "마감 자국", "#546E7A", "마감 자국으로 추정되는 흔적 확인 필요", "표면의 마감 자국을 직접 확인해 주세요."),
        ObservationDefinition("OTHER_CHECK_NEEDED", "other", "하자 의심", "#F4511E", "기타 확인 필요 흔적", "AI가 구체적인 유형을 분류하지 못한 흔적입니다. 사진을 직접 확인해 주세요."),
    )

    fun forClassId(classId: Int): ObservationDefinition? = definitions.getOrNull(classId)
}
