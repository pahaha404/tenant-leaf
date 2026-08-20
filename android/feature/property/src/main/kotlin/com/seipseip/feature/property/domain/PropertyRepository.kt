package com.seipseip.feature.property.domain

import com.seipseip.core.common.AppResult
import com.seipseip.feature.property.domain.model.Property
import com.seipseip.feature.property.domain.model.PropertyDraft
import com.seipseip.feature.property.domain.model.PropertyPage
import com.seipseip.feature.property.domain.model.PropertyPatch
import java.util.UUID

interface PropertyRepository {
    suspend fun list(page: Int = 0, size: Int = 20): AppResult<PropertyPage>

    suspend fun get(id: UUID): AppResult<Property>

    suspend fun create(draft: PropertyDraft): AppResult<Property>

    suspend fun update(id: UUID, patch: PropertyPatch): AppResult<Property>

    suspend fun delete(id: UUID): AppResult<Unit>
}

