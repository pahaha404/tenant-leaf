package com.tenantleaf.api.property

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PropertyRepository : JpaRepository<PropertyEntity, UUID> {
    fun findByIdAndOwnerId(id: UUID, ownerId: UUID): PropertyEntity?

    fun findByIdAndOwnerIdAndDeletedAtIsNull(id: UUID, ownerId: UUID): PropertyEntity?

    fun findAllByOwnerId(ownerId: UUID, pageable: Pageable): Page<PropertyEntity>

    fun findAllByOwnerIdAndDeletedAtIsNull(ownerId: UUID, pageable: Pageable): Page<PropertyEntity>
}
