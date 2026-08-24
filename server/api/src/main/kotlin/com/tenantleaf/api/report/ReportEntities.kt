package com.tenantleaf.api.report

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

enum class ObservationState { ACTIVE, VIEWED, DISMISSED }
enum class ReportState { WAITING_FOR_ANALYSIS, GENERATING, COMPLETED, PARTIAL_COMPLETED, FAILED }

@Entity
@Table(name = "observation_thresholds")
class ObservationThresholdEntity(
    @EmbeddedId val id: ObservationThresholdId,
    @Column(name = "minimum_confidence", nullable = false) val minimumConfidence: Double,
    @Column(name = "configured_at", nullable = false) val configuredAt: OffsetDateTime,
)

@Embeddable
data class ObservationThresholdId(
    @Column(name = "model_version", length = 128) val modelVersion: String = "",
    @Column(name = "class_id") val classId: Int = 0,
) : Serializable

@Entity
@Table(name = "observations")
class ObservationEntity(
    @Id val id: UUID,
    @Column(name = "inspection_id", nullable = false) val inspectionId: UUID,
    @Column(name = "source_detection_id", nullable = false, unique = true) val sourceDetectionId: UUID,
    @Column(nullable = false, length = 48) val type: String,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) var status: ObservationState,
    @Column(name = "ai_zone", nullable = false, length = 32) val aiZone: String,
    @Column(name = "zone_confidence") val zoneConfidence: Double?,
    @Column(name = "zone_uncertain", nullable = false) val zoneUncertain: Boolean,
    @Column(name = "zone_model_version", length = 128) val zoneModelVersion: String?,
    @Column(name = "user_corrected_zone", length = 32) val userCorrectedZone: String?,
    @Column(name = "class_id", nullable = false) val classId: Int,
    @Column(name = "ai_label", nullable = false, length = 64) val aiLabel: String,
    @Column(nullable = false) val confidence: Double,
    @Column(name = "observation_min_confidence", nullable = false) val observationMinConfidence: Double,
    @Column(name = "model_version", nullable = false, length = 128) val modelVersion: String,
    @Column(name = "template_version", nullable = false, length = 64) val templateVersion: String,
    @Column(name = "created_at", nullable = false) val createdAt: OffsetDateTime,
    @Column(name = "updated_at", nullable = false) var updatedAt: OffsetDateTime,
)

@Embeddable
data class ObservationEvidenceId(
    @Column(name = "observation_id") val observationId: UUID = UUID(0, 0),
    @Column(name = "media_id") val mediaId: UUID = UUID(0, 0),
) : Serializable

@Entity
@Table(name = "observation_evidence")
class ObservationEvidenceEntity(
    @EmbeddedId val id: ObservationEvidenceId,
    @Column(name = "detection_id", nullable = false) val detectionId: UUID,
    @Column(name = "is_representative", nullable = false) val isRepresentative: Boolean,
    @Column(name = "coordinate_system", nullable = false, length = 24) val coordinateSystem: String,
    @Column(name = "image_width", nullable = false) val imageWidth: Int,
    @Column(name = "image_height", nullable = false) val imageHeight: Int,
    @Column(name = "bbox_left", nullable = false) val boxLeft: Double,
    @Column(name = "bbox_top", nullable = false) val boxTop: Double,
    @Column(name = "bbox_right", nullable = false) val boxRight: Double,
    @Column(name = "bbox_bottom", nullable = false) val boxBottom: Double,
    @Column(nullable = false) val confidence: Double,
    @Column(name = "created_at", nullable = false) val createdAt: OffsetDateTime,
)

@Entity
@Table(name = "reports")
class ReportEntity(
    @Id val id: UUID,
    @Column(name = "property_id", nullable = false) val propertyId: UUID,
    @Column(name = "inspection_id", nullable = false, unique = true) val inspectionId: UUID,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) var status: ReportState,
    @Column(name = "successful_media_count", nullable = false) var successfulMediaCount: Int,
    @Column(name = "failed_media_count", nullable = false) var failedMediaCount: Int,
    @Column(name = "observation_count", nullable = false) var observationCount: Int,
    @Column(name = "reference_score") var referenceScore: Int?,
    @Column(name = "score_policy_version", length = 64) var scorePolicyVersion: String?,
    @Column(name = "score_is_provisional", nullable = false) var scoreIsProvisional: Boolean,
    @Column(name = "failure_code", length = 64) var failureCode: String?,
    @Column(name = "template_version", nullable = false, length = 64) val templateVersion: String,
    @Column(name = "generated_at") var generatedAt: OffsetDateTime?,
    @Column(name = "created_at", nullable = false) val createdAt: OffsetDateTime,
    @Column(name = "updated_at", nullable = false) var updatedAt: OffsetDateTime,
)
