package com.tenantleaf.api.media

import com.tenantleaf.api.generated.model.CaptureSource
import com.tenantleaf.api.generated.model.CreateMediaUploadBatchRequest
import com.tenantleaf.api.generated.model.CreateMediaUploadBatchResponse
import com.tenantleaf.api.generated.model.CreateMediaUploadRequest
import com.tenantleaf.api.generated.model.FrameOrigin
import com.tenantleaf.api.generated.model.FinalizeInspectionMediaRequest
import com.tenantleaf.api.generated.model.FinalizeInspectionMediaResponse
import com.tenantleaf.api.generated.model.InspectionAnalysisStatus
import com.tenantleaf.api.generated.model.Media
import com.tenantleaf.api.generated.model.MediaAnalysisStatus
import com.tenantleaf.api.generated.model.MediaPage
import com.tenantleaf.api.generated.model.MediaType
import com.tenantleaf.api.generated.model.MediaUploadInstruction
import com.tenantleaf.api.generated.model.MediaUploadStatus
import com.tenantleaf.api.generated.model.Zone
import com.tenantleaf.api.inspection.InspectionLifecycleStatus
import com.tenantleaf.api.inspection.InspectionAggregateStatus
import com.tenantleaf.api.inspection.InspectionNotFoundException
import com.tenantleaf.api.inspection.InspectionRepository
import com.tenantleaf.api.inspection.InspectionStateTransitionException
import com.tenantleaf.api.property.DemoUserContext
import com.tenantleaf.api.report.ReportGenerationCoordinator
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.OffsetDateTime
import java.util.UUID

@Service
class MediaService(
    private val mediaRepository: MediaRepository,
    private val idempotencyRepository: ApiIdempotencyRecordRepository,
    private val analysisJobRepository: MediaAnalysisJobRepository,
    private val inspectionRepository: InspectionRepository,
    private val storage: ObjectStorageGateway,
    private val userContext: DemoUserContext,
    private val reportCoordinator: ReportGenerationCoordinator,
) {
    @Transactional
    fun createUploadRequests(
        inspectionId: UUID,
        idempotencyKey: UUID,
        request: CreateMediaUploadBatchRequest,
    ): CreateMediaUploadBatchResponse {
        val ownerId = userContext.requireUserId()
        val inspection = requireEndedInspection(inspectionId, ownerId)
        if (inspection.mediaFinalizedAt != null) throw MediaSetFinalizedException()
        val requestHash = hash(request.items.joinToString("\n") { it.fingerprint() })
        checkAndRecordIdempotency(ownerId, "CREATE_MEDIA_UPLOADS", inspectionId.toString(), idempotencyKey, requestHash)

        val duplicateIds = request.items.groupingBy { it.clientMediaId }.eachCount().filterValues { it > 1 }.keys
        if (duplicateIds.isNotEmpty()) throw MediaValidationException("clientMediaId", "한 요청에 같은 사진 ID를 중복해서 보낼 수 없습니다.")

        val now = OffsetDateTime.now()
        val entities = request.items.map { item ->
            validate(item)
            mediaRepository.findByInspectionIdAndClientMediaId(inspectionId, item.clientMediaId)?.also {
                if (!it.matches(item)) throw ClientMediaIdConflictException()
            } ?: MediaEntity(
                id = UUID.randomUUID(),
                inspectionId = inspectionId,
                ownerId = ownerId,
                clientMediaId = item.clientMediaId,
                zone = MediaZone.valueOf(item.zone.name),
                declaredFileSize = item.fileSize,
                width = item.width,
                height = item.height,
                sourceVideoId = item.sourceVideoId,
                sourceVideoOffsetMs = item.sourceVideoOffsetMs,
                captureSource = MediaCaptureSource.valueOf(item.captureSource.name),
                capturedAt = item.capturedAt,
                storageKey = "$ownerId/$inspectionId/${item.clientMediaId}.jpg",
                uploadStatus = MediaUploadState.PENDING,
                analysisStatus = MediaAnalysisState.NOT_REQUESTED,
                createdAt = now,
                updatedAt = now,
            )
        }
        mediaRepository.saveAll(entities)
        inspection.analysisStatus = InspectionAggregateStatus.UPLOADING
        return CreateMediaUploadBatchResponse(entities.map(::instruction))
    }

    @Transactional
    fun retryUpload(mediaId: UUID, idempotencyKey: UUID): MediaUploadInstruction {
        val ownerId = userContext.requireUserId()
        val entity = requireMedia(mediaId, ownerId)
        val inspection = requireEndedInspection(entity.inspectionId, ownerId)
        checkAndRecordIdempotency(ownerId, "RETRY_MEDIA_UPLOAD", mediaId.toString(), idempotencyKey, hash(mediaId.toString()))
        if (entity.uploadStatus == MediaUploadState.UPLOADED) throw MediaStateException()
        entity.uploadStatus = MediaUploadState.PENDING
        entity.uploadAttemptCount += 1
        entity.updatedAt = OffsetDateTime.now()
        inspection.analysisStatus = InspectionAggregateStatus.UPLOADING
        return instruction(entity)
    }

    @Transactional
    fun completeUpload(mediaId: UUID, idempotencyKey: UUID): Media {
        val ownerId = userContext.requireUserId()
        val entity = requireMedia(mediaId, ownerId)
        checkAndRecordIdempotency(ownerId, "COMPLETE_MEDIA_UPLOAD", mediaId.toString(), idempotencyKey, hash(mediaId.toString()))
        if (entity.uploadStatus == MediaUploadState.UPLOADED) {
            enqueueAnalysis(entity)
            refreshInspectionAnalysisStatus(entity.inspectionId, ownerId)
            return entity.toApi()
        }
        requireEndedInspection(entity.inspectionId, ownerId)
        val actual = storage.inspectJpeg(entity.storageKey, MAX_JPEG_BYTES)
        if (actual.contentType != null && actual.contentType != "image/jpeg") {
            throw MediaValidationException("contentType", "Content-Type은 image/jpeg여야 합니다.")
        }
        if (actual.size != entity.declaredFileSize) {
            throw MediaValidationException("fileSize", "등록한 파일 크기와 업로드된 파일 크기가 다릅니다.")
        }
        if (actual.width != entity.width || actual.height != entity.height) {
            throw MediaValidationException("width", "등록한 이미지 크기와 업로드된 JPEG 크기가 다릅니다.")
        }
        val now = OffsetDateTime.now()
        entity.actualFileSize = actual.size
        entity.uploadStatus = MediaUploadState.UPLOADED
        entity.analysisStatus = MediaAnalysisState.QUEUED
        entity.uploadedAt = now
        entity.updatedAt = now
        enqueueAnalysis(entity, now)
        refreshInspectionAnalysisStatus(entity.inspectionId, ownerId)
        return entity.toApi()
    }

    @Transactional(readOnly = true)
    fun get(mediaId: UUID): Media = requireMedia(mediaId, userContext.requireUserId()).toApi()

    @Transactional(readOnly = true)
    fun list(inspectionId: UUID, page: Int, size: Int): MediaPage {
        val ownerId = userContext.requireUserId()
        if (inspectionRepository.findByIdAndOwnerId(inspectionId, ownerId) == null) throw InspectionNotFoundException()
        val result = mediaRepository.findAllByInspectionIdAndOwnerIdAndDeletedAtIsNull(
            inspectionId,
            ownerId,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")),
        )
        return MediaPage(result.number, result.size, result.totalElements, result.totalPages, result.content.map { it.toApi() })
    }

    @Transactional
    fun finalizeMedia(
        inspectionId: UUID,
        idempotencyKey: UUID,
        request: FinalizeInspectionMediaRequest,
    ): FinalizeInspectionMediaResponse {
        val ownerId = userContext.requireUserId()
        val inspection = requireEndedInspection(inspectionId, ownerId)
        val requestHash = hash(request.expectedMediaCount.toString())
        checkAndRecordIdempotency(ownerId, "FINALIZE_INSPECTION_MEDIA", inspectionId.toString(), idempotencyKey, requestHash)

        val registeredMediaCount = mediaRepository.countByInspectionIdAndOwnerIdAndDeletedAtIsNull(inspectionId, ownerId).toInt()
        if (registeredMediaCount != request.expectedMediaCount) throw MediaSetCountMismatchException()

        val finalizedAt = inspection.mediaFinalizedAt ?: OffsetDateTime.now().also {
            inspection.mediaFinalizedAt = it
            inspection.expectedMediaCount = request.expectedMediaCount
        }
        if (inspection.expectedMediaCount != request.expectedMediaCount) throw MediaSetCountMismatchException()

        refreshInspectionAnalysisStatus(inspectionId, ownerId)
        reportCoordinator.evaluate(inspectionId)
        return FinalizeInspectionMediaResponse(
            inspectionId = inspectionId,
            expectedMediaCount = request.expectedMediaCount,
            registeredMediaCount = registeredMediaCount,
            mediaFinalizedAt = finalizedAt,
            analysisStatus = InspectionAnalysisStatus.valueOf(inspection.analysisStatus.name),
        )
    }

    private fun requireEndedInspection(inspectionId: UUID, ownerId: UUID) =
        inspectionRepository.findByIdAndOwnerId(inspectionId, ownerId)?.also {
            if (it.status != InspectionLifecycleStatus.ENDED) throw InspectionStateTransitionException()
        } ?: throw InspectionNotFoundException()

    private fun refreshInspectionAnalysisStatus(inspectionId: UUID, ownerId: UUID) {
        val inspection = requireEndedInspection(inspectionId, ownerId)
        val media = mediaRepository.findAllByInspectionIdAndDeletedAtIsNull(inspectionId)
        inspection.analysisStatus = when {
            media.isEmpty() && inspection.mediaFinalizedAt == null -> InspectionAggregateStatus.NOT_STARTED
            media.isEmpty() -> InspectionAggregateStatus.FAILED
            media.any { it.uploadStatus != MediaUploadState.UPLOADED } -> InspectionAggregateStatus.UPLOADING
            media.any { it.analysisStatus == MediaAnalysisState.ANALYZING } -> InspectionAggregateStatus.ANALYZING
            media.any { it.analysisStatus == MediaAnalysisState.QUEUED || it.analysisStatus == MediaAnalysisState.NOT_REQUESTED } ->
                InspectionAggregateStatus.QUEUED
            inspection.mediaFinalizedAt == null -> InspectionAggregateStatus.UPLOADING
            media.all { it.analysisStatus == MediaAnalysisState.COMPLETED } -> InspectionAggregateStatus.COMPLETED
            media.any { it.analysisStatus == MediaAnalysisState.COMPLETED } && media.any { it.analysisStatus == MediaAnalysisState.FAILED } ->
                InspectionAggregateStatus.PARTIAL_COMPLETED
            media.all { it.analysisStatus == MediaAnalysisState.FAILED } -> InspectionAggregateStatus.FAILED
            else -> InspectionAggregateStatus.QUEUED
        }
    }

    private fun requireMedia(mediaId: UUID, ownerId: UUID) =
        mediaRepository.findByIdAndOwnerIdAndDeletedAtIsNull(mediaId, ownerId) ?: throw MediaNotFoundException()

    private fun enqueueAnalysis(entity: MediaEntity, now: OffsetDateTime = OffsetDateTime.now()) {
        val existing = analysisJobRepository.findByMediaId(entity.id)
        if (existing == null) {
            analysisJobRepository.save(
                MediaAnalysisJobEntity(
                    id = UUID.randomUUID(),
                    mediaId = entity.id,
                    status = MediaAnalysisJobState.QUEUED,
                    attemptCount = 0,
                    availableAt = now,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            entity.analysisStatus = MediaAnalysisState.QUEUED
            entity.updatedAt = now
        } else if (existing.status == MediaAnalysisJobState.QUEUED) {
            entity.analysisStatus = MediaAnalysisState.QUEUED
            entity.updatedAt = now
        }
    }

    private fun checkAndRecordIdempotency(
        ownerId: UUID,
        operation: String,
        path: String,
        key: UUID,
        requestHash: String,
    ) {
        val existing = idempotencyRepository.findByOwnerIdAndOperationAndResourcePathAndIdempotencyKey(ownerId, operation, path, key)
        if (existing != null) {
            if (existing.requestHash != requestHash) throw IdempotencyKeyConflictException()
            return
        }
        idempotencyRepository.save(ApiIdempotencyRecordEntity(UUID.randomUUID(), ownerId, operation, path, key, requestHash, OffsetDateTime.now()))
    }

    private fun validate(item: CreateMediaUploadRequest) {
        if (item.contentType.value != "image/jpeg") throw MediaValidationException("contentType", "image/jpeg만 허용합니다.")
        if (item.frameOrigin != CreateMediaUploadRequest.FrameOrigin.POST_RECORDING_EXTRACTION) {
            throw MediaValidationException("frameOrigin", "MVP는 촬영 후 추출한 JPEG만 허용합니다.")
        }
    }

    private fun instruction(entity: MediaEntity): MediaUploadInstruction {
        val signed = storage.createUploadUrl(entity.storageKey)
        return MediaUploadInstruction(entity.id, entity.clientMediaId, signed.url, signed.expiresAt, MediaUploadInstruction.UploadStatus.PENDING)
    }

    private fun CreateMediaUploadRequest.fingerprint() = listOf(
        clientMediaId, zone.value, contentType.value, fileSize, width, height, sourceVideoId,
        sourceVideoOffsetMs, frameOrigin.value, captureSource.name, capturedAt.toInstant(),
    ).joinToString("|")

    private fun MediaEntity.matches(item: CreateMediaUploadRequest) =
        zone?.name == item.zone.name && contentType == item.contentType.value && declaredFileSize == item.fileSize &&
            width == item.width && height == item.height && sourceVideoId == item.sourceVideoId &&
            sourceVideoOffsetMs == item.sourceVideoOffsetMs && frameOrigin == item.frameOrigin.value &&
            captureSource.name == item.captureSource.name && capturedAt.toInstant() == item.capturedAt.toInstant()

    private fun MediaEntity.toApi() = Media(
        id = id,
        clientMediaId = clientMediaId,
        inspectionId = inspectionId,
        mediaType = MediaType.PHOTO,
        zone = (userCorrectedZone ?: aiZone ?: zone)?.let { Zone.valueOf(it.name) },
        aiZone = aiZone?.let { Zone.valueOf(it.name) },
        zoneConfidence = zoneConfidence,
        zoneUncertain = zoneUncertain,
        zoneModelVersion = zoneModelVersion,
        userCorrectedZone = userCorrectedZone?.let { Zone.valueOf(it.name) },
        correctedAt = correctedAt,
        contentType = Media.ContentType.imageSlashJpeg,
        fileSize = actualFileSize ?: declaredFileSize,
        width = width,
        height = height,
        sourceVideoId = sourceVideoId,
        sourceVideoOffsetMs = sourceVideoOffsetMs,
        frameOrigin = FrameOrigin.valueOf(frameOrigin),
        captureSource = CaptureSource.valueOf(captureSource.name),
        capturedAt = capturedAt,
        uploadStatus = MediaUploadStatus.valueOf(uploadStatus.name),
        analysisStatus = MediaAnalysisStatus.valueOf(analysisStatus.name),
        createdAt = createdAt,
    )

    private fun hash(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    companion object {
        const val MAX_JPEG_BYTES = 2_097_152
    }
}
