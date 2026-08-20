package com.seipseip.feature.property.domain.usecase

import com.seipseip.feature.property.domain.PropertyRepository
import com.seipseip.feature.property.domain.model.PropertyDraft
import com.seipseip.feature.property.domain.model.PropertyPatch
import java.util.UUID
import javax.inject.Inject

class ListPropertiesUseCase @Inject constructor(
    private val repository: PropertyRepository,
) {
    suspend operator fun invoke(page: Int = 0, size: Int = 20) = repository.list(page, size)
}

class GetPropertyUseCase @Inject constructor(
    private val repository: PropertyRepository,
) {
    suspend operator fun invoke(id: UUID) = repository.get(id)
}

class CreatePropertyUseCase @Inject constructor(
    private val repository: PropertyRepository,
) {
    suspend operator fun invoke(draft: PropertyDraft) = repository.create(draft)
}

class UpdatePropertyUseCase @Inject constructor(
    private val repository: PropertyRepository,
) {
    suspend operator fun invoke(id: UUID, patch: PropertyPatch) = repository.update(id, patch)
}

class DeletePropertyUseCase @Inject constructor(
    private val repository: PropertyRepository,
) {
    suspend operator fun invoke(id: UUID) = repository.delete(id)
}

