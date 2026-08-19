package com.tenantleaf.api.media

import com.tenantleaf.api.generated.api.MediaApi
import com.tenantleaf.api.generated.model.CreateMediaUploadBatchRequest
import com.tenantleaf.api.generated.model.CreateMediaUploadBatchResponse
import com.tenantleaf.api.generated.model.Media
import com.tenantleaf.api.generated.model.MediaPage
import com.tenantleaf.api.generated.model.MediaUploadInstruction
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class MediaController(private val service: MediaService) : MediaApi {
    override fun createMediaUploadRequests(
        inspectionId: UUID,
        idempotencyKey: UUID,
        createMediaUploadBatchRequest: CreateMediaUploadBatchRequest,
    ): ResponseEntity<CreateMediaUploadBatchResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(service.createUploadRequests(inspectionId, idempotencyKey, createMediaUploadBatchRequest))

    override fun completeMediaUpload(mediaId: UUID, idempotencyKey: UUID): ResponseEntity<Media> =
        ResponseEntity.ok(service.completeUpload(mediaId, idempotencyKey))

    override fun retryMediaUpload(mediaId: UUID, idempotencyKey: UUID): ResponseEntity<MediaUploadInstruction> =
        ResponseEntity.ok(service.retryUpload(mediaId, idempotencyKey))

    override fun getMedia(mediaId: UUID): ResponseEntity<Media> = ResponseEntity.ok(service.get(mediaId))

    override fun listInspectionMedia(inspectionId: UUID, page: Int, size: Int): ResponseEntity<MediaPage> =
        ResponseEntity.ok(service.list(inspectionId, page, size))
}
