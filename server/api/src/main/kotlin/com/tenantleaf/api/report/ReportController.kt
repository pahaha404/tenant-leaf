package com.tenantleaf.api.report

import com.tenantleaf.api.generated.api.ObservationsApi
import com.tenantleaf.api.generated.api.ReportsApi
import com.tenantleaf.api.generated.model.Observation
import com.tenantleaf.api.generated.model.ObservationPage
import com.tenantleaf.api.generated.model.ReportDetail
import com.tenantleaf.api.generated.model.ReportPage
import com.tenantleaf.api.generated.model.UpdateObservationStatusRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class ObservationController(private val service: ReportService) : ObservationsApi {
    override fun listInspectionObservations(inspectionId: UUID, page: Int, size: Int): ResponseEntity<ObservationPage> =
        ResponseEntity.ok(service.listObservations(inspectionId, page, size))

    override fun getObservation(observationId: UUID): ResponseEntity<Observation> =
        ResponseEntity.ok(service.getObservation(observationId))

    override fun updateObservationStatus(
        observationId: UUID,
        updateObservationStatusRequest: UpdateObservationStatusRequest,
    ): ResponseEntity<Observation> = ResponseEntity.ok(service.updateObservationStatus(observationId, updateObservationStatusRequest))
}

@RestController
class ReportController(private val service: ReportService) : ReportsApi {
    override fun getInspectionReport(inspectionId: UUID): ResponseEntity<ReportDetail> =
        ResponseEntity.ok(service.getInspectionReport(inspectionId))

    override fun listPropertyReports(propertyId: UUID, page: Int, size: Int): ResponseEntity<ReportPage> =
        ResponseEntity.ok(service.listPropertyReports(propertyId, page, size))
}
