package com.tenantleaf.api.inspection

import com.tenantleaf.api.generated.api.InspectionsApi
import com.tenantleaf.api.generated.model.Inspection
import com.tenantleaf.api.generated.model.InspectionPage
import com.tenantleaf.api.generated.model.UpdateInspectionStatusRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class InspectionController(
    private val service: InspectionService,
) : InspectionsApi {
    override fun createInspection(propertyId: UUID): ResponseEntity<Inspection> =
        ResponseEntity.status(HttpStatus.CREATED).body(service.create(propertyId))

    override fun listInspections(propertyId: UUID, page: Int, size: Int): ResponseEntity<InspectionPage> =
        ResponseEntity.ok(service.list(propertyId, page, size))

    override fun getInspection(inspectionId: UUID): ResponseEntity<Inspection> =
        ResponseEntity.ok(service.get(inspectionId))

    override fun updateInspectionStatus(
        inspectionId: UUID,
        updateInspectionStatusRequest: UpdateInspectionStatusRequest,
    ): ResponseEntity<Inspection> =
        ResponseEntity.ok(service.updateStatus(inspectionId, updateInspectionStatusRequest.status))
}
