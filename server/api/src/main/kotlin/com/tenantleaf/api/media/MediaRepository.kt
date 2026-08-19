package com.tenantleaf.api.media

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface MediaRepository : JpaRepository<MediaEntity, UUID> {
    fun findByIdAndOwnerIdAndDeletedAtIsNull(id: UUID, ownerId: UUID): MediaEntity?
    fun findByInspectionIdAndClientMediaId(inspectionId: UUID, clientMediaId: UUID): MediaEntity?
    fun findAllByInspectionIdAndDeletedAtIsNull(inspectionId: UUID): List<MediaEntity>
    fun findAllByInspectionIdAndOwnerIdAndDeletedAtIsNull(inspectionId: UUID, ownerId: UUID, pageable: Pageable): Page<MediaEntity>
}

interface ApiIdempotencyRecordRepository : JpaRepository<ApiIdempotencyRecordEntity, UUID> {
    fun findByOwnerIdAndOperationAndResourcePathAndIdempotencyKey(
        ownerId: UUID,
        operation: String,
        resourcePath: String,
        idempotencyKey: UUID,
    ): ApiIdempotencyRecordEntity?
}
