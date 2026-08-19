package com.seipseip.feature.inspection.data

import com.seipseip.core.common.AppResult
import com.seipseip.core.network.executeApiCall
import com.seipseip.core.network.generated.api.InspectionsApi
import com.seipseip.core.network.generated.model.Inspection
import com.seipseip.core.network.generated.model.InspectionPage
import com.seipseip.core.network.generated.model.UpdateInspectionStatusRequest
import com.seipseip.feature.inspection.domain.model.InspectionStatus
import com.squareup.moshi.Moshi
import java.util.UUID
import javax.inject.Inject

internal class InspectionRemoteDataSource @Inject constructor(
    private val inspectionsApi: InspectionsApi,
    private val moshi: Moshi,
) {
    suspend fun create(propertyId: UUID): AppResult<Inspection> =
        executeApiCall(moshi) { inspectionsApi.createInspection(propertyId) }

    suspend fun list(propertyId: UUID, page: Int, size: Int): AppResult<InspectionPage> =
        executeApiCall(moshi) { inspectionsApi.listInspections(propertyId, page, size) }

    suspend fun get(inspectionId: UUID): AppResult<Inspection> =
        executeApiCall(moshi) { inspectionsApi.getInspection(inspectionId) }

    suspend fun updateStatus(inspectionId: UUID, status: InspectionStatus): AppResult<Inspection> =
        executeApiCall(moshi) {
            inspectionsApi.updateInspectionStatus(
                inspectionId,
                UpdateInspectionStatusRequest(
                    status = when (status) {
                        InspectionStatus.ENDED -> UpdateInspectionStatusRequest.Status.ENDED
                        InspectionStatus.CANCELLED -> UpdateInspectionStatusRequest.Status.CANCELLED
                        InspectionStatus.IN_PROGRESS -> error("IN_PROGRESS 상태로 되돌릴 수 없습니다.")
                    },
                ),
            )
        }
}
