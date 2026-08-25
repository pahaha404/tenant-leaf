package com.tenantleaf.api.inspection

import com.tenantleaf.api.generated.model.Inspection
import com.tenantleaf.api.generated.model.InspectionAnalysisStatus
import com.tenantleaf.api.generated.model.InspectionPage
import com.tenantleaf.api.generated.model.InspectionStatus
import com.tenantleaf.api.generated.model.UpdateInspectionStatusRequest
import com.tenantleaf.api.property.DemoUserContext
import com.tenantleaf.api.property.PropertyNotFoundException
import com.tenantleaf.api.property.PropertyRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.OffsetDateTime
import java.util.UUID

@Service
class InspectionService(
    private val inspectionRepository: InspectionRepository,
    private val propertyRepository: PropertyRepository,
    private val userContext: DemoUserContext,
    private val clock: Clock = Clock.systemUTC(),
) {
    @Transactional
    fun create(propertyId: UUID): Inspection {
        val ownerId = userContext.requireUserId()
        requireOwnedProperty(propertyId, ownerId)
        val now = OffsetDateTime.now(clock)
        return inspectionRepository.save(
            InspectionEntity(
                id = UUID.randomUUID(),
                propertyId = propertyId,
                ownerId = ownerId,
                status = InspectionLifecycleStatus.IN_PROGRESS,
                analysisStatus = InspectionAggregateStatus.NOT_STARTED,
                startedAt = now,
                createdAt = now,
            ),
        ).toModel()
    }

    @Transactional(readOnly = true)
    fun list(propertyId: UUID, page: Int, size: Int): InspectionPage {
        val ownerId = userContext.requireUserId()
        requireOwnedProperty(propertyId, ownerId)
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")))
        val result = inspectionRepository.findAllByPropertyIdAndOwnerId(propertyId, ownerId, pageable)
        return InspectionPage(
            page = result.number,
            propertySize = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            items = result.content.map { it.toModel() },
        )
    }

    @Transactional(readOnly = true)
    fun get(inspectionId: UUID): Inspection = ownedInspection(inspectionId).toModel()

    @Transactional
    fun updateStatus(inspectionId: UUID, requestedStatus: UpdateInspectionStatusRequest.Status): Inspection {
        val entity = inspectionRepository.findForUpdateByIdAndOwnerId(inspectionId, userContext.requireUserId())
            ?: throw InspectionNotFoundException()
        if (entity.status != InspectionLifecycleStatus.IN_PROGRESS) {
            throw InspectionStateTransitionException()
        }

        val now = OffsetDateTime.now(clock)
        when (requestedStatus) {
            UpdateInspectionStatusRequest.Status.ENDED -> {
                entity.status = InspectionLifecycleStatus.ENDED
                entity.endedAt = now
            }
            UpdateInspectionStatusRequest.Status.CANCELLED -> {
                entity.status = InspectionLifecycleStatus.CANCELLED
                entity.cancelledAt = now
            }
        }
        return inspectionRepository.save(entity).toModel()
    }

    private fun requireOwnedProperty(propertyId: UUID, ownerId: UUID) {
        propertyRepository.findByIdAndOwnerIdAndDeletedAtIsNull(propertyId, ownerId) ?: throw PropertyNotFoundException()
    }

    private fun ownedInspection(inspectionId: UUID): InspectionEntity =
        inspectionRepository.findByIdAndOwnerId(inspectionId, userContext.requireUserId())
            ?: throw InspectionNotFoundException()

    private fun InspectionEntity.toModel(): Inspection = Inspection(
        id = id,
        propertyId = propertyId,
        status = InspectionStatus.valueOf(status.name),
        analysisStatus = InspectionAnalysisStatus.valueOf(analysisStatus.name),
        startedAt = startedAt,
        createdAt = createdAt,
        endedAt = endedAt,
        cancelledAt = cancelledAt,
        archivedAt = archivedAt,
        mediaFinalizedAt = mediaFinalizedAt,
        expectedMediaCount = expectedMediaCount,
    )
}
