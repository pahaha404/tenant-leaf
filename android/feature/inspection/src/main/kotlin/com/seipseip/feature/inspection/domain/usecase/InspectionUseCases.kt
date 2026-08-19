package com.seipseip.feature.inspection.domain.usecase

import com.seipseip.feature.inspection.domain.InspectionRepository
import com.seipseip.feature.inspection.domain.model.InspectionStatus
import java.util.UUID
import javax.inject.Inject

class CreateInspectionUseCase @Inject constructor(private val repository: InspectionRepository) {
    suspend operator fun invoke(propertyId: UUID) = repository.create(propertyId)
}

class ListInspectionsUseCase @Inject constructor(private val repository: InspectionRepository) {
    suspend operator fun invoke(propertyId: UUID, page: Int = 0, size: Int = 20) =
        repository.list(propertyId, page, size)
}

class GetInspectionUseCase @Inject constructor(private val repository: InspectionRepository) {
    suspend operator fun invoke(inspectionId: UUID) = repository.get(inspectionId)
}

class UpdateInspectionStatusUseCase @Inject constructor(private val repository: InspectionRepository) {
    suspend operator fun invoke(inspectionId: UUID, status: InspectionStatus) =
        repository.updateStatus(inspectionId, status)
}
