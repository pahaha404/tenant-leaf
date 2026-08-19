package com.seipseip.feature.inspection.domain.model

import java.time.OffsetDateTime
import java.util.UUID

enum class InspectionStatus {
    IN_PROGRESS,
    ENDED,
    CANCELLED,
}

enum class InspectionAnalysisStatus {
    NOT_STARTED,
    UPLOADING,
    QUEUED,
    ANALYZING,
    PARTIAL_COMPLETED,
    COMPLETED,
    FAILED,
}

data class Inspection(
    val id: UUID,
    val propertyId: UUID,
    val status: InspectionStatus,
    val analysisStatus: InspectionAnalysisStatus,
    val startedAt: OffsetDateTime,
    val endedAt: OffsetDateTime?,
    val cancelledAt: OffsetDateTime?,
    val archivedAt: OffsetDateTime?,
    val createdAt: OffsetDateTime,
)

data class InspectionPage(
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val items: List<Inspection>,
)
