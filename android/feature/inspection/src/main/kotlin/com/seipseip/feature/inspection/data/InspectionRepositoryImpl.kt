package com.seipseip.feature.inspection.data

import com.seipseip.core.common.AppResult
import com.seipseip.core.common.map
import com.seipseip.feature.inspection.domain.InspectionRepository
import com.seipseip.feature.inspection.domain.model.Inspection
import com.seipseip.feature.inspection.domain.model.InspectionPage
import com.seipseip.feature.inspection.domain.model.InspectionStatus
import java.util.UUID
import javax.inject.Inject

internal class InspectionRepositoryImpl @Inject constructor(
    private val remoteDataSource: InspectionRemoteDataSource,
) : InspectionRepository {
    override suspend fun create(propertyId: UUID): AppResult<Inspection> =
        remoteDataSource.create(propertyId).map { it.toDomain() }

    override suspend fun list(propertyId: UUID, page: Int, size: Int): AppResult<InspectionPage> =
        remoteDataSource.list(propertyId, page, size).map { it.toDomain() }

    override suspend fun get(inspectionId: UUID): AppResult<Inspection> =
        remoteDataSource.get(inspectionId).map { it.toDomain() }

    override suspend fun updateStatus(inspectionId: UUID, status: InspectionStatus): AppResult<Inspection> =
        remoteDataSource.updateStatus(inspectionId, status).map { it.toDomain() }
}
