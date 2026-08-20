package com.seipseip.feature.property.data

import com.seipseip.core.network.generated.model.Property
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.PATCH
import retrofit2.http.Path
import java.util.UUID

/**
 * Generated nullable PATCH DTO cannot represent omitted and explicit-null fields separately.
 * This narrow adapter keeps the OpenAPI path/response model while preserving the contract's PATCH semantics.
 */
internal interface PropertyPatchApi {
    @PATCH("properties/{propertyId}")
    suspend fun updateProperty(
        @Path("propertyId") propertyId: UUID,
        @Body body: RequestBody,
    ): Response<Property>
}

