package com.tenantleaf.api.media

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.OffsetDateTime
import java.util.UUID

enum class MediaZone { KITCHEN, LIVING_ROOM, BATHROOM, UNKNOWN }
enum class MediaCaptureSource { META_GLASS, ANDROID_CAMERA }
enum class MediaUploadState { PENDING, UPLOADING, UPLOADED, FAILED }
enum class MediaAnalysisState { NOT_REQUESTED, QUEUED, ANALYZING, COMPLETED, FAILED }

@Entity
@Table(
    name = "media",
    uniqueConstraints = [UniqueConstraint(name = "uq_media_inspection_client", columnNames = ["inspection_id", "client_media_id"])],
)
class MediaEntity(
    @Id val id: UUID,
    @Column(name = "inspection_id", nullable = false) val inspectionId: UUID,
    @Column(name = "owner_id", nullable = false) val ownerId: UUID,
    @Column(name = "client_media_id", nullable = false) val clientMediaId: UUID,
    @Enumerated(EnumType.STRING) @Column(length = 32) val zone: MediaZone? = null,
    @Enumerated(EnumType.STRING) @Column(name = "ai_zone", length = 32) var aiZone: MediaZone? = null,
    @Column(name = "zone_confidence") var zoneConfidence: Double? = null,
    @Column(name = "zone_uncertain") var zoneUncertain: Boolean? = null,
    @Column(name = "zone_model_version", length = 128) var zoneModelVersion: String? = null,
    @Enumerated(EnumType.STRING) @Column(name = "user_corrected_zone", length = 32) var userCorrectedZone: MediaZone? = null,
    @Column(name = "corrected_at") var correctedAt: OffsetDateTime? = null,
    @Column(name = "media_type", nullable = false, length = 16) val mediaType: String = "PHOTO",
    @Column(name = "content_type", nullable = false, length = 32) val contentType: String = "image/jpeg",
    @Column(name = "declared_file_size", nullable = false) val declaredFileSize: Long,
    @Column(name = "actual_file_size") var actualFileSize: Long? = null,
    @Column(nullable = false) val width: Int,
    @Column(nullable = false) val height: Int,
    @Column(name = "source_video_id", nullable = false) val sourceVideoId: UUID,
    @Column(name = "source_video_offset_ms", nullable = false) val sourceVideoOffsetMs: Long,
    @Column(name = "frame_origin", nullable = false, length = 48) val frameOrigin: String = "POST_RECORDING_EXTRACTION",
    @Enumerated(EnumType.STRING) @Column(name = "capture_source", nullable = false, length = 32) val captureSource: MediaCaptureSource,
    @Column(name = "captured_at", nullable = false) val capturedAt: OffsetDateTime,
    @Column(name = "storage_key", nullable = false, unique = true, length = 512) val storageKey: String,
    @Enumerated(EnumType.STRING) @Column(name = "upload_status", nullable = false, length = 24) var uploadStatus: MediaUploadState,
    @Enumerated(EnumType.STRING) @Column(name = "analysis_status", nullable = false, length = 24) var analysisStatus: MediaAnalysisState,
    @Column(name = "upload_attempt_count", nullable = false) var uploadAttemptCount: Int = 0,
    @Column(name = "uploaded_at") var uploadedAt: OffsetDateTime? = null,
    @Column(name = "deleted_at") var deletedAt: OffsetDateTime? = null,
    @Column(name = "created_at", nullable = false) val createdAt: OffsetDateTime,
    @Column(name = "updated_at", nullable = false) var updatedAt: OffsetDateTime,
)

@Entity
@Table(
    name = "api_idempotency_records",
    uniqueConstraints = [UniqueConstraint(name = "uq_api_idempotency", columnNames = ["owner_id", "operation", "resource_path", "idempotency_key"])],
)
class ApiIdempotencyRecordEntity(
    @Id val id: UUID,
    @Column(name = "owner_id", nullable = false) val ownerId: UUID,
    @Column(nullable = false, length = 64) val operation: String,
    @Column(name = "resource_path", nullable = false, length = 512) val resourcePath: String,
    @Column(name = "idempotency_key", nullable = false) val idempotencyKey: UUID,
    @Column(name = "request_hash", nullable = false, length = 64) val requestHash: String,
    @Column(name = "created_at", nullable = false) val createdAt: OffsetDateTime,
)
