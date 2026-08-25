package com.tenantleaf.api.report

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ObservationThresholdRepository : JpaRepository<ObservationThresholdEntity, ObservationThresholdId>

interface ObservationRepository : JpaRepository<ObservationEntity, UUID> {
    fun findByIdAndInspectionIdIn(id: UUID, inspectionIds: Collection<UUID>): ObservationEntity?
    fun findAllByInspectionId(inspectionId: UUID, pageable: Pageable): Page<ObservationEntity>
    fun findAllByInspectionIdAndStatusNot(inspectionId: UUID, status: ObservationState): List<ObservationEntity>
    fun existsBySourceDetectionId(sourceDetectionId: UUID): Boolean
}

interface ObservationEvidenceRepository : JpaRepository<ObservationEvidenceEntity, ObservationEvidenceId> {
    fun findAllByIdObservationIdIn(observationIds: Collection<UUID>): List<ObservationEvidenceEntity>
    fun findAllByIdMediaIdIn(mediaIds: Collection<UUID>): List<ObservationEvidenceEntity>
}

interface ReportRepository : JpaRepository<ReportEntity, UUID> {
    fun findByInspectionId(inspectionId: UUID): ReportEntity?
    fun findAllByPropertyId(propertyId: UUID, pageable: Pageable): Page<ReportEntity>
}
