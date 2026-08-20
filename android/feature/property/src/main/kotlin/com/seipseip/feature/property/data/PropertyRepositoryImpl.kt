package com.seipseip.feature.property.data

import com.seipseip.core.common.AppResult
import com.seipseip.core.common.map
import com.seipseip.feature.property.domain.PropertyRepository
import com.seipseip.feature.property.domain.model.Property
import com.seipseip.feature.property.domain.model.PropertyDraft
import com.seipseip.feature.property.domain.model.PropertyPage
import com.seipseip.feature.property.domain.model.PropertyPatch
import java.util.UUID
import javax.inject.Inject

internal class PropertyRepositoryImpl @Inject constructor(
    private val remoteDataSource: PropertyRemoteDataSource,
) : PropertyRepository {
    override suspend fun list(page: Int, size: Int): AppResult<PropertyPage> =
        remoteDataSource.list(page, size).map { it.toDomain() }

    override suspend fun get(id: UUID): AppResult<Property> =
        remoteDataSource.get(id).map { it.toDomain() }

    override suspend fun create(draft: PropertyDraft): AppResult<Property> =
        remoteDataSource.create(draft).map { it.toDomain() }

    override suspend fun update(id: UUID, patch: PropertyPatch): AppResult<Property> =
        remoteDataSource.update(id, patch).map { it.toDomain() }

    override suspend fun delete(id: UUID): AppResult<Unit> = remoteDataSource.delete(id)
}

