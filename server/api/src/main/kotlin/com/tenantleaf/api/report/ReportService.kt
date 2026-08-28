package com.tenantleaf.api.report

import com.tenantleaf.api.generated.model.AiLabel
import com.tenantleaf.api.generated.model.Bbox
import com.tenantleaf.api.generated.model.BboxCoordinateSystem
import com.tenantleaf.api.generated.model.EvidenceDetection
import com.tenantleaf.api.generated.model.ImageDimensions
import com.tenantleaf.api.generated.model.Observation
import com.tenantleaf.api.generated.model.ObservationEvidence
import com.tenantleaf.api.generated.model.ObservationPage
import com.tenantleaf.api.generated.model.ObservationStatus
import com.tenantleaf.api.generated.model.ObservationType
import com.tenantleaf.api.generated.model.ReportDetail
import com.tenantleaf.api.generated.model.ReportFailureCode
import com.tenantleaf.api.generated.model.ReportPage
import com.tenantleaf.api.generated.model.ReportRepresentativePhoto
import com.tenantleaf.api.generated.model.ReportStatus
import com.tenantleaf.api.generated.model.ReportSummary
import com.tenantleaf.api.generated.model.UpdateObservationStatusRequest
import com.tenantleaf.api.generated.model.Zone
import com.tenantleaf.api.inspection.InspectionNotFoundException
import com.tenantleaf.api.inspection.InspectionRepository
import com.tenantleaf.api.media.MediaRepository
import com.tenantleaf.api.media.MediaAnalysisState
import com.tenantleaf.api.media.MediaEntity
import com.tenantleaf.api.media.MediaUploadState
import com.tenantleaf.api.media.MediaZone
import com.tenantleaf.api.media.ObjectStorageGateway
import com.tenantleaf.api.property.DemoUserContext
import com.tenantleaf.api.property.PropertyNotFoundException
import com.tenantleaf.api.property.PropertyRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

@Service
class ReportService(
    private val reportRepository: ReportRepository,
    private val observationRepository: ObservationRepository,
    private val evidenceRepository: ObservationEvidenceRepository,
    private val inspectionRepository: InspectionRepository,
    private val propertyRepository: PropertyRepository,
    private val mediaRepository: MediaRepository,
    private val storage: ObjectStorageGateway,
    private val coordinator: ReportGenerationCoordinator,
    private val userContext: DemoUserContext,
) {
    @Transactional
    fun getInspectionReport(inspectionId: UUID): ReportDetail {
        val inspection = ownedInspection(inspectionId)
        coordinator.evaluate(inspectionId)
        val report = reportRepository.findByInspectionId(inspectionId) ?: throw ReportNotFoundException()
        val property = propertyRepository.findByIdAndOwnerId(report.propertyId, userContext.requireUserId())
            ?: throw PropertyNotFoundException()
        val observations = observationRepository
            .findAllByInspectionIdAndStatusNot(inspectionId, ObservationState.DISMISSED)
        val media = mediaRepository.findAllByInspectionIdAndDeletedAtIsNull(inspectionId)
        return report.toDetail(
            propertyName = property.name,
            inspectionEndedAt = inspection.endedAt ?: inspection.startedAt,
            totalMediaCount = inspection.expectedMediaCount
                ?: (report.successfulMediaCount + report.failedMediaCount),
            representativePhotos = mapRepresentativePhotos(media),
            observations = mapObservations(observations),
        )
    }

    @Transactional(readOnly = true)
    fun listPropertyReports(propertyId: UUID, page: Int, size: Int): ReportPage {
        val ownerId = userContext.requireUserId()
        val property = propertyRepository.findByIdAndOwnerId(propertyId, ownerId) ?: throw PropertyNotFoundException()
        val result = reportRepository.findAllByPropertyId(
            propertyId,
            PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))),
        )
        val inspections = result.content.mapNotNull { inspectionRepository.findByIdAndOwnerId(it.inspectionId, ownerId) }
            .associateBy { it.id }
        return ReportPage(
            page = result.number,
            propertySize = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            items = result.content.map { report ->
                val inspection = inspections[report.inspectionId]
                report.toSummary(
                    propertyName = property.name,
                    inspectionEndedAt = inspection?.endedAt ?: report.createdAt,
                    totalMediaCount = inspection?.expectedMediaCount
                        ?: (report.successfulMediaCount + report.failedMediaCount),
                )
            },
        )
    }

    @Transactional(readOnly = true)
    fun listObservations(inspectionId: UUID, page: Int, size: Int): ObservationPage {
        ownedInspection(inspectionId)
        val result = observationRepository.findAllByInspectionId(
            inspectionId,
            PageRequest.of(page, size, Sort.by(Sort.Order.asc("aiZone"), Sort.Order.desc("confidence"))),
        )
        return ObservationPage(
            page = result.number,
            propertySize = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            items = mapObservations(result.content),
        )
    }

    @Transactional(readOnly = true)
    fun getObservation(observationId: UUID): Observation = mapObservations(listOf(ownedObservation(observationId))).single()

    @Transactional
    fun updateObservationStatus(observationId: UUID, request: UpdateObservationStatusRequest): Observation {
        val entity = ownedObservation(observationId)
        entity.status = when (request.status) {
            UpdateObservationStatusRequest.Status.VIEWED -> ObservationState.VIEWED
            UpdateObservationStatusRequest.Status.DISMISSED -> ObservationState.DISMISSED
        }
        entity.updatedAt = OffsetDateTime.now()
        observationRepository.save(entity)
        coordinator.evaluate(entity.inspectionId)
        return mapObservations(listOf(entity)).single()
    }

    private fun mapObservations(entities: List<ObservationEntity>): List<Observation> {
        if (entities.isEmpty()) return emptyList()
        val evidence = evidenceRepository.findAllByIdObservationIdIn(entities.map { it.id })
        val evidenceByObservation = evidence.groupBy { it.id.observationId }
        val allInspectionObservations = entities.map { it.inspectionId }.distinct().flatMap {
            observationRepository.findAllByInspectionIdAndStatusNot(it, ObservationState.DISMISSED)
        }.distinctBy { it.id }
        val allEvidence = evidenceRepository.findAllByIdObservationIdIn(allInspectionObservations.map { it.id })
        val observationById = allInspectionObservations.associateBy { it.id }
        val detectionsByMedia = allEvidence.groupBy { it.id.mediaId }
        val mediaIds = evidence.map { it.id.mediaId }.toSet()
        val mediaById = mediaRepository.findAllById(mediaIds).associateBy { it.id }

        return entities.map { entity ->
            val definition = ObservationCatalog.forClassId(entity.classId) ?: throw IllegalStateException("Unknown classId")
            Observation(
                id = entity.id,
                inspectionId = entity.inspectionId,
                type = ObservationType.valueOf(entity.type),
                zone = Zone.valueOf(entity.userCorrectedZone ?: entity.aiZone),
                aiZone = Zone.valueOf(entity.aiZone),
                zoneConfidence = entity.zoneConfidence,
                zoneUncertain = entity.zoneUncertain,
                zoneModelVersion = entity.zoneModelVersion,
                userCorrectedZone = entity.userCorrectedZone?.let(Zone::valueOf),
                correctedAt = null,
                title = definition.title,
                description = definition.description,
                confidence = entity.confidence,
                observationMinConfidence = entity.observationMinConfidence,
                status = ObservationStatus.valueOf(entity.status.name),
                aiClassId = entity.classId,
                aiLabel = AiLabel.forValue(entity.aiLabel),
                modelVersion = entity.modelVersion,
                evidence = evidenceByObservation[entity.id].orEmpty().map { item ->
                    val media = mediaById[item.id.mediaId]
                    val signed = media?.takeIf { it.deletedAt == null }?.let { storage.createViewUrl(it.storageKey) }
                    ObservationEvidence(
                        mediaId = item.id.mediaId,
                        primary = item.isRepresentative,
                        available = signed != null,
                        sourceVideoOffsetMs = media?.sourceVideoOffsetMs ?: 0,
                        image = ImageDimensions(item.imageWidth, item.imageHeight),
                        coordinateSystem = BboxCoordinateSystem.PIXEL_XYXY,
                        detections = detectionsByMedia[item.id.mediaId].orEmpty().mapNotNull { sameMediaEvidence ->
                            val sameObservation = observationById[sameMediaEvidence.id.observationId] ?: return@mapNotNull null
                            val sameDefinition = ObservationCatalog.forClassId(sameObservation.classId) ?: return@mapNotNull null
                            EvidenceDetection(
                                observationId = sameObservation.id,
                                classId = sameObservation.classId,
                                label = AiLabel.forValue(sameObservation.aiLabel),
                                displayLabel = sameDefinition.displayLabel,
                                displayColor = sameDefinition.displayColor,
                                confidence = sameMediaEvidence.confidence,
                                box = Bbox(
                                    left = sameMediaEvidence.boxLeft,
                                    top = sameMediaEvidence.boxTop,
                                    right = sameMediaEvidence.boxRight,
                                    bottom = sameMediaEvidence.boxBottom,
                                ),
                            )
                        },
                        viewUrl = signed?.url,
                        viewUrlExpiresAt = signed?.expiresAt,
                    )
                },
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
            )
        }
    }

    private fun ReportEntity.toDetail(
        propertyName: String,
        inspectionEndedAt: OffsetDateTime,
        totalMediaCount: Int,
        representativePhotos: List<ReportRepresentativePhoto>,
        observations: List<Observation>,
    ) = ReportDetail(
        id = id,
        propertyId = propertyId,
        inspectionId = inspectionId,
        status = ReportStatus.valueOf(status.name),
        totalMediaCount = totalMediaCount,
        successfulMediaCount = successfulMediaCount,
        failedMediaCount = failedMediaCount,
        observationCount = observations.size,
        propertyDisplayName = propertyName,
        inspectionEndedAt = inspectionEndedAt,
        failureCode = failureCode?.let(ReportFailureCode::forValue),
        templateVersion = templateVersion,
        generatedAt = generatedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
        emptyObservationMessage = if (status == ReportState.COMPLETED && observations.isEmpty()) EMPTY_MESSAGE else null,
        representativePhotos = representativePhotos,
        observations = observations,
    )

    private fun mapRepresentativePhotos(media: List<MediaEntity>): List<ReportRepresentativePhoto> =
        selectReportRepresentativeMedia(media).map { item ->
            val signed = storage.createViewUrl(item.storageKey)
            ReportRepresentativePhoto(
                mediaId = item.id,
                zone = Zone.valueOf((item.aiZone ?: MediaZone.UNKNOWN).name),
                zoneUncertain = item.zoneUncertain != false || item.aiZone == null || item.aiZone == MediaZone.UNKNOWN,
                zoneModelVersion = item.zoneModelVersion,
                sourceVideoOffsetMs = item.sourceVideoOffsetMs,
                image = ImageDimensions(item.width, item.height),
                viewUrl = signed.url,
                viewUrlExpiresAt = signed.expiresAt,
            )
        }

    private fun ReportEntity.toSummary(
        propertyName: String,
        inspectionEndedAt: OffsetDateTime,
        totalMediaCount: Int,
    ) = ReportSummary(
        id = id,
        propertyId = propertyId,
        inspectionId = inspectionId,
        status = ReportStatus.valueOf(status.name),
        totalMediaCount = totalMediaCount,
        successfulMediaCount = successfulMediaCount,
        failedMediaCount = failedMediaCount,
        observationCount = observationCount,
        propertyDisplayName = propertyName,
        inspectionEndedAt = inspectionEndedAt,
        failureCode = failureCode?.let(ReportFailureCode::forValue),
        templateVersion = templateVersion,
        generatedAt = generatedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun ownedInspection(id: UUID) =
        inspectionRepository.findByIdAndOwnerId(id, userContext.requireUserId()) ?: throw InspectionNotFoundException()

    private fun ownedObservation(id: UUID): ObservationEntity {
        val observation = observationRepository.findById(id).orElse(null) ?: throw ObservationNotFoundException()
        ownedInspection(observation.inspectionId)
        return observation
    }

    companion object {
        const val EMPTY_MESSAGE = "현재 촬영 근거에서 확인 필요 관찰이 생성되지 않았습니다."
    }
}

internal fun selectReportRepresentativeMedia(
    media: List<MediaEntity>,
    limit: Int = 12,
    perZoneLimit: Int = 3,
    minimumOffsetGapMs: Long = 6_000,
): List<MediaEntity> {
    val completed = media
        .filter {
            it.uploadStatus == MediaUploadState.UPLOADED &&
                it.analysisStatus == MediaAnalysisState.COMPLETED &&
                it.containsPerson == false
        }
        .sortedWith(compareBy<MediaEntity> { it.sourceVideoOffsetMs }.thenBy { it.id })
    if (completed.isEmpty() || limit <= 0) return emptyList()

    require(perZoneLimit > 0) { "perZoneLimit must be greater than zero" }
    require(minimumOffsetGapMs >= 0) { "minimumOffsetGapMs must not be negative" }

    val classifiedRepresentatives = completed
        .filter { it.aiZone != null && it.aiZone != MediaZone.UNKNOWN && it.zoneUncertain != true }
        .groupBy { it.aiZone!! }
        .values
        .flatMap { sameZone ->
            selectTemporallyDistinct(sameZone, perZoneLimit, minimumOffsetGapMs)
        }
        .sortedBy { it.sourceVideoOffsetMs }

    if (classifiedRepresentatives.isNotEmpty()) return classifiedRepresentatives.take(limit)
    if (completed.size <= 3) return completed.take(limit)

    return listOf(completed.first(), completed[(completed.lastIndex) / 2], completed.last())
        .distinctBy { it.id }
        .take(limit)
}

private fun selectTemporallyDistinct(
    media: List<MediaEntity>,
    limit: Int,
    minimumOffsetGapMs: Long,
): List<MediaEntity> {
    val selected = mutableListOf<MediaEntity>()
    media.sortedBy { it.sourceVideoOffsetMs }.forEach { candidate ->
        if (selected.none { kotlin.math.abs(it.sourceVideoOffsetMs - candidate.sourceVideoOffsetMs) < minimumOffsetGapMs }) {
            selected += candidate
        }
    }
    return selected.take(limit)
}
