package com.tenantleaf.api.inspection

import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface InspectionRepository : JpaRepository<InspectionEntity, UUID> {
    fun findByIdAndOwnerId(id: UUID, ownerId: UUID): InspectionEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select inspection from InspectionEntity inspection where inspection.id = :id and inspection.ownerId = :ownerId")
    fun findForUpdateByIdAndOwnerId(@Param("id") id: UUID, @Param("ownerId") ownerId: UUID): InspectionEntity?

    fun findAllByPropertyIdAndOwnerId(propertyId: UUID, ownerId: UUID, pageable: Pageable): Page<InspectionEntity>

    fun existsByPropertyIdAndOwnerId(propertyId: UUID, ownerId: UUID): Boolean

    fun findAllByStatusAndMediaFinalizedAtIsNotNull(status: InspectionLifecycleStatus): List<InspectionEntity>
}
