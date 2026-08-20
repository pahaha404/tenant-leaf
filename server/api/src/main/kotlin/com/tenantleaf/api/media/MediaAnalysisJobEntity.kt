package com.tenantleaf.api.media

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

enum class MediaAnalysisJobState { QUEUED, ANALYZING, COMPLETED, FAILED }

@Entity
@Table(name = "media_analysis_jobs")
class MediaAnalysisJobEntity(
    @Id val id: UUID,
    @Column(name = "media_id", nullable = false, unique = true) val mediaId: UUID,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) var status: MediaAnalysisJobState,
    @Column(name = "attempt_count", nullable = false) var attemptCount: Int,
    @Column(name = "available_at", nullable = false) var availableAt: OffsetDateTime,
    @Column(name = "started_at") var startedAt: OffsetDateTime? = null,
    @Column(name = "completed_at") var completedAt: OffsetDateTime? = null,
    @Column(name = "failure_code", length = 64) var failureCode: String? = null,
    @Column(name = "failure_message", length = 500) var failureMessage: String? = null,
    @Column(name = "model_version", length = 128) var modelVersion: String? = null,
    @Column(name = "created_at", nullable = false) val createdAt: OffsetDateTime,
    @Column(name = "updated_at", nullable = false) var updatedAt: OffsetDateTime,
)
