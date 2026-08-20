package com.seipseip.feature.inspection.domain

import com.seipseip.core.common.AppResult
import com.seipseip.feature.inspection.domain.model.Inspection
import com.seipseip.feature.inspection.domain.model.InspectionPage
import com.seipseip.feature.inspection.domain.model.InspectionStatus
import java.util.UUID

interface InspectionRepository {
    suspend fun create(propertyId: UUID): AppResult<Inspection>

    suspend fun list(propertyId: UUID, page: Int = 0, size: Int = 20): AppResult<InspectionPage>

    suspend fun get(inspectionId: UUID): AppResult<Inspection>

    suspend fun updateStatus(inspectionId: UUID, status: InspectionStatus): AppResult<Inspection>
}

