package com.seipseip.feature.property.data

import com.seipseip.core.common.AppResult
import com.seipseip.core.network.executeApiCall
import com.seipseip.core.network.executeEmptyApiCall
import com.seipseip.core.network.generated.api.PropertiesApi
import com.seipseip.core.network.generated.model.Property
import com.seipseip.core.network.generated.model.PropertyPage
import com.seipseip.feature.property.domain.model.PropertyDraft
import com.seipseip.feature.property.domain.model.PropertyPatch
import com.squareup.moshi.Moshi
import java.util.UUID
import javax.inject.Inject

internal class PropertyRemoteDataSource @Inject constructor(
    private val propertiesApi: PropertiesApi,
    private val propertyPatchApi: PropertyPatchApi,
    private val patchEncoder: PropertyPatchJsonEncoder,
    private val moshi: Moshi,
) {
    suspend fun list(page: Int, size: Int): AppResult<PropertyPage> =
        executeApiCall(moshi) { propertiesApi.listProperties(page, size) }

    suspend fun get(id: UUID): AppResult<Property> =
        executeApiCall(moshi) { propertiesApi.getProperty(id) }

    suspend fun create(draft: PropertyDraft): AppResult<Property> =
        executeApiCall(moshi) { propertiesApi.createProperty(draft.toApiRequest()) }

    suspend fun update(id: UUID, patch: PropertyPatch): AppResult<Property> =
        executeApiCall(moshi) { propertyPatchApi.updateProperty(id, patchEncoder.encode(patch)) }

    suspend fun delete(id: UUID): AppResult<Unit> =
        executeEmptyApiCall(moshi) { propertiesApi.deleteProperty(id) }
}
