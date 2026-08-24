package com.seipseip.feature.inspection.data

import com.seipseip.feature.inspection.domain.model.Inspection
import com.seipseip.feature.inspection.domain.model.InspectionAnalysisStatus
import com.seipseip.feature.inspection.domain.model.InspectionPage
import com.seipseip.feature.inspection.domain.model.InspectionStatus
import com.seipseip.core.network.generated.model.Inspection as ApiInspection
import com.seipseip.core.network.generated.model.InspectionAnalysisStatus as ApiInspectionAnalysisStatus
import com.seipseip.core.network.generated.model.InspectionPage as ApiInspectionPage
import com.seipseip.core.network.generated.model.InspectionStatus as ApiInspectionStatus

internal fun ApiInspection.toDomain(): Inspection = Inspection(
    id = id,
    propertyId = propertyId,
    status = status.toDomain(),
    analysisStatus = analysisStatus.toDomain(),
    startedAt = startedAt,
    endedAt = endedAt,
    cancelledAt = cancelledAt,
    archivedAt = archivedAt,
    createdAt = createdAt,
)

internal fun ApiInspectionPage.toDomain(): InspectionPage = InspectionPage(
    page = page,
    size = propertySize,
    totalElements = totalElements,
    totalPages = totalPages,
    items = items.map(ApiInspection::toDomain),
)

private fun ApiInspectionStatus.toDomain(): InspectionStatus = when (this) {
    ApiInspectionStatus.IN_PROGRESS -> InspectionStatus.IN_PROGRESS
    ApiInspectionStatus.ENDED -> InspectionStatus.ENDED
    ApiInspectionStatus.CANCELLED -> InspectionStatus.CANCELLED
}

private fun ApiInspectionAnalysisStatus.toDomain(): InspectionAnalysisStatus = when (this) {
    ApiInspectionAnalysisStatus.NOT_STARTED -> InspectionAnalysisStatus.NOT_STARTED
    ApiInspectionAnalysisStatus.UPLOADING -> InspectionAnalysisStatus.UPLOADING
    ApiInspectionAnalysisStatus.QUEUED -> InspectionAnalysisStatus.QUEUED
    ApiInspectionAnalysisStatus.ANALYZING -> InspectionAnalysisStatus.ANALYZING
    ApiInspectionAnalysisStatus.PARTIAL_COMPLETED -> InspectionAnalysisStatus.PARTIAL_COMPLETED
    ApiInspectionAnalysisStatus.COMPLETED -> InspectionAnalysisStatus.COMPLETED
    ApiInspectionAnalysisStatus.FAILED -> InspectionAnalysisStatus.FAILED
}

