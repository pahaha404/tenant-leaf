package com.tenantleaf.api.inspection

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

enum class InspectionLifecycleStatus {
    IN_PROGRESS,
    ENDED,
    CANCELLED,
}

enum class InspectionAggregateStatus {
    NOT_STARTED,
    UPLOADING,
    QUEUED,
    ANALYZING,
    PARTIAL_COMPLETED,
    COMPLETED,
    FAILED,
}

@Entity
@Table(name = "inspections")
class InspectionEntity(
    @Id
    val id: UUID,

    @Column(name = "property_id", nullable = false)
    val propertyId: UUID,

    @Column(name = "owner_id", nullable = false)
    val ownerId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var status: InspectionLifecycleStatus,

    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_status", nullable = false, length = 32)
    var analysisStatus: InspectionAggregateStatus,

    @Column(name = "started_at", nullable = false)
    val startedAt: OffsetDateTime,

    @Column(name = "ended_at")
    var endedAt: OffsetDateTime? = null,

    @Column(name = "cancelled_at")
    var cancelledAt: OffsetDateTime? = null,

    @Column(name = "archived_at")
    var archivedAt: OffsetDateTime? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime,
)
